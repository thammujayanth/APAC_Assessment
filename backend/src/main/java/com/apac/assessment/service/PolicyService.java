package com.apac.assessment.service;

import com.apac.assessment.repository.PolicyRepository;
import com.apac.assessment.repository.PolicyPremiumTotalProjection;
import com.apac.assessment.repository.PolicySpecifications;
import com.apac.assessment.dto.request.PolicyListQueryDto;
import com.apac.assessment.dto.response.ExpiringSoonDto;
import com.apac.assessment.dto.response.FlagPoliciesResultDto;
import com.apac.assessment.dto.response.PolicyDto;
import com.apac.assessment.dto.response.PolicyPageDto;
import com.apac.assessment.dto.response.PolicySummaryDto;
import com.apac.assessment.dto.response.PremiumTotalDto;
import com.apac.assessment.entity.Policy;
import com.apac.assessment.enums.LineOfBusiness;
import com.apac.assessment.enums.PolicyStatus;
import com.apac.assessment.exception.BadRequestException;
import com.apac.assessment.exception.PolicyNotFoundException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PolicyService {

	private static final int EXPIRING_SOON_WINDOW_DAYS = 30;
	private static final Set<String> REGIONS = Set.of(
			"Singapore", "Hong Kong", "Australia", "Japan",
			"Thailand", "Indonesia", "Malaysia", "Philippines");
	private static final Set<String> SORTABLE_FIELDS = Set.of(
			"policyNumber", "policyholderName", "lineOfBusiness", "status",
			"premiumAmount", "currency", "effectiveDate", "expiryDate",
			"region", "underwriter", "createdAt", "updatedAt");

	private final PolicyRepository policyRepository;

	@Transactional(readOnly = true)
	public PolicyPageDto listPolicies(PolicyListQueryDto query) {
		validateListQuery(query);
		PolicyStatus status = parseStatus(query.status());
		LineOfBusiness lineOfBusiness = parseLineOfBusiness(query.lineOfBusiness());
		Specification<Policy> specification = PolicySpecifications.matching(query, status, lineOfBusiness);
		Sort sort = parseSort(query.sort());

		Page<Policy> policies = policyRepository.findAll(
				specification,
				PageRequest.of(query.page(), query.size(), sort));

		return new PolicyPageDto(
				policies.getContent().stream().map(this::toDto).toList(),
				policies.getNumber(),
				policies.getSize(),
				policies.getTotalElements(),
				policies.getTotalPages());
	}

	@Transactional(readOnly = true)
	public PolicyDto getPolicy(UUID id) {
		Policy policy = policyRepository.findById(id)
				.orElseThrow(() -> new PolicyNotFoundException("Policy was not found."));
		return toDto(policy);
	}

	@Transactional
	public FlagPoliciesResultDto flagPolicies(List<UUID> policyIds) {
		if (policyIds == null || policyIds.isEmpty() || policyIds.size() > 1000) {
			throw new BadRequestException("policyIds must contain between 1 and 1000 IDs.");
		}
		if (policyIds.stream().anyMatch(id -> id == null)) {
			throw new BadRequestException("policyIds must not contain null IDs.");
		}
		if (new HashSet<>(policyIds).size() != policyIds.size()) {
			throw new BadRequestException("policyIds must not contain duplicate IDs.");
		}

		List<Policy> policies = policyRepository.findAllById(policyIds);
		if (policies.size() != policyIds.size()) {
			throw new PolicyNotFoundException("One or more policies were not found.");
		}

		List<Policy> changedPolicies = new ArrayList<>();
		int alreadyFlaggedCount = 0;
		for (Policy policy : policies) {
			if (policy.isFlaggedForReview()) {
				alreadyFlaggedCount++;
			} else {
				policy.setFlaggedForReview(true);
				changedPolicies.add(policy);
			}
		}
		if (!changedPolicies.isEmpty()) {
			policyRepository.saveAll(changedPolicies);
		}

		return new FlagPoliciesResultDto(
				policyIds.size(),
				changedPolicies.size(),
				alreadyFlaggedCount);
	}

	@Transactional(readOnly = true)
	public PolicySummaryDto getSummary() {
		Map<String, Long> countsByStatus = new LinkedHashMap<>();
		for (PolicyStatus status : PolicyStatus.values()) {
			countsByStatus.put(status.getValue(), policyRepository.countByStatus(status));
		}

		List<PremiumTotalDto> premiumTotals = policyRepository
				.summarizePremiumByLineOfBusinessAndCurrency()
				.stream()
				.map(this::toPremiumTotalDto)
				.toList();

		LocalDate asOfDate = LocalDate.now(ZoneOffset.UTC);
		long expiringSoonCount = policyRepository.countByStatusAndExpiryDateBetween(
				PolicyStatus.ACTIVE,
				asOfDate,
				asOfDate.plusDays(EXPIRING_SOON_WINDOW_DAYS));

		return new PolicySummaryDto(
				countsByStatus,
				premiumTotals,
				new ExpiringSoonDto(expiringSoonCount, EXPIRING_SOON_WINDOW_DAYS, asOfDate));
	}

	private void validateListQuery(PolicyListQueryDto query) {
		if (query == null) {
			throw new BadRequestException("List query is required.");
		}
		if (query.page() == null || query.page() < 0) {
			throw new BadRequestException("page must be zero or greater.");
		}
		if (query.size() == null || query.size() < 1 || query.size() > 100) {
			throw new BadRequestException("size must be between 1 and 100.");
		}
		if (query.effectiveDateFrom() != null
				&& query.effectiveDateTo() != null
				&& query.effectiveDateFrom().isAfter(query.effectiveDateTo())) {
			throw new BadRequestException("effectiveDateFrom must not be after effectiveDateTo.");
		}
		if (query.region() != null && !REGIONS.contains(query.region())) {
			throw new BadRequestException("region is not supported.");
		}
	}

	private PolicyStatus parseStatus(String value) {
		if (value == null) {
			return null;
		}
		try {
			return PolicyStatus.fromValue(value);
		} catch (IllegalArgumentException exception) {
			throw new BadRequestException("status is not supported.");
		}
	}

	private LineOfBusiness parseLineOfBusiness(String value) {
		if (value == null) {
			return null;
		}
		try {
			return LineOfBusiness.fromValue(value);
		} catch (IllegalArgumentException exception) {
			throw new BadRequestException("lineOfBusiness is not supported.");
		}
	}

	private Sort parseSort(String value) {
		String[] parts = value == null ? new String[0] : value.split(",", -1);
		if (parts.length != 2
				|| !SORTABLE_FIELDS.contains(parts[0])
				|| !("asc".equals(parts[1]) || "desc".equals(parts[1]))) {
			throw new BadRequestException("sort must use an allowed field and asc or desc direction.");
		}
		Sort.Direction direction = "asc".equals(parts[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
		return Sort.by(direction, parts[0]);
	}

	private PolicyDto toDto(Policy policy) {
		return new PolicyDto(
				policy.getId(),
				policy.getPolicyNumber(),
				policy.getPolicyholderName(),
				policy.getLineOfBusiness(),
				policy.getStatus(),
				policy.getPremiumAmount(),
				policy.getCurrency(),
				policy.getEffectiveDate(),
				policy.getExpiryDate(),
				policy.getRegion(),
				policy.getUnderwriter(),
				policy.isFlaggedForReview(),
				policy.getCreatedAt().atOffset(ZoneOffset.UTC),
				policy.getUpdatedAt().atOffset(ZoneOffset.UTC));
	}

	private PremiumTotalDto toPremiumTotalDto(PolicyPremiumTotalProjection projection) {
		return new PremiumTotalDto(
				projection.getLineOfBusiness(),
				projection.getCurrency(),
				projection.getTotalPremium());
	}
}

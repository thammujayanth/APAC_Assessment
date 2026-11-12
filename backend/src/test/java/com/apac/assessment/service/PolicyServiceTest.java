package com.apac.assessment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.apac.assessment.dto.request.PolicyListQueryDto;
import com.apac.assessment.entity.Policy;
import com.apac.assessment.enums.LineOfBusiness;
import com.apac.assessment.enums.PolicyStatus;
import com.apac.assessment.exception.BadRequestException;
import com.apac.assessment.exception.PolicyNotFoundException;
import com.apac.assessment.repository.PolicyPremiumTotalProjection;
import com.apac.assessment.repository.PolicyRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

	@Mock
	private PolicyRepository policyRepository;

	@InjectMocks
	private PolicyService policyService;

	@Test
	void listPoliciesUsesRequestedPaginationAndSort() {
		Page<Policy> page = new PageImpl<>(
				List.of(),
				PageRequest.of(2, 15, Sort.by(Sort.Direction.DESC, "premiumAmount")),
				31);
		when(policyRepository.findAll(anySpecification(), any(Pageable.class))).thenReturn(page);

		var result = policyService.listPolicies(new PolicyListQueryDto(
				2, 15, "premiumAmount,desc", null, null, null, null, null, null));

		assertEquals(2, result.page());
		assertEquals(15, result.size());
		assertEquals(31, result.totalElements());
		assertEquals(3, result.totalPages());

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(policyRepository).findAll(anySpecification(), pageable.capture());
		assertEquals(2, pageable.getValue().getPageNumber());
		assertEquals(15, pageable.getValue().getPageSize());
		assertEquals(Sort.Direction.DESC, pageable.getValue().getSort().getOrderFor("premiumAmount").getDirection());
	}

	@Test
	void listPoliciesRejectsReversedEffectiveDateRangeBeforeQuerying() {
		PolicyListQueryDto query = new PolicyListQueryDto(
				0,
				20,
				"policyNumber,asc",
				null,
				null,
				null,
				LocalDate.of(2026, 2, 1),
				LocalDate.of(2026, 1, 1),
				null);

		assertThrows(BadRequestException.class, () -> policyService.listPolicies(query));
		verifyNoInteractions(policyRepository);
	}

	@Test
	void flagPoliciesRejectsDuplicateIdsBeforeQuerying() {
		UUID id = UUID.randomUUID();

		assertThrows(BadRequestException.class, () -> policyService.flagPolicies(List.of(id, id)));
		verifyNoInteractions(policyRepository);
	}

	@Test
	void flagPoliciesDoesNotSaveWhenAnyIdIsMissing() {
		UUID requestedId = UUID.randomUUID();
		UUID missingId = UUID.randomUUID();
		Policy foundPolicy = mock(Policy.class);
		when(policyRepository.findAllById(List.of(requestedId, missingId))).thenReturn(List.of(foundPolicy));

		assertThrows(
				PolicyNotFoundException.class,
				() -> policyService.flagPolicies(List.of(requestedId, missingId)));
		verify(policyRepository, never()).saveAll(any());
	}

	@Test
	void flagPoliciesCountsUpdatedAndAlreadyFlaggedPolicies() {
		UUID newId = UUID.randomUUID();
		UUID alreadyFlaggedId = UUID.randomUUID();
		Policy unflagged = mock(Policy.class);
		Policy alreadyFlagged = mock(Policy.class);
		when(unflagged.isFlaggedForReview()).thenReturn(false);
		when(alreadyFlagged.isFlaggedForReview()).thenReturn(true);
		when(policyRepository.findAllById(List.of(newId, alreadyFlaggedId)))
				.thenReturn(List.of(unflagged, alreadyFlagged));
		when(policyRepository.saveAll(List.of(unflagged))).thenReturn(List.of(unflagged));

		var result = policyService.flagPolicies(List.of(newId, alreadyFlaggedId));

		assertEquals(2, result.requestedCount());
		assertEquals(1, result.updatedCount());
		assertEquals(1, result.alreadyFlaggedCount());
		verify(unflagged).setFlaggedForReview(true);
		verify(policyRepository).saveAll(List.of(unflagged));
	}

	@Test
	void summaryReturnsAllStatusesAndUsesThirtyDayUtcExpiryWindow() {
		PolicyPremiumTotalProjection premiumTotal = mock(PolicyPremiumTotalProjection.class);
		when(premiumTotal.getLineOfBusiness()).thenReturn(LineOfBusiness.PROPERTY);
		when(premiumTotal.getCurrency()).thenReturn("SGD");
		when(premiumTotal.getTotalPremium()).thenReturn(new BigDecimal("125000.00"));
		when(policyRepository.countByStatus(any(PolicyStatus.class))).thenReturn(2L);
		when(policyRepository.summarizePremiumByLineOfBusinessAndCurrency()).thenReturn(List.of(premiumTotal));
		when(policyRepository.countByStatusAndExpiryDateBetween(
				eq(PolicyStatus.ACTIVE), any(LocalDate.class), any(LocalDate.class))).thenReturn(3L);

		var result = policyService.getSummary();

		assertEquals(4, result.countsByStatus().size());
		assertEquals(2L, result.countsByStatus().get("Active"));
		assertEquals(new BigDecimal("125000.00"), result.totalPremiumByLineOfBusiness().getFirst().totalPremium());
		assertEquals(3L, result.expiringSoon().count());
		assertEquals(30, result.expiringSoon().windowDays());

		ArgumentCaptor<LocalDate> from = ArgumentCaptor.forClass(LocalDate.class);
		ArgumentCaptor<LocalDate> to = ArgumentCaptor.forClass(LocalDate.class);
		verify(policyRepository).countByStatusAndExpiryDateBetween(eq(PolicyStatus.ACTIVE), from.capture(), to.capture());
		assertEquals(from.getValue().plusDays(30), to.getValue());
	}

	@SuppressWarnings("unchecked")
	private Specification<Policy> anySpecification() {
		return any(Specification.class);
	}
}

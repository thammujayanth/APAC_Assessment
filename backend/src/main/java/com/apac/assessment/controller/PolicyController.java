package com.apac.assessment.controller;

import com.apac.assessment.service.PolicyService;
import com.apac.assessment.dto.request.PolicyListQueryDto;
import com.apac.assessment.dto.response.FlagPoliciesResultDto;
import com.apac.assessment.dto.response.PolicyDto;
import com.apac.assessment.dto.response.PolicyPageDto;
import com.apac.assessment.dto.response.PolicySummaryDto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
@Validated
public class PolicyController {

	private final PolicyService policyService;

	@GetMapping
	public PolicyPageDto listPolicies(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(defaultValue = "policyNumber,asc") String sort,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String lineOfBusiness,
			@RequestParam(required = false) String region,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDateFrom,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDateTo,
			@RequestParam(required = false) String search) {
		PolicyListQueryDto query = new PolicyListQueryDto(
				page,
				size,
				sort,
				status,
				lineOfBusiness,
				region,
				effectiveDateFrom,
				effectiveDateTo,
				search);
		return policyService.listPolicies(query);
	}

	@GetMapping("/{id}")
	public PolicyDto getPolicy(@PathVariable UUID id) {
		return policyService.getPolicy(id);
	}

	@PatchMapping("/flag")
	public FlagPoliciesResultDto flagPolicies(
			@RequestBody
			@Size(min = 1, max = 1000)
			List<@NotNull UUID> policyIds) {
		return policyService.flagPolicies(policyIds);
	}

	@GetMapping("/summary")
	public PolicySummaryDto getSummary() {
		return policyService.getSummary();
	}
}

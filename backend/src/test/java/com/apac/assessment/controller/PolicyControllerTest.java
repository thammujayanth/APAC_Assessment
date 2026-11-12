package com.apac.assessment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import com.apac.assessment.dto.request.PolicyListQueryDto;
import com.apac.assessment.dto.response.ExpiringSoonDto;
import com.apac.assessment.dto.response.FlagPoliciesResultDto;
import com.apac.assessment.dto.response.PolicyDto;
import com.apac.assessment.dto.response.PolicyPageDto;
import com.apac.assessment.dto.response.PolicySummaryDto;
import com.apac.assessment.dto.response.PremiumTotalDto;
import com.apac.assessment.enums.LineOfBusiness;
import com.apac.assessment.enums.PolicyStatus;
import com.apac.assessment.exception.PolicyNotFoundException;
import com.apac.assessment.service.PolicyService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PolicyController.class)
class PolicyControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PolicyService policyService;

	@Test
	void listPoliciesBindsQueryParametersAndReturnsPage() throws Exception {
		when(policyService.listPolicies(any())).thenReturn(new PolicyPageDto(List.of(), 1, 10, 0, 0));

		mockMvc.perform(get("/api/v1/policies")
						.param("page", "1")
						.param("size", "10")
						.param("sort", "premiumAmount,desc")
						.param("status", "Active")
						.param("lineOfBusiness", "A&H")
						.param("region", "Singapore")
						.param("effectiveDateFrom", "2026-01-01")
						.param("effectiveDateTo", "2026-12-31")
						.param("search", "pol-123"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page").value(1))
				.andExpect(jsonPath("$.size").value(10));

		verify(policyService).listPolicies(new PolicyListQueryDto(
				1,
				10,
				"premiumAmount,desc",
				"Active",
				"A&H",
				"Singapore",
				java.time.LocalDate.of(2026, 1, 1),
				java.time.LocalDate.of(2026, 12, 31),
				"pol-123"));
	}

	@Test
	void flagPoliciesAcceptsRawUuidArrayAndReturnsCounts() throws Exception {
		UUID id = UUID.randomUUID();
		when(policyService.flagPolicies(List.of(id))).thenReturn(new FlagPoliciesResultDto(1, 1, 0));

		mockMvc.perform(patch("/api/v1/policies/flag")
						.contentType(MediaType.APPLICATION_JSON)
						.content("[\"" + id + "\"]"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.requestedCount").value(1))
				.andExpect(jsonPath("$.updatedCount").value(1))
				.andExpect(jsonPath("$.alreadyFlaggedCount").value(0));
	}

	@Test
	void getPolicyReturnsPolicyFieldsAndAssessmentEnumValues() throws Exception {
		UUID id = UUID.randomUUID();
		PolicyDto policy = new PolicyDto(
				id,
				"POL-123456",
				"Mei Tan",
				LineOfBusiness.ACCIDENT_AND_HEALTH,
				PolicyStatus.ACTIVE,
				new BigDecimal("125000.00"),
				"SGD",
				LocalDate.of(2026, 1, 1),
				LocalDate.of(2026, 12, 31),
				"Singapore",
				"A. Tan",
				false,
				OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
				OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
		when(policyService.getPolicy(id)).thenReturn(policy);

		mockMvc.perform(get("/api/v1/policies/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.policyNumber").value("POL-123456"))
				.andExpect(jsonPath("$.lineOfBusiness").value("A&H"))
				.andExpect(jsonPath("$.status").value("Active"));
	}

	@Test
	void summaryRouteReturnsAggregatedResponse() throws Exception {
		when(policyService.getSummary()).thenReturn(new PolicySummaryDto(
				Map.of("Active", 2L, "Expired", 1L, "Pending", 0L, "Cancelled", 0L),
				List.of(new PremiumTotalDto(
						LineOfBusiness.PROPERTY,
						"SGD",
						new BigDecimal("125000.00"))),
				new ExpiringSoonDto(1, 30, LocalDate.of(2026, 11, 12))));

		mockMvc.perform(get("/api/v1/policies/summary"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.countsByStatus.Active").value(2))
				.andExpect(jsonPath("$.totalPremiumByLineOfBusiness[0].currency").value("SGD"))
				.andExpect(jsonPath("$.expiringSoon.windowDays").value(30));
	}

	@Test
	void getPolicyReturnsProblemDetailsWhenPolicyIsMissing() throws Exception {
		UUID id = UUID.randomUUID();
		when(policyService.getPolicy(id)).thenThrow(new PolicyNotFoundException("Policy was not found."));

		mockMvc.perform(get("/api/v1/policies/{id}", id))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.detail").value("Policy was not found."));
	}

	@Test
	void listPoliciesRejectsPageSizeAboveContractMaximum() throws Exception {
		mockMvc.perform(get("/api/v1/policies").param("size", "101"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("Bad Request"));
	}
}

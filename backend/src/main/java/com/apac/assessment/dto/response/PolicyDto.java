package com.apac.assessment.dto.response;

import com.apac.assessment.enums.LineOfBusiness;
import com.apac.assessment.enums.PolicyStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PolicyDto(
		UUID id,
		String policyNumber,
		String policyholderName,
		LineOfBusiness lineOfBusiness,
		PolicyStatus status,
		BigDecimal premiumAmount,
		String currency,
		LocalDate effectiveDate,
		LocalDate expiryDate,
		String region,
		String underwriter,
		boolean flaggedForReview,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {
}

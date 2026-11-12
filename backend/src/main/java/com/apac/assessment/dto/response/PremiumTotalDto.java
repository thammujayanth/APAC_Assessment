package com.apac.assessment.dto.response;

import com.apac.assessment.enums.LineOfBusiness;
import java.math.BigDecimal;

public record PremiumTotalDto(
		LineOfBusiness lineOfBusiness,
		String currency,
		BigDecimal totalPremium) {
}

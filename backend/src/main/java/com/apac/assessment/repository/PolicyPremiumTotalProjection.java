package com.apac.assessment.repository;

import com.apac.assessment.enums.LineOfBusiness;
import java.math.BigDecimal;

public interface PolicyPremiumTotalProjection {

	LineOfBusiness getLineOfBusiness();

	String getCurrency();

	BigDecimal getTotalPremium();
}

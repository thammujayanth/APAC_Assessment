package com.apac.assessment.dto.response;

import java.util.List;
import java.util.Map;

public record PolicySummaryDto(
		Map<String, Long> countsByStatus,
		List<PremiumTotalDto> totalPremiumByLineOfBusiness,
		ExpiringSoonDto expiringSoon) {
}

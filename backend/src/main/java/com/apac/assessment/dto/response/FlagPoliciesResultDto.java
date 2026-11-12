package com.apac.assessment.dto.response;

public record FlagPoliciesResultDto(
		int requestedCount,
		int updatedCount,
		int alreadyFlaggedCount) {
}

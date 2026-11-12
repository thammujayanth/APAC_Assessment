package com.apac.assessment.dto.response;

import java.time.LocalDate;

public record ExpiringSoonDto(
		long count,
		int windowDays,
		LocalDate asOfDate) {
}

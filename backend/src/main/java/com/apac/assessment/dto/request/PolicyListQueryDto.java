package com.apac.assessment.dto.request;

import java.time.LocalDate;

public record PolicyListQueryDto(
		Integer page,
		Integer size,
		String sort,
		String status,
		String lineOfBusiness,
		String region,
		LocalDate effectiveDateFrom,
		LocalDate effectiveDateTo,
		String search) {
}

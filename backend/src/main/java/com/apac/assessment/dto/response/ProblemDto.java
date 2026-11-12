package com.apac.assessment.dto.response;

import java.util.List;

public record ProblemDto(
		String type,
		String title,
		int status,
		String detail,
		String instance,
		List<ProblemFieldErrorDto> errors) {
}

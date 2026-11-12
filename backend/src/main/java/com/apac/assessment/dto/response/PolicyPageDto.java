package com.apac.assessment.dto.response;

import java.util.List;

public record PolicyPageDto(
		List<PolicyDto> items,
		int page,
		int size,
		long totalElements,
		int totalPages) {
}

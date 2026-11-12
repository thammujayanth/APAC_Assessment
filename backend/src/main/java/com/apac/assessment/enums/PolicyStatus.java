package com.apac.assessment.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PolicyStatus {
	ACTIVE("Active"),
	EXPIRED("Expired"),
	PENDING("Pending"),
	CANCELLED("Cancelled");

	private final String value;

	PolicyStatus(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@JsonCreator
	public static PolicyStatus fromValue(String value) {
		for (PolicyStatus status : values()) {
			if (status.value.equals(value)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Unknown policy status: " + value);
	}
}

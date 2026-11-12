package com.apac.assessment.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum LineOfBusiness {
	PROPERTY("Property"),
	CASUALTY("Casualty"),
	ACCIDENT_AND_HEALTH("A&H"),
	MARINE("Marine");

	private final String value;

	LineOfBusiness(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@JsonCreator
	public static LineOfBusiness fromValue(String value) {
		for (LineOfBusiness lineOfBusiness : values()) {
			if (lineOfBusiness.value.equals(value)) {
				return lineOfBusiness;
			}
		}
		throw new IllegalArgumentException("Unknown line of business: " + value);
	}
}

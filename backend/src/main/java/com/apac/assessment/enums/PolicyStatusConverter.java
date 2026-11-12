package com.apac.assessment.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PolicyStatusConverter implements AttributeConverter<PolicyStatus, String> {

	@Override
	public String convertToDatabaseColumn(PolicyStatus attribute) {
		return attribute == null ? null : attribute.getValue();
	}

	@Override
	public PolicyStatus convertToEntityAttribute(String databaseValue) {
		return databaseValue == null ? null : PolicyStatus.fromValue(databaseValue);
	}
}

package com.apac.assessment.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class LineOfBusinessConverter implements AttributeConverter<LineOfBusiness, String> {

	@Override
	public String convertToDatabaseColumn(LineOfBusiness attribute) {
		return attribute == null ? null : attribute.getValue();
	}

	@Override
	public LineOfBusiness convertToEntityAttribute(String databaseValue) {
		return databaseValue == null ? null : LineOfBusiness.fromValue(databaseValue);
	}
}

package com.screenleads.backend.app.domain.model;


import java.time.Duration;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;


@Converter(autoApply = true)
public class DurationToLongConverter implements AttributeConverter<Duration, Long> {
@Override
public Long convertToDatabaseColumn(Duration attribute) {
return attribute == null ? null : attribute.getSeconds();
}
@Override
public Duration convertToEntityAttribute(Long dbData) {
return dbData == null ? null : Duration.ofSeconds(dbData);
}
}
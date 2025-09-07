package com.example.streammatemoviesvc.app.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) return "{}";
        return "{" + String.join(",", attribute) + "}";
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.equals("{}")) return List.of();
        String cleaned = dbData.replaceAll("[{}]", "");
        return Arrays.stream(cleaned.split(",")).collect(Collectors.toList());
    }
}

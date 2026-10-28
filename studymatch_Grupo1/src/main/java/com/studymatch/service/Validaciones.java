package com.studymatch.service;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.util.Map;
import org.springframework.web.server.ResponseStatusException;

/** Reúne comprobaciones sencillas de los datos recibidos. */
public final class Validaciones {

    private Validaciones() {}

    public static String text(Map<String, ?> data, String key, int max) {
        Object value = data.get(key);
        return required(value == null ? null : value.toString(), key, max);
    }

    public static String required(String value, String field, int max) {
        if (
            value == null || value.isBlank() || value.trim().length() > max
        ) throw new ResponseStatusException(BAD_REQUEST, "Revisa el campo " + field + ".");
        return value.trim();
    }

    public static long number(Map<String, ?> data, String key) {
        try {
            long value = Long.parseLong(String.valueOf(data.get(key)));
            if (value > 0) return value;
        } catch (NumberFormatException ignored) {}
        throw new ResponseStatusException(BAD_REQUEST, "Revisa el campo " + key + ".");
    }

    public static long id(Map<String, Object> row, String key) {
        return ((Number) row.get(key)).longValue();
    }
}

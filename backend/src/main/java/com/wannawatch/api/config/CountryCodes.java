package com.wannawatch.api.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

public final class CountryCodes {

    private static final Set<String> ISO_CODES = Set.copyOf(Arrays.asList(Locale.getISOCountries()));

    private CountryCodes() {}

    public static String normalize(String value) {
        String code = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!ISO_CODES.contains(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use a valid ISO 3166-1 country code.");
        }
        return code;
    }

    public static boolean contains(String value) {
        return value != null && ISO_CODES.contains(value.trim().toUpperCase(Locale.ROOT));
    }
}

package com.wannawatch.api.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/countries")
public class CountryController {

    @GetMapping
    public List<CountryOptionDto> list() {
        return Arrays.stream(Locale.getISOCountries())
                .map(code -> new CountryOptionDto(code,
                        new Locale.Builder().setRegion(code).build().getDisplayCountry(Locale.ENGLISH)))
                .sorted(Comparator.comparing(CountryOptionDto::name))
                .toList();
    }

    public record CountryOptionDto(String code, String name) {}
}

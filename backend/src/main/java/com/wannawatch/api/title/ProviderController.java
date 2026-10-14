package com.wannawatch.api.title;

import org.springframework.data.domain.Sort;
import com.wannawatch.api.config.CountryCodes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/providers")
public class ProviderController {

    private final ProviderRepository providers;

    public ProviderController(ProviderRepository providers) {
        this.providers = providers;
    }

    @GetMapping
    public List<ProviderOptionDto> list(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "${app.default-country:US}") String country) {
        String market = CountryCodes.normalize(country);
        return providers.findAllForCountry(market).stream()
                .map(provider -> new ProviderOptionDto(provider.getName(), provider.getLogoUrl()))
                .toList();
    }

    public record ProviderOptionDto(String name, String logoUrl) {}
}

package com.wannawatch.api.title;

import com.wannawatch.api.config.CountryCodes;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class TitleService {

    private static final int MAX_PAGE_SIZE = 50;
    private final TitleRepository titles;
    private final String country;

    public TitleService(TitleRepository titles,
                        @org.springframework.beans.factory.annotation.Value("${app.default-country:US}") String country) {
        this.titles = titles;
        this.country = CountryCodes.normalize(country);
    }

    /** Feed ordered by popularity and scoped to a selected market. */
    public TitlePageDto feed(String query, Title.Type type, String genre, String provider,
                             String country, int page, int size) {
        String market = CountryCodes.normalize(country);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        var pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "popularity"));
        var result = titles.search(blankToNull(query), type, blankToNull(genre),
                blankToNull(provider), market, pageable);
        return new TitlePageDto(result.map(title -> TitleDto.from(title, market)).getContent(),
                safePage, safeSize, result.getTotalElements(), result.getTotalPages());
    }

    public Optional<TitleDto> get(long id, String country) {
        String market = CountryCodes.normalize(country);
        return titles.findById(id).map(title -> TitleDto.from(title, market));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

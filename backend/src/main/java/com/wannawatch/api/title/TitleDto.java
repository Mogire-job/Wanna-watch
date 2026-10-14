package com.wannawatch.api.title;

import java.math.BigDecimal;
import java.util.List;

public record TitleDto(
        Long id,
        Title.Type type,
        String name,
        Integer releaseYear,
        String synopsis,
        String posterUrl,
        BigDecimal rating,
        List<String> genres,
        String trailerKey,
        List<TitleAvailabilityDto> availability) {

    static TitleDto from(Title t, String country) {
        String trailer = t.getVideos().stream()
                .filter(v -> "OFFICIAL_TRAILER".equals(v.getKind()))
                .map(Video::getYoutubeKey)
                .findFirst()
                .orElse(null);
        List<String> genres = t.getGenres().stream().map(Genre::getName).sorted().toList();
        List<TitleAvailabilityDto> availability = t.getAvailability().stream()
                .filter(value -> value.getCountry().equalsIgnoreCase(country))
                .map(TitleAvailabilityDto::from)
                .sorted(java.util.Comparator.comparing(TitleAvailabilityDto::provider))
                .toList();
        return new TitleDto(t.getId(), t.getType(), t.getName(), t.getReleaseYear(),
                t.getSynopsis(), t.getPosterUrl(), t.getRating(), genres, trailer, availability);
    }
}

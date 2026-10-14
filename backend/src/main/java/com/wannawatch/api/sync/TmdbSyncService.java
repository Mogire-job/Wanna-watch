package com.wannawatch.api.sync;

import com.wannawatch.api.config.CountryCodes;
import com.wannawatch.api.title.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@Service
public class TmdbSyncService {

    private static final String POSTER_BASE = "https://image.tmdb.org/t/p/w500";
    private static final String LOGO_BASE = "https://image.tmdb.org/t/p/w92";
    private static final Set<String> OFFER_TYPES = Set.of("flatrate", "free", "ads", "rent", "buy");

    private final TmdbClient client;
    private final TitleRepository titles;
    private final GenreRepository genres;
    private final ProviderRepository providers;
    private final VideoRepository videos;
    private final AvailabilityRepository availability;
    private final String syncToken;
    private final int pages;

    private static final int PROVIDERS_PER_QUERY = 20;

    public TmdbSyncService(TmdbClient client,
                           TitleRepository titles,
                           GenreRepository genres,
                           ProviderRepository providers,
                           VideoRepository videos,
                           AvailabilityRepository availability,
                           @Value("${app.sync-token:}") String syncToken,
                           @Value("${app.tmdb.sync-pages:1}") int pages) {
        this.client = client;
        this.titles = titles;
        this.genres = genres;
        this.providers = providers;
        this.videos = videos;
        this.availability = availability;
        this.syncToken = syncToken;
        this.pages = Math.min(Math.max(pages, 1), 10);
    }

    public boolean isSyncEnabled() {
        return syncToken != null && !syncToken.isBlank();
    }

    public boolean acceptsToken(String suppliedToken) {
        if (!isSyncEnabled() || suppliedToken == null) return false;
        return java.security.MessageDigest.isEqual(
                syncToken.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                suppliedToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Transactional
    public TmdbSyncResult sync(String country) {
        if (!client.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Catalogue sync is not configured. Set TMDB_API_READ_ACCESS_TOKEN.");
        }
        String market = CountryCodes.normalize(country);

        Map<Long, String> movieGenres = genreMap("movie");
        Map<Long, String> seriesGenres = genreMap("tv");
        int movieCount = 0;
        int seriesCount = 0;
        int trailers = 0;
        int offers = 0;

        for (String mediaType : List.of("movie", "tv")) {
            Map<Long, String> names = mediaType.equals("movie") ? movieGenres : seriesGenres;
            Map<?, ?> providerResponse = client.get("/watch/providers/" + mediaType,
                    Map.of("language", "en-US", "watch_region", market));
            List<Long> providerIds = list(providerResponse.get("results")).stream()
                    .map(TmdbSyncService::map)
                    .map(record -> number(record.get("provider_id")))
                    .filter(id -> id > 0)
                    .distinct()
                    .toList();
            if (providerIds.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "TMDB has no streaming-provider catalogue for " + market + ".");
            }

            Map<String, Map<?, ?>> discovered = new LinkedHashMap<>();
            for (int start = 0; start < providerIds.size(); start += PROVIDERS_PER_QUERY) {
                String providerFilter = providerIds.subList(
                        start, Math.min(start + PROVIDERS_PER_QUERY, providerIds.size()))
                        .stream().map(String::valueOf).collect(java.util.stream.Collectors.joining("|"));
                for (int page = 1; page <= pages; page++) {
                    Map<?, ?> response = client.get("/discover/" + mediaType,
                            Map.of("language", "en-US",
                                    "page", page,
                                    "sort_by", "popularity.desc",
                                    "watch_region", market,
                                    "with_watch_providers", providerFilter,
                                    "with_watch_monetization_types", "flatrate|free|ads|rent|buy"));
                    for (Object item : list(response.get("results"))) {
                        Map<?, ?> record = map(item);
                        long id = number(record.get("id"));
                        if (id > 0) discovered.putIfAbsent(mediaType + ":" + id, record);
                    }
                }
            }

            for (Map.Entry<String, Map<?, ?>> entry : discovered.entrySet()) {
                long externalNumericId = number(entry.getValue().get("id"));
                Map<?, ?> videoResults = client.get("/" + mediaType + "/" + externalNumericId + "/videos",
                        Map.of("language", "en-US"));
                Map<?, ?> availabilityResults = client.get("/" + mediaType + "/" + externalNumericId + "/watch/providers",
                        Map.of());
                SyncCounts counts = upsert(entry.getValue(), mediaType, names,
                        videoResults, availabilityResults, market);
                trailers += counts.trailers();
                offers += counts.offers();
                if (mediaType.equals("movie")) movieCount++; else seriesCount++;
            }
        }
        return new TmdbSyncResult(movieCount, seriesCount, trailers, offers);
    }

    private Map<Long, String> genreMap(String mediaType) {
        Map<?, ?> response = client.get("/genre/" + mediaType + "/list", Map.of("language", "en"));
        Map<Long, String> result = new HashMap<>();
        for (Object value : list(response.get("genres"))) {
            Map<?, ?> genre = map(value);
            result.put(number(genre.get("id")), text(genre.get("name")));
        }
        return result;
    }

    private SyncCounts upsert(Map<?, ?> source,
                              String mediaType,
                              Map<Long, String> genreNames,
                              Map<?, ?> videoResponse,
                              Map<?, ?> providerResponse,
                              String country) {
        String externalId = mediaType + ":" + number(source.get("id"));
        Title.Type type = mediaType.equals("movie") ? Title.Type.MOVIE : Title.Type.SERIES;
        String name = text(source.get(mediaType.equals("movie") ? "title" : "name"));
        if (name.isBlank()) return new SyncCounts(0, 0);

        Title values = new Title();
        values.setImportedValues(externalId, type, name, releaseYear(source, mediaType),
                nullableText(source.get("overview")), poster(source.get("poster_path")),
                decimal(source.get("vote_average")), decimal(source.get("popularity")));
        Title title = titles.findByExternalId(externalId).map(existing -> {
            existing.updateFrom(values);
            return existing;
        }).orElseGet(() -> values);
        title = titles.save(title);

        Set<Genre> importedGenres = new LinkedHashSet<>();
        for (Object genreId : list(source.get("genre_ids"))) {
            String genreName = genreNames.get(number(genreId));
            if (genreName != null && !genreName.isBlank()) {
                importedGenres.add(genres.findByNameIgnoreCase(genreName)
                        .orElseGet(() -> genres.save(new Genre(genreName))));
            }
        }
        title.replaceGenres(importedGenres);
        title = titles.save(title);

        int trailerCount = replaceTrailers(title, videoResponse);
        int offerCount = replaceAvailability(title, providerResponse, country);
        return new SyncCounts(trailerCount, offerCount);
    }

    private int replaceTrailers(Title title, Map<?, ?> response) {
        videos.deleteForTitle(title.getId());
        List<Video> imported = new ArrayList<>();
        for (Object item : list(response.get("results"))) {
            Map<?, ?> video = map(item);
            if ("YouTube".equalsIgnoreCase(text(video.get("site")))
                    && "Trailer".equalsIgnoreCase(text(video.get("type")))
                    && Boolean.TRUE.equals(video.get("official"))
                    && !text(video.get("key")).isBlank()) {
                imported.add(new Video(title, text(video.get("key")), nullableText(video.get("iso_639_1"))));
            }
        }
        videos.saveAll(imported);
        return imported.size();
    }

    private int replaceAvailability(Title title, Map<?, ?> response, String country) {
        availability.deleteForTitleAndCountry(title.getId(), country);
        Map<?, ?> regions = map(response.get("results"));
        Map<?, ?> region = map(regions.get(country));
        int count = 0;
        Set<String> savedOffers = new HashSet<>();
        for (String offer : OFFER_TYPES) {
            for (Object item : list(region.get(offer))) {
                Map<?, ?> record = map(item);
                String name = text(record.get("provider_name"));
                if (name.isBlank()) continue;
                String logo = nullableText(record.get("logo_path"));
                Provider provider = providers.findByNameIgnoreCase(name).orElseGet(() ->
                        providers.save(new Provider(name, logo == null ? null : LOGO_BASE + logo)));
                if (logo != null) provider.updateLogo(LOGO_BASE + logo);
                provider = providers.save(provider);
                String offerType = switch (offer) {
                    case "flatrate" -> "STREAM";
                    case "rent" -> "RENT";
                    case "buy" -> "BUY";
                    default -> "FREE";
                };
                if (!savedOffers.add(name.toLowerCase(Locale.ROOT) + ":" + offerType)) continue;
                availability.save(new Availability(title, provider, country, offerType,
                        nullableText(region.get("link"))));
                count++;
            }
        }
        return count;
    }

    private static Integer releaseYear(Map<?, ?> source, String mediaType) {
        String date = text(source.get(mediaType.equals("movie") ? "release_date" : "first_air_date"));
        if (date.length() < 4) return null;
        try {
            return Integer.valueOf(date.substring(0, 4));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String poster(Object path) {
        String value = nullableText(path);
        return value == null ? null : POSTER_BASE + value;
    }

    private static BigDecimal decimal(Object value) {
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        return null;
    }

    private static long number(Object value) {
        if (value instanceof Number number) return number.longValue();
        try {
            return Long.parseLong(text(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String nullableText(Object value) {
        String result = text(value);
        return result.isBlank() ? null : result;
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private static List<?> list(Object value) {
        return value instanceof List<?> items ? items : List.of();
    }

    private static Map<?, ?> map(Object value) {
        return value instanceof Map<?, ?> result ? result : Map.of();
    }

    private record SyncCounts(int trailers, int offers) {}

}

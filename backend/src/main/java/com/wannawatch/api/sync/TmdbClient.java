package com.wannawatch.api.sync;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
public class TmdbClient {

    private final RestClient restClient;
    private final String token;

    public TmdbClient(RestClient.Builder builder,
                      @Value("${app.tmdb.api-read-access-token:}") String token) {
        this.restClient = builder
                .baseUrl("https://api.themoviedb.org/3")
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
        this.token = token;
    }

    public boolean isConfigured() {
        return token != null && !token.isBlank();
    }

    public Map<?, ?> get(String path, Map<String, ?> query) {
        if (!isConfigured()) {
            throw new IllegalStateException("Set TMDB_API_READ_ACCESS_TOKEN before syncing the catalogue.");
        }
        var uri = UriComponentsBuilder.fromPath(path);
        query.forEach(uri::queryParam);
        Map<?, ?> response = restClient.get()
                .uri(uri.build().encode().toUriString())
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(Map.class);
        if (response == null) {
            throw new IllegalStateException("TMDB returned an empty response for " + path);
        }
        return response;
    }
}

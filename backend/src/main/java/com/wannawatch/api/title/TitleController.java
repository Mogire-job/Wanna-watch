package com.wannawatch.api.title;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/titles")
public class TitleController {

    private final TitleService service;

    public TitleController(TitleService service) {
        this.service = service;
    }

    @GetMapping
    public TitlePageDto feed(@RequestParam(required = false) String q,
                             @RequestParam(required = false) Title.Type type,
                             @RequestParam(required = false) String genre,
                             @RequestParam(required = false) String provider,
                             @RequestParam(defaultValue = "${app.default-country:US}") String country,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size) {
        return service.feed(q, type, genre, provider, country, page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TitleDto> get(
            @PathVariable long id,
            @RequestParam(defaultValue = "${app.default-country:US}") String country) {
        return service.get(id, country).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}

package com.wannawatch.api.title;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/titles")
public class TitleController {

    private final TitleService service;

    public TitleController(TitleService service) {
        this.service = service;
    }

    @GetMapping
    public List<TitleDto> feed(@RequestParam(required = false) String q,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return service.feed(q, page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TitleDto> get(@PathVariable long id) {
        return service.get(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}

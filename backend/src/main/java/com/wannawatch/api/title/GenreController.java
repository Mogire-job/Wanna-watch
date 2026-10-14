package com.wannawatch.api.title;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/genres")
public class GenreController {

    private final GenreRepository genres;

    public GenreController(GenreRepository genres) {
        this.genres = genres;
    }

    @GetMapping
    public List<String> list() {
        return genres.findAllNames();
    }
}

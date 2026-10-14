package com.wannawatch.api.title;

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

    public TitleService(TitleRepository titles) {
        this.titles = titles;
    }

    /** Feed ordered by popularity, optionally filtered by a name search. */
    public List<TitleDto> feed(String query, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "popularity"));
        var result = (query == null || query.isBlank())
                ? titles.findAll(pageable)
                : titles.findByNameContainingIgnoreCase(query.trim(), pageable);
        return result.map(TitleDto::from).getContent();
    }

    public Optional<TitleDto> get(long id) {
        return titles.findById(id).map(TitleDto::from);
    }
}

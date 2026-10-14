package com.wannawatch.api.title;

import java.util.List;

public record TitlePageDto(List<TitleDto> items, int page, int size, long totalItems, int totalPages) {}

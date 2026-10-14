package com.wannawatch.api.title;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TitleController.class)
@AutoConfigureMockMvc(addFilters = false)
class TitleControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    TitleService service;

    @Test
    void feedReturnsTitles() throws Exception {
        var dto = new TitleDto(1L, Title.Type.SERIES, "Yellowstone", 2018, "A ranching family.",
                null, null, List.of("Drama"), null, List.of());
        when(service.feed(null, null, null, null, "US", 0, 20))
                .thenReturn(new TitlePageDto(List.of(dto), 0, 20, 1, 1));

        mvc.perform(get("/api/titles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("Yellowstone"))
                .andExpect(jsonPath("$.items[0].genres[0]").value("Drama"))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void feedPassesSearchFiltersAndPagination() throws Exception {
        when(service.feed("dune", Title.Type.MOVIE, "Science Fiction", null, "US", 1, 10))
                .thenReturn(new TitlePageDto(List.of(), 1, 10, 0, 0));

        mvc.perform(get("/api/titles")
                        .param("q", "dune")
                        .param("type", "MOVIE")
                        .param("genre", "Science Fiction")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void unknownTitleIs404() throws Exception {
        when(service.get(99, "US")).thenReturn(Optional.empty());
        mvc.perform(get("/api/titles/99")).andExpect(status().isNotFound());
    }
}

package com.wannawatch.api.title;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TitleController.class)
class TitleControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    TitleService service;

    @Test
    void feedReturnsTitles() throws Exception {
        var dto = new TitleDto(1L, Title.Type.SERIES, "Yellowstone", 2018, "A ranching family.",
                null, null, List.of("Drama"), null);
        when(service.feed(null, 0, 20)).thenReturn(List.of(dto));

        mvc.perform(get("/api/titles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Yellowstone"))
                .andExpect(jsonPath("$[0].genres[0]").value("Drama"));
    }

    @Test
    void unknownTitleIs404() throws Exception {
        when(service.get(99)).thenReturn(Optional.empty());
        mvc.perform(get("/api/titles/99")).andExpect(status().isNotFound());
    }
}

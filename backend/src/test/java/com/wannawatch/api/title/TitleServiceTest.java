package com.wannawatch.api.title;

import com.wannawatch.api.config.CountryCodes;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TitleServiceTest {

    @Test
    void normalizesFiltersAndClampsPagination() {
        TitleRepository repository = mock(TitleRepository.class);
        when(repository.search("Dune", Title.Type.MOVIE, "Drama", "Prime Video", "GB",
                PageRequest.of(0, 50, org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "popularity"))))
                .thenReturn(Page.empty());
        TitleService service = new TitleService(repository, "US");

        var result = service.feed(" Dune ", Title.Type.MOVIE, " Drama ", " Prime Video ",
                "gb", -2, 100);

        assertEquals(0, result.page());
        assertEquals(50, result.size());
        assertEquals(0, result.totalItems());
        verify(repository).search("Dune", Title.Type.MOVIE, "Drama", "Prime Video", "GB",
                PageRequest.of(0, 50, org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "popularity")));
    }

    @Test
    void rejectsUnknownMarketCodes() {
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> CountryCodes.normalize("ZZ"));
    }

    @Test
    void normalizesCountryCodes() {
        assertEquals("GB", CountryCodes.normalize(" gb "));
    }
}

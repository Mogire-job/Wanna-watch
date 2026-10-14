package com.wannawatch.api.sync;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SyncController.class)
@AutoConfigureMockMvc(addFilters = false)
class SyncControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    TmdbSyncService syncService;

    @Test
    void syncIsDisabledWithoutServerTokenConfiguration() throws Exception {
        when(syncService.isSyncEnabled()).thenReturn(false);

        mvc.perform(post("/api/admin/sync"))
                .andExpect(status().isServiceUnavailable());

        verify(syncService, never()).sync(anyString());
    }

    @Test
    void syncRequiresValidTokenAndReturnsImportCounts() throws Exception {
        when(syncService.isSyncEnabled()).thenReturn(true);
        when(syncService.acceptsToken("private-token")).thenReturn(true);
        when(syncService.sync("GB")).thenReturn(new TmdbSyncResult(12, 8, 14, 27));

        mvc.perform(post("/api/admin/sync")
                        .header("X-Sync-Token", "private-token")
                                .param("country", "GB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moviesProcessed").value(12))
                .andExpect(jsonPath("$.seriesProcessed").value(8))
                .andExpect(jsonPath("$.providerOffersFound").value(27));

        verify(syncService).sync("GB");
    }
}

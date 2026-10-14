package com.wannawatch.api.sync;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class SyncController {

    private final TmdbSyncService syncService;

    public SyncController(TmdbSyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/sync")
    public TmdbSyncResult sync(@RequestHeader(name = "X-Sync-Token", required = false) String token,
                               @RequestParam(defaultValue = "${app.default-country:US}") String country) {
        if (!syncService.isSyncEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Catalogue sync is disabled. Set SYNC_TOKEN to enable it.");
        }
        if (!syncService.acceptsToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid sync token.");
        }
        return syncService.sync(country);
    }
}

package com.wannawatch.api.sync;

public record TmdbSyncResult(int moviesProcessed, int seriesProcessed, int trailersFound,
                             int providerOffersFound) {}

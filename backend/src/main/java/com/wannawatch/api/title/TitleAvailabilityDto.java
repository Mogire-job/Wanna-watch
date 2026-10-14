package com.wannawatch.api.title;

public record TitleAvailabilityDto(
        String provider,
        String logoUrl,
        String offerType,
        String url) {

    static TitleAvailabilityDto from(Availability availability) {
        return new TitleAvailabilityDto(
                availability.getProvider().getName(),
                availability.getProvider().getLogoUrl(),
                availability.getOfferType(),
                availability.getDeepLink());
    }
}

package com.wannawatch.api.title;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "availability")
public class Availability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "title_id", nullable = false)
    private Title title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @Column(nullable = false, length = 2)
    private String country;

    @Column(name = "offer_type", nullable = false)
    private String offerType;

    @Column(name = "deep_link")
    private String deepLink;

    @Column(name = "last_checked", nullable = false)
    private Instant lastChecked = Instant.now();

    protected Availability() {}

    public Availability(Title title, Provider provider, String country, String offerType, String deepLink) {
        this.title = title;
        this.provider = provider;
        this.country = country;
        this.offerType = offerType;
        this.deepLink = deepLink;
    }

    public Provider getProvider() { return provider; }
    public String getCountry() { return country; }
    public String getOfferType() { return offerType; }
    public String getDeepLink() { return deepLink; }
    public Instant getLastChecked() { return lastChecked; }
}

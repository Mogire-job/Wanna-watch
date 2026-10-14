package com.wannawatch.api.title;

import jakarta.persistence.*;

@Entity
@Table(name = "providers")
public class Provider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "logo_url")
    private String logoUrl;

    private String website;

    protected Provider() {}

    public Provider(String name, String logoUrl) {
        this.name = name;
        this.logoUrl = logoUrl;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getLogoUrl() { return logoUrl; }
    public String getWebsite() { return website; }

    public void updateLogo(String value) { logoUrl = value; }
}

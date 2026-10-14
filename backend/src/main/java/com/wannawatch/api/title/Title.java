package com.wannawatch.api.title;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "titles")
public class Title {

    public enum Type { MOVIE, SERIES }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false)
    private String name;

    @Column(name = "release_year")
    private Integer releaseYear;

    private String synopsis;

    @Column(name = "poster_url")
    private String posterUrl;

    private BigDecimal rating;

    @Column(nullable = false)
    private BigDecimal popularity = BigDecimal.ZERO;

    @ManyToMany
    @JoinTable(name = "title_genres",
            joinColumns = @JoinColumn(name = "title_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"))
    @BatchSize(size = 50)
    private Set<Genre> genres = new LinkedHashSet<>();

    @OneToMany(mappedBy = "title")
    @BatchSize(size = 50)
    private Set<Video> videos = new LinkedHashSet<>();

    @OneToMany(mappedBy = "title")
    @BatchSize(size = 50)
    private Set<Availability> availability = new LinkedHashSet<>();

    public Title() {}

    public Long getId() { return id; }
    public String getExternalId() { return externalId; }
    public Type getType() { return type; }
    public String getName() { return name; }
    public Integer getReleaseYear() { return releaseYear; }
    public String getSynopsis() { return synopsis; }
    public String getPosterUrl() { return posterUrl; }
    public BigDecimal getRating() { return rating; }
    public BigDecimal getPopularity() { return popularity; }
    public Set<Genre> getGenres() { return genres; }
    public Set<Video> getVideos() { return videos; }
    public Set<Availability> getAvailability() { return availability; }

    public void setImportedValues(String externalId, Type type, String name, Integer releaseYear,
                                  String synopsis, String posterUrl, BigDecimal rating, BigDecimal popularity) {
        this.externalId = externalId;
        this.type = type;
        this.name = name;
        this.releaseYear = releaseYear;
        this.synopsis = synopsis;
        this.posterUrl = posterUrl;
        this.rating = rating;
        this.popularity = popularity == null ? BigDecimal.ZERO : popularity;
    }

    public void updateFrom(Title source) {
        type = source.type;
        name = source.name;
        releaseYear = source.releaseYear;
        synopsis = source.synopsis;
        posterUrl = source.posterUrl;
        rating = source.rating;
        popularity = source.popularity;
    }

    public void replaceGenres(Set<Genre> values) {
        genres.clear();
        genres.addAll(values);
    }
}

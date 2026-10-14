package com.wannawatch.api.title;

import jakarta.persistence.*;

@Entity
@Table(name = "videos")
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "title_id")
    private Title title;

    @Column(name = "youtube_key", nullable = false)
    private String youtubeKey;

    @Column(nullable = false)
    private String kind = "OFFICIAL_TRAILER";

    private String language;

    protected Video() {}

    public Video(Title title, String youtubeKey, String language) {
        this.title = title;
        this.youtubeKey = youtubeKey;
        this.language = language;
    }

    public Long getId() { return id; }
    public Title getTitle() { return title; }
    public String getYoutubeKey() { return youtubeKey; }
    public String getKind() { return kind; }
    public String getLanguage() { return language; }
}

package com.wannawatch.api.title;

import jakarta.persistence.*;

@Entity
@Table(name = "genres")
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    protected Genre() {}

    public Long getId() { return id; }
    public String getName() { return name; }
}

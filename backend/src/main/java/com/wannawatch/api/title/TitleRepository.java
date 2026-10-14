package com.wannawatch.api.title;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TitleRepository extends JpaRepository<Title, Long> {

    Page<Title> findByNameContainingIgnoreCase(String query, Pageable pageable);
}

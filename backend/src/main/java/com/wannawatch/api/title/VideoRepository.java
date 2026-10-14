package com.wannawatch.api.title;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VideoRepository extends JpaRepository<Video, Long> {
    @Modifying
    @Query("delete from Video v where v.title.id = :titleId")
    void deleteForTitle(@Param("titleId") Long titleId);
}

package com.wannawatch.api.title;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {
    @Modifying
    @Query("delete from Availability a where a.title.id = :titleId and a.country = :country")
    void deleteForTitleAndCountry(@Param("titleId") Long titleId, @Param("country") String country);
}

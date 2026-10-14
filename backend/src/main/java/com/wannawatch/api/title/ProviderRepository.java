package com.wannawatch.api.title;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProviderRepository extends JpaRepository<Provider, Long> {
    Optional<Provider> findByNameIgnoreCase(String name);

    @Query("select distinct a.provider from Availability a where a.country = :country order by a.provider.name")
    List<Provider> findAllForCountry(@Param("country") String country);
}

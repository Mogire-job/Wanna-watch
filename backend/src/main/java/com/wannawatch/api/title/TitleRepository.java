package com.wannawatch.api.title;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TitleRepository extends JpaRepository<Title, Long> {

    java.util.Optional<Title> findByExternalId(String externalId);

    @Query("""
            select distinct t from Title t
            left join t.genres g
            left join t.availability a on a.country = :country
            left join a.provider p
            where (:query is null or lower(t.name) like lower(concat('%', :query, '%')))
              and (:type is null or t.type = :type)
              and (:genre is null or lower(g.name) = lower(:genre))
              and (:provider is null or lower(p.name) = lower(:provider))
            """)
    Page<Title> search(@Param("query") String query,
                       @Param("type") Title.Type type,
                       @Param("genre") String genre,
                       @Param("provider") String provider,
                       @Param("country") String country,
                       Pageable pageable);
}

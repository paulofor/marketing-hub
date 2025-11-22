package com.marketinghub.leadportal.repository;

import com.marketinghub.leadportal.entity.FlowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface FlowRepository extends JpaRepository<FlowEntity, String> {

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update FlowEntity f set f.accessCount = f.accessCount + 1 where f.slug = :slug")
    void incrementAccessCount(@Param("slug") String slug);
}

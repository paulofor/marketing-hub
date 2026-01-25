package com.marketinghub.audience.repository;

import com.marketinghub.audience.AudienceTargetingSeed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AudienceTargetingSeedRepository extends JpaRepository<AudienceTargetingSeed, Long> {
    List<AudienceTargetingSeed> findByAudienceId(Long audienceId);

    @Modifying
    @Query("delete from AudienceTargetingSeed s where s.audience.id = :audienceId")
    void deleteByAudienceId(Long audienceId);
}

package com.marketinghub.leadportal.repository;

import com.marketinghub.leadportal.LeadPortalSimpleFormStyle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeadPortalSimpleFormStyleRepository extends JpaRepository<LeadPortalSimpleFormStyle, Long> {
    Optional<LeadPortalSimpleFormStyle> findBySlug(String slug);
}

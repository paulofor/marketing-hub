package com.marketinghub.repository.jpa.leadportal;

import com.marketinghub.leadportal.LeadPortalSimpleFormStyle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de LeadPortalSimpleFormStyle.
 */
public interface LeadPortalSimpleFormStyleRepository extends JpaRepository<LeadPortalSimpleFormStyle, Long> {
    Optional<LeadPortalSimpleFormStyle> findBySlug(String slug);

    List<LeadPortalSimpleFormStyle> findByGenerationStatusOrderByUpdatedAtAscIdAsc(String generationStatus,
                                                                                    Pageable pageable);
}

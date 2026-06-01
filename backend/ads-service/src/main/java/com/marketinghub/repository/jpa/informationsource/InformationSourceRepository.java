package com.marketinghub.repository.jpa.informationsource;

import com.marketinghub.informationsource.InformationSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * JPA repository for {@link InformationSource} entities.
 */
public interface InformationSourceRepository extends JpaRepository<InformationSource, Long> {
    List<InformationSource> findByNicheIdOrderByCreatedAtDesc(Long nicheId);
}

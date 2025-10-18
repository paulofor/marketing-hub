package com.marketinghub.appidea.repository;

import com.marketinghub.appidea.AppIdea;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for persisting {@link AppIdea} entities.
 */
public interface AppIdeaRepository extends JpaRepository<AppIdea, Long> {
    List<AppIdea> findByNicheIdOrderByCreatedAtDesc(Long nicheId);
}

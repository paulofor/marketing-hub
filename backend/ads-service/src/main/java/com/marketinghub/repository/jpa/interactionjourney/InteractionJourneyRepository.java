package com.marketinghub.repository.jpa.interactionjourney;

import com.marketinghub.interactionjourney.model.InteractionJourney;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de InteractionJourney.
 */
public interface InteractionJourneyRepository extends JpaRepository<InteractionJourney, Long> {
}

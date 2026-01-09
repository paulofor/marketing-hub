package com.marketinghub.interactionjourney.repository;

import com.marketinghub.interactionjourney.model.InteractionJourney;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InteractionJourneyRepository extends JpaRepository<InteractionJourney, Long> {
}

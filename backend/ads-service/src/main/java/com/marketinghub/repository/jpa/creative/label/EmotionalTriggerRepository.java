package com.marketinghub.repository.jpa.creative.label;

import com.marketinghub.creative.label.EmotionalTrigger;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de EmotionalTrigger.
 */
public interface EmotionalTriggerRepository extends JpaRepository<EmotionalTrigger, Long> {
}

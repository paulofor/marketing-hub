package com.marketinghub.audience.repository;

import com.marketinghub.audience.Audience;
import org.springframework.data.repository.CrudRepository;

/**
 * Repository for persisting {@link Audience} entities.
 */
public interface AudienceRepository extends CrudRepository<Audience, Long> {
}

package com.marketinghub.repository.jpa.microservice;

import com.marketinghub.microservice.Microservice;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for {@link Microservice} entities.
 */
public interface MicroserviceRepository extends JpaRepository<Microservice, Long> {
}

package com.marketinghub.repository.jpa.differentiatedtechnology;

import com.marketinghub.differentiatedtechnology.DifferentiatedTechnology;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de DifferentiatedTechnology.
 */
public interface DifferentiatedTechnologyRepository extends JpaRepository<DifferentiatedTechnology, Long> {
}

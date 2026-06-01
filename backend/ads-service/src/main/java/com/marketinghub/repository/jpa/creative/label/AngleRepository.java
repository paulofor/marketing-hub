package com.marketinghub.repository.jpa.creative.label;

import com.marketinghub.creative.label.Angle;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de Angle.
 */
public interface AngleRepository extends JpaRepository<Angle, Long> {
}

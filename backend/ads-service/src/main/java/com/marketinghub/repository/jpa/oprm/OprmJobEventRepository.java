package com.marketinghub.repository.jpa.oprm;

import com.marketinghub.oprm.OprmJobEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de OprmJobEvent.
 */
public interface OprmJobEventRepository extends JpaRepository<OprmJobEvent, Long> {
}

package com.marketinghub.repository.jpa.oprm.market;

import com.marketinghub.oprm.market.OprmCnpjImportRun;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de OprmCnpjImportRun.
 */
public interface OprmCnpjImportRunRepository extends JpaRepository<OprmCnpjImportRun, Long> {
    Optional<OprmCnpjImportRun> findFirstByStatusOrderByStartedAtDesc(String status);
}

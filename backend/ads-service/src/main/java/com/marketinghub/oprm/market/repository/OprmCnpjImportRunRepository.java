package com.marketinghub.oprm.market.repository;

import com.marketinghub.oprm.market.OprmCnpjImportRun;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OprmCnpjImportRunRepository extends JpaRepository<OprmCnpjImportRun, Long> {
    Optional<OprmCnpjImportRun> findFirstByStatusOrderByStartedAtDesc(String status);
}

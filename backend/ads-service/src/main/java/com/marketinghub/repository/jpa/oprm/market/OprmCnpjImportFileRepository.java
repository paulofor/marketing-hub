package com.marketinghub.repository.jpa.oprm.market;

import com.marketinghub.oprm.market.OprmCnpjImportFile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de OprmCnpjImportFile.
 */
public interface OprmCnpjImportFileRepository extends JpaRepository<OprmCnpjImportFile, Long> {
    List<OprmCnpjImportFile> findByRunId(Long runId);
}

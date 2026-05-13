package com.marketinghub.oprm.market.repository;

import com.marketinghub.oprm.market.OprmCnpjImportFile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OprmCnpjImportFileRepository extends JpaRepository<OprmCnpjImportFile, Long> {
    List<OprmCnpjImportFile> findByRunId(Long runId);
}

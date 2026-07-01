package com.marketinghub.repository.jpa.gerasalespage.v1;

import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationStageAudit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: consultar snapshots de etapas usadas por uma pagina de venda publicada. */
public interface GeraSalesPagePublicationStageAuditRepository
        extends JpaRepository<GeraSalesPagePublicationStageAudit, Long> {
    /** Lista etapas auditadas de uma publicacao na ordem do pipeline. */
    List<GeraSalesPagePublicationStageAudit> findByPublicationAuditIdOrderByStageOrderAsc(Long publicationAuditId);
}

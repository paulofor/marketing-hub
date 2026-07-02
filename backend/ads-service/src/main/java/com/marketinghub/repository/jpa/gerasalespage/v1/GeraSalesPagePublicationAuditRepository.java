package com.marketinghub.repository.jpa.gerasalespage.v1;

import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationAudit;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: consultar snapshots historicos de paginas de venda do GeraSalesPage v1. */
public interface GeraSalesPagePublicationAuditRepository
        extends JpaRepository<GeraSalesPagePublicationAudit, Long> {
    /** Verifica se um job final de publicacao ja possui snapshot historico. */
    boolean existsByPublicationJobId(String publicationJobId);

    /** Lista snapshots de publicacao de um experimento do mais recente para o mais antigo. */
    List<GeraSalesPagePublicationAudit> findByExperimentIdOrderByPublishedAtDesc(Long experimentId);

    /** Busca o snapshot publicado mais recente de um experimento. */
    Optional<GeraSalesPagePublicationAudit> findTopByExperimentIdOrderByPublishedAtDesc(Long experimentId);
}

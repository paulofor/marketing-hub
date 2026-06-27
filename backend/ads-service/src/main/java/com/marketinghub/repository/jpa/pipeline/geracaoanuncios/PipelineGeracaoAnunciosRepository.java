package com.marketinghub.repository.jpa.pipeline.geracaoanuncios;

import com.marketinghub.pipeline.geracaoanuncios.PipelineGeracaoAnuncios;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pela auditoria do pipeline de geração de anúncios. */
public interface PipelineGeracaoAnunciosRepository extends JpaRepository<PipelineGeracaoAnuncios, Long> {
    /** Lista auditorias vinculadas a um job interno em ordem cronológica. */
    List<PipelineGeracaoAnuncios> findByJobIdOrderByDataHoraAsc(String jobId);

    /** Lista auditorias vinculadas a uma etapa em ordem cronológica. */
    List<PipelineGeracaoAnuncios> findByCodigoEtapaOrderByDataHoraAsc(String codigoEtapa);

    /** Verifica se já existe auditoria vinculada a um identificador externo. */
    boolean existsByIdExterno(String idExterno);

    /** Busca a auditoria mais recente de uma etapa vinculada a um identificador externo. */
    Optional<PipelineGeracaoAnuncios> findTopByIdExternoAndCodigoEtapaOrderByDataHoraDesc(String idExterno, String codigoEtapa);
}

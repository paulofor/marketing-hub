package com.marketinghub.oprm.cnae.repository;

import com.marketinghub.oprm.cnae.OprmCnaeOpportunityScore;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório responsável por consultar scores de oportunidade de CNAE gravados pelo OPRM.
 */
public interface OprmCnaeOpportunityScoreRepository extends JpaRepository<OprmCnaeOpportunityScore, String> {
    /**
     * Lista os melhores scores ainda sem enriquecimento para o scheduler OPRM.
     */
    List<OprmCnaeOpportunityScore> findByEnrichedAtIsNullOrderByOpportunityScoreDescCnaeCodeAsc(Pageable pageable);

    /**
     * Lista os melhores scores já enriquecidos para acompanhamento dos nichos prontos.
     */
    List<OprmCnaeOpportunityScore> findByEnrichedAtIsNotNullOrderByOpportunityScoreDescCnaeCodeAsc(Pageable pageable);

    /**
     * Lista os melhores scores persistidos para acompanhamento operacional no frontend.
     */
    List<OprmCnaeOpportunityScore> findAllByOrderByOpportunityScoreDescCnaeCodeAsc(Pageable pageable);
}

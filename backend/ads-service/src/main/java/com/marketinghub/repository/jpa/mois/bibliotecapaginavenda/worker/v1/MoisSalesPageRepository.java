package com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1;

import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.entity.MoisSalesPage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável por localizar e salvar a página/produto da biblioteca de vendas MOIS. */
public interface MoisSalesPageRepository extends JpaRepository<MoisSalesPage, Long> {

    /** Lista até dez páginas/produtos iniciados na etapa atual, priorizando os registros mais antigos. */
    List<MoisSalesPage> findTop10ByDossieProdutoStatusAndDossieProdutoCurrentStageOrderByDossieProdutoUpdatedAtAscIdAsc(
            String status, String currentStage);

    /** Lista até dez páginas/produtos iniciados no pipeline de padrões, priorizando os registros mais antigos. */
    List<MoisSalesPage> findTop10BySalesPagePatternsStatusAndSalesPagePatternsCurrentStageOrderBySalesPagePatternsUpdatedAtAscIdAsc(
            String status, String currentStage);
}

package com.marketinghub.repository.jpa.productai;

import com.marketinghub.productai.delivery.ProductAiPaidDeliveryStageExecution;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir execuções da entrega paga de Produto IA. */
public interface ProductAiPaidDeliveryStageExecutionRepository
        extends JpaRepository<ProductAiPaidDeliveryStageExecution, String> {

    /** Busca execução pelo identificador externo do job. */
    Optional<ProductAiPaidDeliveryStageExecution> findTopByIdJobOrderByExecutionRequestedAtDesc(String idJob);

    /** Busca execução ativa mais recente de uma compra. */
    Optional<ProductAiPaidDeliveryStageExecution> findTopByPurchaseIdOrderByExecutionRequestedAtDesc(Long purchaseId);

    /** Lista pendências de etapa para consumo do worker. */
    List<ProductAiPaidDeliveryStageExecution> findTop20ByPipelineCodeAndStageCodeAndStatusOrderByExecutionRequestedAtAsc(
            String pipelineCode,
            String stageCode,
            String status);
}

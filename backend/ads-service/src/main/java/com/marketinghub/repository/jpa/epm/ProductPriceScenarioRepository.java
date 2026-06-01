package com.marketinghub.repository.jpa.epm;

import com.marketinghub.epm.ProductPriceScenario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositório JPA para persistir e consultar registros ProductPriceScenario do EPM.
 */
public interface ProductPriceScenarioRepository extends JpaRepository<ProductPriceScenario, Long> {
    /** Lista os cenários de preço vinculados ao plano financeiro informado. */
    List<ProductPriceScenario> findByFinancialPlanIdOrderByIdAsc(Long financialPlanId);
}

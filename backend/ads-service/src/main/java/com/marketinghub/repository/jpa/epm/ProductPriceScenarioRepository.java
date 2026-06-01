package com.marketinghub.repository.jpa.epm;

import com.marketinghub.epm.ProductPriceScenario;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA para persistir e consultar registros ProductPriceScenario do EPM.
 */
public interface ProductPriceScenarioRepository extends JpaRepository<ProductPriceScenario, Long> {
}

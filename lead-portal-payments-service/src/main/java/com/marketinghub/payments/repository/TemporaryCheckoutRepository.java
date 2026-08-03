package com.marketinghub.payments.repository;

import com.marketinghub.payments.model.TemporaryCheckout;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acessa a configuração vigente de checkout temporário por produto. */
public interface TemporaryCheckoutRepository extends JpaRepository<TemporaryCheckout, Long> {
    /** Busca a configuração única do produto. */
    Optional<TemporaryCheckout> findByProductKey(String productKey);
}

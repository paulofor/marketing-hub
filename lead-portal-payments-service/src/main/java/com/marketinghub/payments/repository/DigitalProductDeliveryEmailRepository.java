package com.marketinghub.payments.repository;

import com.marketinghub.payments.model.DigitalProductDeliveryEmail;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persiste o controle idempotente de emails de entrega de produto digital.
 */
public interface DigitalProductDeliveryEmailRepository
        extends JpaRepository<DigitalProductDeliveryEmail, Long> {

    /** Busca o envio registrado para um pagamento Mercado Pago. */
    Optional<DigitalProductDeliveryEmail> findByPaymentId(String paymentId);
}

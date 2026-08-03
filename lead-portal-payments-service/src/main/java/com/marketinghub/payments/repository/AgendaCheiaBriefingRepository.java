package com.marketinghub.payments.repository;

import com.marketinghub.payments.model.AgendaCheiaBriefing;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persiste e recupera briefings do Agenda Cheia pelo pagamento aprovado. */
public interface AgendaCheiaBriefingRepository extends JpaRepository<AgendaCheiaBriefing, Long> {
    /** Localiza um briefing já enviado para tornar o pós-compra idempotente. */
    Optional<AgendaCheiaBriefing> findByPaymentId(String paymentId);
}

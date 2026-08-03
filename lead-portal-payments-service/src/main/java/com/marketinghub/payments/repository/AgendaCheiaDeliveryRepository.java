package com.marketinghub.payments.repository;

import com.marketinghub.payments.model.AgendaCheiaDelivery;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acessa execuções de produção do Agenda Cheia. */
public interface AgendaCheiaDeliveryRepository extends JpaRepository<AgendaCheiaDelivery, Long> {
    /** Localiza a execução idempotente pelo briefing. */
    Optional<AgendaCheiaDelivery> findByBriefingId(Long briefingId);
    /** Localiza um pacote por token público opaco. */
    Optional<AgendaCheiaDelivery> findByDownloadToken(String downloadToken);
}

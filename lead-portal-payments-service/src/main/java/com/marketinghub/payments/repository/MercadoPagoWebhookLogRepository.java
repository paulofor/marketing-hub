package com.marketinghub.payments.repository;

import com.marketinghub.payments.model.MercadoPagoWebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MercadoPagoWebhookLogRepository extends JpaRepository<MercadoPagoWebhookLog, Long> {
}

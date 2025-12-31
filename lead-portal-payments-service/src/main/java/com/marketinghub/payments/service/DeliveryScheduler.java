package com.marketinghub.payments.service;

import com.marketinghub.payments.config.DeliveryProperties;
import com.marketinghub.payments.model.LeadPortalPurchase;
import com.marketinghub.payments.model.PurchaseStatus;
import com.marketinghub.payments.repository.LeadPortalPurchaseRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DeliveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(DeliveryScheduler.class);

    private final DeliveryProperties properties;
    private final LeadPortalPurchaseRepository purchaseRepository;
    private final PremiumDeliveryService premiumDeliveryService;

    public DeliveryScheduler(DeliveryProperties properties,
                             LeadPortalPurchaseRepository purchaseRepository,
                             PremiumDeliveryService premiumDeliveryService) {
        this.properties = properties;
        this.purchaseRepository = purchaseRepository;
        this.premiumDeliveryService = premiumDeliveryService;
    }

    @Scheduled(initialDelayString = "${delivery.initial-delay:20000}", fixedDelayString = "${delivery.fixed-delay:60000}")
    public void dispatchApprovedPurchases() {
        if (!properties.isEnabled()) {
            return;
        }
        List<LeadPortalPurchase> purchases = purchaseRepository.findByStatusAndDeliveredAtIsNull(
                PurchaseStatus.APPROVED,
                PageRequest.of(0, Math.max(1, properties.getBatchSize())));
        if (purchases.isEmpty()) {
            return;
        }
        for (LeadPortalPurchase purchase : purchases) {
            Integer attempts = purchase.getDeliveryAttempts();
            if (attempts != null && attempts >= properties.getMaxAttempts()) {
                log.warn("Compra {} acima do limite de tentativas de entrega ({}). Ignorando até intervenção manual.",
                        purchase.getId(), properties.getMaxAttempts());
                continue;
            }
            try {
                premiumDeliveryService.ensureQueued(purchase);
            } catch (Exception ex) {
                log.error("Erro ao enfileirar entrega premium da compra {}", purchase.getId(), ex);
            }
        }
    }
}

package com.marketinghub.payments.service;

import com.marketinghub.payments.dto.LeadPortalPackageSummary;
import com.marketinghub.payments.model.LeadPortalPremiumDelivery;
import com.marketinghub.payments.model.LeadPortalPurchase;
import com.marketinghub.payments.model.PremiumDeliveryStatus;
import com.marketinghub.payments.model.PurchaseStatus;
import com.marketinghub.payments.repository.LeadPortalPremiumDeliveryRepository;
import com.marketinghub.payments.repository.LeadPortalPurchaseRepository;
import jakarta.transaction.Transactional;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PremiumDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(PremiumDeliveryService.class);

    private final LeadPortalPremiumDeliveryRepository deliveryRepository;
    private final LeadPortalPurchaseRepository purchaseRepository;
    private final LeadPortalPackageGateway packageGateway;

    public PremiumDeliveryService(LeadPortalPremiumDeliveryRepository deliveryRepository,
                                  LeadPortalPurchaseRepository purchaseRepository,
                                  LeadPortalPackageGateway packageGateway) {
        this.deliveryRepository = deliveryRepository;
        this.purchaseRepository = purchaseRepository;
        this.packageGateway = packageGateway;
    }

    @Transactional
    public void ensureQueued(LeadPortalPurchase purchase) {
        if (purchase == null) {
            throw new IllegalArgumentException("Compra inválida");
        }
        Optional<LeadPortalPremiumDelivery> existing = deliveryRepository.findByPurchaseId(purchase.getId());
        if (existing.isPresent()) {
            log.debug("Entrega premium já registrada para a compra {} (status={})",
                    purchase.getId(), existing.get().getStatus());
            if (purchase.getStatus() == PurchaseStatus.APPROVED) {
                purchase.setStatus(PurchaseStatus.DELIVERING);
                purchaseRepository.save(purchase);
            }
            return;
        }
        LeadPortalPackageSummary summary = packageGateway.loadPackage(purchase.getPackageId());
        LeadPortalPremiumDelivery delivery = new LeadPortalPremiumDelivery();
        delivery.setPurchaseId(purchase.getId());
        hydrateDelivery(delivery, purchase, summary);
        delivery.resetZipState();
        delivery.resetEmailState();
        delivery.setStatus(PremiumDeliveryStatus.PENDING_ZIP);

        deliveryRepository.save(delivery);
        markPurchaseDelivering(purchase);
        log.info("Entrega premium registrada para compra {} (packageId={})", purchase.getId(), purchase.getPackageId());
    }

    @Transactional
    public void ensureQueued(Long purchaseId) {
        LeadPortalPurchase purchase = purchaseRepository.lockById(purchaseId)
                .orElseThrow(() -> new IllegalStateException("Compra " + purchaseId + " não encontrada"));
        ensureQueued(purchase);
    }

    @Transactional
    public void forceResend(Long purchaseId) {
        LeadPortalPurchase purchase = purchaseRepository.lockById(purchaseId)
                .orElseThrow(() -> new IllegalStateException("Compra " + purchaseId + " não encontrada"));
        LeadPortalPremiumDelivery delivery = deliveryRepository.findByPurchaseId(purchaseId)
                .orElseThrow(() -> new IllegalStateException("Compra " + purchaseId + " não possui registro de entrega"));

        if (StringUtils.hasText(delivery.getZipObjectKey())) {
            delivery.setStatus(PremiumDeliveryStatus.ZIP_READY);
            delivery.resetEmailState();
            log.info("Reenfileirando apenas o envio do e-mail da compra {}", purchaseId);
        } else {
            delivery.resetZipState();
            delivery.resetEmailState();
            delivery.setStatus(PremiumDeliveryStatus.PENDING_ZIP);
            log.info("Reenfileirando geração do ZIP e envio do e-mail da compra {}", purchaseId);
        }
        deliveryRepository.save(delivery);
        markPurchaseDelivering(purchase);
    }

    private void hydrateDelivery(LeadPortalPremiumDelivery delivery,
                                 LeadPortalPurchase purchase,
                                 LeadPortalPackageSummary summary) {
        delivery.setPackageId(purchase.getPackageId());
        delivery.setSubmissionId(summary.submissionId() != null ? summary.submissionId().toString() : null);
        delivery.setSubmissionName(summary.submissionName());
        delivery.setSubmissionEmail(summary.submissionEmail());
        delivery.setBuyerName(purchase.getBuyerName());
        delivery.setBuyerEmail(purchase.getBuyerEmail());
        String recipientEmail = resolveRecipientEmail(purchase, summary);
        delivery.setRecipientEmail(recipientEmail);
        delivery.setRecipientName(resolveRecipientName(purchase, summary));
    }

    private String resolveRecipientEmail(LeadPortalPurchase purchase, LeadPortalPackageSummary summary) {
        if (StringUtils.hasText(purchase.getBuyerEmail())) {
            return purchase.getBuyerEmail();
        }
        if (summary != null && StringUtils.hasText(summary.submissionEmail())) {
            return summary.submissionEmail();
        }
        throw new IllegalStateException("Compra " + purchase.getId() + " sem e-mail do comprador ou da submissão");
    }

    private String resolveRecipientName(LeadPortalPurchase purchase, LeadPortalPackageSummary summary) {
        if (StringUtils.hasText(purchase.getBuyerName())) {
            return purchase.getBuyerName();
        }
        if (summary != null && StringUtils.hasText(summary.submissionName())) {
            return summary.submissionName();
        }
        return null;
    }

    private void markPurchaseDelivering(LeadPortalPurchase purchase) {
        purchase.setStatus(PurchaseStatus.DELIVERING);
        purchase.setDeliveryAttempts((purchase.getDeliveryAttempts() == null ? 0 : purchase.getDeliveryAttempts()) + 1);
        purchase.setDeliveryError(null);
        purchaseRepository.save(purchase);
    }
}

package com.marketinghub.payments.service;

import com.marketinghub.payments.dto.LeadPortalPackageSummary;
import com.marketinghub.payments.dto.OriginalAsset;
import com.marketinghub.payments.model.LeadPortalPurchase;
import com.marketinghub.payments.model.PurchaseStatus;
import com.marketinghub.payments.repository.LeadPortalPurchaseRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);

    private final LeadPortalPurchaseRepository purchaseRepository;
    private final LeadPortalPackageGateway packageGateway;
    private final OriginalZipService zipService;
    private final StorageService storageService;
    private final EmailDeliveryService emailDeliveryService;

    public DeliveryService(LeadPortalPurchaseRepository purchaseRepository,
                           LeadPortalPackageGateway packageGateway,
                           OriginalZipService zipService,
                           StorageService storageService,
                           EmailDeliveryService emailDeliveryService) {
        this.purchaseRepository = purchaseRepository;
        this.packageGateway = packageGateway;
        this.zipService = zipService;
        this.storageService = storageService;
        this.emailDeliveryService = emailDeliveryService;
    }

    @Transactional
    public void deliver(Long purchaseId) {
        LeadPortalPurchase purchase = purchaseRepository.lockById(purchaseId)
                .orElseThrow(() -> new IllegalStateException("Compra não encontrada"));
        if (purchase.getStatus() != PurchaseStatus.APPROVED && purchase.getStatus() != PurchaseStatus.DELIVERING) {
            log.debug("Compra {} ignorada pois está em {}", purchaseId, purchase.getStatus());
            return;
        }
        purchase.setStatus(PurchaseStatus.DELIVERING);
        purchase.setDeliveryAttempts((purchase.getDeliveryAttempts() == null ? 0 : purchase.getDeliveryAttempts()) + 1);
        purchaseRepository.save(purchase);

        try {
            LeadPortalPackageSummary summary = packageGateway.loadPackage(purchase.getPackageId());
            List<OriginalAsset> assets = packageGateway.listOriginalAssets(purchase.getPackageId());
            String objectKey = purchase.getZipObjectKey();
            Long size = purchase.getZipSizeBytes();
            if (!StringUtils.hasText(objectKey)) {
                OriginalZipService.GeneratedZip generatedZip = zipService.buildAndStoreZip(purchase.getPackageId(), assets);
                objectKey = generatedZip.objectKey();
                size = generatedZip.sizeBytes();
                purchase.setZipObjectKey(objectKey);
                purchase.setZipSizeBytes(size);
                purchase.setZipGeneratedAt(generatedZip.generatedAt());
            }
            String downloadUrl = storageService.resolvePublicUrl(objectKey).orElse(null);
            emailDeliveryService.sendOriginalsEmail(resolveRecipient(purchase, summary), summary, downloadUrl);

            purchase.setStatus(PurchaseStatus.DELIVERED);
            purchase.setDeliveryError(null);
            purchase.setDeliveredAt(java.time.Instant.now());
            purchaseRepository.save(purchase);
        } catch (Exception ex) {
            log.error("Falha ao entregar compra {}", purchaseId, ex);
            purchase.setStatus(PurchaseStatus.APPROVED);
            purchase.setDeliveryError(ex.getMessage());
            purchaseRepository.save(purchase);
            throw ex;
        }
    }

    private String resolveRecipient(LeadPortalPurchase purchase, LeadPortalPackageSummary summary) {
        if (StringUtils.hasText(purchase.getBuyerEmail())) {
            return purchase.getBuyerEmail();
        }
        if (summary != null && StringUtils.hasText(summary.submissionEmail())) {
            return summary.submissionEmail();
        }
        throw new IllegalStateException("Destinatário não informado para a compra " + purchase.getId());
    }
}

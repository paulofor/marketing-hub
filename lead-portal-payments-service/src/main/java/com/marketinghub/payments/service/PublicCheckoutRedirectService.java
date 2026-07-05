package com.marketinghub.payments.service;

import com.marketinghub.payments.model.LeadPortalPurchase;
import com.marketinghub.payments.repository.LeadPortalPurchaseRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Responsabilidade: registrar acesso público ao checkout e resolver o redirecionamento do cliente. */
@Service
public class PublicCheckoutRedirectService {

    private final LeadPortalPurchaseRepository purchaseRepository;

    /** Inicializa o serviço com o repositório de compras do Lead Portal Payments. */
    public PublicCheckoutRedirectService(LeadPortalPurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    /** Registra o acesso e retorna a URL de checkout quando a compra e o token são válidos. */
    @Transactional
    public Optional<String> registerCheckoutAccess(Long purchaseId, String submissionToken) {
        if (purchaseId == null) {
            return Optional.empty();
        }
        Optional<LeadPortalPurchase> purchase = purchaseRepository.findById(purchaseId);
        if (purchase.isEmpty() || !matchesSubmission(purchase.get(), submissionToken)) {
            return Optional.empty();
        }
        String checkoutUrl = purchase.get().getCheckoutUrl();
        if (!StringUtils.hasText(checkoutUrl)) {
            return Optional.empty();
        }
        purchaseRepository.markCheckoutAccessed(purchaseId, Instant.now());
        return Optional.of(checkoutUrl);
    }

    /** Valida o token de submissão quando o link público inclui esse parâmetro. */
    private boolean matchesSubmission(LeadPortalPurchase purchase, String submissionToken) {
        if (!StringUtils.hasText(submissionToken) || !StringUtils.hasText(purchase.getSubmissionId())) {
            return true;
        }
        return submissionToken.trim().equalsIgnoreCase(purchase.getSubmissionId());
    }
}

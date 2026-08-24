package com.marketinghub.pde.service;

import com.marketinghub.pde.dto.PepperRefundResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Reconcilia reembolso Pepper com auditoria, acesso e métrica líquida do produto PDE. */
@Service
public class PepperRefundSyncService {
    private static final Logger log = LoggerFactory.getLogger(PepperRefundSyncService.class);

    private final PepperTransactionGateway pepperTransactionGateway;
    private final PaymentAuditService paymentAuditService;
    private final AccessService accessService;
    private final String defaultProductSlug;

    /** Recebe as portas que comprovam o estado financeiro antes de alterar acesso ou métrica. */
    public PepperRefundSyncService(
            PepperTransactionGateway pepperTransactionGateway,
            PaymentAuditService paymentAuditService,
            AccessService accessService,
            @Value("${pde.access.pepper.product-slug:metodo-musa-7-dias}") String defaultProductSlug) {
        this.pepperTransactionGateway = pepperTransactionGateway;
        this.paymentAuditService = paymentAuditService;
        this.accessService = accessService;
        this.defaultProductSlug = defaultProductSlug;
    }

    /** Confirma o reembolso no provedor e aplica cada efeito comercial no máximo uma vez. */
    public PepperRefundResponse reconcile(String productSlug, String transactionId) {
        String resolvedProductSlug = productSlug == null || productSlug.isBlank()
                ? defaultProductSlug
                : productSlug.trim();
        PepperTransactionSnapshot transaction = pepperTransactionGateway.findTransactionByHash(transactionId);
        PaymentAuditService.RefundAuditResult audit =
                paymentAuditService.recordVerifiedRefund(resolvedProductSlug, transaction);
        boolean revoked = accessService.revokePepperPaidAccess(
                audit.accessReferenceHash(), transaction.transactionId(), transaction.paymentStatus());
        log.info(
                "Reembolso Pepper reconciliado; productSlug={}, transactionId={}, status={}, newlyRecorded={}, accessRevoked={}",
                resolvedProductSlug,
                transaction.transactionId(),
                transaction.paymentStatus(),
                audit.newlyRecorded(),
                revoked);
        return new PepperRefundResponse(
                resolvedProductSlug,
                transaction.transactionId(),
                transaction.paymentStatus(),
                audit.newlyRecorded(),
                revoked,
                audit.refundedAt().toString());
    }
}

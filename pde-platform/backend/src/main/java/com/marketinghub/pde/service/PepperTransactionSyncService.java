package com.marketinghub.pde.service;

import com.marketinghub.pde.dto.AccessResponse;
import com.marketinghub.pde.dto.PepperSyncResponse;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Reconciliador de compras Pepper que libera acessos quando o webhook nao chega. */
@Service
public class PepperTransactionSyncService {
    private static final Logger log = LoggerFactory.getLogger(PepperTransactionSyncService.class);

    private final AccessService accessService;
    private final PepperTransactionGateway pepperTransactionGateway;
    private final PaymentAuditService paymentAuditService;
    private final String defaultProductSlug;

    /** Recebe dependencias de acesso e consulta Pepper. */
    public PepperTransactionSyncService(
            AccessService accessService,
            PepperTransactionGateway pepperTransactionGateway,
            PaymentAuditService paymentAuditService,
            @Value("${pde.access.pepper.product-slug:metodo-musa-7-dias}") String defaultProductSlug) {
        this.accessService = accessService;
        this.pepperTransactionGateway = pepperTransactionGateway;
        this.paymentAuditService = paymentAuditService;
        this.defaultProductSlug = defaultProductSlug;
    }

    /** Sincroniza compras pagas da Pepper para o produto informado ou para o produto padrao. */
    public PepperSyncResponse syncPaidTransactions(String productSlug, String search) {
        return syncPaidTransactions(productSlug, search, null);
    }

    /** Sincroniza compras pagas por busca ou por hash especifico da transacao Pepper. */
    public PepperSyncResponse syncPaidTransactions(String productSlug, String search, String transactionHash) {
        String resolvedProductSlug = productSlug == null || productSlug.isBlank() ? defaultProductSlug : productSlug;
        PepperTransactionGateway.PepperTransactionSearchResult result = transactionHash == null
                        || transactionHash.isBlank()
                ? pepperTransactionGateway.findPaidTransactions(search)
                : pepperTransactionGateway.findPaidTransactionByHash(transactionHash);
        List<AccessResponse> accesses = new ArrayList<>();
        for (PepperPaidTransaction transaction : result.paidTransactions()) {
            boolean newlyVerifiedPayment = paymentAuditService.recordVerifiedPayment(resolvedProductSlug, transaction);
            AccessResponse access = accessService.releasePepperPaidTransaction(
                    resolvedProductSlug,
                    transaction.buyerEmail(),
                    transaction.transactionId(),
                    transaction.offerHash(),
                    transaction.amount(),
                    transaction.currency(),
                    transaction.paymentStatus(),
                    transaction.experienceVersion(),
                    newlyVerifiedPayment);
            paymentAuditService.linkReleasedAccess(transaction.transactionId(), access.token());
            accesses.add(access);
        }
        log.info(
                "Reconciliacao Pepper concluida; productSlug={}, searchPresent={}, scanned={}, paid={}, released={}",
                resolvedProductSlug,
                search != null && !search.isBlank(),
                result.scannedTransactions(),
                result.paidTransactions().size(),
                accesses.size());
        return new PepperSyncResponse(
                resolvedProductSlug,
                result.scannedTransactions(),
                result.paidTransactions().size(),
                accesses.size(),
                accesses);
    }
}

package com.marketinghub.payments.service;

import com.marketinghub.payments.config.MercadoPagoProperties;
import com.marketinghub.payments.config.PaymentProperties;
import com.marketinghub.payments.dto.CreateCheckoutRequest;
import com.marketinghub.payments.dto.CreateCheckoutResponse;
import com.marketinghub.payments.dto.LeadPortalPackageSummary;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoClient;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPaymentDetails;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPreferenceRequest;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPreferenceResponse;
import com.marketinghub.payments.model.FlowSubmissionImagePackageStatus;
import com.marketinghub.payments.model.LeadPortalPurchase;
import com.marketinghub.payments.model.PurchaseStatus;
import com.marketinghub.payments.repository.LeadPortalPurchaseRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private final MercadoPagoClient mercadoPagoClient;
    private final LeadPortalPackageGateway packageGateway;
    private final LeadPortalPurchaseRepository purchaseRepository;
    private final MercadoPagoProperties mercadoPagoProperties;
    private final PaymentProperties paymentProperties;

    public CheckoutService(MercadoPagoClient mercadoPagoClient,
                           LeadPortalPackageGateway packageGateway,
                           LeadPortalPurchaseRepository purchaseRepository,
                           MercadoPagoProperties mercadoPagoProperties,
                           PaymentProperties paymentProperties) {
        this.mercadoPagoClient = mercadoPagoClient;
        this.packageGateway = packageGateway;
        this.purchaseRepository = purchaseRepository;
        this.mercadoPagoProperties = mercadoPagoProperties;
        this.paymentProperties = paymentProperties;
    }

    public Optional<MercadoPagoPaymentDetails> fetchPayment(String paymentId) {
        return mercadoPagoClient.fetchPayment(paymentId);
    }

    @Transactional
    public CreateCheckoutResponse createCheckout(CreateCheckoutRequest request) {
        LeadPortalPackageSummary imagePackage = packageGateway.loadPackage(request.getPackageId());
        if (imagePackage.status() == null) {
            throw new IllegalStateException("Status do pacote desconhecido");
        }
        if (imagePackage.status() == FlowSubmissionImagePackageStatus.FAILED
                || imagePackage.status() == FlowSubmissionImagePackageStatus.RECEIVED
                || imagePackage.status() == FlowSubmissionImagePackageStatus.PROCESSING) {
            throw new IllegalStateException("Pacote ainda não está pronto para compra");
        }

        BigDecimal amount = imagePackage.totalPrice() != null ? imagePackage.totalPrice() : paymentProperties.getDefaultAmount();
        String currency = StringUtils.hasText(imagePackage.currency()) ? imagePackage.currency() : paymentProperties.getDefaultCurrency();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("packageId", imagePackage.packageId());
        if (imagePackage.submissionId() != null) {
            metadata.put("submissionId", imagePackage.submissionId().toString());
        }
        if (imagePackage.submissionEmail() != null) {
            metadata.put("submissionEmail", imagePackage.submissionEmail());
        }

        MercadoPagoPreferenceRequest preferenceRequest = new MercadoPagoPreferenceRequest(
                java.util.List.of(new MercadoPagoPreferenceRequest.Item(
                        "Pacote de imagens " + imagePackage.packageId(),
                        1,
                        amount,
                        currency)),
                new MercadoPagoPreferenceRequest.Payer(resolveBuyerName(request, imagePackage),
                        resolveBuyerEmail(request, imagePackage)),
                new MercadoPagoPreferenceRequest.BackUrls(
                        mercadoPagoProperties.getSuccessUrl(),
                        mercadoPagoProperties.getFailureUrl(),
                        mercadoPagoProperties.getPendingUrl()),
                metadata,
                mercadoPagoProperties.getNotificationUrl(),
                mercadoPagoProperties.getStatementDescriptor()
        );

        MercadoPagoPreferenceResponse response = mercadoPagoClient.createPreference(preferenceRequest);
        LeadPortalPurchase purchase = purchaseRepository.findTopByPackageIdOrderByCreatedAtDesc(imagePackage.packageId())
                .orElse(new LeadPortalPurchase());
        purchase.setPackageId(imagePackage.packageId());
        purchase.setSubmissionId(imagePackage.submissionId() != null ? imagePackage.submissionId().toString() : null);
        purchase.setBuyerEmail(resolveBuyerEmail(request, imagePackage));
        purchase.setBuyerName(resolveBuyerName(request, imagePackage));
        purchase.setStatus(PurchaseStatus.PREFERENCE_CREATED);
        purchase.setMercadoPagoPreferenceId(response != null ? response.id() : null);
        purchase.setCheckoutUrl(response != null ? response.initPoint() : null);
        purchase.setAmount(amount);
        purchase.setCurrency(currency);
        purchaseRepository.save(purchase);

        log.info("Preferência {} criada para o pacote {} (valor {} {})", purchase.getMercadoPagoPreferenceId(),
                imagePackage.packageId(), amount, currency);

        return new CreateCheckoutResponse(purchase.getId(), imagePackage.packageId(), purchase.getMercadoPagoPreferenceId(),
                purchase.getCheckoutUrl(), purchase.getStatus().name());
    }

    @Transactional
    public LeadPortalPurchase updateFromPayment(MercadoPagoPaymentDetails paymentDetails, String rawPayload) {
        if (paymentDetails == null || !StringUtils.hasText(paymentDetails.id())) {
            throw new IllegalArgumentException("Pagamento inválido");
        }
        Long packageId = extractPackageId(paymentDetails.metadata());
        if (packageId == null) {
            throw new IllegalStateException("Pagamento sem packageId na metadata");
        }
        LeadPortalPurchase purchase = purchaseRepository.findByMercadoPagoPaymentId(paymentDetails.id())
                .orElseGet(() -> purchaseRepository.findTopByPackageIdOrderByCreatedAtDesc(packageId).orElse(new LeadPortalPurchase()));

        purchase.setPackageId(packageId);
        purchase.setSubmissionId((String) paymentDetails.metadata().getOrDefault("submissionId", null));
        purchase.setBuyerEmail(paymentDetails.email());
        purchase.setMercadoPagoPaymentId(paymentDetails.id());
        purchase.setMercadoPagoStatus(paymentDetails.status());
        purchase.setNotificationPayload(rawPayload);
        purchase.setAmount(paymentDetails.amount());
        purchase.setCurrency(paymentDetails.currency());

        if ("approved".equalsIgnoreCase(paymentDetails.status())) {
            purchase.setStatus(PurchaseStatus.APPROVED);
            purchase.setPaymentApprovedAt(paymentDetails.dateApproved());
        } else if ("rejected".equalsIgnoreCase(paymentDetails.status())) {
            purchase.setStatus(PurchaseStatus.FAILED);
        } else {
            purchase.setStatus(PurchaseStatus.PENDING_PAYMENT);
        }

        return purchaseRepository.save(purchase);
    }

    private Long extractPackageId(Map<String, Object> metadata) {
        Object value = metadata != null ? metadata.get("packageId") : null;
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str && StringUtils.hasText(str)) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private String resolveBuyerEmail(CreateCheckoutRequest request, LeadPortalPackageSummary summary) {
        if (request != null && StringUtils.hasText(request.getBuyerEmail())) {
            return request.getBuyerEmail();
        }
        return summary.submissionEmail();
    }

    private String resolveBuyerName(CreateCheckoutRequest request, LeadPortalPackageSummary summary) {
        if (request != null && StringUtils.hasText(request.getBuyerName())) {
            return request.getBuyerName();
        }
        return summary.submissionName();
    }
}

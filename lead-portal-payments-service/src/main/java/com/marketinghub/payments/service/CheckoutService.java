package com.marketinghub.payments.service;

import com.marketinghub.payments.config.MercadoPagoProperties;
import com.marketinghub.payments.config.PaymentProperties;
import com.marketinghub.payments.dto.CreateCheckoutRequest;
import com.marketinghub.payments.dto.CreateCheckoutResponse;
import com.marketinghub.payments.dto.LeadPortalPackageSummary;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoClient;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPreferenceDetails;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPaymentDetails;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPreferenceRequest;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPreferenceResponse;
import com.marketinghub.payments.model.FlowSubmissionImagePackageStatus;
import com.marketinghub.payments.model.LeadPortalPurchase;
import com.marketinghub.payments.model.PurchaseStatus;
import com.marketinghub.payments.repository.LeadPortalPurchaseRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

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
        log.info("Validando pacote {} para checkout (status={})", imagePackage.packageId(), imagePackage.status());
        validatePackageStatus(imagePackage.status());

        BigDecimal amount = resolveAmount(imagePackage);
        String currency = resolveCurrency(imagePackage);
        String buyerEmail = resolveBuyerEmail(request, imagePackage);
        String buyerName = resolveBuyerName(request, imagePackage);

        log.info("Dados do checkout do pacote {}: valor={} {}, comprador={} ({})", imagePackage.packageId(), amount,
                currency, buyerName, buyerEmail);

        LeadPortalPurchase latestPurchase = purchaseRepository
                .findTopByPackageIdOrderByCreatedAtDesc(imagePackage.packageId())
                .orElse(null);

        if (isReusableCheckout(latestPurchase)) {
            refreshPurchase(latestPurchase, imagePackage, amount, currency, buyerName, buyerEmail);
            purchaseRepository.save(latestPurchase);
            log.info("Reutilizando preferência {} para o pacote {}", latestPurchase.getMercadoPagoPreferenceId(),
                    imagePackage.packageId());
            return toResponse(latestPurchase);
        }

        MercadoPagoPreferenceRequest preferenceRequest = buildPreferenceRequest(imagePackage,
                amount, currency, buyerName, buyerEmail);
        log.info("Enviando preferência do pacote {} ao Mercado Pago (notificationUrl={}, statementDescriptor={})",
                imagePackage.packageId(), mercadoPagoProperties.getNotificationUrl(),
                mercadoPagoProperties.getStatementDescriptor());
        MercadoPagoPreferenceResponse response = mercadoPagoClient.createPreference(preferenceRequest);

        if (response == null || !StringUtils.hasText(response.initPoint())) {
            log.error("Mercado Pago não retornou init_point para preferência do pacote {} (preferenceId={})",
                    imagePackage.packageId(), response != null ? response.id() : null);
            throw new IllegalStateException("Mercado Pago não retornou link de checkout");
        }

        LeadPortalPurchase purchase = latestPurchase != null ? latestPurchase : new LeadPortalPurchase();
        purchase.setPackageId(imagePackage.packageId());
        purchase.setSubmissionId(imagePackage.submissionId() != null ? imagePackage.submissionId().toString() : null);
        purchase.setBuyerEmail(buyerEmail);
        purchase.setBuyerName(buyerName);
        purchase.setStatus(PurchaseStatus.PREFERENCE_CREATED);
        purchase.setMercadoPagoPreferenceId(response.id());
        purchase.setCheckoutUrl(response.initPoint());
        purchase.setCheckoutExpiresAt(resolveCheckoutExpiration());
        purchase.setAmount(amount);
        purchase.setCurrency(currency);
        purchaseRepository.save(purchase);

        log.info("Preferência {} criada para o pacote {} (valor {} {})", purchase.getMercadoPagoPreferenceId(),
                imagePackage.packageId(), amount, currency);

        return toResponse(purchase);
    }

    @Transactional(readOnly = true)
    public CreateCheckoutResponse findCheckoutByPackage(Long packageId) {
        LeadPortalPurchase purchase = purchaseRepository.findTopByPackageIdOrderByCreatedAtDesc(packageId)
                .orElseThrow(() -> new IllegalStateException(
                        "Pacote %d não possui preferências criadas".formatted(packageId)));
        if (!StringUtils.hasText(purchase.getCheckoutUrl())) {
            throw new IllegalStateException("Pacote " + packageId + " ainda não possui link de checkout");
        }
        return toResponse(purchase);
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
                .orElseGet(() -> purchaseRepository.findTopByPackageIdOrderByCreatedAtDesc(packageId)
                        .orElse(new LeadPortalPurchase()));

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

    private void validatePackageStatus(FlowSubmissionImagePackageStatus status) {
        if (status == null) {
            throw new IllegalStateException("Status do pacote desconhecido");
        }
        if (status == FlowSubmissionImagePackageStatus.FAILED
                || status == FlowSubmissionImagePackageStatus.RECEIVED
                || status == FlowSubmissionImagePackageStatus.PROCESSING) {
            log.warn("Pacote em status {} ainda não está pronto para compra", status);
            throw new IllegalStateException("Pacote ainda não está pronto para compra");
        }
    }

    private BigDecimal resolveAmount(LeadPortalPackageSummary imagePackage) {
        BigDecimal amount = imagePackage.totalPrice();
        if (amount == null) {
            BigDecimal defaultAmount = paymentProperties.getDefaultAmount();
            return defaultAmount != null ? defaultAmount.setScale(2, RoundingMode.HALF_UP) : null;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveCurrency(LeadPortalPackageSummary imagePackage) {
        String currency = StringUtils.hasText(imagePackage.currency())
                ? imagePackage.currency()
                : paymentProperties.getDefaultCurrency();
        return StringUtils.hasText(currency) ? currency.trim().toUpperCase(Locale.ROOT) : null;
    }

    private MercadoPagoPreferenceRequest buildPreferenceRequest(LeadPortalPackageSummary imagePackage,
                                                                BigDecimal amount,
                                                                String currency,
                                                                String buyerName,
                                                                String buyerEmail) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("packageId", imagePackage.packageId());
        if (imagePackage.submissionId() != null) {
            metadata.put("submissionId", imagePackage.submissionId().toString());
        }
        if (imagePackage.submissionEmail() != null) {
            metadata.put("submissionEmail", imagePackage.submissionEmail());
        }

        String successUrl = buildBackUrl(mercadoPagoProperties.getSuccessUrl(), imagePackage.packageId(), "success");
        String failureUrl = buildBackUrl(mercadoPagoProperties.getFailureUrl(), imagePackage.packageId(), "failure");
        String pendingUrl = buildBackUrl(mercadoPagoProperties.getPendingUrl(), imagePackage.packageId(), "pending");

        return new MercadoPagoPreferenceRequest(
                List.of(new MercadoPagoPreferenceRequest.Item(
                        "Pacote de imagens " + imagePackage.packageId(),
                        1,
                        amount,
                        currency)),
                new MercadoPagoPreferenceRequest.Payer(buyerName, buyerEmail),
                new MercadoPagoPreferenceRequest.BackUrls(
                        successUrl,
                        failureUrl,
                        pendingUrl),
                metadata,
                mercadoPagoProperties.getNotificationUrl(),
                mercadoPagoProperties.getStatementDescriptor()
        );
    }

    private String buildBackUrl(String baseUrl, Long packageId, String flow) {
        if (!StringUtils.hasText(baseUrl)) {
            return null;
        }
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl.trim());
            if (packageId != null) {
                builder.replaceQueryParam("packageId", packageId);
            }
            if (StringUtils.hasText(flow)) {
                builder.replaceQueryParam("flow", flow);
            }
            return builder.build(true).toUriString();
        } catch (IllegalArgumentException ex) {
            log.warn("URL de retorno inválida configurada: {}", baseUrl, ex);
            return baseUrl;
        }
    }

    private void refreshPurchase(LeadPortalPurchase purchase,
                                 LeadPortalPackageSummary imagePackage,
                                 BigDecimal amount,
                                 String currency,
                                 String buyerName,
                                 String buyerEmail) {
        if (imagePackage.submissionId() != null) {
            purchase.setSubmissionId(imagePackage.submissionId().toString());
        }
        purchase.setBuyerEmail(buyerEmail);
        purchase.setBuyerName(buyerName);
        purchase.setAmount(amount);
        purchase.setCurrency(currency);
        if (purchase.getStatus() == null) {
            purchase.setStatus(PurchaseStatus.PREFERENCE_CREATED);
        }
    }

    private boolean isReusableCheckout(LeadPortalPurchase purchase) {
        if (purchase == null) {
            return false;
        }
        if (!StringUtils.hasText(purchase.getCheckoutUrl())) {
            return false;
        }
        if (StringUtils.hasText(purchase.getMercadoPagoPaymentId())) {
            return false;
        }
        if (!StringUtils.hasText(purchase.getMercadoPagoPreferenceId())) {
            return false;
        }
        if (purchase.getStatus() == PurchaseStatus.PENDING_PAYMENT
                || purchase.getStatus() == PurchaseStatus.APPROVED
                || purchase.getStatus() == PurchaseStatus.FAILED
                || purchase.getStatus() == PurchaseStatus.CANCELED) {
            return false;
        }
        if (!isMercadoPagoPreferenceReusable(purchase.getMercadoPagoPreferenceId())) {
            return false;
        }
        Instant expiresAt = purchase.getCheckoutExpiresAt();
        return expiresAt == null || expiresAt.isAfter(Instant.now());
    }

    private boolean isMercadoPagoPreferenceReusable(String preferenceId) {
        try {
            Optional<MercadoPagoPreferenceDetails> preference = mercadoPagoClient.fetchPreference(preferenceId);
            if (preference.isEmpty()) {
                return false;
            }
            MercadoPagoPreferenceDetails details = preference.get();
            if (details.expirationDateTo() != null && details.expirationDateTo().isBefore(Instant.now())) {
                return false;
            }
            return "active".equalsIgnoreCase(details.status());
        } catch (Exception ex) {
            log.warn("Falha ao consultar status da preferência {} no Mercado Pago", preferenceId, ex);
            return false;
        }
    }

    private Instant resolveCheckoutExpiration() {
        Duration ttl = paymentProperties.getCheckoutTtl();
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return null;
        }
        return Instant.now().plus(ttl);
    }

    private CreateCheckoutResponse toResponse(LeadPortalPurchase purchase) {
        return new CreateCheckoutResponse(
                purchase.getId(),
                purchase.getPackageId(),
                purchase.getMercadoPagoPreferenceId(),
                purchase.getCheckoutUrl(),
                purchase.getStatus() != null ? purchase.getStatus().name() : PurchaseStatus.PREFERENCE_CREATED.name(),
                purchase.getAmount(),
                purchase.getCurrency(),
                purchase.getCheckoutExpiresAt(),
                mercadoPagoProperties.getStatementDescriptor()
        );
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

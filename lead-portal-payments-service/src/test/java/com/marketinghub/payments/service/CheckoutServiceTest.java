package com.marketinghub.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.payments.config.MercadoPagoProperties;
import com.marketinghub.payments.config.PaymentProperties;
import com.marketinghub.payments.dto.CreateCheckoutRequest;
import com.marketinghub.payments.dto.CreateCheckoutResponse;
import com.marketinghub.payments.dto.LeadPortalPackageSummary;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoClient;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPreferenceDetails;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPreferenceResponse;
import com.marketinghub.payments.model.FlowSubmissionImagePackageStatus;
import com.marketinghub.payments.model.LeadPortalPurchase;
import com.marketinghub.payments.model.PurchaseStatus;
import com.marketinghub.payments.repository.LeadPortalPurchaseRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock
    private MercadoPagoClient mercadoPagoClient;

    @Mock
    private LeadPortalPackageGateway packageGateway;

    @Mock
    private LeadPortalPurchaseRepository purchaseRepository;

    private PaymentProperties paymentProperties;
    private MercadoPagoProperties mercadoPagoProperties;
    private CheckoutService checkoutService;

    @BeforeEach
    void setUp() {
        paymentProperties = new PaymentProperties();
        mercadoPagoProperties = new MercadoPagoProperties();
        checkoutService = new CheckoutService(
                mercadoPagoClient,
                packageGateway,
                purchaseRepository,
                mercadoPagoProperties,
                paymentProperties);
    }

    @Test
    void shouldCreateCheckoutWhenMercadoPagoReturnsInitPoint() {
        CreateCheckoutRequest request = new CreateCheckoutRequest();
        request.setPackageId(1L);
        request.setBuyerEmail("buyer@example.com");
        request.setBuyerName("Buyer");

        LeadPortalPackageSummary summary = new LeadPortalPackageSummary(
                1L,
                UUID.randomUUID(),
                "Submission",
                "submission@example.com",
                FlowSubmissionImagePackageStatus.COMPLETED,
                "prompt",
                "model",
                BigDecimal.TEN,
                "BRL",
                Instant.now());

        MercadoPagoPreferenceResponse mpResponse = new MercadoPagoPreferenceResponse(
                "pref-123",
                "https://mercadopago.com/checkout/123");

        when(packageGateway.loadPackage(1L)).thenReturn(summary);
        when(purchaseRepository.findTopByPackageIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());
        when(mercadoPagoClient.createPreference(any())).thenReturn(mpResponse);
        when(purchaseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreateCheckoutResponse response = checkoutService.createCheckout(request);

        assertThat(response.checkoutUrl()).isEqualTo(mpResponse.initPoint());
        verify(purchaseRepository).save(argThat(purchase -> mpResponse.initPoint().equals(purchase.getCheckoutUrl())));
    }

    @Test
    void shouldFailWhenMercadoPagoDoesNotReturnCheckoutUrl() {
        CreateCheckoutRequest request = new CreateCheckoutRequest();
        request.setPackageId(2L);

        LeadPortalPackageSummary summary = new LeadPortalPackageSummary(
                2L,
                UUID.randomUUID(),
                "Submission",
                "submission@example.com",
                FlowSubmissionImagePackageStatus.COMPLETED,
                "prompt",
                "model",
                BigDecimal.TEN,
                "BRL",
                Instant.now());

        when(packageGateway.loadPackage(2L)).thenReturn(summary);
        when(purchaseRepository.findTopByPackageIdOrderByCreatedAtDesc(2L)).thenReturn(Optional.empty());
        when(mercadoPagoClient.createPreference(any())).thenReturn(new MercadoPagoPreferenceResponse("pref-456", null));

        assertThatThrownBy(() -> checkoutService.createCheckout(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("link de checkout");
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void shouldCreateNewPreferenceWhenPreviousPurchaseIsPendingPayment() {
        CreateCheckoutRequest request = new CreateCheckoutRequest();
        request.setPackageId(3L);

        LeadPortalPackageSummary summary = new LeadPortalPackageSummary(
                3L,
                UUID.randomUUID(),
                "Submission",
                "submission@example.com",
                FlowSubmissionImagePackageStatus.COMPLETED,
                "prompt",
                "model",
                BigDecimal.TEN,
                "BRL",
                Instant.now());

        LeadPortalPurchase previousPurchase = new LeadPortalPurchase();
        previousPurchase.setPackageId(3L);
        previousPurchase.setStatus(PurchaseStatus.PENDING_PAYMENT);
        previousPurchase.setMercadoPagoPreferenceId("pref-old");
        previousPurchase.setCheckoutUrl("https://mercadopago.com/checkout/old");
        previousPurchase.setCheckoutExpiresAt(Instant.now().plusSeconds(300));

        MercadoPagoPreferenceResponse mpResponse = new MercadoPagoPreferenceResponse(
                "pref-789",
                "https://mercadopago.com/checkout/new");

        when(packageGateway.loadPackage(3L)).thenReturn(summary);
        when(purchaseRepository.findTopByPackageIdOrderByCreatedAtDesc(3L)).thenReturn(Optional.of(previousPurchase));
        when(mercadoPagoClient.createPreference(any())).thenReturn(mpResponse);
        when(purchaseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreateCheckoutResponse response = checkoutService.createCheckout(request);

        assertThat(response.checkoutUrl()).isEqualTo(mpResponse.initPoint());
        assertThat(response.preferenceId()).isEqualTo(mpResponse.id());
        verify(mercadoPagoClient).createPreference(any());
        verify(purchaseRepository).save(argThat(purchase ->
                mpResponse.initPoint().equals(purchase.getCheckoutUrl())
                        && mpResponse.id().equals(purchase.getMercadoPagoPreferenceId())
                        && purchase.getStatus() == PurchaseStatus.PREFERENCE_CREATED));
    }

    @Test
    void shouldCreateNewPreferenceWhenPreviousOneIsClosedInMercadoPago() {
        CreateCheckoutRequest request = new CreateCheckoutRequest();
        request.setPackageId(3L);

        LeadPortalPackageSummary summary = new LeadPortalPackageSummary(
                3L,
                UUID.randomUUID(),
                "Submission",
                "submission@example.com",
                FlowSubmissionImagePackageStatus.COMPLETED,
                "prompt",
                "model",
                BigDecimal.TEN,
                "BRL",
                Instant.now());

        LeadPortalPurchase previousPurchase = new LeadPortalPurchase();
        previousPurchase.setPackageId(3L);
        previousPurchase.setStatus(PurchaseStatus.PREFERENCE_CREATED);
        previousPurchase.setMercadoPagoPreferenceId("pref-old");
        previousPurchase.setCheckoutUrl("https://mercadopago.com/checkout/old");
        previousPurchase.setCheckoutExpiresAt(Instant.now().plusSeconds(300));

        MercadoPagoPreferenceResponse mpResponse = new MercadoPagoPreferenceResponse(
                "pref-new",
                "https://mercadopago.com/checkout/new");

        when(packageGateway.loadPackage(3L)).thenReturn(summary);
        when(purchaseRepository.findTopByPackageIdOrderByCreatedAtDesc(3L)).thenReturn(Optional.of(previousPurchase));
        when(mercadoPagoClient.fetchPreference("pref-old"))
                .thenReturn(Optional.of(new MercadoPagoPreferenceDetails("pref-old", "closed", null, Instant.now().plusSeconds(60))));
        when(mercadoPagoClient.createPreference(any())).thenReturn(mpResponse);
        when(purchaseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreateCheckoutResponse response = checkoutService.createCheckout(request);

        assertThat(response.preferenceId()).isEqualTo(mpResponse.id());
        assertThat(response.checkoutUrl()).isEqualTo(mpResponse.initPoint());
        verify(mercadoPagoClient).fetchPreference("pref-old");
        verify(mercadoPagoClient).createPreference(any());
        verify(purchaseRepository).save(argThat(purchase ->
                mpResponse.id().equals(purchase.getMercadoPagoPreferenceId())
                        && mpResponse.initPoint().equals(purchase.getCheckoutUrl())));
    }

    @Test
    void shouldCreateNewPreferenceWhenPreviousPurchaseIsApproved() {
        CreateCheckoutRequest request = new CreateCheckoutRequest();
        request.setPackageId(4L);

        LeadPortalPackageSummary summary = new LeadPortalPackageSummary(
                4L,
                UUID.randomUUID(),
                "Submission",
                "submission@example.com",
                FlowSubmissionImagePackageStatus.COMPLETED,
                "prompt",
                "model",
                BigDecimal.TEN,
                "BRL",
                Instant.now());

        LeadPortalPurchase previousPurchase = new LeadPortalPurchase();
        previousPurchase.setPackageId(4L);
        previousPurchase.setStatus(PurchaseStatus.APPROVED);
        previousPurchase.setMercadoPagoPreferenceId("pref-approved");
        previousPurchase.setMercadoPagoPaymentId("pay-123");
        previousPurchase.setCheckoutUrl("https://mercadopago.com/checkout/approved");
        previousPurchase.setCheckoutExpiresAt(Instant.now().plusSeconds(300));

        MercadoPagoPreferenceResponse mpResponse = new MercadoPagoPreferenceResponse(
                "pref-987",
                "https://mercadopago.com/checkout/new-approved");

        when(packageGateway.loadPackage(4L)).thenReturn(summary);
        when(purchaseRepository.findTopByPackageIdOrderByCreatedAtDesc(4L)).thenReturn(Optional.of(previousPurchase));
        when(mercadoPagoClient.createPreference(any())).thenReturn(mpResponse);
        when(purchaseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreateCheckoutResponse response = checkoutService.createCheckout(request);

        assertThat(response.checkoutUrl()).isEqualTo(mpResponse.initPoint());
        assertThat(response.preferenceId()).isEqualTo(mpResponse.id());
        verify(mercadoPagoClient).createPreference(any());
        verify(purchaseRepository).save(argThat(purchase ->
                mpResponse.initPoint().equals(purchase.getCheckoutUrl())
                        && mpResponse.id().equals(purchase.getMercadoPagoPreferenceId())
                        && purchase.getStatus() == PurchaseStatus.PREFERENCE_CREATED));
    }

    @Test
    void shouldRejectCheckoutWhenPackageIsNotReady() {
        CreateCheckoutRequest request = new CreateCheckoutRequest();
        request.setPackageId(5L);

        LeadPortalPackageSummary summary = new LeadPortalPackageSummary(
                5L,
                UUID.randomUUID(),
                "Submission",
                "submission@example.com",
                FlowSubmissionImagePackageStatus.PROCESSING,
                "prompt",
                "model",
                BigDecimal.TEN,
                "BRL",
                Instant.now());

        when(packageGateway.loadPackage(5L)).thenReturn(summary);

        assertThatThrownBy(() -> checkoutService.createCheckout(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Pacote ainda não está pronto");

        verify(mercadoPagoClient, never()).createPreference(any());
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void shouldSendAmountCurrencyAndMetadataToMercadoPago() {
        CreateCheckoutRequest request = new CreateCheckoutRequest();
        request.setPackageId(6L);
        request.setBuyerEmail("buyer@example.com");
        request.setBuyerName("Buyer");

        LeadPortalPackageSummary summary = new LeadPortalPackageSummary(
                6L,
                UUID.randomUUID(),
                "Submission",
                "submission@example.com",
                FlowSubmissionImagePackageStatus.COMPLETED,
                "prompt",
                "model",
                new BigDecimal("10"),
                "brl",
                Instant.now());

        MercadoPagoPreferenceResponse mpResponse = new MercadoPagoPreferenceResponse(
                "pref-amount",
                "https://mercadopago.com/checkout/amount");

        ArgumentCaptor<com.marketinghub.payments.integration.mercadopago.MercadoPagoPreferenceRequest> requestCaptor =
                ArgumentCaptor.forClass(com.marketinghub.payments.integration.mercadopago.MercadoPagoPreferenceRequest.class);

        when(packageGateway.loadPackage(6L)).thenReturn(summary);
        when(purchaseRepository.findTopByPackageIdOrderByCreatedAtDesc(6L)).thenReturn(Optional.empty());
        when(mercadoPagoClient.createPreference(requestCaptor.capture())).thenReturn(mpResponse);
        when(purchaseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        checkoutService.createCheckout(request);

        com.marketinghub.payments.integration.mercadopago.MercadoPagoPreferenceRequest sentRequest = requestCaptor.getValue();
        assertThat(sentRequest.items()).hasSize(1);
        assertThat(sentRequest.items().get(0).unitPrice()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(sentRequest.items().get(0).currencyId()).isEqualTo("BRL");
        assertThat(sentRequest.payer().email()).isEqualTo("buyer@example.com");
        assertThat(sentRequest.payer().name()).isEqualTo("Buyer");
        assertThat(sentRequest.metadata()).containsEntry("packageId", 6L);
        assertThat(sentRequest.metadata()).containsEntry("submissionId", summary.submissionId().toString());
        assertThat(sentRequest.metadata()).containsEntry("submissionEmail", summary.submissionEmail());
    }

    @Test
    void shouldUseDefaultAmountAndCurrencyWhenPackageDoesNotProvide() {
        CreateCheckoutRequest request = new CreateCheckoutRequest();
        request.setPackageId(7L);

        LeadPortalPackageSummary summary = new LeadPortalPackageSummary(
                7L,
                UUID.randomUUID(),
                "Submission",
                "submission@example.com",
                FlowSubmissionImagePackageStatus.COMPLETED,
                "prompt",
                "model",
                null,
                null,
                Instant.now());

        MercadoPagoPreferenceResponse mpResponse = new MercadoPagoPreferenceResponse(
                "pref-default",
                "https://mercadopago.com/checkout/default");

        ArgumentCaptor<com.marketinghub.payments.integration.mercadopago.MercadoPagoPreferenceRequest> requestCaptor =
                ArgumentCaptor.forClass(com.marketinghub.payments.integration.mercadopago.MercadoPagoPreferenceRequest.class);

        when(packageGateway.loadPackage(7L)).thenReturn(summary);
        when(purchaseRepository.findTopByPackageIdOrderByCreatedAtDesc(7L)).thenReturn(Optional.empty());
        when(mercadoPagoClient.createPreference(requestCaptor.capture())).thenReturn(mpResponse);
        when(purchaseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        checkoutService.createCheckout(request);

        com.marketinghub.payments.integration.mercadopago.MercadoPagoPreferenceRequest sentRequest = requestCaptor.getValue();
        assertThat(sentRequest.items().get(0).unitPrice()).isEqualByComparingTo(new BigDecimal("49.90"));
        assertThat(sentRequest.items().get(0).currencyId()).isEqualTo("BRL");
        assertThat(sentRequest.payer().email()).isEqualTo(summary.submissionEmail());
        assertThat(sentRequest.payer().name()).isEqualTo(summary.submissionName());
    }
}

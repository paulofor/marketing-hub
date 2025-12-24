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
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPreferenceResponse;
import com.marketinghub.payments.model.FlowSubmissionImagePackageStatus;
import com.marketinghub.payments.repository.LeadPortalPurchaseRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
}

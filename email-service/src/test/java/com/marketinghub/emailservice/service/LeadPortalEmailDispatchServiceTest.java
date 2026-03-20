package com.marketinghub.emailservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.emailservice.config.LeadPortalDispatchProperties;
import com.marketinghub.emailservice.config.LeadPortalPaymentLinkProperties;
import com.marketinghub.emailservice.leadportal.service.LeadPortalImagePackageExportItem;
import com.marketinghub.emailservice.leadportal.service.LeadPortalPackageNotificationService;
import com.marketinghub.emailservice.service.client.FlowSubmissionImagePackageStatus;
import com.marketinghub.emailservice.leadportal.service.LeadPortalTrackingLinkService;
import com.marketinghub.emailservice.settings.EmailSmtpConfigurationService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.HtmlUtils;

class LeadPortalEmailDispatchServiceTest {

    private LeadPortalEmailDispatchService service;
    private LeadPortalPaymentLinkProperties paymentLinkProperties;
    private LeadPortalTrackingLinkService trackingLinkService;

    @BeforeEach
    void setUp() {
        paymentLinkProperties = new LeadPortalPaymentLinkProperties();
        trackingLinkService = mock(LeadPortalTrackingLinkService.class);
        when(trackingLinkService.buildCheckoutTrackingUrl(any(), any()))
                .thenReturn(java.util.Optional.empty());
        when(trackingLinkService.trackingHost()).thenReturn(java.util.Optional.empty());
        service = new LeadPortalEmailDispatchService(
                mock(LeadPortalPackageNotificationService.class),
                mock(EmailSenderService.class),
                new LeadPortalDispatchProperties(true, 1, 1L, 1L, 1L),
                mock(EmailLogService.class),
                mock(TrackingPixelService.class),
                paymentLinkProperties,
                trackingLinkService,
                mock(EmailSmtpConfigurationService.class));
    }

    @Test
    void shouldNotAppendPaymentBlockWhenHtmlAlreadyContainsDirectCheckoutLink() {
        String checkoutUrl = "https://www.mercadopago.com.br/checkout?pref_id=abc123&foo=bar";
        LeadPortalImagePackageExportItem.PaymentInfo paymentInfo = samplePaymentInfo(checkoutUrl);
        LeadPortalImagePackageExportItem item = sampleItem(42L, "sub-1", paymentInfo);

        String htmlBody = "<html><body><a href=\"" + HtmlUtils.htmlEscape(checkoutUrl) + "\">Liberar</a></body></html>";
        String plainBody = "Acesse " + checkoutUrl;

        LeadPortalEmailDispatchService.PaymentBodies bodies =
                service.enrichWithPaymentLink(htmlBody, plainBody, item);

        assertThat(bodies.htmlBody())
                .contains(paymentLinkProperties.getEntrypointBaseUrl())
                .doesNotContain(checkoutUrl);
        assertThat(bodies.plainBody())
                .contains(paymentLinkProperties.getEntrypointBaseUrl())
                .doesNotContain(checkoutUrl);
    }

    @Test
    void shouldAppendPaymentBlockWhenTemplateDoesNotContainAnyCheckoutLink() {
        String checkoutUrl = "https://www.mercadopago.com.br/checkout?pref_id=xyz789";
        LeadPortalImagePackageExportItem.PaymentInfo paymentInfo = samplePaymentInfo(checkoutUrl);
        LeadPortalImagePackageExportItem item = sampleItem(77L, "sub-2", paymentInfo);

        String htmlBody = "<html><body>Sem link</body></html>";
        String plainBody = "Sem link";

        LeadPortalEmailDispatchService.PaymentBodies bodies =
                service.enrichWithPaymentLink(htmlBody, plainBody, item);

        assertThat(bodies.htmlBody()).contains(paymentLinkProperties.getEntrypointBaseUrl());
        assertThat(bodies.plainBody())
                .contains(paymentLinkProperties.getPlainTextIntro())
                .contains(paymentLinkProperties.getEntrypointBaseUrl());
    }

    @Test
    void replacesCheckoutLinksWithTrackingUrlWhenAvailable() {
        String checkoutUrl = "https://www.mercadopago.com.br/checkout?pref_id=track-123";
        LeadPortalImagePackageExportItem.PaymentInfo paymentInfo = samplePaymentInfo(checkoutUrl);
        LeadPortalImagePackageExportItem item = sampleItem(88L, "sub-3", paymentInfo);

        when(trackingLinkService.buildCheckoutTrackingUrl(paymentInfo.purchaseId(), item.submissionId()))
                .thenReturn(java.util.Optional.of("https://api.example.com/api/public/lead-portal/purchases/999/checkout"));
        when(trackingLinkService.trackingHost()).thenReturn(java.util.Optional.of("api.example.com"));

        String htmlBody = "<html><body><a href=\"" + HtmlUtils.htmlEscape(checkoutUrl) + "\">Pagar</a></body></html>";
        String plainBody = "Link direto: " + checkoutUrl;

        LeadPortalEmailDispatchService.PaymentBodies bodies =
                service.enrichWithPaymentLink(htmlBody, plainBody, item);

        assertThat(bodies.htmlBody()).contains("https://api.example.com/api/public/lead-portal/purchases/999/checkout");
        assertThat(bodies.htmlBody()).doesNotContain(checkoutUrl);
        assertThat(bodies.plainBody()).contains("https://api.example.com/api/public/lead-portal/purchases/999/checkout");
    }

    private LeadPortalImagePackageExportItem.PaymentInfo samplePaymentInfo(String checkoutUrl) {
        return new LeadPortalImagePackageExportItem.PaymentInfo(
                999L,
                checkoutUrl,
                new BigDecimal("127.00"),
                "BRL",
                Instant.now().plusSeconds(3600),
                "Mercado Pago");
    }

    private LeadPortalImagePackageExportItem sampleItem(long packageId, String submissionId,
                                                        LeadPortalImagePackageExportItem.PaymentInfo paymentInfo) {
        return new LeadPortalImagePackageExportItem(
                packageId,
                submissionId,
                null,
                null,
                FlowSubmissionImagePackageStatus.COMPLETED,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                null,
                null,
                null,
                null,
                0,
                null,
                null,
                null,
                paymentInfo);
    }
}

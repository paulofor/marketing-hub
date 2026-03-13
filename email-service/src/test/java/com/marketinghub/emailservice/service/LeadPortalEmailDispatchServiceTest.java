package com.marketinghub.emailservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.marketinghub.emailservice.config.LeadPortalDispatchProperties;
import com.marketinghub.emailservice.config.LeadPortalPaymentLinkProperties;
import com.marketinghub.emailservice.leadportal.service.LeadPortalImagePackageExportItem;
import com.marketinghub.emailservice.leadportal.service.LeadPortalPackageNotificationService;
import com.marketinghub.emailservice.settings.EmailSmtpConfigurationService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.HtmlUtils;

class LeadPortalEmailDispatchServiceTest {

    private LeadPortalEmailDispatchService service;
    private LeadPortalPaymentLinkProperties paymentLinkProperties;

    @BeforeEach
    void setUp() {
        paymentLinkProperties = new LeadPortalPaymentLinkProperties();
        service = new LeadPortalEmailDispatchService(
                mock(LeadPortalPackageNotificationService.class),
                mock(EmailSenderService.class),
                new LeadPortalDispatchProperties(true, 1, 1L, 1L, 1L),
                mock(EmailLogService.class),
                mock(TrackingPixelService.class),
                paymentLinkProperties,
                mock(EmailSmtpConfigurationService.class));
    }

    @Test
    void shouldNotAppendPaymentBlockWhenHtmlAlreadyContainsDirectCheckoutLink() {
        String checkoutUrl = "https://www.mercadopago.com.br/checkout?pref_id=abc123&foo=bar";
        LeadPortalImagePackageExportItem.PaymentInfo paymentInfo = samplePaymentInfo(checkoutUrl);

        String htmlBody = "<html><body><a href=\"" + HtmlUtils.htmlEscape(checkoutUrl) + "\">Liberar</a></body></html>";
        String plainBody = "Acesse " + checkoutUrl;

        LeadPortalEmailDispatchService.PaymentBodies bodies =
                service.enrichWithPaymentLink(htmlBody, plainBody, paymentInfo, 42L);

        assertThat(bodies.htmlBody()).isEqualTo(htmlBody);
        assertThat(bodies.plainBody()).isEqualTo(plainBody);
    }

    @Test
    void shouldAppendPaymentBlockWhenTemplateDoesNotContainAnyCheckoutLink() {
        String checkoutUrl = "https://www.mercadopago.com.br/checkout?pref_id=xyz789";
        LeadPortalImagePackageExportItem.PaymentInfo paymentInfo = samplePaymentInfo(checkoutUrl);

        String htmlBody = "<html><body>Sem link</body></html>";
        String plainBody = "Sem link";

        LeadPortalEmailDispatchService.PaymentBodies bodies =
                service.enrichWithPaymentLink(htmlBody, plainBody, paymentInfo, 77L);

        assertThat(bodies.htmlBody()).contains(paymentLinkProperties.getEntrypointBaseUrl());
        assertThat(bodies.plainBody())
                .contains(paymentLinkProperties.getPlainTextIntro())
                .contains(paymentLinkProperties.getEntrypointBaseUrl());
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
}

package com.marketinghub.emailservice.leadportal.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.cert.CertPathBuilderException;
import javax.net.ssl.SSLHandshakeException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

class LeadPortalPaymentsClientTest {

    @Test
    void shouldDetectTlsCertificateErrorsFromCauseChain() {
        SSLHandshakeException handshake = new SSLHandshakeException("PKIX path building failed");
        ResourceAccessException exception = new ResourceAccessException("I/O error", handshake);

        boolean result = LeadPortalPaymentsClient.hasTlsCertificateError(exception);

        assertThat(result).isTrue();
    }

    @Test
    void shouldDetectCertPathBuilderExceptionFromCauseChain() {
        CertPathBuilderException certPathError = new CertPathBuilderException("unable to build cert path");
        RuntimeException exception = new RuntimeException("wrapper", certPathError);

        boolean result = LeadPortalPaymentsClient.hasTlsCertificateError(exception);

        assertThat(result).isTrue();
    }

    @Test
    void shouldIgnoreNonTlsErrors() {
        RuntimeException exception = new RuntimeException("boom");

        boolean result = LeadPortalPaymentsClient.hasTlsCertificateError(exception);

        assertThat(result).isFalse();
    }
}

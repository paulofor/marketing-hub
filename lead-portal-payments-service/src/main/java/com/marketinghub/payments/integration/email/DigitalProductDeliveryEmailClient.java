package com.marketinghub.payments.integration.email;

import com.marketinghub.payments.config.DigitalProductEmailDeliveryProperties;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente HTTP responsável por solicitar ao email-service o envio de entrega digital.
 */
@Component
public class DigitalProductDeliveryEmailClient {

    private static final Logger log = LoggerFactory.getLogger(DigitalProductDeliveryEmailClient.class);

    private final DigitalProductEmailDeliveryProperties properties;

    public DigitalProductDeliveryEmailClient(DigitalProductEmailDeliveryProperties properties) {
        this.properties = properties;
    }

    /** Envia ao email-service uma solicitação de email pós-compra. */
    public DigitalProductDeliveryEmailResponse send(DigitalProductDeliveryEmailRequest request) {
        RestClient client = RestClient.builder()
                .baseUrl(properties.getEmailServiceBaseUrl())
                .requestFactory(requestFactory(properties.getConnectTimeout(), properties.getReadTimeout()))
                .build();
        try {
            log.info("Solicitando email de entrega digital (paymentId={}, externalReference={}, to={})",
                    request.paymentId(), request.externalReference(), request.to());
            return client.post()
                    .uri(properties.getSendPath())
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, response) -> {
                        throw new IllegalStateException("email-service retornou status " + response.getStatusCode());
                    })
                    .body(DigitalProductDeliveryEmailResponse.class);
        } catch (RestClientException ex) {
            log.error("Falha HTTP ao solicitar email de entrega digital (paymentId={}, endpoint={}{}).",
                    request.paymentId(), properties.getEmailServiceBaseUrl(), properties.getSendPath(), ex);
            throw new IllegalStateException("Falha ao chamar email-service", ex);
        }
    }

    /** Cria a fábrica HTTP com timeouts configuráveis. */
    private SimpleClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}

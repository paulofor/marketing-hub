package com.marketinghub.facebookadsworker.configuration;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

/** Limita esperas HTTP para que uma integração indisponível não paralise as rotinas do worker. */
@Configuration(proxyBeanMethods = false)
public class FacebookHttpClientConfiguration {

    /** Aplica limites de conexão e de inatividade da resposta, inclusive durante a leitura do corpo. */
    @Bean
    public WebClientCustomizer facebookHttpTimeouts(
            @Value("${facebook.http.connect-timeout:PT10S}") Duration connectTimeout,
            @Value("${facebook.http.response-timeout:PT60S}") Duration responseTimeout) {
        if (connectTimeout.isZero() || connectTimeout.isNegative()
                || responseTimeout.isZero() || responseTimeout.isNegative()) {
            throw new IllegalArgumentException("Os limites HTTP do Facebook devem ser positivos");
        }
        HttpClient client = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(connectTimeout.toMillis()))
                .responseTimeout(responseTimeout);
        return builder -> builder.clientConnector(new ReactorClientHttpConnector(client));
    }
}

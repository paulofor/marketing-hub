package com.marketinghub.openai;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/** Responsabilidade: configurar clientes HTTP usados na integração backend com a OpenAI. */
@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
public class OpenAiConfiguration {

    /** Cria o WebClient autenticado da OpenAI usando token direto ou arquivo seguro configurado. */
    @Bean(name = "openAiWebClient")
    public WebClient openAiWebClient(WebClient.Builder builder, OpenAiProperties properties, OpenAiApiKeyResolver apiKeyResolver) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) properties.getConnectTimeout().toMillis())
                .responseTimeout(properties.getRequestTimeout())
                .doOnConnected(conn -> {
                    Duration timeout = properties.getRequestTimeout();
                    int seconds = (int) Math.max(1, timeout.getSeconds());
                    conn.addHandlerLast(new ReadTimeoutHandler(seconds));
                    conn.addHandlerLast(new WriteTimeoutHandler(seconds));
                });

        WebClient.Builder configured = builder
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        String apiKey = apiKeyResolver.resolve(properties);
        if (StringUtils.hasText(apiKey)) {
            configured.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }

        return configured.build();
    }
}

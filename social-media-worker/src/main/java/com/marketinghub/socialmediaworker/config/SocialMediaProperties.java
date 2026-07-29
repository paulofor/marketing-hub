package com.marketinghub.socialmediaworker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Centraliza as configuracoes externas do worker de midias sociais.
 */
@ConfigurationProperties(prefix = "social")
public record SocialMediaProperties(Backend backend, Youtube youtube) {

    /**
     * Define valores padrao quando propriedades aninhadas nao forem informadas.
     */
    public SocialMediaProperties {
        backend = backend == null ? new Backend("http://191.252.181.168", "") : backend;
        youtube = youtube == null ? new Youtube(true, "", "private") : youtube;
    }

    /**
     * Representa a conexao do worker com o backend principal.
     */
    public record Backend(String baseUrl, String authToken) {}

    /**
     * Representa a configuracao operacional da integracao com YouTube.
     */
    public record Youtube(boolean dryRun, String accessToken, String defaultPrivacyStatus) {}
}

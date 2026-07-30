package com.marketinghub.tiktokadsworker;

import java.time.Instant;

/** Representa uma conta TikTok Ads cadastrada no módulo de integração. */
public class TiktokAccount {
    private Long id;
    private String name;
    private String advertiserId;
    private String accessToken;
    private String appId;
    private String clientKey;
    private String appSecret;
    private boolean metricsEnabled;
    private boolean publicationEnabled;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastDiagnosticAt;
    private String lastDiagnosticStatus;
    private String lastDiagnosticMessage;

    /** Retorna o identificador interno da conta. */
    public Long getId() {
        return id;
    }

    /** Define o identificador interno da conta. */
    public void setId(Long id) {
        this.id = id;
    }

    /** Retorna o nome operacional da conta. */
    public String getName() {
        return name;
    }

    /** Define o nome operacional da conta. */
    public void setName(String name) {
        this.name = name;
    }

    /** Retorna o advertiser ID usado pela API do TikTok. */
    public String getAdvertiserId() {
        return advertiserId;
    }

    /** Define o advertiser ID usado pela API do TikTok. */
    public void setAdvertiserId(String advertiserId) {
        this.advertiserId = advertiserId;
    }

    /** Retorna o access token armazenado para chamadas futuras. */
    public String getAccessToken() {
        return accessToken;
    }

    /** Define o access token armazenado para chamadas futuras. */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /** Retorna o app ID informado no TikTok for Business. */
    public String getAppId() {
        return appId;
    }

    /** Define o app ID informado no TikTok for Business. */
    public void setAppId(String appId) {
        this.appId = appId;
    }

    /** Retorna a client key do aplicativo TikTok. */
    public String getClientKey() {
        return clientKey;
    }

    /** Define a client key do aplicativo TikTok. */
    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }

    /** Retorna o app secret armazenado para OAuth futuro. */
    public String getAppSecret() {
        return appSecret;
    }

    /** Define o app secret armazenado para OAuth futuro. */
    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    /** Indica se a sincronização de métricas está liberada para a conta. */
    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    /** Define se a sincronização de métricas está liberada para a conta. */
    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }

    /** Indica se a publicação automática está liberada para a conta. */
    public boolean isPublicationEnabled() {
        return publicationEnabled;
    }

    /** Define se a publicação automática está liberada para a conta. */
    public void setPublicationEnabled(boolean publicationEnabled) {
        this.publicationEnabled = publicationEnabled;
    }

    /** Retorna quando a conta foi criada no módulo. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Define quando a conta foi criada no módulo. */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /** Retorna quando a conta foi atualizada pela última vez. */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Define quando a conta foi atualizada pela última vez. */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /** Retorna quando o último diagnóstico foi executado. */
    public Instant getLastDiagnosticAt() {
        return lastDiagnosticAt;
    }

    /** Define quando o último diagnóstico foi executado. */
    public void setLastDiagnosticAt(Instant lastDiagnosticAt) {
        this.lastDiagnosticAt = lastDiagnosticAt;
    }

    /** Retorna o status do último diagnóstico. */
    public String getLastDiagnosticStatus() {
        return lastDiagnosticStatus;
    }

    /** Define o status do último diagnóstico. */
    public void setLastDiagnosticStatus(String lastDiagnosticStatus) {
        this.lastDiagnosticStatus = lastDiagnosticStatus;
    }

    /** Retorna a mensagem do último diagnóstico. */
    public String getLastDiagnosticMessage() {
        return lastDiagnosticMessage;
    }

    /** Define a mensagem do último diagnóstico. */
    public void setLastDiagnosticMessage(String lastDiagnosticMessage) {
        this.lastDiagnosticMessage = lastDiagnosticMessage;
    }
}

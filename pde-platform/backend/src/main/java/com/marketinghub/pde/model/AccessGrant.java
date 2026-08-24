package com.marketinghub.pde.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Guarda um acesso liberado para uma cliente dentro da experiência PDE. */
public class AccessGrant {

    private final String token;
    private final String productSlug;
    private String email;
    private String source;
    private final Instant createdAt;
    private String experienceVersion;
    private Instant paidAt;
    private Instant expiresAt;
    private final Set<String> completedMissionIds = new LinkedHashSet<>();
    private final Map<String, Map<String, String>> missionInteractions = new LinkedHashMap<>();

    /** Cria um acesso liberado para produto, e-mail e origem informados. */
    public AccessGrant(String token, String productSlug, String email, String source, Instant createdAt) {
        this(token, productSlug, email, source, createdAt, "", null, null);
    }

    /** Cria um acesso preservando versão, pagamento e expiração contratual. */
    public AccessGrant(
            String token,
            String productSlug,
            String email,
            String source,
            Instant createdAt,
            String experienceVersion,
            Instant paidAt,
            Instant expiresAt) {
        this.token = token;
        this.productSlug = productSlug;
        this.email = email;
        this.source = source;
        this.createdAt = createdAt;
        this.experienceVersion = experienceVersion == null ? "" : experienceVersion;
        this.paidAt = paidAt;
        this.expiresAt = expiresAt;
    }

    /** Cria um acesso com progresso ja persistido anteriormente. */
    public AccessGrant(
            String token,
            String productSlug,
            String email,
            String source,
            Instant createdAt,
            Set<String> completedMissionIds) {
        this(token, productSlug, email, source, createdAt);
        if (completedMissionIds != null) {
            this.completedMissionIds.addAll(completedMissionIds);
        }
    }

    /** Cria um acesso com progresso e interações persistidas anteriormente. */
    public AccessGrant(
            String token,
            String productSlug,
            String email,
            String source,
            Instant createdAt,
            Set<String> completedMissionIds,
            Map<String, Map<String, String>> missionInteractions) {
        this(token, productSlug, email, source, createdAt, completedMissionIds);
        if (missionInteractions != null) {
            missionInteractions.forEach(this::saveMissionInteraction);
        }
    }

    /** Reconstrói um acesso versionado com prazo e progresso já persistidos. */
    public AccessGrant(
            String token,
            String productSlug,
            String email,
            String source,
            Instant createdAt,
            String experienceVersion,
            Instant paidAt,
            Instant expiresAt,
            Set<String> completedMissionIds,
            Map<String, Map<String, String>> missionInteractions) {
        this(token, productSlug, email, source, createdAt, experienceVersion, paidAt, expiresAt);
        if (completedMissionIds != null) {
            this.completedMissionIds.addAll(completedMissionIds);
        }
        if (missionInteractions != null) {
            missionInteractions.forEach(this::saveMissionInteraction);
        }
    }

    /** Retorna o token público de acesso da cliente. */
    public String getToken() {
        return token;
    }

    /** Retorna o produto liberado para a cliente. */
    public String getProductSlug() {
        return productSlug;
    }

    /** Retorna o e-mail da cliente. */
    public String getEmail() {
        return email;
    }

    /** Retorna a origem da liberação, como Pepper ou validação local. */
    public String getSource() {
        return source;
    }

    /** Atualiza a origem quando um acesso de entrada vira assinatura aprovada. */
    public void updateSource(String source) {
        this.source = source;
    }

    /** Vincula a versão comercial quando o acesso nasce em uma superfície versionada. */
    public void updateExperienceVersion(String experienceVersion) {
        if (experienceVersion != null && !experienceVersion.isBlank()) {
            this.experienceVersion = experienceVersion.trim();
        }
    }

    /** Registra a aprovação do pagamento e a data limite do acesso comprado. */
    public void activatePaidAccess(Instant paidAt, Instant expiresAt) {
        this.paidAt = paidAt;
        this.expiresAt = expiresAt;
    }

    /** Revoga a continuidade paga após reembolso confirmado sem apagar o histórico da cliente. */
    public void revokePaidAccess(Instant refundedAt) {
        this.source = "PEPPER_REFUNDED";
        this.expiresAt = refundedAt;
    }

    /** Corrige o e-mail do acesso após solicitação autenticada da titular. */
    public void updateEmail(String email) {
        this.email = email;
    }

    /** Remove dados de uso e invalida o acesso preservando somente auditoria anônima. */
    public void anonymizeForPrivacy(String anonymousEmail, String reason, Instant executedAt) {
        this.email = anonymousEmail;
        this.source = "PRIVACY_DELETED";
        this.paidAt = null;
        this.expiresAt = null;
        this.completedMissionIds.clear();
        this.missionInteractions.clear();
        this.missionInteractions.put("privacy", Map.of(
                "requestType", reason,
                "requestStatus", "COMPLETED",
                "executedAt", executedAt.toString()));
    }

    /** Retorna a data de criação do acesso. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Retorna a versão da experiência associada ao acesso. */
    public String getExperienceVersion() {
        return experienceVersion;
    }

    /** Retorna o instante em que o pagamento foi aprovado. */
    public Instant getPaidAt() {
        return paidAt;
    }

    /** Retorna o instante de expiração quando o produto possui prazo contratual. */
    public Instant getExpiresAt() {
        return expiresAt;
    }

    /** Marca uma missão como concluída. */
    public void completeMission(String missionId) {
        completedMissionIds.add(missionId);
    }

    /** Salva respostas da cliente para personalizar uma missão. */
    public void saveMissionInteraction(String missionId, Map<String, String> answers) {
        if (answers == null || answers.isEmpty()) {
            return;
        }
        missionInteractions.computeIfAbsent(missionId, ignored -> new LinkedHashMap<>()).putAll(answers);
    }

    /** Retorna as missões concluídas pela cliente. */
    public Set<String> getCompletedMissionIds() {
        return Set.copyOf(completedMissionIds);
    }

    /** Retorna as respostas salvas por missão. */
    public Map<String, Map<String, String>> getMissionInteractions() {
        Map<String, Map<String, String>> copy = new LinkedHashMap<>();
        missionInteractions.forEach((missionId, answers) -> copy.put(missionId, Map.copyOf(answers)));
        return Map.copyOf(copy);
    }
}

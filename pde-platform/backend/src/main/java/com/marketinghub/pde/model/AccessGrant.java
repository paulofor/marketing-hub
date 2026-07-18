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
    private final String email;
    private String source;
    private final Instant createdAt;
    private final Set<String> completedMissionIds = new LinkedHashSet<>();
    private final Map<String, Map<String, String>> missionInteractions = new LinkedHashMap<>();

    /** Cria um acesso liberado para produto, e-mail e origem informados. */
    public AccessGrant(String token, String productSlug, String email, String source, Instant createdAt) {
        this.token = token;
        this.productSlug = productSlug;
        this.email = email;
        this.source = source;
        this.createdAt = createdAt;
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

    /** Retorna a data de criação do acesso. */
    public Instant getCreatedAt() {
        return createdAt;
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

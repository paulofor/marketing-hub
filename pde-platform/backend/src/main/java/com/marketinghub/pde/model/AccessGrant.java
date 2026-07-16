package com.marketinghub.pde.model;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/** Guarda um acesso liberado para uma cliente dentro da experiência PDE. */
public class AccessGrant {

    private final String token;
    private final String productSlug;
    private final String email;
    private final String source;
    private final Instant createdAt;
    private final Set<String> completedMissionIds = new LinkedHashSet<>();

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

    /** Retorna a data de criação do acesso. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Marca uma missão como concluída. */
    public void completeMission(String missionId) {
        completedMissionIds.add(missionId);
    }

    /** Retorna as missões concluídas pela cliente. */
    public Set<String> getCompletedMissionIds() {
        return Set.copyOf(completedMissionIds);
    }
}

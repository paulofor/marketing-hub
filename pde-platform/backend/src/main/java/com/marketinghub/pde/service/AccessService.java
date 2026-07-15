package com.marketinghub.pde.service;

import com.marketinghub.pde.dto.AccessResponse;
import com.marketinghub.pde.dto.ProductExperienceResponse;
import com.marketinghub.pde.dto.WorkspaceResponse;
import com.marketinghub.pde.model.AccessGrant;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/** Controla liberação de acesso e progresso da cliente na experiência PDE. */
@Service
public class AccessService {

    private final ProductCatalogService productCatalogService;
    private final Map<String, AccessGrant> accessByToken = new ConcurrentHashMap<>();

    /** Recebe o catálogo para validar produtos antes de liberar acesso. */
    public AccessService(ProductCatalogService productCatalogService) {
        this.productCatalogService = productCatalogService;
    }

    /** Cria um acesso para um produto existente e retorna a URL da área da cliente. */
    public AccessResponse createAccess(String productSlug, String email, String source) {
        productCatalogService.getProduct(productSlug);
        String token = UUID.randomUUID().toString();
        AccessGrant grant = new AccessGrant(token, productSlug, email, source, Instant.now());
        accessByToken.put(token, grant);
        return new AccessResponse(token, productSlug, email, source, "/access/" + token);
    }

    /** Retorna a área de trabalho da cliente com produto e progresso atuais. */
    public WorkspaceResponse getWorkspace(String token) {
        AccessGrant grant = getGrant(token);
        ProductExperienceResponse product = productCatalogService.getProduct(grant.getProductSlug());
        Set<String> completedMissionIds = grant.getCompletedMissionIds();
        int totalMissions = product.missions().size();
        int completedMissions = completedMissionIds.size();
        int progressPercent = totalMissions == 0 ? 0 : Math.round((completedMissions * 100f) / totalMissions);
        return new WorkspaceResponse(
                product,
                grant.getEmail(),
                completedMissions,
                totalMissions,
                progressPercent,
                completedMissionIds.stream().toList());
    }

    /** Marca uma missão do produto como concluída após validar se ela existe. */
    public void completeMission(String token, String missionId) {
        AccessGrant grant = getGrant(token);
        ProductExperienceResponse product = productCatalogService.getProduct(grant.getProductSlug());
        boolean missionExists = product.missions().stream().anyMatch(mission -> mission.id().equals(missionId));
        if (!missionExists) {
            throw new IllegalArgumentException("Missao PDE nao encontrada: " + missionId);
        }
        grant.completeMission(missionId);
    }

    /** Busca o acesso pelo token ou falha quando ele não existir. */
    private AccessGrant getGrant(String token) {
        AccessGrant grant = accessByToken.get(token);
        if (grant == null) {
            throw new IllegalArgumentException("Acesso PDE nao encontrado");
        }
        return grant;
    }
}

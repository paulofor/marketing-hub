package com.marketinghub.pde.service;

import com.marketinghub.pde.dto.AccessResponse;
import com.marketinghub.pde.dto.ProductExperienceResponse;
import com.marketinghub.pde.dto.WorkspaceResponse;
import com.marketinghub.pde.model.AccessGrant;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Controla liberação de acesso e progresso da cliente na experiência PDE. */
@Service
public class AccessService {
    private static final Logger log = LoggerFactory.getLogger(AccessService.class);
    private static final TypeReference<Map<String, StoredAccessGrant>> STORE_TYPE = new TypeReference<>() {};

    private final ProductCatalogService productCatalogService;
    private final ObjectMapper objectMapper;
    private final Path storagePath;
    private final Map<String, AccessGrant> accessByToken = new ConcurrentHashMap<>();

    /** Recebe dependências e carrega os acessos persistidos em disco. */
    public AccessService(
            ProductCatalogService productCatalogService,
            ObjectMapper objectMapper,
            @Value("${pde.access.storage-path:/data/pde/access-grants.json}") String storagePath) {
        this.productCatalogService = productCatalogService;
        this.objectMapper = objectMapper;
        this.storagePath = Path.of(storagePath);
        loadPersistedAccess();
    }

    /** Cria um acesso para um produto existente e retorna a URL da área da cliente. */
    public AccessResponse createAccess(String productSlug, String email, String source) {
        productCatalogService.getProduct(productSlug);
        String token = UUID.randomUUID().toString();
        AccessGrant grant = new AccessGrant(token, productSlug, email, source, Instant.now());
        accessByToken.put(token, grant);
        persistAccess();
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
        persistAccess();
    }

    /** Busca o acesso pelo token ou falha quando ele não existir. */
    private AccessGrant getGrant(String token) {
        AccessGrant grant = accessByToken.get(token);
        if (grant == null) {
            throw new IllegalArgumentException("Acesso PDE nao encontrado");
        }
        return grant;
    }

    /** Carrega acessos persistidos para evitar perda de progresso em reinícios. */
    private void loadPersistedAccess() {
        if (!Files.exists(storagePath)) {
            return;
        }
        try {
            Map<String, StoredAccessGrant> stored = objectMapper.readValue(storagePath.toFile(), STORE_TYPE);
            stored.forEach((token, value) -> accessByToken.put(token, value.toAccessGrant(token)));
        } catch (Exception ex) {
            log.error("Falha ao carregar acessos PDE persistidos em {}", storagePath, ex);
            throw new IllegalStateException("Nao foi possivel carregar acessos PDE persistidos", ex);
        }
    }

    /** Persiste os acessos e progresso em arquivo JSON local. */
    private synchronized void persistAccess() {
        try {
            if (storagePath.getParent() != null) {
                Files.createDirectories(storagePath.getParent());
            }
            Map<String, StoredAccessGrant> stored = new LinkedHashMap<>();
            accessByToken.forEach((token, grant) -> stored.put(token, StoredAccessGrant.from(grant)));
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storagePath.toFile(), stored);
        } catch (IOException ex) {
            log.error("Falha ao persistir acessos PDE em {}", storagePath, ex);
            throw new IllegalStateException("Nao foi possivel persistir acesso PDE", ex);
        }
    }

    /** Representa o formato persistido do acesso para armazenamento em JSON. */
    private record StoredAccessGrant(
            String productSlug,
            String email,
            String source,
            String createdAt,
            List<String> completedMissionIds) {

        /** Converte o acesso em memoria para o formato persistido. */
        private static StoredAccessGrant from(AccessGrant grant) {
            return new StoredAccessGrant(
                    grant.getProductSlug(),
                    grant.getEmail(),
                    grant.getSource(),
                    grant.getCreatedAt().toString(),
                    grant.getCompletedMissionIds().stream().toList());
        }

        /** Reconstrói o acesso de memoria a partir do JSON salvo. */
        private AccessGrant toAccessGrant(String token) {
            return new AccessGrant(
                    token,
                    productSlug,
                    email,
                    source,
                    Instant.parse(createdAt),
                    completedMissionIds != null ? Set.copyOf(completedMissionIds) : Set.of());
        }
    }
}

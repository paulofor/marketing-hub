package com.marketinghub.pde.support;

import com.marketinghub.pde.dto.BuildIdentityResponse;
import com.marketinghub.pde.service.DeployStatusService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/** Publica a identidade da build PDE no Actuator para auditoria via MCP. */
@Component
public class PdeBuildIdentityInfoContributor implements InfoContributor {

    private final DeployStatusService deployStatusService;

    /** Recebe o serviço que conhece os metadados de deploy do PDE. */
    public PdeBuildIdentityInfoContributor(DeployStatusService deployStatusService) {
        this.deployStatusService = deployStatusService;
    }

    /** Adiciona versão, commit, branch, imagem e ambiente ao endpoint `/actuator/info`. */
    @Override
    public void contribute(Info.Builder builder) {
        BuildIdentityResponse identity = deployStatusService.buildIdentity();
        builder.withDetail("build", buildDetails(identity));
        builder.withDetail("git", gitDetails(identity));
        builder.withDetail("pde", pdeDetails(identity));
    }

    /** Monta o bloco `build` no formato lido pelo MCP. */
    private Map<String, Object> buildDetails(BuildIdentityResponse identity) {
        Map<String, Object> details = new LinkedHashMap<>();
        putIfPresent(details, "version", identity.buildVersion());
        putIfPresent(details, "artifact", identity.artifact());
        putIfPresent(details, "name", identity.applicationName());
        if (identity.deployedAt() != null) {
            details.put("time", identity.deployedAt().toString());
        }
        return details;
    }

    /** Monta o bloco `git` no formato lido pelo MCP. */
    private Map<String, Object> gitDetails(BuildIdentityResponse identity) {
        Map<String, Object> commit = new LinkedHashMap<>();
        putIfPresent(commit, "id", identity.commitSha());
        putIfPresent(commit, "id.abbrev", abbreviate(identity.commitSha()));

        Map<String, Object> details = new LinkedHashMap<>();
        putIfPresent(details, "branch", identity.branch());
        details.put("commit", commit);
        return details;
    }

    /** Monta o bloco específico do PDE para auditoria comercial do cockpit. */
    private Map<String, Object> pdeDetails(BuildIdentityResponse identity) {
        Map<String, Object> details = new LinkedHashMap<>();
        putIfPresent(details, "environment", identity.environment());
        putIfPresent(details, "imageTag", identity.imageTag());
        putIfPresent(details, "backendImage", identity.backendImage());
        putIfPresent(details, "backendUrl", identity.backendUrl());
        putIfPresent(details, "frontendUrl", identity.frontendUrl());
        putIfPresent(details, "marketingHubBaseUrl", identity.marketingHubBaseUrl());
        return details;
    }

    /** Adiciona texto ao mapa somente quando há valor útil. */
    private void putIfPresent(Map<String, Object> details, String key, String value) {
        if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
            details.put(key, value);
        }
    }

    /** Gera o hash curto usado em diagnósticos humanos. */
    private String abbreviate(String commitSha) {
        if (commitSha == null || commitSha.isBlank() || "unknown".equalsIgnoreCase(commitSha)) {
            return "";
        }
        return commitSha.length() <= 7 ? commitSha : commitSha.substring(0, 7);
    }
}

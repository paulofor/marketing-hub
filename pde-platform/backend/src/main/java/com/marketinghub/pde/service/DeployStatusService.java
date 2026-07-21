package com.marketinghub.pde.service;

import com.marketinghub.pde.dto.DeployServiceStatusResponse;
import com.marketinghub.pde.dto.DeployStatusResponse;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Consolida o manifesto de deploy PDE informado pelo pipeline versionado. */
@Service
public class DeployStatusService {

    private static final Logger log = LoggerFactory.getLogger(DeployStatusService.class);

    private final String environment;
    private final String composeFile;
    private final String commitSha;
    private final String imageTag;
    private final String experienceVersion;
    private final String frontendUrl;
    private final String backendUrl;
    private final Instant deployedAt;
    private final String backendImage;
    private final String frontendImage;
    private final String aiWorkerImage;
    private final int backendPublicPort;
    private final int frontendPublicPort;

    /** Recebe os metadados de deploy publicados como variáveis de ambiente. */
    public DeployStatusService(
            @Value("${pde.deploy.environment:local}") String environment,
            @Value("${pde.deploy.compose-file:docker-compose.yml}") String composeFile,
            @Value("${pde.deploy.commit-sha:unknown}") String commitSha,
            @Value("${pde.deploy.image-tag:unknown}") String imageTag,
            @Value("${pde.catalog.experience-version-override:}") String experienceVersion,
            @Value("${pde.deploy.frontend-url:http://localhost:5176}") String frontendUrl,
            @Value("${pde.deploy.backend-url:http://localhost:8096}") String backendUrl,
            @Value("${pde.deploy.deployed-at:}") String deployedAt,
            @Value("${pde.deploy.backend-image:}") String backendImage,
            @Value("${pde.deploy.frontend-image:}") String frontendImage,
            @Value("${pde.deploy.ai-worker-image:}") String aiWorkerImage,
            @Value("${pde.deploy.backend-public-port:8096}") int backendPublicPort,
            @Value("${pde.deploy.frontend-public-port:5176}") int frontendPublicPort) {
        this.environment = environment;
        this.composeFile = composeFile;
        this.commitSha = commitSha;
        this.imageTag = imageTag;
        this.experienceVersion = experienceVersion;
        this.frontendUrl = frontendUrl;
        this.backendUrl = backendUrl;
        this.deployedAt = parseInstant(deployedAt);
        this.backendImage = backendImage;
        this.frontendImage = frontendImage;
        this.aiWorkerImage = aiWorkerImage;
        this.backendPublicPort = backendPublicPort;
        this.frontendPublicPort = frontendPublicPort;
    }

    /** Retorna o manifesto de ambiente e serviços para o painel do Marketing Hub. */
    public DeployStatusResponse currentStatus() {
        return new DeployStatusResponse(
                environment,
                composeFile,
                commitSha,
                imageTag,
                experienceVersion,
                frontendUrl,
                backendUrl,
                deployedAt,
                services());
    }

    /** Lista os containers esperados pela stack publicada do PDE. */
    private List<DeployServiceStatusResponse> services() {
        String suffix = "homolog".equalsIgnoreCase(environment) ? "-homolog" : "";
        return List.of(
                new DeployServiceStatusResponse(
                        "pde-platform-backend",
                        "pde-platform-backend" + suffix,
                        backendImage,
                        backendPublicPort,
                        8096,
                        "backend"),
                new DeployServiceStatusResponse(
                        "pde-platform-frontend",
                        "pde-platform-frontend" + suffix,
                        frontendImage,
                        frontendPublicPort,
                        80,
                        "frontend"),
                new DeployServiceStatusResponse(
                        "pde-ai-worker",
                        "pde-ai-worker" + suffix,
                        aiWorkerImage,
                        null,
                        null,
                        "worker"));
    }

    /** Converte o horário de deploy do pipeline sem quebrar ambientes locais. */
    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (RuntimeException ex) {
            log.warn("Horário de deploy PDE inválido; value={}", value, ex);
            return null;
        }
    }
}

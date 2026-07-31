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
    private final String frontendV5Image;
    private final String frontendV6Image;
    private final String frontendV7Image;
    private final String aiWorkerImage;
    private final int backendPublicPort;
    private final int frontendV5PublicPort;
    private final int frontendV6PublicPort;
    private final int frontendV7PublicPort;
    private final PdeOperationalHealthService operationalHealthService;

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
            @Value("${pde.deploy.frontend-v5-image:}") String frontendV5Image,
            @Value("${pde.deploy.frontend-v6-image:}") String frontendV6Image,
            @Value("${pde.deploy.frontend-v7-image:}") String frontendV7Image,
            @Value("${pde.deploy.ai-worker-image:}") String aiWorkerImage,
            @Value("${pde.deploy.backend-public-port:8096}") int backendPublicPort,
            @Value("${pde.deploy.frontend-v5-public-port:5176}") int frontendV5PublicPort,
            @Value("${pde.deploy.frontend-v6-public-port:5177}") int frontendV6PublicPort,
            @Value("${pde.deploy.frontend-v7-public-port:5178}") int frontendV7PublicPort,
            PdeOperationalHealthService operationalHealthService) {
        this.environment = environment;
        this.composeFile = composeFile;
        this.commitSha = commitSha;
        this.imageTag = imageTag;
        this.experienceVersion = experienceVersion;
        this.frontendUrl = frontendUrl;
        this.backendUrl = backendUrl;
        this.deployedAt = parseInstant(deployedAt);
        this.backendImage = backendImage;
        this.frontendV5Image = frontendV5Image;
        this.frontendV6Image = frontendV6Image;
        this.frontendV7Image = frontendV7Image;
        this.aiWorkerImage = aiWorkerImage;
        this.backendPublicPort = backendPublicPort;
        this.frontendV5PublicPort = frontendV5PublicPort;
        this.frontendV6PublicPort = frontendV6PublicPort;
        this.frontendV7PublicPort = frontendV7PublicPort;
        this.operationalHealthService = operationalHealthService;
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
                services(),
                operationalHealthService.schemaStatus(),
                operationalHealthService.operationalAlerts());
    }

    /** Lista os containers esperados pela stack publicada do PDE. */
    private List<DeployServiceStatusResponse> services() {
        return List.of(
                new DeployServiceStatusResponse(
                        "pde-platform-backend",
                        "pde-platform-backend",
                        backendImage,
                        backendPublicPort,
                        8096,
                        "backend"),
                new DeployServiceStatusResponse(
                        "pde-platform-frontend-v5",
                        "pde-platform-frontend-v5",
                        frontendV5Image,
                        frontendV5PublicPort,
                        80,
                        "frontend-v5"),
                new DeployServiceStatusResponse(
                        "pde-platform-frontend-v6",
                        "pde-platform-frontend-v6",
                        frontendV6Image,
                        frontendV6PublicPort,
                        80,
                        "frontend-v6"),
                new DeployServiceStatusResponse(
                        "pde-platform-frontend-v7",
                        "pde-platform-frontend-v7",
                        frontendV7Image,
                        frontendV7PublicPort,
                        80,
                        "frontend-v7"),
                new DeployServiceStatusResponse(
                        "pde-ai-worker",
                        "pde-ai-worker",
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

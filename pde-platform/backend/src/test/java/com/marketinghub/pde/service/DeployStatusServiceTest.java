package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Valida a identidade de build publicada pelo backend PDE. */
class DeployStatusServiceTest {

    /** Confirma que a identidade carrega commit, imagem e backend administrativo configurados. */
    @Test
    void buildIdentityExposesDeployMetadataForCockpitAudit() {
        DeployStatusService service = new DeployStatusService(
                "production",
                "pde-platform-backend",
                "pde-platform-backend",
                "0.0.1-SNAPSHOT",
                "docker-compose.deploy.yml",
                "abc123",
                "main",
                "pde-v6-abc123",
                "musa-pde-entry-v6-video-motivacional",
                "https://v6.clubemusa.com.br",
                "http://163.245.200.7:8096",
                "http://191.252.181.168:8000,http://191.252.181.168",
                "2026-07-31T10:00:00Z",
                "registry/pde-platform-backend:pde-v6-abc123",
                "",
                "registry/pde-platform-frontend:v6-abc123",
                "",
                "registry/pde-ai-worker:abc123",
                8096,
                5176,
                5177,
                5178,
                mock(PdeOperationalHealthService.class));

        var identity = service.buildIdentity();

        assertThat(identity.commitSha()).isEqualTo("abc123");
        assertThat(identity.branch()).isEqualTo("main");
        assertThat(identity.backendImage()).isEqualTo("registry/pde-platform-backend:pde-v6-abc123");
        assertThat(identity.frontendUrl()).isEqualTo("https://v6.clubemusa.com.br");
        assertThat(identity.marketingHubBaseUrl()).contains("191.252.181.168");
        assertThat(identity.deployedAt()).isEqualTo(Instant.parse("2026-07-31T10:00:00Z"));
    }
}

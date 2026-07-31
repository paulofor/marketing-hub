package com.marketinghub.pde.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.marketinghub.pde.service.DeployStatusService;
import com.marketinghub.pde.service.PdeOperationalHealthService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;

/** Valida a identidade de build publicada pelo Actuator do backend PDE. */
class PdeBuildIdentityInfoContributorTest {

    /** Confirma que `/actuator/info` publica campos lidos pela tool `runtime_build_info`. */
    @Test
    void shouldPublishMcpReadableBuildIdentity() {
        DeployStatusService service = new DeployStatusService(
                "production",
                "pde-platform-backend",
                "pde-platform-backend",
                "0.0.1-SNAPSHOT",
                "docker-compose.deploy.yml",
                "abcdef123456",
                "main",
                "abcdef123456",
                "musa-pde-entry-v6-video-motivacional",
                "https://v6.clubemusa.com.br",
                "http://163.245.200.7:8096",
                "http://191.252.181.168",
                "2026-07-31T10:00:00Z",
                "registry/pde-platform-backend:abcdef123456",
                "",
                "registry/pde-platform-frontend-v6:abcdef123456",
                "",
                "registry/pde-ai-worker:abcdef123456",
                8096,
                5176,
                5177,
                5178,
                mock(PdeOperationalHealthService.class));
        Info.Builder builder = new Info.Builder();

        new PdeBuildIdentityInfoContributor(service).contribute(builder);

        Map<String, Object> details = builder.build().getDetails();
        assertThat(details).containsKeys("build", "git", "pde");
        assertThat(details).extractingByKey("build").asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("version", "0.0.1-SNAPSHOT")
                .containsEntry("time", "2026-07-31T10:00:00Z");
        assertThat(details).extractingByKey("git").asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("branch", "main");
        @SuppressWarnings("unchecked")
        Map<String, Object> commit = (Map<String, Object>) ((Map<?, ?>) details.get("git")).get("commit");
        assertThat(commit)
                .containsEntry("id", "abcdef123456")
                .containsEntry("id.abbrev", "abcdef1");
    }
}

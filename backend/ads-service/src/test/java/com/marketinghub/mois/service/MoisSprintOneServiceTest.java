package com.marketinghub.mois.service;

import com.marketinghub.mois.dto.MoisWorkspaceDtos;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoisSprintOneServiceTest {

    private final MoisSprintOneService service = new MoisSprintOneService();

    @Test
    void shouldCreateCollectionJobWithDefaults() {
        MoisWorkspaceDtos.CollectionJobResponse job = service.createCollectionJob(
                new MoisWorkspaceDtos.CreateCollectionJobRequest(
                        "workspace-001",
                        "nutricao",
                        "perda de gordura",
                        List.of("META_AD_LIBRARY", "CLICKBANK"),
                        "LAST_7_DAYS",
                        null,
                        "pt-BR",
                        "BR",
                        null
                )
        );

        assertThat(job.jobId()).isNotBlank();
        assertThat(job.status()).isEqualTo("QUEUED");
        assertThat(job.limitPerSource()).isEqualTo(50);
        assertThat(job.minSuccessScore()).isEqualTo(50);
        assertThat(job.sources()).containsExactly("META_AD_LIBRARY", "CLICKBANK");
    }

    @Test
    void shouldListReferencesForCollectionJob() {
        MoisWorkspaceDtos.CollectionJobResponse job = service.createCollectionJob(
                new MoisWorkspaceDtos.CreateCollectionJobRequest(
                        "workspace-001",
                        "nutricao",
                        "perda de gordura",
                        List.of("CLICKBANK", "JVZOO"),
                        "LAST_30_DAYS",
                        20,
                        "pt-BR",
                        "BR",
                        65
                )
        );

        MoisWorkspaceDtos.CollectedReferenceListResponse result = service.listCollectedReferencesByJob(job.jobId());

        assertThat(result.jobId()).isEqualTo(job.jobId());
        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).source()).isEqualTo("CLICKBANK");
        assertThat(result.items().get(0).successScore()).isGreaterThanOrEqualTo(65);
    }

    @Test
    void shouldFailWhenCollectionJobDoesNotExist() {
        assertThatThrownBy(() -> service.listCollectedReferencesByJob("job-missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("collection job not found");
    }
}

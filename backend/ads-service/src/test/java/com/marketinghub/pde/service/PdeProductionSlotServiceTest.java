package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotRequestDto;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: validar regras de cadastro de versões produtivas PDE por produto. */
@ExtendWith(MockitoExtension.class)
class PdeProductionSlotServiceTest {

    @Mock
    private PdeProductionSlotRepository repository;

    /** Deve normalizar domínio e URL ao salvar uma versão PDE do produto. */
    @Test
    void savesProductPdeProductionSlotWithNormalizedDomain() {
        PdeProductionSlotService service = new PdeProductionSlotService(repository);
        when(repository.findByProductSlugAndSlotCode("metodo-musa-7-dias", "v2"))
                .thenReturn(Optional.empty());
        when(repository.save(org.mockito.ArgumentMatchers.any(PdeProductionSlot.class)))
                .thenAnswer(invocation -> {
                    PdeProductionSlot slot = invocation.getArgument(0);
                    slot.setId(2L);
                    slot.setCreatedAt(Instant.parse("2026-07-24T10:00:00Z"));
                    slot.setUpdatedAt(Instant.parse("2026-07-24T10:00:00Z"));
                    return slot;
                });

        var response = service.saveProductionSlot(
                "metodo-musa-7-dias",
                71L,
                new PostDeployPdeProductionSlotRequestDto(
                        "v2",
                        null,
                        "https://v2.clubemusa.com.br/",
                        null,
                        null,
                        "musa-pde-entry-v5-estrada-desejo",
                        null,
                        PdeProductionSlotStatus.PLANNED,
                        null,
                        "Hipotese 2"));

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.productSlug()).isEqualTo("metodo-musa-7-dias");
        assertThat(response.domain()).isEqualTo("v2.clubemusa.com.br");
        assertThat(response.publicUrl()).isEqualTo("https://v2.clubemusa.com.br");
        assertThat(response.targetEnvironment()).isEqualTo("production-v2");
        assertThat(response.sourceExperimentId()).isEqualTo(71L);
    }
}

package com.marketinghub.pde.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.marketinghub.pde.service.AccessService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: proteger o endpoint interno dos marcos operacionais do PDE assistido. */
class AssistedOperationControllerTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withBean(AccessService.class, () -> mock(AccessService.class))
            .withUserConfiguration(AssistedOperationController.class);

    /** Mantém o endpoint operacional ausente quando o ambiente não o habilita explicitamente. */
    @Test
    void disablesAssistedOperationByDefault() {
        context.run(result -> assertThat(result).doesNotHaveBean(AssistedOperationController.class));
    }

    /** Habilita o endpoint operacional somente com configuração e segredo explícitos. */
    @Test
    void enablesAssistedOperationExplicitly() {
        context.withPropertyValues(
                        "pde.assisted-operation.enabled=true",
                        "pde.assisted-operation.token=segredo-local")
                .run(result -> assertThat(result).hasSingleBean(AssistedOperationController.class));
    }

    /** Rejeita um token incorreto antes de alterar o progresso da cliente. */
    @Test
    void rejectsInvalidOperationToken() {
        AccessService accessService = mock(AccessService.class);
        AssistedOperationController controller = new AssistedOperationController(accessService, "segredo-local");

        assertThatThrownBy(() -> controller.completeOperationalMission("acesso", "diagnostico", "incorreto", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
        verifyNoInteractions(accessService);
    }
}

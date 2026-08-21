package com.marketinghub.pde.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.marketinghub.pde.service.AccessService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Responsabilidade: proteger o acesso de homologação contra exposição em ambientes não autorizados. */
class DevAccessControllerTest {
    private final ApplicationContextRunner context =
            new ApplicationContextRunner()
                    .withBean(AccessService.class, () -> mock(AccessService.class))
                    .withUserConfiguration(DevAccessController.class);

    /** Mantém o endpoint DEV ausente quando nenhuma autorização explícita foi configurada. */
    @Test
    void disablesDevAccessByDefault() {
        context.run(result -> assertThat(result).doesNotHaveBean(DevAccessController.class));
    }

    /** Habilita o endpoint DEV somente na topologia segregada de homologação. */
    @Test
    void enablesDevAccessOnlyWhenExplicitlyConfigured() {
        context
                .withPropertyValues("pde.access.dev-enabled=true")
                .run(result -> assertThat(result).hasSingleBean(DevAccessController.class));
    }
}

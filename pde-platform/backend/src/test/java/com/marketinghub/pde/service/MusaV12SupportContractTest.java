package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Responsabilidade: comprovar a rota persistente de suporte da versão comercial MUSA v12. */
class MusaV12SupportContractTest {
    private static final String PRODUCT_SLUG = "metodo-musa-7-dias";
    private static final String EXPERIENCE_VERSION =
            "musa-pde-entry-v12-primeiro-ajuste-aplicavel";

    @TempDir
    Path tempDir;

    /** Preserva o pedido de suporte no mesmo acesso v12 após reiniciar o serviço. */
    @Test
    void persistsSupportRequestForExactV12Access() {
        var catalog = new ProductCatalogService();
        String storagePath = tempDir.resolve("musa-v12-support.json").toString();
        var service = new AccessService(catalog, new ObjectMapper(), storagePath);
        var access = service.createInternalQaAccess(
                PRODUCT_SLUG, "teste+vega-support@sandbox.local", EXPERIENCE_VERSION);

        var support = service.requestSupport(
                access.token(), "Preciso de ajuda para recuperar meu acesso e revisar a entrega.");
        var restarted = new AccessService(catalog, new ObjectMapper(), storagePath);
        var workspace = restarted.getWorkspace(access.token());

        assertThat(support.supportStatus()).isEqualTo("OPEN");
        assertThat(workspace.experienceVersion()).isEqualTo(EXPERIENCE_VERSION);
        assertThat(workspace.supportStatus()).isEqualTo("OPEN");
    }

    /** Recusa pedido vazio sem apagar o acesso ou inventar atendimento concluído. */
    @Test
    void rejectsEmptySupportRequestWithoutChangingWorkspace() {
        var service = new AccessService(
                new ProductCatalogService(),
                new ObjectMapper(),
                tempDir.resolve("musa-v12-empty-support.json").toString());
        var access = service.createInternalQaAccess(
                PRODUCT_SLUG, "teste+vega-empty-support@sandbox.local", EXPERIENCE_VERSION);

        assertThatThrownBy(() -> service.requestSupport(access.token(), "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("suporte necessário");
        assertThat(service.getWorkspace(access.token()).supportStatus()).isEqualTo("NONE");
    }
}

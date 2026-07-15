package com.marketinghub.aiprompt;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger contratos Liquibase dos templates de prompt e schema de IA. */
class AiPromptSchemaTemplateChangelogTest {
    private static final Path PROMPT_SCHEMA_CHANGELOG = Path.of(
            "src/main/resources/db/changelog/changesets/2026-07-02-experiment-ai-prompt-schema-usage.yaml");
    private static final Path GERA_SALES_PAGE_RECOVERY_CHANGELOG = Path.of(
            "src/main/resources/db/changelog/changesets/2026-07-15-gera-sales-page-template-recovery-v7.yaml");
    private static final Path GERA_SALES_PAGE_EXP66_DELIVERY_CHANGELOG = Path.of(
            "src/main/resources/db/changelog/changesets/2026-07-15-gera-sales-page-exp66-delivery-v8.yaml");

    /** Garante que a etapa Prova use o mesmo campo de evidencias aceito pelo AI Worker. */
    @Test
    void hypothesisProofTemplateShouldUseEvidenceSignals() throws IOException {
        String changelog = Files.readString(PROMPT_SCHEMA_CHANGELOG, StandardCharsets.UTF_8);
        String changeSetId = "2026-07-03-experiment-ai-prompt-schema-usage-proof-evidence-signals";
        String correctionChangeSet = changelog.substring(changelog.indexOf(changeSetId));

        assertThat(correctionChangeSet)
                .contains(changeSetId)
                .contains("\"evidenceSignals\"")
                .doesNotContain("\"proofSignals\"");
    }

    /** Garante que o recovery do GeraSalesPage recrie todas as etapas comerciais com pacote FEO real. */
    @Test
    void geraSalesPageRecoveryShouldSeedAllCommercialTemplates() throws IOException {
        String changelog = Files.readString(GERA_SALES_PAGE_RECOVERY_CHANGELOG, StandardCharsets.UTF_8);

        assertThat(changelog)
                .contains("gera-sales-page-v1:sales-page-offer-brief:v7")
                .contains("gera-sales-page-v1:sales-page-wireframe:v7")
                .contains("gera-sales-page-v1:sales-page-copy:v7")
                .contains("gera-sales-page-v1:sales-page-visual-plan:v7")
                .contains("gera-sales-page-v1:sales-page-html:v7")
                .contains("gera-sales-page-v1:sales-page-checkout-quality-review:v7")
                .contains("gera-sales-page-v1:sales-page-publication-package:v7")
                .contains("experiment.feoDeliverablePackage")
                .contains("DIRECT_CHECKOUT")
                .contains("data-transform-visual")
                .contains("ON DUPLICATE KEY UPDATE");
    }

    /** Garante que a correção do experimento 66 bloqueia linguagem interna e explica entrega digital. */
    @Test
    void geraSalesPageExp66DeliveryShouldExplainZipDeliveryWithoutInternalTerms() throws IOException {
        String changelog = Files.readString(GERA_SALES_PAGE_EXP66_DELIVERY_CHANGELOG, StandardCharsets.UTF_8);

        assertThat(changelog)
                .contains("gera-sales-page-v1:sales-page-copy:v8")
                .contains("gera-sales-page-v1:sales-page-html:v8")
                .contains("gera-sales-page-v1:sales-page-checkout-quality-review:v8")
                .contains("gera-sales-page-v1:sales-page-publication-package:v8")
                .contains("https://pagamentopalf.site/obrigado-exp66-metodo-musa.html")
                .contains("https://pagamentopalf.site/downloads/experimento-66-entregaveis.zip")
                .contains("Bloqueie qualquer página que exponha termos internos")
                .contains("após pagamento aprovado no Mercado Pago")
                .contains("ON DUPLICATE KEY UPDATE");
    }
}

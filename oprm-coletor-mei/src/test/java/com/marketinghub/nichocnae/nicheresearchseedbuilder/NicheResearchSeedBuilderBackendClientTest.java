package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.oprmcoletormei.marketimport.config.OprmMarketImportCollectorProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** Valida o contrato entre o coletor OPRM e o backend para conclusão da etapa dois. */
class NicheResearchSeedBuilderBackendClientTest {
    /** Deve achatar o seed gerado pela IA no DTO esperado pelo backend. */
    @Test
    void toBackendCompletionRequestFlattensSeedFieldsForBackendContract() {
        NicheResearchSeedBuilderBackendClient client = client();
        NicheResearchSeed seed = new NicheResearchSeed(
                1L,
                "9602501",
                "Cabeleireiros, manicure e pedicure",
                "Cabeleireiros MEI e profissionais autônomos",
                "Serviços de beleza",
                "Atendimento presencial com agenda recorrente",
                "Consumidores finais",
                "cortes, coloração e manicure",
                "clientes sofrem com agenda vazia",
                "HIGH",
                "AI");
        ResearchQuery query = new ResearchQuery(
                1L,
                "MEI salão beleza rotina agenda clientes Brasil",
                "MEI_ROUTINE_DISCOVERY",
                "WEB",
                1,
                "PENDING",
                "AI");

        NicheResearchSeedBuilderBackendCompletionRequest request = client.toBackendCompletionRequest(
                new OpenAiSeedBuilderResult(
                        new NicheResearchSeedBuilderOutput(1L, seed, List.of(query)),
                        "{\"seed\":true}",
                        "{\"input\":true}",
                        "{\"id\":\"resp_seed\"}",
                        1200,
                        800,
                        "resp_seed",
                        "gpt-5.4"));

        assertThat(request.nicheName()).isEqualTo("Cabeleireiros MEI e profissionais autônomos");
        assertThat(request.businessType()).isEqualTo("Serviços de beleza");
        assertThat(request.operationType()).isEqualTo("Atendimento presencial com agenda recorrente");
        assertThat(request.customerType()).isEqualTo("Consumidores finais");
        assertThat(request.commercialObjects()).isEqualTo("cortes, coloração e manicure");
        assertThat(request.initialAssumptions()).isEqualTo("clientes sofrem com agenda vazia");
        assertThat(request.confidenceLevel()).isEqualTo("HIGH");
        assertThat(request.createdBy()).isEqualTo("AI");
        assertThat(request.model()).isEqualTo("gpt-5.4");
        assertThat(request.rawOpenAiRequest()).isEqualTo("{\"input\":true}");
        assertThat(request.rawOpenAiResponse()).isEqualTo("{\"id\":\"resp_seed\"}");
        assertThat(request.inputTokens()).isEqualTo(1200);
        assertThat(request.outputTokens()).isEqualTo(800);
        assertThat(request.openAiResponseId()).isEqualTo("resp_seed");
        assertThat(request.queries()).containsExactly(query);
    }

    /** Deve converter a resposta do backend para o formato interno usado pelo worker da etapa dois. */
    @Test
    void toOutputConvertsBackendResponseToWorkerOutput() {
        NicheResearchSeedBuilderBackendClient client = client();
        NicheResearchSeedBuilderBackendQueryResponse query = new NicheResearchSeedBuilderBackendQueryResponse(
                10L,
                1L,
                20L,
                "MEI salão beleza rotina agenda clientes Brasil",
                "MEI_ROUTINE_DISCOVERY",
                "WEB",
                1,
                "PENDING",
                0,
                "AI",
                null,
                null);
        NicheResearchSeedBuilderBackendCompletionResponse response = new NicheResearchSeedBuilderBackendCompletionResponse(
                1L,
                20L,
                "9602501",
                "Cabeleireiros, manicure e pedicure",
                "Cabeleireiros MEI e profissionais autônomos",
                "Serviços de beleza",
                "Atendimento presencial com agenda recorrente",
                "Consumidores finais",
                "cortes, coloração e manicure",
                "clientes sofrem com agenda vazia",
                "HIGH",
                "AI",
                null,
                "gpt-5.4",
                "{\"seed\":true}",
                "{\"input\":true}",
                "{\"id\":\"resp_seed\"}",
                1200,
                800,
                new java.math.BigDecimal("0.0123"),
                "resp_seed",
                1,
                List.of(query));

        NicheResearchSeedBuilderOutput output = client.toOutput(response);

        assertThat(output.researchCycleId()).isEqualTo(1L);
        assertThat(output.seed().nicheName()).isEqualTo("Cabeleireiros MEI e profissionais autônomos");
        assertThat(output.seed().cnaeCode()).isEqualTo("9602501");
        assertThat(output.queries()).hasSize(1);
        assertThat(output.queries().getFirst().queryText()).isEqualTo("MEI salão beleza rotina agenda clientes Brasil");
        assertThat(output.queries().getFirst().status()).isEqualTo("PENDING");
    }

    /** Cria o cliente com URL base fictícia para testar apenas mapeamentos locais. */
    private NicheResearchSeedBuilderBackendClient client() {
        return new NicheResearchSeedBuilderBackendClient(
                new OprmMarketImportCollectorProperties("http://backend.test", "manual"), RestClient.builder().build());
    }
}

package com.marketinghub.moissaleslibraryworker.pipelines.salespagepatterns.v1.pagepatternextraction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.moissaleslibraryworker.pipelines.salespagepatterns.v1.pagepatternextraction.service.receberesponse.SalesPagePatternsPagePatternExtractionRecebeResponseRequest;
import com.marketinghub.repository.jpa.aiprompt.AiPromptSchemaTemplateRepository;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageRepository;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.entity.MoisSalesPage;
import com.marketinghub.repository.jpa.mois.dossieproduto.DossierProductContextGateway;
import com.marketinghub.repository.jpa.mois.dossieproduto.PipelineDossieProdutoRepository;
import com.marketinghub.repository.jpa.mois.dossieproduto.entity.PipelineDossieProduto;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/** Testa os callbacks da etapa de extração de padrões de página de venda. */
@ExtendWith(MockitoExtension.class)
class SalesPagePatternsPagePatternExtractionServiceTest {
    @Mock
    private MoisSalesPageRepository salesPageRepository;

    @Mock
    private PipelineDossieProdutoRepository pipelineDossieProdutoRepository;

    @Mock
    private DossierProductContextGateway productContextGateway;

    @Mock
    private AiPromptSchemaTemplateRepository templateRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private SalesPagePatternsPagePatternExtractionService service;

    /** Garante que a resposta funcional limpa seja persistida separada do envelope bruto da OpenAI. */
    @Test
    void shouldPersistCleanFinalResponseFromResponsesApiEnvelope() {
        MoisSalesPage page = new MoisSalesPage();
        page.setId(401L);
        when(salesPageRepository.findById(401L)).thenReturn(Optional.of(page));
        String rawResponse = """
                {
                  "id": "resp_salespagepatterns",
                  "output": [
                    {
                      "type": "message",
                      "content": [
                        {
                          "type": "output_text",
                          "text": "{\\\"headlinePattern\\\":\\\"dor concreta + mecanismo específico\\\"}"
                        }
                      ]
                    }
                  ]
                }
                """;

        service.recebeResponse(
                "401",
                "job-401",
                new SalesPagePatternsPagePatternExtractionRecebeResponseRequest(
                        rawResponse, 120, 80, null, "gpt-5-mini", null, "template-key", "v1", "schema-name"));

        ArgumentCaptor<PipelineDossieProduto> pipelineCaptor = ArgumentCaptor.forClass(PipelineDossieProduto.class);
        verify(pipelineDossieProdutoRepository).save(pipelineCaptor.capture());
        PipelineDossieProduto pipeline = pipelineCaptor.getValue();
        assertThat(pipeline.getResponse()).isEqualTo(rawResponse);
        assertThat(pipeline.getRespostaFinal())
                .isEqualTo("{\"headlinePattern\":\"dor concreta + mecanismo específico\"}");
        assertThat(pipeline.getPipelineCode()).isEqualTo("salespagepatterns.v1");
        verify(salesPageRepository).save(any(MoisSalesPage.class));
    }
}

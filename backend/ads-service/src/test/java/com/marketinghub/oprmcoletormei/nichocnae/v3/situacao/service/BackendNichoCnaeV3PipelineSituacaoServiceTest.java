package com.marketinghub.oprmcoletormei.nichocnae.v3.situacao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.nichocnae.PipelineNichoCnae;
import com.marketinghub.repository.jpa.oprm.nichocnae.PipelineNichoCnaeRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida a consulta de situação auditada do pipeline NichoCNAE v3. */
@ExtendWith(MockitoExtension.class)
class BackendNichoCnaeV3PipelineSituacaoServiceTest {
    @Mock
    private PipelineNichoCnaeRepository repository;

    @InjectMocks
    private BackendNichoCnaeV3PipelineSituacaoService service;

    /** Deve consultar por OR na lista de status normalizada e preservar a ordenação do repository. */
    @Test
    void searchShouldFilterByStageExternalIdAndStatuses() {
        PipelineNichoCnae record = new PipelineNichoCnae();
        record.setId(1L);
        record.setIdExterno("4781400");
        record.setCodigoEtapa("source-searcher");
        record.setStatus("CONCLUIDO");
        record.setDataHora(Instant.parse("2026-06-27T10:00:00Z"));
        record.setJobId("job-1");
        record.setRequestInput("prompt operacional");
        record.setRespostaFinal("{\"persona\":\"dono de loja\"}");
        when(repository.findByCodigoEtapaAndIdExternoAndStatusInOrderByDataHoraDesc(
                        "source-searcher", "4781400", List.of("CONCLUIDO", "FALHA")))
                .thenReturn(List.of(record));

        var response = service.search("source-searcher", "4781400", List.of(" CONCLUIDO ", "FALHA", "CONCLUIDO", ""));

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().idExterno()).isEqualTo("4781400");
        assertThat(response.getFirst().codigoEtapa()).isEqualTo("source-searcher");
        assertThat(response.getFirst().status()).isEqualTo("CONCLUIDO");
        assertThat(response.getFirst().requestInput()).isEqualTo("prompt operacional");
        assertThat(response.getFirst().respostaFinal()).isEqualTo("{\"persona\":\"dono de loja\"}");
        ArgumentCaptor<List<String>> statusCaptor = ArgumentCaptor.forClass(List.class);
        verify(repository).findByCodigoEtapaAndIdExternoAndStatusInOrderByDataHoraDesc(
                org.mockito.ArgumentMatchers.eq("source-searcher"),
                org.mockito.ArgumentMatchers.eq("4781400"),
                statusCaptor.capture());
        assertThat(statusCaptor.getValue()).containsExactly("CONCLUIDO", "FALHA");
    }

    /** Deve retornar request e response como eventos separados para a tela montar a auditoria real da OpenAI. */
    @Test
    void searchShouldPreserveSeparateRequestAndResponseAuditEvents() {
        PipelineNichoCnae requestRecord = new PipelineNichoCnae();
        requestRecord.setId(1L);
        requestRecord.setIdExterno("4781400");
        requestRecord.setCodigoEtapa("persona-candidate-generator");
        requestRecord.setStatus("AGUARDANDO_MODULO");
        requestRecord.setDataHora(Instant.parse("2026-06-27T10:00:00Z"));
        requestRecord.setJobId("job-1");
        requestRecord.setRequest("{\"model\":\"gpt-5.2\",\"service_tier\":\"flex\"}");
        requestRecord.setRequestInput("# prompt enviado");
        PipelineNichoCnae responseRecord = new PipelineNichoCnae();
        responseRecord.setId(2L);
        responseRecord.setIdExterno("4781400");
        responseRecord.setCodigoEtapa("persona-candidate-generator");
        responseRecord.setStatus("CONCLUIDO");
        responseRecord.setDataHora(Instant.parse("2026-06-27T10:01:00Z"));
        responseRecord.setJobId("job-1");
        responseRecord.setResponse("{\"status\":\"completed\"}");
        when(repository.findByCodigoEtapaAndIdExternoAndStatusInOrderByDataHoraDesc(
                        "persona-candidate-generator", "4781400", List.of("AGUARDANDO_MODULO", "CONCLUIDO")))
                .thenReturn(List.of(responseRecord, requestRecord));

        var response = service.search("persona-candidate-generator", "4781400", List.of("AGUARDANDO_MODULO", "CONCLUIDO"));

        assertThat(response).hasSize(2);
        assertThat(response.get(0).response()).isEqualTo("{\"status\":\"completed\"}");
        assertThat(response.get(1).request()).isEqualTo("{\"model\":\"gpt-5.2\",\"service_tier\":\"flex\"}");
        assertThat(response.get(1).requestInput()).isEqualTo("# prompt enviado");
    }
}

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
        record.setIdExterno("4781400");
        record.setCodigoEtapa("source-searcher");
        record.setStatus("CONCLUIDO");
        record.setDataHora(Instant.parse("2026-06-27T10:00:00Z"));
        record.setJobId("job-1");
        when(repository.findByCodigoEtapaAndIdExternoAndStatusInOrderByDataHoraDesc(
                        "source-searcher", "4781400", List.of("CONCLUIDO", "FALHA")))
                .thenReturn(List.of(record));

        var response = service.search("source-searcher", "4781400", List.of(" CONCLUIDO ", "FALHA", "CONCLUIDO", ""));

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().idExterno()).isEqualTo("4781400");
        assertThat(response.getFirst().codigoEtapa()).isEqualTo("source-searcher");
        assertThat(response.getFirst().status()).isEqualTo("CONCLUIDO");
        ArgumentCaptor<List<String>> statusCaptor = ArgumentCaptor.forClass(List.class);
        verify(repository).findByCodigoEtapaAndIdExternoAndStatusInOrderByDataHoraDesc(
                org.mockito.ArgumentMatchers.eq("source-searcher"),
                org.mockito.ArgumentMatchers.eq("4781400"),
                statusCaptor.capture());
        assertThat(statusCaptor.getValue()).containsExactly("CONCLUIDO", "FALHA");
    }
}

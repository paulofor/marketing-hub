package com.marketinghub.businessprocesschain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.service.BusinessProcessChainService;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: comprovar a leitura ordenada e versionada das cadeias de processos. */
class BusinessProcessChainServiceTest {
  /** Lista a quantidade de processos somente da cadeia publicada em uso. */
  @Test
  void listsChainWithBackendProcessCount() {
    var repository = mock(BusinessProcessChainDefinitionRepository.class);
    BusinessProcessChainDefinition chain = chain();
    chain.setItems(List.of(item(chain, process(2L, "second", "Segundo"), 2)));
    when(repository.findAllByStatusOrderByNameAscVersionNumberDesc("PUBLISHED"))
        .thenReturn(List.of(chain));
    var service = new BusinessProcessChainService(repository);

    var result = service.listChains();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().processCount()).isEqualTo(1);
    assertThat(result.getFirst().primaryMetric())
        .isEqualTo("Tempo até venda entregue com satisfação");
  }

  /** Lista somente as cadeias vinculadas à versão exata do processo selecionado. */
  @Test
  void listsChainsByProcessDefinition() {
    var repository = mock(BusinessProcessChainDefinitionRepository.class);
    BusinessProcessChainDefinition chain = chain();
    chain.setItems(List.of(item(chain, process(22L, "discovery", "Descoberta PDE"), 1)));
    when(repository.findByProcessDefinitionId(22L)).thenReturn(List.of(chain));
    var service = new BusinessProcessChainService(repository);

    var result = service.listChainsByProcess(22L);

    assertThat(result)
        .singleElement()
        .satisfies(
            summary -> {
              assertThat(summary.id()).isEqualTo(10L);
              assertThat(summary.name()).isEqualTo("Criação e entrega de valor PDE");
              assertThat(summary.processCount()).isEqualTo(1);
            });
  }

  /** Ordena os processos pela contribuição de valor mesmo quando a coleção chega fora de ordem. */
  @Test
  void getsExactProcessVersionsInValueOrder() {
    var repository = mock(BusinessProcessChainDefinitionRepository.class);
    BusinessProcessChainDefinition chain = chain();
    BusinessProcessDefinition first = process(1L, "first", "Primeiro");
    BusinessProcessDefinition second = process(2L, "second", "Segundo");
    chain.setItems(List.of(item(chain, second, 2), item(chain, first, 1)));
    when(repository.findById(10L)).thenReturn(Optional.of(chain));
    var service = new BusinessProcessChainService(repository);

    var result = service.getChain(10L);

    assertThat(result.processes()).extracting("name").containsExactly("Primeiro", "Segundo");
    assertThat(result.processes().getFirst().versionNumber()).isEqualTo(1);
    assertThat(result.processes().getFirst().status()).isEqualTo("PUBLISHED");
  }

  /** Responde 404 quando a cadeia solicitada não existe. */
  @Test
  void rejectsUnknownChain() {
    var repository = mock(BusinessProcessChainDefinitionRepository.class);
    when(repository.findById(99L)).thenReturn(Optional.empty());
    var service = new BusinessProcessChainService(repository);

    assertThatThrownBy(() -> service.getChain(99L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Cadeia de processos não encontrada");
  }

  /** Monta a cadeia publicada usada pelos cenários de leitura. */
  private BusinessProcessChainDefinition chain() {
    BusinessProcessChainDefinition chain = new BusinessProcessChainDefinition();
    chain.setId(10L);
    chain.setChainCode("pde-value-creation-delivery");
    chain.setName("Criação e entrega de valor PDE");
    chain.setPurpose("Transformar oportunidade em valor entregue.");
    chain.setOutcomeDescription("Venda entregue com satisfação.");
    chain.setPrimaryMetric("Tempo até venda entregue com satisfação");
    chain.setVersionNumber(1);
    chain.setStatus("PUBLISHED");
    chain.setCreatedAt(Instant.parse("2026-08-20T10:00:00Z"));
    chain.setPublishedAt(Instant.parse("2026-08-20T10:00:00Z"));
    return chain;
  }

  /** Monta uma definição de processo publicada para validar a versão fixada pela cadeia. */
  private BusinessProcessDefinition process(Long id, String code, String name) {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(id);
    process.setProcessCode(code);
    process.setName(name);
    process.setPurpose("Criar valor.");
    process.setOwnerName("Operação");
    process.setTriggerDescription("Entrada aprovada.");
    process.setOutcomeDescription("Resultado aprovado.");
    process.setVersionNumber(1);
    process.setStatus("PUBLISHED");
    return process;
  }

  /** Vincula um processo a uma posição específica da cadeia. */
  private BusinessProcessChainItem item(
      BusinessProcessChainDefinition chain, BusinessProcessDefinition process, int sequenceNumber) {
    BusinessProcessChainItem item = new BusinessProcessChainItem();
    item.setChainDefinition(chain);
    item.setProcessDefinition(process);
    item.setSequenceNumber(sequenceNumber);
    item.setValueContribution("Contribuição " + sequenceNumber);
    item.setCreatedAt(Instant.parse("2026-08-20T10:00:00Z"));
    return item;
  }
}

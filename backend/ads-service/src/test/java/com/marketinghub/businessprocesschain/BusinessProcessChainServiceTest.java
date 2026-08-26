package com.marketinghub.businessprocesschain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.service.BusinessProcessChainService;
import com.marketinghub.businessprocesschain.service.updateDraft.BusinessProcessChainItemRequest;
import com.marketinghub.businessprocesschain.service.updateDraft.BusinessProcessChainSaveRequest;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainItemRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: comprovar a leitura e o versionamento seguro das cadeias de processos. */
class BusinessProcessChainServiceTest {
  private final BusinessProcessChainDefinitionRepository repository =
      mock(BusinessProcessChainDefinitionRepository.class);
  private final BusinessProcessChainItemRepository itemRepository =
      mock(BusinessProcessChainItemRepository.class);
  private final BusinessProcessDefinitionRepository processRepository =
      mock(BusinessProcessDefinitionRepository.class);
  private final BusinessProcessChainService service =
      new BusinessProcessChainService(repository, itemRepository, processRepository);

  /** Lista a quantidade de processos somente da cadeia publicada em uso. */
  @Test
  void listsChainWithBackendProcessCount() {
    BusinessProcessChainDefinition chain = chain(1, "PUBLISHED");
    chain.setItems(List.of(item(chain, process(2L, "second", "Segundo", 1), 2)));
    when(repository.findAllByStatusOrderByNameAscVersionNumberDesc("PUBLISHED"))
        .thenReturn(List.of(chain));

    var result = service.listChains();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().processCount()).isEqualTo(1);
    assertThat(result.getFirst().primaryMetric())
        .isEqualTo("Tempo até venda entregue com satisfação");
  }

  /** Lista rascunhos e versões publicadas no catálogo administrativo. */
  @Test
  void listsEditableCatalogWithoutRetiredVersions() {
    when(repository.findAllByStatusNotOrderByNameAscVersionNumberDesc("RETIRED"))
        .thenReturn(List.of(chain(2, "DRAFT"), chain(1, "PUBLISHED")));

    var result = service.listCatalog();

    assertThat(result).extracting("status").containsExactly("DRAFT", "PUBLISHED");
  }

  /** Lista somente as cadeias vinculadas à versão exata do processo selecionado. */
  @Test
  void listsChainsByProcessDefinition() {
    BusinessProcessChainDefinition chain = chain(1, "PUBLISHED");
    chain.setItems(
        List.of(item(chain, process(22L, "discovery", "Descoberta PDE", 1), 1)));
    when(repository.findByProcessDefinitionId(22L)).thenReturn(List.of(chain));

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
    BusinessProcessChainDefinition chain = chain(1, "PUBLISHED");
    BusinessProcessDefinition first = process(1L, "first", "Primeiro", 1);
    BusinessProcessDefinition second = process(2L, "second", "Segundo", 1);
    chain.setItems(List.of(item(chain, second, 2), item(chain, first, 1)));
    when(repository.findById(10L)).thenReturn(Optional.of(chain));

    var result = service.getChain(10L);

    assertThat(result.processes()).extracting("name").containsExactly("Primeiro", "Segundo");
    assertThat(result.processes().getFirst().versionNumber()).isEqualTo(1);
    assertThat(result.processes().getFirst().status()).isEqualTo("PUBLISHED");
  }

  /** Clona a cadeia publicada como próxima versão sem alterar o original. */
  @Test
  void createsNextDraftVersion() {
    BusinessProcessChainDefinition source = chain(5, "PUBLISHED");
    source.setItems(
        List.of(
            item(source, process(37L, "discovery", "Descoberta PDE", 4), 1),
            item(source, process(38L, "offer", "Plano Comercial", 4), 2)));
    when(repository.findById(10L)).thenReturn(Optional.of(source));
    when(repository.findFirstByChainCodeAndStatusOrderByVersionNumberDesc(
            source.getChainCode(), "DRAFT"))
        .thenReturn(Optional.empty());
    when(repository.findAllByChainCodeOrderByVersionNumberDesc(source.getChainCode()))
        .thenReturn(List.of(source));
    when(repository.saveAndFlush(any(BusinessProcessChainDefinition.class)))
        .thenAnswer(
            invocation -> {
              BusinessProcessChainDefinition saved = invocation.getArgument(0);
              saved.setId(20L);
              return saved;
            });

    var result = service.createDraft(10L);

    assertThat(result.id()).isEqualTo(20L);
    assertThat(result.versionNumber()).isEqualTo(6);
    assertThat(result.status()).isEqualTo("DRAFT");
    assertThat(result.processes()).extracting("processDefinitionId").containsExactly(37L, 38L);
    assertThat(source.getStatus()).isEqualTo("PUBLISHED");
    verify(itemRepository).saveAllAndFlush(anyList());
  }

  /** Reutiliza o rascunho existente para tornar o comando de criação idempotente. */
  @Test
  void reusesExistingDraft() {
    BusinessProcessChainDefinition source = chain(5, "PUBLISHED");
    BusinessProcessChainDefinition draft = chain(6, "DRAFT");
    when(repository.findById(10L)).thenReturn(Optional.of(source));
    when(repository.findFirstByChainCodeAndStatusOrderByVersionNumberDesc(
            source.getChainCode(), "DRAFT"))
        .thenReturn(Optional.of(draft));

    var result = service.createDraft(10L);

    assertThat(result.versionNumber()).isEqualTo(6);
    verify(repository, never()).saveAndFlush(any());
  }

  /** Reconstrói a ordem pelo contrato e permite trocar somente a versão da descoberta. */
  @Test
  void updatesDraftWithContiguousSequence() {
    BusinessProcessChainDefinition draft = chain(6, "DRAFT");
    BusinessProcessDefinition discovery = process(49L, "discovery", "Descoberta PDE", 5);
    BusinessProcessDefinition offer = process(38L, "offer", "Plano Comercial", 4);
    when(repository.findById(10L)).thenReturn(Optional.of(draft));
    when(processRepository.findById(49L)).thenReturn(Optional.of(discovery));
    when(processRepository.findById(38L)).thenReturn(Optional.of(offer));

    var result =
        service.updateDraft(
            10L,
            request(
                new BusinessProcessChainItemRequest(49L, "Escolhe oportunidade comprovada."),
                new BusinessProcessChainItemRequest(38L, "Define a oferta.")));

    assertThat(result.processes()).extracting("sequenceNumber").containsExactly(1, 2);
    assertThat(result.processes()).extracting("processDefinitionId").containsExactly(49L, 38L);
    verify(itemRepository).deleteByChainDefinitionId(10L);
    verify(itemRepository).flush();
    verify(itemRepository).saveAllAndFlush(anyList());
  }

  /** Rejeita processo aposentado para impedir cadeia nova presa a contrato obsoleto. */
  @Test
  void rejectsRetiredProcessInDraft() {
    BusinessProcessChainDefinition draft = chain(6, "DRAFT");
    BusinessProcessDefinition retired = process(37L, "discovery", "Descoberta PDE", 4);
    retired.setStatus("RETIRED");
    when(repository.findById(10L)).thenReturn(Optional.of(draft));
    when(processRepository.findById(37L)).thenReturn(Optional.of(retired));

    assertThatThrownBy(
            () ->
                service.updateDraft(
                    10L,
                    request(
                        new BusinessProcessChainItemRequest(
                            37L, "Escolhe oportunidade comprovada."))))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("precisam estar publicados");
  }

  /** Rejeita duas versões do mesmo processo na mesma cadeia. */
  @Test
  void rejectsDuplicateProcessCode() {
    BusinessProcessChainDefinition draft = chain(6, "DRAFT");
    BusinessProcessDefinition versionFour = process(37L, "discovery", "Descoberta PDE", 4);
    BusinessProcessDefinition versionFive = process(49L, "discovery", "Descoberta PDE", 5);
    when(repository.findById(10L)).thenReturn(Optional.of(draft));
    when(processRepository.findById(37L)).thenReturn(Optional.of(versionFour));
    when(processRepository.findById(49L)).thenReturn(Optional.of(versionFive));

    assertThatThrownBy(
            () ->
                service.updateDraft(
                    10L,
                    request(
                        new BusinessProcessChainItemRequest(37L, "Versão anterior."),
                        new BusinessProcessChainItemRequest(49L, "Versão atual."))))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("duas versões do mesmo processo");
  }

  /** Publica o rascunho e aposenta somente a versão anteriormente vigente. */
  @Test
  void publishesDraftAndRetiresPreviousVersion() {
    BusinessProcessChainDefinition selected = chain(6, "DRAFT");
    selected.setItems(
        List.of(item(selected, process(49L, "discovery", "Descoberta PDE", 5), 1)));
    BusinessProcessChainDefinition previous = chain(5, "PUBLISHED");
    when(repository.findById(10L)).thenReturn(Optional.of(selected));
    when(repository.findAllByChainCodeAndStatusOrderByVersionNumberDesc(
            selected.getChainCode(), "PUBLISHED"))
        .thenReturn(List.of(previous));
    when(repository.save(selected)).thenReturn(selected);

    var result = service.publish(10L);

    assertThat(result.status()).isEqualTo("PUBLISHED");
    assertThat(result.publishedAt()).isNotNull();
    assertThat(previous.getStatus()).isEqualTo("RETIRED");
  }

  /** Responde 404 quando a cadeia solicitada não existe. */
  @Test
  void rejectsUnknownChain() {
    when(repository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getChain(99L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Cadeia de processos não encontrada");
  }

  /** Monta uma versão persistível da cadeia PDE para o cenário informado. */
  private BusinessProcessChainDefinition chain(int version, String status) {
    BusinessProcessChainDefinition chain = new BusinessProcessChainDefinition();
    chain.setId(10L);
    chain.setChainCode("pde-value-creation-delivery");
    chain.setName("Criação e entrega de valor PDE");
    chain.setPurpose("Transformar oportunidade em valor entregue.");
    chain.setOutcomeDescription("Venda entregue com satisfação.");
    chain.setPrimaryMetric("Tempo até venda entregue com satisfação");
    chain.setVersionNumber(version);
    chain.setStatus(status);
    chain.setCreatedAt(Instant.parse("2026-08-20T10:00:00Z"));
    chain.setPublishedAt(
        "PUBLISHED".equals(status) ? Instant.parse("2026-08-20T10:00:00Z") : null);
    return chain;
  }

  /** Monta uma definição de processo publicada para validar a versão fixada pela cadeia. */
  private BusinessProcessDefinition process(Long id, String code, String name, int version) {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(id);
    process.setProcessCode(code);
    process.setName(name);
    process.setPurpose("Criar valor.");
    process.setOwnerName("Operação");
    process.setTriggerDescription("Entrada aprovada.");
    process.setOutcomeDescription("Resultado aprovado.");
    process.setVersionNumber(version);
    process.setStatus("PUBLISHED");
    process.setProcessType("VALUE_PROCESS");
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

  /** Monta o contrato completo de edição com a ordem informada pelo usuário. */
  private BusinessProcessChainSaveRequest request(BusinessProcessChainItemRequest... processes) {
    return new BusinessProcessChainSaveRequest(
        "Criação e entrega de valor PDE",
        "Transformar oportunidade em valor entregue.",
        "Venda entregue com satisfação.",
        "Tempo até venda entregue com satisfação",
        List.of(processes));
  }
}

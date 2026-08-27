package com.marketinghub.businessprocesschain.service;

import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.businessprocesschain.service.getChain.BusinessProcessChainDetailResponse;
import com.marketinghub.businessprocesschain.service.getChain.BusinessProcessChainProcessResponse;
import com.marketinghub.businessprocesschain.service.listChains.BusinessProcessChainSummaryResponse;
import com.marketinghub.businessprocesschain.service.updateDraft.BusinessProcessChainItemRequest;
import com.marketinghub.businessprocesschain.service.updateDraft.BusinessProcessChainSaveRequest;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainItemRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: governar leitura, rascunho e publicação das cadeias sem executar trabalho. */
@Service
@RequiredArgsConstructor
public class BusinessProcessChainService {
  private static final String PUBLISHED_STATUS = "PUBLISHED";
  private static final String DRAFT_STATUS = "DRAFT";
  private static final String RETIRED_STATUS = "RETIRED";

  private final BusinessProcessChainDefinitionRepository repository;
  private final BusinessProcessChainItemRepository itemRepository;
  private final BusinessProcessDefinitionRepository processRepository;

  /** Lista somente as cadeias publicadas em uso, com contagens calculadas pelo backend. */
  @Transactional(readOnly = true)
  public List<BusinessProcessChainSummaryResponse> listChains() {
    return repository.findAllByStatusOrderByNameAscVersionNumberDesc(PUBLISHED_STATUS).stream()
        .map(this::summary)
        .toList();
  }

  /** Lista a fonte publicada e os rascunhos editáveis, sem misturar o histórico aposentado. */
  @Transactional(readOnly = true)
  public List<BusinessProcessChainSummaryResponse> listCatalog() {
    return repository.findAllByStatusNotOrderByNameAscVersionNumberDesc(RETIRED_STATUS).stream()
        .map(this::summary)
        .toList();
  }

  /** Lista as cadeias às quais pertence uma versão exata de processo. */
  @Transactional(readOnly = true)
  public List<BusinessProcessChainSummaryResponse> listChainsByProcess(Long processDefinitionId) {
    return repository.findByProcessDefinitionId(processDefinitionId).stream()
        .map(this::summary)
        .toList();
  }

  /** Exibe a cadeia selecionada com as versões exatas de seus processos em ordem. */
  @Transactional(readOnly = true)
  public BusinessProcessChainDetailResponse getChain(Long id) {
    return detail(required(id));
  }

  /** Clona uma cadeia como próxima versão ou reutiliza o rascunho já aberto para o mesmo código. */
  @Transactional
  public BusinessProcessChainDetailResponse createDraft(Long sourceId) {
    BusinessProcessChainDefinition source = required(sourceId);
    if (DRAFT_STATUS.equals(source.getStatus())) return detail(source);
    var existingDraft =
        repository.findFirstByChainCodeAndStatusOrderByVersionNumberDesc(
            source.getChainCode(), DRAFT_STATUS);
    if (existingDraft.isPresent()) return detail(existingDraft.get());

    int nextVersion =
        repository.findAllByChainCodeOrderByVersionNumberDesc(source.getChainCode()).stream()
                .map(BusinessProcessChainDefinition::getVersionNumber)
                .max(Integer::compareTo)
                .orElse(source.getVersionNumber())
            + 1;
    Instant now = Instant.now();
    BusinessProcessChainDefinition draft = new BusinessProcessChainDefinition();
    draft.setChainCode(source.getChainCode());
    draft.setName(source.getName());
    draft.setPurpose(source.getPurpose());
    draft.setOutcomeDescription(source.getOutcomeDescription());
    draft.setPrimaryMetric(source.getPrimaryMetric());
    draft.setVersionNumber(nextVersion);
    draft.setStatus(DRAFT_STATUS);
    draft.setCreatedAt(now);
    BusinessProcessChainDefinition saved = repository.saveAndFlush(draft);
    List<BusinessProcessChainItem> items =
        source.getItems().stream()
            .sorted(Comparator.comparing(BusinessProcessChainItem::getSequenceNumber))
            .map(
                sourceItem ->
                    item(
                        saved,
                        sourceItem.getProcessDefinition(),
                        sourceItem.getSequenceNumber(),
                        sourceItem.getValueContribution(),
                        now))
            .toList();
    itemRepository.saveAllAndFlush(items);
    saved.setItems(new ArrayList<>(items));
    return detail(saved);
  }

  /** Salva somente um rascunho e reconstrói sua sequência a partir da ordem recebida. */
  @Transactional
  public BusinessProcessChainDetailResponse updateDraft(
      Long id, BusinessProcessChainSaveRequest request) {
    BusinessProcessChainDefinition draft = required(id);
    requireDraft(draft);
    List<ResolvedProcess> resolved = resolveProcesses(request.processes());
    draft.setName(request.name().trim());
    draft.setPurpose(request.purpose().trim());
    draft.setOutcomeDescription(request.outcomeDescription().trim());
    draft.setPrimaryMetric(request.primaryMetric().trim());
    repository.save(draft);
    itemRepository.deleteByChainDefinitionId(draft.getId());
    itemRepository.flush();
    Instant now = Instant.now();
    List<BusinessProcessChainItem> items = new ArrayList<>();
    for (int index = 0; index < resolved.size(); index++) {
      ResolvedProcess process = resolved.get(index);
      items.add(item(draft, process.definition(), index + 1, process.valueContribution(), now));
    }
    itemRepository.saveAllAndFlush(items);
    draft.setItems(items);
    return detail(draft);
  }

  /** Publica o rascunho validado e aposenta somente as versões publicadas do mesmo código. */
  @Transactional
  public BusinessProcessChainDetailResponse publish(Long id) {
    BusinessProcessChainDefinition selected = required(id);
    if (PUBLISHED_STATUS.equals(selected.getStatus())) return detail(selected);
    requireDraft(selected);
    validateStoredProcesses(selected.getItems());
    repository
        .findAllByChainCodeAndStatusOrderByVersionNumberDesc(
            selected.getChainCode(), PUBLISHED_STATUS)
        .forEach(chain -> chain.setStatus(RETIRED_STATUS));
    selected.setStatus(PUBLISHED_STATUS);
    selected.setPublishedAt(Instant.now());
    return detail(repository.save(selected));
  }

  /** Exclui somente rascunhos e preserva integralmente versões publicadas e aposentadas. */
  @Transactional
  public void deleteDraft(Long id) {
    BusinessProcessChainDefinition draft = required(id);
    requireDraft(draft);
    itemRepository.deleteByChainDefinitionId(draft.getId());
    itemRepository.flush();
    repository.delete(draft);
  }

  /** Exibe uma entidade já carregada com os processos na ordem de geração de valor. */
  private BusinessProcessChainDetailResponse detail(BusinessProcessChainDefinition chain) {
    List<BusinessProcessChainProcessResponse> processes =
        chain.getItems().stream()
            .sorted(Comparator.comparing(BusinessProcessChainItem::getSequenceNumber))
            .map(this::process)
            .toList();
    return new BusinessProcessChainDetailResponse(
        chain.getId(),
        chain.getChainCode(),
        chain.getName(),
        chain.getPurpose(),
        chain.getOutcomeDescription(),
        chain.getPrimaryMetric(),
        chain.getVersionNumber(),
        chain.getStatus(),
        processes.size(),
        chain.getCreatedAt(),
        chain.getPublishedAt(),
        processes);
  }

  /** Localiza uma cadeia ou responde ausência pelo contrato administrativo. */
  private BusinessProcessChainDefinition required(Long id) {
    return repository
        .findById(id)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Cadeia de processos não encontrada."));
  }

  /** Impede edição ou exclusão de uma versão imutável. */
  private void requireDraft(BusinessProcessChainDefinition chain) {
    if (!DRAFT_STATUS.equals(chain.getStatus())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Somente versões em rascunho podem ser alteradas.");
    }
  }

  /** Resolve processos publicados, únicos por código e restritos a processos de valor. */
  private List<ResolvedProcess> resolveProcesses(List<BusinessProcessChainItemRequest> requested) {
    Set<String> processCodes = new HashSet<>();
    List<ResolvedProcess> resolved = new ArrayList<>();
    for (BusinessProcessChainItemRequest item : requested) {
      var process =
          processRepository
              .findById(item.processDefinitionId())
              .orElseThrow(
                  () ->
                      new ResponseStatusException(
                          HttpStatus.BAD_REQUEST, "Processo da cadeia não encontrado."));
      validatePublishedValueProcess(process.getStatus(), process.getProcessType());
      if (!processCodes.add(process.getProcessCode())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "A cadeia não pode conter duas versões do mesmo processo.");
      }
      resolved.add(new ResolvedProcess(process, item.valueContribution().trim()));
    }
    return List.copyOf(resolved);
  }

  /**
   * Revalida processos no instante da publicação para bloquear versões aposentadas no intervalo.
   */
  private void validateStoredProcesses(List<BusinessProcessChainItem> items) {
    if (items.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A cadeia precisa conter pelo menos um processo.");
    }
    Set<String> processCodes = new HashSet<>();
    for (BusinessProcessChainItem item : items) {
      var process = item.getProcessDefinition();
      validatePublishedValueProcess(process.getStatus(), process.getProcessType());
      if (!processCodes.add(process.getProcessCode())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "A cadeia não pode conter duas versões do mesmo processo.");
      }
    }
  }

  /** Exige que a cadeia use somente processos de valor publicados e disponíveis. */
  private void validatePublishedValueProcess(String status, String processType) {
    if (!PUBLISHED_STATUS.equals(status)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Todos os processos da cadeia precisam estar publicados.");
    }
    if ("SUBPROCESS".equals(processType)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Subprocessos não podem ocupar uma etapa da cadeia de valor.");
    }
  }

  /** Monta um vínculo persistível com sequência contínua e contribuição explícita. */
  private BusinessProcessChainItem item(
      BusinessProcessChainDefinition chain,
      BusinessProcessDefinition process,
      int sequenceNumber,
      String valueContribution,
      Instant createdAt) {
    BusinessProcessChainItem item = new BusinessProcessChainItem();
    item.setChainDefinition(chain);
    item.setProcessDefinition(process);
    item.setSequenceNumber(sequenceNumber);
    item.setValueContribution(valueContribution.trim());
    item.setCreatedAt(createdAt);
    return item;
  }

  /** Mantém juntos o processo resolvido e a contribuição validada antes da persistência. */
  private record ResolvedProcess(BusinessProcessDefinition definition, String valueContribution) {}

  /** Converte a cadeia persistida no contrato enxuto da listagem. */
  private BusinessProcessChainSummaryResponse summary(BusinessProcessChainDefinition chain) {
    return new BusinessProcessChainSummaryResponse(
        chain.getId(),
        chain.getChainCode(),
        chain.getName(),
        chain.getPurpose(),
        chain.getOutcomeDescription(),
        chain.getPrimaryMetric(),
        chain.getVersionNumber(),
        chain.getStatus(),
        chain.getItems().size(),
        chain.getPublishedAt());
  }

  /** Converte um vínculo da cadeia no contrato comercial de processo e contribuição de valor. */
  private BusinessProcessChainProcessResponse process(BusinessProcessChainItem item) {
    var definition = item.getProcessDefinition();
    return new BusinessProcessChainProcessResponse(
        item.getSequenceNumber(),
        item.getValueContribution(),
        definition.getId(),
        definition.getProcessCode(),
        definition.getName(),
        definition.getPurpose(),
        definition.getOwnerName(),
        definition.getTriggerDescription(),
        definition.getOutcomeDescription(),
        definition.getVersionNumber(),
        definition.getStatus());
  }
}

package com.marketinghub.businessprocesschain.controller;

import com.marketinghub.businessprocesschain.service.BusinessProcessChainService;
import com.marketinghub.businessprocesschain.service.getChain.BusinessProcessChainDetailResponse;
import com.marketinghub.businessprocesschain.service.listChains.BusinessProcessChainSummaryResponse;
import com.marketinghub.businessprocesschain.service.updateDraft.BusinessProcessChainSaveRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor a leitura e o versionamento administrativo das cadeias de valor. */
@RestController
@RequestMapping("/api/business-process-chains")
@RequiredArgsConstructor
public class BusinessProcessChainController {
  private final BusinessProcessChainService service;

  /** Lista as versões publicadas das cadeias atualmente em uso. */
  @GetMapping
  public List<BusinessProcessChainSummaryResponse> listChains() {
    return service.listChains();
  }

  /** Lista versões publicadas e rascunhos disponíveis para edição administrativa. */
  @GetMapping("/catalog")
  public List<BusinessProcessChainSummaryResponse> listCatalog() {
    return service.listCatalog();
  }

  /** Lista as cadeias que contêm a versão de processo informada. */
  @GetMapping("/by-process/{processDefinitionId}")
  public List<BusinessProcessChainSummaryResponse> listChainsByProcess(
      @PathVariable Long processDefinitionId) {
    return service.listChainsByProcess(processDefinitionId);
  }

  /** Exibe os processos da cadeia na ordem em que criam e entregam valor. */
  @GetMapping("/{id}")
  public BusinessProcessChainDetailResponse getChain(@PathVariable Long id) {
    return service.getChain(id);
  }

  /** Cria a próxima versão em rascunho a partir da cadeia selecionada. */
  @PostMapping("/{id}/draft")
  @ResponseStatus(HttpStatus.CREATED)
  public BusinessProcessChainDetailResponse createDraft(@PathVariable Long id) {
    return service.createDraft(id);
  }

  /** Atualiza metadados e processos de uma cadeia que ainda está em rascunho. */
  @PutMapping("/{id}")
  public BusinessProcessChainDetailResponse updateDraft(
      @PathVariable Long id, @Valid @RequestBody BusinessProcessChainSaveRequest request) {
    return service.updateDraft(id, request);
  }

  /** Publica o rascunho e aposenta a versão anteriormente vigente da mesma cadeia. */
  @PostMapping("/{id}/publish")
  public BusinessProcessChainDetailResponse publish(@PathVariable Long id) {
    return service.publish(id);
  }

  /** Exclui um rascunho de cadeia sem afetar versões publicadas ou aposentadas. */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteDraft(@PathVariable Long id) {
    service.deleteDraft(id);
  }
}

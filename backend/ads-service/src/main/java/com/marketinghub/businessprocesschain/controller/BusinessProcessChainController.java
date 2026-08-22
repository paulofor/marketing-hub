package com.marketinghub.businessprocesschain.controller;

import com.marketinghub.businessprocesschain.service.BusinessProcessChainService;
import com.marketinghub.businessprocesschain.service.getChain.BusinessProcessChainDetailResponse;
import com.marketinghub.businessprocesschain.service.listChains.BusinessProcessChainSummaryResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor a leitura administrativa das cadeias de processos de valor. */
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
}

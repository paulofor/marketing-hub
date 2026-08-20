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

  /** Lista as cadeias cadastradas e suas versões. */
  @GetMapping
  public List<BusinessProcessChainSummaryResponse> listChains() {
    return service.listChains();
  }

  /** Exibe os processos da cadeia na ordem em que criam e entregam valor. */
  @GetMapping("/{id}")
  public BusinessProcessChainDetailResponse getChain(@PathVariable Long id) {
    return service.getChain(id);
  }
}

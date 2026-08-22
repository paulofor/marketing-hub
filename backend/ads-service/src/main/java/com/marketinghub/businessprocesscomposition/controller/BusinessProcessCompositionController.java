package com.marketinghub.businessprocesscomposition.controller;

import com.marketinghub.businessprocesscomposition.service.BusinessProcessCompositionService;
import com.marketinghub.businessprocesscomposition.service.getcomposition.BusinessProcessCompositionResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor a hierarquia navegável de processos e subprocessos. */
@RestController
@RequestMapping("/api/business-processes/{processDefinitionId}/composition")
public class BusinessProcessCompositionController {
  private final BusinessProcessCompositionService service;

  /** Configura o serviço canônico de composição dos processos. */
  public BusinessProcessCompositionController(BusinessProcessCompositionService service) {
    this.service = service;
  }

  /** Retorna o processo, seu pai opcional e seus subprocessos vigentes. */
  @GetMapping
  public BusinessProcessCompositionResponse getComposition(@PathVariable Long processDefinitionId) {
    return service.getComposition(processDefinitionId);
  }
}

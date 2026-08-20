package com.marketinghub.businessprocessresource.controller;

import com.marketinghub.businessprocessresource.service.BusinessProcessExecutionResourceService;
import com.marketinghub.businessprocessresource.service.listResources.BusinessProcessExecutionResourceResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor os recursos especializados disponíveis para atividades de processo. */
@RestController
@RequestMapping("/api/business-process-execution-resources")
public class BusinessProcessExecutionResourceController {
  private final BusinessProcessExecutionResourceService service;

  /** Inicializa o controller único com o serviço canônico do módulo. */
  public BusinessProcessExecutionResourceController(
      BusinessProcessExecutionResourceService service) {
    this.service = service;
  }

  /** Entrega a lista oficial de recursos ativos para a tela de processos. */
  @GetMapping
  public List<BusinessProcessExecutionResourceResponse> listResources() {
    return service.listResources();
  }
}

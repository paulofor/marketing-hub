package com.marketinghub.researchintelligence.v1.controller;

import com.marketinghub.researchintelligence.v1.service.ResearchIntelligenceService;
import com.marketinghub.researchintelligence.v1.service.catalog.ResearchIntelligenceCatalogResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Disponibiliza o catálogo global consumido pelo Estúdio e pelos agentes. */
@RestController
public class ResearchIntelligenceController {
  private final ResearchIntelligenceService service;

  /** Inicializa os endpoints com a governança canônica da biblioteca. */
  public ResearchIntelligenceController(ResearchIntelligenceService service) {
    this.service = service;
  }

  /** Lista todos os cartões compilados e as políticas de roteamento vigentes. */
  @GetMapping("/api/research-intelligence/v1/catalog")
  public ResearchIntelligenceCatalogResponse getCatalog() {
    return service.getCatalog();
  }
}

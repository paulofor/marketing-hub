package com.marketinghub.researchintelligence.v1.controller;

import com.marketinghub.researchintelligence.v1.service.ResearchIntelligenceService;
import com.marketinghub.researchintelligence.v1.service.catalog.ResearchIntelligenceCatalogResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Disponibiliza a Biblioteca de Inteligência global para consulta no Marketing Hub. */
@RestController
@RequestMapping("/api/research-intelligence/v1")
public class ResearchIntelligenceController {
  private final ResearchIntelligenceService service;

  /** Inicializa o endpoint com o compilador canônico da biblioteca. */
  public ResearchIntelligenceController(ResearchIntelligenceService service) {
    this.service = service;
  }

  /** Lista todos os cartões compilados e as políticas de roteamento vigentes. */
  @GetMapping("/catalog")
  public ResearchIntelligenceCatalogResponse getCatalog() {
    return service.getCatalog();
  }
}

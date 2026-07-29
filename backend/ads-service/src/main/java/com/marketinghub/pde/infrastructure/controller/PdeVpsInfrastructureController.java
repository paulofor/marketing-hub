package com.marketinghub.pde.infrastructure.controller;

import com.marketinghub.pde.infrastructure.service.PdeVpsInfrastructureService;
import com.marketinghub.pde.infrastructure.service.listVps.PdeVpsServerResponse;
import com.marketinghub.pde.infrastructure.service.listVps.PdeVpsSummaryResponse;
import com.marketinghub.pde.infrastructure.service.saveVps.SavePdeVpsServerRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor endpoints administrativos para gestão de VPS dos PDEs. */
@RestController
@RequestMapping("/api/pde/vps")
public class PdeVpsInfrastructureController {

  private final PdeVpsInfrastructureService service;

  /** Inicializa o controller com o serviço de infraestrutura dos PDEs. */
  public PdeVpsInfrastructureController(PdeVpsInfrastructureService service) {
    this.service = service;
  }

  /** Lista VPS cadastradas e custo fixo mensal consolidado. */
  @GetMapping
  public PdeVpsSummaryResponse listServers() {
    return service.listServers();
  }

  /** Cadastra uma nova VPS usada por PDEs. */
  @PostMapping
  public PdeVpsServerResponse createServer(@RequestBody SavePdeVpsServerRequest request) {
    return service.createServer(request);
  }

  /** Atualiza uma VPS existente usada por PDEs. */
  @PutMapping("/{id}")
  public PdeVpsServerResponse updateServer(
      @PathVariable Long id, @RequestBody SavePdeVpsServerRequest request) {
    return service.updateServer(id, request);
  }

  /** Remove uma VPS do cadastro administrativo dos PDEs. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteServer(@PathVariable Long id) {
    service.deleteServer(id);
    return ResponseEntity.noContent().build();
  }
}

package com.marketinghub.businessprocess;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: expor o catálogo administrativo de processos do Marketing Hub. */
@RestController
@RequestMapping("/api/business-processes")
public class BusinessProcessDefinitionController {
  private final BusinessProcessDefinitionService service;

  /** Configura o serviço governado de processos. */
  public BusinessProcessDefinitionController(BusinessProcessDefinitionService service) {
    this.service = service;
  }

  /** Lista o catálogo e seu histórico de versões. */
  @GetMapping
  public List<BusinessProcessDefinitionResponse> list() {
    return service.list();
  }

  /** Exibe uma versão com seu diagrama completo. */
  @GetMapping("/{id}")
  public BusinessProcessDefinitionResponse get(@PathVariable Long id) {
    return service.get(id);
  }

  /** Cadastra uma nova versão em rascunho. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BusinessProcessDefinitionResponse create(
      @Valid @RequestBody BusinessProcessDefinitionRequest request) {
    return service.create(request);
  }

  /** Salva alterações em uma versão que ainda está em rascunho. */
  @PutMapping("/{id}")
  public BusinessProcessDefinitionResponse updateDraft(
      @PathVariable Long id, @Valid @RequestBody BusinessProcessDefinitionRequest request) {
    return service.updateDraft(id, request);
  }

  /** Promove explicitamente uma versão válida a fonte de verdade vigente. */
  @PostMapping("/{id}/publish")
  public BusinessProcessDefinitionResponse publish(@PathVariable Long id) {
    return service.publish(id);
  }

  /** Exclui um rascunho sem uso operacional. */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteDraft(@PathVariable Long id) {
    service.deleteDraft(id);
  }
}

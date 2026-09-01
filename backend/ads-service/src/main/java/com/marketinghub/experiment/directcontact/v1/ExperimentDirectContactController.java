package com.marketinghub.experiment.directcontact.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor o registro e o placar da amostra individual consentida. */
@RestController
@RequestMapping("/api/experiments/{experimentId}/direct-contact-sample")
@Tag(
    name = "Experimentos — amostra direta",
    description = "Registra contatos consentidos sem persistir telefone ou e-mail em claro.")
public class ExperimentDirectContactController {
  private final ExperimentDirectContactService service;

  /** Configura o serviço canônico da amostra direta. */
  public ExperimentDirectContactController(ExperimentDirectContactService service) {
    this.service = service;
  }

  /** Retorna meta, avanço e contatos pseudonimizados do experimento. */
  @GetMapping
  @Operation(summary = "Consulta a amostra direta consentida")
  public ExperimentDirectContactSampleResponse getSample(@PathVariable Long experimentId) {
    return service.getSample(experimentId);
  }

  /** Registra um contato já realizado e devolve o placar atualizado. */
  @PostMapping("/contacts")
  @Operation(summary = "Registra um contato consentido e aderente")
  public ExperimentDirectContactSampleResponse register(
      @PathVariable Long experimentId,
      @Valid @RequestBody RegisterExperimentDirectContactRequest request) {
    return service.register(experimentId, request);
  }
}

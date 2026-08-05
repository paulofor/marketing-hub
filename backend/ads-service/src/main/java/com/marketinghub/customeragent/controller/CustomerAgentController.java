package com.marketinghub.customeragent.controller;

import static com.marketinghub.customeragent.service.CustomerAgentContracts.*;

import com.marketinghub.customeragent.service.CustomerAgentService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: expor Biblioteca de Personas e Agente Cliente v1. */
@RestController
@RequestMapping("/api/customer-agent/v1")
public class CustomerAgentController {
  private final CustomerAgentService service;

  public CustomerAgentController(CustomerAgentService service) {
    this.service = service;
  }

  /** Cadastra uma persona baseada em evidencias. */
  @PostMapping("/personas")
  public PersonaResponse createPersona(@RequestBody SavePersonaRequest request) {
    return service.createPersona(request);
  }

  /** Lista personas ativas. */
  @GetMapping("/personas")
  public List<PersonaResponse> listPersonas() {
    return service.listPersonas();
  }

  /** Solicita avaliacao simulada de um ativo. */
  @PostMapping("/evaluations")
  public EvaluationResponse start(@RequestBody StartEvaluationRequest request) {
    return service.start(request);
  }

  /** Lista avaliacoes e resultados humanos separados. */
  @GetMapping("/evaluations")
  public List<EvaluationResponse> list() {
    return service.listEvaluations();
  }

  /** Reserva a proxima avaliacao para o worker. */
  @PostMapping("/internal/evaluations/pending/claim")
  public EvaluationResponse claim() {
    return service.claimPending();
  }

  /** Recebe a avaliacao simulada. */
  @PostMapping("/internal/evaluations/{id}/complete")
  public EvaluationResponse complete(
      @PathVariable Long id, @RequestBody CompleteEvaluationRequest request) {
    return service.complete(id, request);
  }

  /** Registra resultado humano posterior por fluxo oficial. */
  @PostMapping("/evaluations/{id}/human-result")
  public EvaluationResponse humanResult(
      @PathVariable Long id, @RequestBody RecordHumanResultRequest request) {
    return service.recordHumanResult(id, request);
  }
}

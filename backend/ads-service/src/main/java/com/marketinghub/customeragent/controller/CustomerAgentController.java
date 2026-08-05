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

  /** Solicita uma experiencia digital observacional para uma persona. */
  @PostMapping("/digital-observations")
  public DigitalObservationResponse startObservation(
      @RequestBody StartDigitalObservationRequest request) {
    return service.startObservation(request);
  }

  /** Lista a memoria digital observacional auditavel. */
  @GetMapping("/digital-observations")
  public List<DigitalObservationResponse> listObservations() {
    return service.listObservations();
  }

  /** Reserva uma experiencia digital pendente para o worker. */
  @PostMapping("/internal/digital-observations/pending/claim")
  public DigitalObservationResponse claimObservation() {
    return service.claimPendingObservation();
  }

  /** Recebe observacao, reacao simulada e hipotese em camadas separadas. */
  @PostMapping("/internal/digital-observations/{id}/complete")
  public DigitalObservationResponse completeObservation(
      @PathVariable Long id, @RequestBody CompleteDigitalObservationRequest request) {
    return service.completeObservation(id, request);
  }

  /** Registra confirmacao humana posterior por fonte oficial. */
  @PostMapping("/digital-observations/{id}/human-confirmation")
  public DigitalObservationResponse confirmObservation(
      @PathVariable Long id, @RequestBody RecordObservationHumanConfirmationRequest request) {
    return service.recordObservationHumanConfirmation(id, request);
  }
}

package com.marketinghub.customeragent.controller;

import static com.marketinghub.customeragent.service.CustomerAgentContracts.*;

import com.marketinghub.customeragent.memory.CustomerAgentMemoryEvidenceService;
import com.marketinghub.customeragent.memory.CustomerAgentMemoryEvidenceService.EvidenceResponse;
import com.marketinghub.customeragent.service.CustomerAgentMotivationService;
import com.marketinghub.customeragent.service.CustomerAgentService;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Responsabilidade: expor Biblioteca de Personas e Agente Cliente v1. */
@RestController
@RequestMapping("/api/customer-agent/v1")
public class CustomerAgentController {
  private final CustomerAgentService service;
  private final CustomerAgentMemoryEvidenceService memoryEvidenceService;
  private final CustomerAgentMotivationService motivationService;

  /** Inicializa os contratos do Agente Cliente e seu armazenamento de evidencias. */
  public CustomerAgentController(
      CustomerAgentService service,
      CustomerAgentMemoryEvidenceService memoryEvidenceService,
      CustomerAgentMotivationService motivationService) {
    this.service = service;
    this.memoryEvidenceService = memoryEvidenceService;
    this.motivationService = motivationService;
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

  /** Recebe falha terminal de uma avaliação reservada. */
  @PostMapping("/internal/evaluations/{id}/fail")
  public EvaluationResponse failEvaluation(
      @PathVariable Long id, @RequestBody FailExecutionRequest request) {
    return service.failEvaluation(id, request);
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

  /** Recebe falha terminal de uma observação reservada. */
  @PostMapping("/internal/digital-observations/{id}/fail")
  public DigitalObservationResponse failObservation(
      @PathVariable Long id, @RequestBody FailExecutionRequest request) {
    return service.failObservation(id, request);
  }

  /** Registra confirmacao humana posterior por fonte oficial. */
  @PostMapping("/digital-observations/{id}/human-confirmation")
  public DigitalObservationResponse confirmObservation(
      @PathVariable Long id, @RequestBody RecordObservationHumanConfirmationRequest request) {
    return service.recordObservationHumanConfirmation(id, request);
  }

  /** Armazena evidencia pesada no S3 e sua procedencia canônica no MySQL. */
  @PostMapping(
      value = "/personas/{personaId}/memory-evidence",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public EvidenceResponse uploadMemoryEvidence(
      @PathVariable Long personaId,
      @RequestParam(required = false) Long observationId,
      @RequestParam String memoryLayer,
      @RequestParam(required = false) String sourceUrl,
      @RequestPart MultipartFile file)
      throws java.io.IOException {
    return memoryEvidenceService.store(personaId, observationId, memoryLayer, sourceUrl, file);
  }

  /** Lista metadados auditaveis das evidencias da persona. */
  @GetMapping("/personas/{personaId}/memory-evidence")
  public List<EvidenceResponse> listMemoryEvidence(@PathVariable Long personaId) {
    return memoryEvidenceService.list(personaId);
  }

  /** Lista pesos motivacionais com origem simulada ou humana explicitamente separada. */
  @GetMapping("/personas/{personaId}/memory-motivations")
  public List<MotivationalVectorResponse> listMemoryMotivations(@PathVariable Long personaId) {
    return motivationService.list(personaId);
  }

  /** Entrega evidencia privada por rota governada do backend. */
  @GetMapping("/memory-evidence/{id}/content")
  public ResponseEntity<byte[]> readMemoryEvidence(@PathVariable Long id) {
    var content = memoryEvidenceService.read(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(content.contentType()))
        .body(content.bytes());
  }
}

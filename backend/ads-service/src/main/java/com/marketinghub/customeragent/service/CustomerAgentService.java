package com.marketinghub.customeragent.service;

import static com.marketinghub.customeragent.service.CustomerAgentContracts.*;

import com.marketinghub.customeragent.CustomerAgentEvaluation;
import com.marketinghub.customeragent.CustomerDigitalObservation;
import com.marketinghub.customeragent.CustomerPersona;
import com.marketinghub.repository.jpa.customeragent.CustomerAgentEvaluationRepository;
import com.marketinghub.repository.jpa.customeragent.CustomerDigitalObservationRepository;
import com.marketinghub.repository.jpa.customeragent.CustomerPersonaRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: governar personas e avaliacoes simuladas sem fabricar validacao humana. */
@Service
public class CustomerAgentService {
  private static final int ASSET_REFERENCE_MAX_LENGTH = 255;
  private static final String BASELINE_V1 = "BASELINE_V1";
  private static final String BEHAVIORAL_V1 = "BEHAVIORAL_V1";
  private static final String BEHAVIORAL_V2 = "BEHAVIORAL_V2";
  private static final long OBSERVATION_LEASE_MINUTES = 15;
  private static final int MAX_EXPIRED_OBSERVATIONS_PER_CLAIM = 20;
  private final CustomerPersonaRepository personas;
  private final CustomerAgentEvaluationRepository evaluations;
  private final CustomerDigitalObservationRepository observations;
  private final CustomerAgentMotivationService motivations;

  /** Inicializa a governanca de personas, observacoes e memoria motivacional. */
  public CustomerAgentService(
      CustomerPersonaRepository personas,
      CustomerAgentEvaluationRepository evaluations,
      CustomerDigitalObservationRepository observations,
      CustomerAgentMotivationService motivations) {
    this.personas = personas;
    this.evaluations = evaluations;
    this.observations = observations;
    this.motivations = motivations;
  }

  /** Agenda uma experiencia mobile limitada a fontes publicas explicitamente autorizadas. */
  @Transactional
  public DigitalObservationResponse startObservation(StartDigitalObservationRequest request) {
    if (request == null || blank(request.objective()) || blank(request.authorizedSourcesJson())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Observacao exige objetivo e fontes autorizadas.");
    }
    String sources = request.authorizedSourcesJson().trim();
    if (!sources.startsWith("[")
        || sources.contains("localhost")
        || sources.contains("127.0.0.1")) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Fontes devem ser uma lista publica governada.");
    }
    CustomerDigitalObservation value = new CustomerDigitalObservation();
    value.setPersona(findPersona(request.personaId()));
    value.setObjective(request.objective());
    value.setAuthorizedSourcesJson(sources);
    value.setDeviceProfile(blank(request.deviceProfile()) ? "MOBILE" : request.deviceProfile());
    value.setStatus("PENDING");
    return observationResponse(observations.save(value));
  }

  /** Reserva a proxima experiencia digital para o worker observacional. */
  @Transactional
  public DigitalObservationResponse claimPendingObservation() {
    expireAbandonedObservations();
    List<CustomerDigitalObservation> pending =
        observations.findByStatusOrderByCreatedAtAsc("PENDING", PageRequest.of(0, 1));
    if (pending.isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhuma observacao pendente.");
    CustomerDigitalObservation value = pending.getFirst();
    value.setStatus("RUNNING");
    value.setStartedAt(Instant.now());
    return observationResponse(observations.save(value));
  }

  /** Encerra reservas expiradas para que falhas de processo não permaneçam como execução ativa. */
  private void expireAbandonedObservations() {
    Instant threshold = Instant.now().minus(OBSERVATION_LEASE_MINUTES, ChronoUnit.MINUTES);
    List<CustomerDigitalObservation> abandoned =
        observations.findByStatusAndStartedAtBeforeOrderByStartedAtAsc(
            "RUNNING", threshold, PageRequest.of(0, MAX_EXPIRED_OBSERVATIONS_PER_CLAIM));
    Instant finishedAt = Instant.now();
    abandoned.forEach(
        observation -> {
          observation.setStatus("FAILED");
          observation.setFinishedAt(finishedAt);
          observation.setRawModelResponse(
              "Execução encerrada automaticamente: lease observacional expirado sem callback.");
        });
    if (!abandoned.isEmpty()) observations.saveAll(abandoned);
  }

  /** Persiste as quatro camadas sem promover hipotese a aprendizado humano. */
  @Transactional
  public DigitalObservationResponse completeObservation(
      Long id, CompleteDigitalObservationRequest request) {
    CustomerDigitalObservation value = findRunningObservation(id);
    if (request == null
        || blank(request.observationJson())
        || blank(request.simulatedReactionJson())
        || blank(request.commercialHypothesisJson())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Observacao incompleta.");
    }
    value.setObservationJson(request.observationJson());
    value.setSimulatedReactionJson(request.simulatedReactionJson());
    value.setCommercialHypothesisJson(request.commercialHypothesisJson());
    value.setRawModelResponse(request.rawModelResponse());
    value.setModelName(request.model());
    value.setStatus("COMPLETED");
    value.setFinishedAt(Instant.now());
    CustomerDigitalObservation completed = observations.save(value);
    motivations.recordSimulated(completed, request.motivationalVector());
    return observationResponse(completed);
  }

  /** Encerra uma observação com falha técnica auditável. */
  @Transactional
  public DigitalObservationResponse failObservation(Long id, FailExecutionRequest request) {
    CustomerDigitalObservation value = findRunningObservation(id);
    value.setStatus("FAILED");
    value.setRawModelResponse(request == null ? "Falha não informada." : request.error());
    value.setFinishedAt(Instant.now());
    return observationResponse(observations.save(value));
  }

  /** Registra confirmacao humana posterior sem alterar a observacao original. */
  @Transactional
  public DigitalObservationResponse recordObservationHumanConfirmation(
      Long id, RecordObservationHumanConfirmationRequest request) {
    CustomerDigitalObservation value =
        observations
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (request == null || blank(request.humanConfirmationJson()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirmacao humana vazia.");
    value.setHumanConfirmationJson(request.humanConfirmationJson());
    CustomerDigitalObservation confirmed = observations.save(value);
    motivations.recordHumanConfirmed(confirmed, request.motivationalVector());
    return observationResponse(confirmed);
  }

  /** Lista a memoria observacional sem fundir suas camadas. */
  @Transactional(readOnly = true)
  public List<DigitalObservationResponse> listObservations() {
    return observations.findAll().stream().map(this::observationResponse).toList();
  }

  /** Cadastra uma persona como hipotese auditavel sustentada por ao menos uma evidencia. */
  @Transactional
  public PersonaResponse createPersona(SavePersonaRequest request) {
    if (request == null
        || blank(request.personaKey())
        || blank(request.name())
        || blank(request.evidenceJson())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Persona exige chave, nome e evidencias.");
    }
    CustomerPersona persona = new CustomerPersona();
    persona.setProductId(request.productId());
    persona.setPersonaKey(request.personaKey());
    persona.setName(request.name());
    persona.setConfidenceLevel(normalizeConfidence(request.confidenceLevel()));
    persona.setLifeContext(required(request.lifeContext()));
    persona.setPain(required(request.pain()));
    persona.setDesiredProgress(required(request.desiredProgress()));
    persona.setAwarenessLevel(request.awarenessLevel());
    persona.setObjections(request.objections());
    persona.setTrustCriteria(request.trustCriteria());
    persona.setLanguageSamples(request.languageSamples());
    persona.setEvidenceJson(request.evidenceJson());
    return personaResponse(personas.save(persona));
  }

  /** Lista somente as personas ativas que podem compor o prompt. */
  @Transactional(readOnly = true)
  public List<PersonaResponse> listPersonas() {
    return personas.findByActiveTrueOrderByNameAsc().stream().map(this::personaResponse).toList();
  }

  /** Cria uma avaliacao pendente preservando a referencia exata do ativo. */
  @Transactional
  public EvaluationResponse start(StartEvaluationRequest request) {
    if (request == null || blank(request.assetType()) || blank(request.assetReference())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Ativo da avaliacao e obrigatorio.");
    }
    if (request.assetReference().length() > ASSET_REFERENCE_MAX_LENGTH) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Referencia do ativo deve ter no maximo " + ASSET_REFERENCE_MAX_LENGTH + " caracteres.");
    }
    String simulationVersion = normalizeSimulationVersion(request.simulationVersion());
    CustomerAgentEvaluation evaluation = new CustomerAgentEvaluation();
    evaluation.setPersona(findPersona(request.personaId()));
    evaluation.setAssetType(request.assetType());
    evaluation.setAssetReference(request.assetReference());
    evaluation.setSimulationVersion(simulationVersion);
    evaluation.setStatus("PENDING");
    return evaluationResponse(evaluations.save(evaluation));
  }

  /** Reserva uma avaliacao pendente para o worker somente leitura. */
  @Transactional
  public EvaluationResponse claimPending() {
    List<CustomerAgentEvaluation> pending =
        evaluations.findByStatusOrderByCreatedAtAsc("PENDING", PageRequest.of(0, 1));
    if (pending.isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhuma avaliacao pendente.");
    CustomerAgentEvaluation evaluation = pending.getFirst();
    evaluation.setStatus("RUNNING");
    evaluation.setStartedAt(Instant.now());
    return evaluationResponse(evaluations.save(evaluation));
  }

  /** Persiste apenas a simulacao e suas hipoteses, nunca um resultado humano. */
  @Transactional
  public EvaluationResponse complete(Long id, CompleteEvaluationRequest request) {
    CustomerAgentEvaluation evaluation = findRunning(id);
    if (request == null || blank(request.assessment()) || blank(request.hypothesisJson())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avaliacao simulada incompleta.");
    }
    evaluation.setSimulatedAssessment(request.assessment());
    evaluation.setHypothesisJson(request.hypothesisJson());
    evaluation.setBaselineResultJson(request.baselineResultJson());
    evaluation.setBehavioralResultJson(request.behavioralResultJson());
    evaluation.setRawModelResponse(request.rawModelResponse());
    evaluation.setLastError(null);
    evaluation.setModelName(request.model());
    evaluation.setStatus("COMPLETED");
    evaluation.setFinishedAt(Instant.now());
    return evaluationResponse(evaluations.save(evaluation));
  }

  /** Registra separadamente o comportamento humano obtido por fonte oficial posterior. */
  @Transactional
  public EvaluationResponse recordHumanResult(Long id, RecordHumanResultRequest request) {
    CustomerAgentEvaluation evaluation =
        evaluations
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (request == null || blank(request.humanResultJson()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resultado humano vazio.");
    evaluation.setHumanResultJson(request.humanResultJson());
    return evaluationResponse(evaluations.save(evaluation));
  }

  /** Lista o historico completo sem fundir simulacao e realidade. */
  @Transactional(readOnly = true)
  public List<EvaluationResponse> listEvaluations() {
    return evaluations.findAll().stream().map(this::evaluationResponse).toList();
  }

  /** Entrega ao MCP o contexto congelado de uma avaliacao pelo identificador reservado. */
  @Transactional(readOnly = true)
  public EvaluationResponse getEvaluation(Long id) {
    return evaluationResponse(findRunning(id));
  }

  /** Encerra uma avaliação com falha técnica sem fabricar resultado simulado. */
  @Transactional
  public EvaluationResponse failEvaluation(Long id, FailExecutionRequest request) {
    CustomerAgentEvaluation value = findRunning(id);
    value.setStatus("FAILED");
    String error =
        request == null || blank(request.error()) ? "Falha não informada." : request.error();
    value.setLastError(error);
    value.setRawModelResponse(error);
    value.setFinishedAt(Instant.now());
    return evaluationResponse(evaluations.save(value));
  }

  /** Reabre somente uma avaliação falha, preservando sua identidade e a última causa técnica. */
  @Transactional
  public EvaluationResponse retryEvaluation(Long id) {
    CustomerAgentEvaluation value =
        evaluations
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!"FAILED".equals(value.getStatus())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Somente avaliações com falha podem ser reprocessadas.");
    }
    value.setStatus("PENDING");
    value.setStartedAt(null);
    value.setFinishedAt(null);
    value.setRetryCount((value.getRetryCount() == null ? 0 : value.getRetryCount()) + 1);
    return evaluationResponse(evaluations.save(value));
  }

  /** Busca a persona solicitada. */
  private CustomerPersona findPersona(Long id) {
    return personas
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  /** Busca uma observacao legitimamente reservada pelo worker. */
  private CustomerDigitalObservation findRunningObservation(Long id) {
    CustomerDigitalObservation value =
        observations
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!"RUNNING".equals(value.getStatus()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Observacao fora de execucao.");
    return value;
  }

  /** Busca avaliacao legitimamente em execucao. */
  private CustomerAgentEvaluation findRunning(Long id) {
    CustomerAgentEvaluation value =
        evaluations
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!"RUNNING".equals(value.getStatus()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Avaliacao fora de execucao.");
    return value;
  }

  /** Impede que uma simulacao nasca marcada como validada. */
  private String normalizeConfidence(String value) {
    return "PARCIALMENTE_VALIDADA".equals(value) ? value : "HIPOTESE";
  }

  /** Aceita somente versões implementadas e usa Psique humana v2 nas novas avaliações. */
  private String normalizeSimulationVersion(String value) {
    if (blank(value)) return BEHAVIORAL_V2;
    if (BASELINE_V1.equals(value) || BEHAVIORAL_V1.equals(value) || BEHAVIORAL_V2.equals(value))
      return value;
    throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Versão de simulação não suportada: " + value);
  }

  /** Exige texto nos campos estruturantes. */
  private String required(String value) {
    if (blank(value))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Persona incompleta.");
    return value;
  }

  /** Detecta texto ausente. */
  private boolean blank(String value) {
    return value == null || value.isBlank();
  }

  /** Converte persona para resposta sem dados implicitos. */
  private PersonaResponse personaResponse(CustomerPersona p) {
    return new PersonaResponse(
        p.getId(),
        p.getProductId(),
        p.getPersonaKey(),
        p.getName(),
        p.getVersionNumber(),
        p.getConfidenceLevel(),
        p.getLifeContext(),
        p.getPain(),
        p.getDesiredProgress(),
        p.getAwarenessLevel(),
        p.getObjections(),
        p.getTrustCriteria(),
        p.getLanguageSamples(),
        p.getEvidenceJson(),
        p.getUpdatedAt());
  }

  /** Converte avaliacao preservando a separacao entre previsao e resultado. */
  private EvaluationResponse evaluationResponse(CustomerAgentEvaluation e) {
    return new EvaluationResponse(
        e.getId(),
        e.getPersona().getId(),
        personaResponse(e.getPersona()),
        e.getAssetType(),
        e.getAssetReference(),
        e.getSimulationVersion(),
        e.getStatus(),
        e.getSimulatedAssessment(),
        e.getHypothesisJson(),
        e.getBaselineResultJson(),
        e.getBehavioralResultJson(),
        e.getHumanResultJson(),
        e.getLastError(),
        e.getRetryCount(),
        e.getModelName(),
        e.getStartedAt(),
        e.getFinishedAt(),
        e.getCreatedAt());
  }

  /** Converte a memoria observacional preservando todas as fronteiras de confianca. */
  private DigitalObservationResponse observationResponse(CustomerDigitalObservation value) {
    return new DigitalObservationResponse(
        value.getId(),
        personaResponse(value.getPersona()),
        value.getObjective(),
        value.getAuthorizedSourcesJson(),
        value.getStatus(),
        value.getDeviceProfile(),
        value.getObservationJson(),
        value.getSimulatedReactionJson(),
        value.getCommercialHypothesisJson(),
        value.getHumanConfirmationJson(),
        value.getModelName(),
        value.getStartedAt(),
        value.getFinishedAt(),
        value.getCreatedAt());
  }
}

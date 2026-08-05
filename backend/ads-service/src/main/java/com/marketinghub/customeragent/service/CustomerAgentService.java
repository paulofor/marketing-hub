package com.marketinghub.customeragent.service;

import static com.marketinghub.customeragent.service.CustomerAgentContracts.*;

import com.marketinghub.customeragent.CustomerAgentEvaluation;
import com.marketinghub.customeragent.CustomerPersona;
import com.marketinghub.repository.jpa.customeragent.CustomerAgentEvaluationRepository;
import com.marketinghub.repository.jpa.customeragent.CustomerPersonaRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: governar personas e avaliacoes simuladas sem fabricar validacao humana. */
@Service
public class CustomerAgentService {
  private final CustomerPersonaRepository personas;
  private final CustomerAgentEvaluationRepository evaluations;

  public CustomerAgentService(
      CustomerPersonaRepository personas, CustomerAgentEvaluationRepository evaluations) {
    this.personas = personas;
    this.evaluations = evaluations;
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
    CustomerAgentEvaluation evaluation = new CustomerAgentEvaluation();
    evaluation.setPersona(findPersona(request.personaId()));
    evaluation.setAssetType(request.assetType());
    evaluation.setAssetReference(request.assetReference());
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
    evaluation.setRawModelResponse(request.rawModelResponse());
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

  /** Busca a persona solicitada. */
  private CustomerPersona findPersona(Long id) {
    return personas
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
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
        e.getStatus(),
        e.getSimulatedAssessment(),
        e.getHypothesisJson(),
        e.getHumanResultJson(),
        e.getModelName(),
        e.getStartedAt(),
        e.getFinishedAt(),
        e.getCreatedAt());
  }
}

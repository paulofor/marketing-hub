package com.marketinghub.customeragent.service;

import static com.marketinghub.customeragent.service.CustomerAgentContracts.*;

import com.marketinghub.customeragent.CustomerAgentMemoryMotivation;
import com.marketinghub.customeragent.CustomerDigitalObservation;
import com.marketinghub.repository.jpa.customeragent.CustomerAgentMemoryMotivationRepository;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar e preservar pesos motivacionais simulados e humanos sem mistura. */
@Service
public class CustomerAgentMotivationService {
  private static final Set<String> DIRECTIONS =
      Set.of("AWAY_FROM_PAIN", "TOWARD_PLEASURE", "MIXED");
  private final CustomerAgentMemoryMotivationRepository repository;

  /** Inicializa a trilha motivacional append-only. */
  public CustomerAgentMotivationService(CustomerAgentMemoryMotivationRepository repository) {
    this.repository = repository;
  }

  /** Registra uma hipotese calculada pelo agente sem autoridade de confirmacao. */
  @Transactional
  public MotivationalVectorResponse recordSimulated(
      CustomerDigitalObservation observation, MotivationalVectorRequest request) {
    return record(observation, request, "SIMULATED_HYPOTHESIS");
  }

  /** Registra pesos derivados exclusivamente de resultado humano oficial. */
  @Transactional
  public MotivationalVectorResponse recordHumanConfirmed(
      CustomerDigitalObservation observation, MotivationalVectorRequest request) {
    return record(observation, request, "HUMAN_CONFIRMED");
  }

  /** Lista o historico completo da persona sem consolidacao destrutiva. */
  @Transactional(readOnly = true)
  public List<MotivationalVectorResponse> list(Long personaId) {
    return repository.findByPersonaIdOrderByCreatedAtDesc(personaId).stream()
        .map(this::response)
        .toList();
  }

  /** Valida escala, procedencia e direcao antes de anexar o vetor à memoria. */
  private MotivationalVectorResponse record(
      CustomerDigitalObservation observation, MotivationalVectorRequest request, String origin) {
    if (request == null
        || !DIRECTIONS.contains(request.motivationalDirection())
        || blank(request.sourceReference())
        || blank(request.rationale())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Vetor motivacional exige direcao, fonte e justificativa.");
    }
    Integer[] weights =
        new Integer[] {
          request.painIntensity(),
          request.pleasureIntensity(),
          request.fearWeight(),
          request.frustrationWeight(),
          request.effortWeight(),
          request.reliefWeight(),
          request.desireWeight(),
          request.trustWeight(),
          request.belongingWeight(),
          request.evidenceStrength(),
          request.confidenceScore()
        };
    if (java.util.Arrays.stream(weights)
        .anyMatch(value -> value == null || value < 0 || value > 5)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pesos devem estar entre 0 e 5.");
    }
    CustomerAgentMemoryMotivation value = new CustomerAgentMemoryMotivation();
    value.setPersona(observation.getPersona());
    value.setObservation(observation);
    value.setOriginType(origin);
    value.setMotivationalDirection(request.motivationalDirection());
    value.setPainIntensity(request.painIntensity());
    value.setPleasureIntensity(request.pleasureIntensity());
    value.setFearWeight(request.fearWeight());
    value.setFrustrationWeight(request.frustrationWeight());
    value.setEffortWeight(request.effortWeight());
    value.setReliefWeight(request.reliefWeight());
    value.setDesireWeight(request.desireWeight());
    value.setTrustWeight(request.trustWeight());
    value.setBelongingWeight(request.belongingWeight());
    value.setEvidenceStrength(request.evidenceStrength());
    value.setConfidenceScore(request.confidenceScore());
    value.setSourceReference(request.sourceReference());
    value.setRationale(request.rationale());
    return response(repository.save(value));
  }

  /** Detecta texto obrigatorio ausente. */
  private boolean blank(String value) {
    return value == null || value.isBlank();
  }

  /** Converte o registro persistido para contrato auditavel. */
  private MotivationalVectorResponse response(CustomerAgentMemoryMotivation value) {
    return new MotivationalVectorResponse(
        value.getId(),
        value.getPersona().getId(),
        value.getObservation().getId(),
        value.getOriginType(),
        value.getMotivationalDirection(),
        value.getPainIntensity(),
        value.getPleasureIntensity(),
        value.getFearWeight(),
        value.getFrustrationWeight(),
        value.getEffortWeight(),
        value.getReliefWeight(),
        value.getDesireWeight(),
        value.getTrustWeight(),
        value.getBelongingWeight(),
        value.getEvidenceStrength(),
        value.getConfidenceScore(),
        value.getSourceReference(),
        value.getRationale(),
        value.getCreatedAt());
  }
}

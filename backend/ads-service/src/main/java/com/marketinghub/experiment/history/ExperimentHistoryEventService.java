package com.marketinghub.experiment.history;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.history.ExperimentHistoryEventContracts.CreateRequest;
import com.marketinghub.experiment.history.ExperimentHistoryEventContracts.Response;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar e registrar fatos auditáveis sem convertê-los em inferências. */
@Service
public class ExperimentHistoryEventService {
  private final ExperimentRepository experimentRepository;
  private final ExperimentHistoryEventRepository historyRepository;
  private final ObjectMapper objectMapper;

  /** Inicializa o serviço com persistência do experimento, do histórico e validação JSON. */
  public ExperimentHistoryEventService(
      ExperimentRepository experimentRepository,
      ExperimentHistoryEventRepository historyRepository,
      ObjectMapper objectMapper) {
    this.experimentRepository = experimentRepository;
    this.historyRepository = historyRepository;
    this.objectMapper = objectMapper;
  }

  /** Registra uma ocorrência mantendo evidência, origem e data informadas. */
  @Transactional
  public Response create(Long experimentId, CreateRequest request) {
    Experiment experiment =
        experimentRepository
            .findById(experimentId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Experimento não encontrado"));
    validateEvidence(request.evidenceJson());
    ExperimentHistoryEvent event = new ExperimentHistoryEvent();
    event.setExperiment(experiment);
    event.setCategory(request.category());
    event.setTitle(request.title().trim());
    event.setDescription(request.description().trim());
    event.setEvidenceJson(blankToNull(request.evidenceJson()));
    event.setSource(blankToNull(request.source()));
    event.setOccurredAt(request.occurredAt());
    return toResponse(historyRepository.save(event));
  }

  /** Lista o histórico de um único experimento em ordem cronológica reversa. */
  @Transactional(readOnly = true)
  public List<Response> list(Long experimentId) {
    if (!experimentRepository.existsById(experimentId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Experimento não encontrado");
    }
    return historyRepository.findByExperimentIdOrderByOccurredAtDescIdDesc(experimentId).stream()
        .map(this::toResponse)
        .toList();
  }

  /** Rejeita evidência que não seja um objeto ou array JSON válido. */
  private void validateEvidence(String evidenceJson) {
    if (evidenceJson == null || evidenceJson.isBlank()) return;
    try {
      objectMapper.readTree(evidenceJson);
    } catch (Exception ex) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "evidenceJson deve conter JSON válido", ex);
    }
  }

  /** Converte texto vazio em ausência de valor. */
  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  /** Converte a entidade persistida no contrato público imutável. */
  private Response toResponse(ExperimentHistoryEvent event) {
    return new Response(
        event.getId(),
        event.getCategory(),
        event.getTitle(),
        event.getDescription(),
        event.getEvidenceJson(),
        event.getSource(),
        event.getOccurredAt(),
        event.getCreatedAt());
  }
}

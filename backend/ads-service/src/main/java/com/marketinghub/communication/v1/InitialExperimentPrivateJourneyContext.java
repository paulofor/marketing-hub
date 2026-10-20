package com.marketinghub.communication.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.CommunicationMaterializationContextProvider;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Responsabilidade: delimitar a jornada privada do primeiro experimento pelo contexto vigente. */
@Component
@RequiredArgsConstructor
@Slf4j
public class InitialExperimentPrivateJourneyContext {
  private final CommunicationMaterializationContextProvider contexts;
  private final ExperimentRepository experiments;
  private final LearningSalesCycleRepository cycles;
  private final ObjectMapper json;

  /** Reconhece o modo privado mesmo bloqueado, sem convertê-lo em aprovação comercial. */
  public Optional<Map<String, Object>> resolve(String reference) {
    if (reference == null || !reference.matches("experiment:[1-9][0-9]*")) return Optional.empty();
    return contexts
        .resolve(reference)
        .filter(
            input ->
                IrisCommunicationMaterializationContextProvider.INITIAL_EXPERIMENT_PRIVATE_MODE
                    .equals(input.get("mode")))
        .map(input -> validate(reference, input));
  }

  /** Confere a identidade persistida e impede novas conclusões depois da preparação. */
  private Map<String, Object> validate(String reference, Map<String, Object> input) {
    var result = new LinkedHashMap<>(input);
    long id = Long.parseLong(reference.substring(11));
    try {
      var experiment =
          experiments
              .findById(id)
              .orElseThrow(
                  () -> new IllegalStateException("O primeiro experimento não foi encontrado."));
      var node = json.valueToTree(input);
      if (experiment.getProduct() == null
          || experiment.getProduct().getId() != node.path("product").path("id").asLong()
          || !reference.equals(node.path("sourceReference").asText())
          || id != node.path("experiment").path("id").asLong()
          || experiment.getSourceExperiment() != null
          || cycles.findByExperimentId(id).isPresent()) {
        throw new IllegalStateException(
            "Produto, origem ou ciclo divergente na jornada do primeiro experimento.");
      }
      if (experiment.getStatus() != ExperimentStatus.PLANNED) {
        result.put("initialPrivateJourneyArchived", true);
        result.put("inputReadiness", "BLOCKED");
        result.put(
            "reason",
            "A preparação privada do primeiro experimento já terminou; preserve suas provas históricas.");
      }
    } catch (RuntimeException ex) {
      log.warn(
          "Contexto da jornada inicial bloqueado. sourceReference={} experimentId={}",
          reference,
          id,
          ex);
      result.put("inputReadiness", "BLOCKED");
      result.put("reason", ex.getMessage());
    }
    return java.util.Collections.unmodifiableMap(result);
  }
}

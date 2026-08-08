package com.marketinghub.experiment.history;

import com.marketinghub.experiment.history.ExperimentHistoryEventContracts.CreateRequest;
import com.marketinghub.experiment.history.ExperimentHistoryEventContracts.Response;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor o diário factual e auditável de cada experimento. */
@RestController
@RequestMapping("/api/experiments/{experimentId}/history-events")
public class ExperimentHistoryEventController {
  private final ExperimentHistoryEventService service;

  /** Inicializa o controller com o serviço canônico do histórico. */
  public ExperimentHistoryEventController(ExperimentHistoryEventService service) {
    this.service = service;
  }

  /** Lista todas as ocorrências registradas no experimento. */
  @GetMapping
  public List<Response> list(@PathVariable Long experimentId) {
    return service.list(experimentId);
  }

  /** Registra uma nova ocorrência informada pela operação. */
  @PostMapping
  public Response create(
      @PathVariable Long experimentId, @Valid @RequestBody CreateRequest request) {
    return service.create(experimentId, request);
  }
}

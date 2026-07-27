package com.marketinghub.experiment.web;

import com.marketinghub.experiment.dto.CreateExperimentAudienceTestRequest;
import com.marketinghub.experiment.dto.ExperimentAudienceTestDto;
import com.marketinghub.experiment.service.ExperimentAudienceTestService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Expõe os endpoints de planejamento de testes de público por experimento. */
@RestController
@RequestMapping("/api/experiments/{experimentId}/audience-tests")
public class ExperimentAudienceTestController {
  private final ExperimentAudienceTestService service;

  public ExperimentAudienceTestController(ExperimentAudienceTestService service) {
    this.service = service;
  }

  /** Lista as variações de público planejadas para o experimento. */
  @GetMapping
  public List<ExperimentAudienceTestDto> list(@PathVariable Long experimentId) {
    return service.list(experimentId);
  }

  /** Cria uma nova variação de público em rascunho. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ExperimentAudienceTestDto create(
      @PathVariable Long experimentId,
      @Valid @RequestBody CreateExperimentAudienceTestRequest request) {
    return service.create(experimentId, request);
  }

  /** Remove uma variação de público que ainda não está em execução. */
  @DeleteMapping("/{audienceTestId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long experimentId, @PathVariable Long audienceTestId) {
    service.delete(experimentId, audienceTestId);
  }
}

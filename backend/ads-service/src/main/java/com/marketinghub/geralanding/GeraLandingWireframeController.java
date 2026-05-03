package com.marketinghub.geralanding;

import com.marketinghub.experiment.pipeline.ExperimentPipelineSection;
import com.marketinghub.experiment.pipeline.dto.ExperimentPipelineGenerationRequest;
import com.marketinghub.experiment.pipeline.service.ExperimentPipelineGenerationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/experiments/{experimentId}/geralanding")
public class GeraLandingWireframeController {

  private final ExperimentPipelineGenerationService generationService;
  private final GeraLandingStageExecutionService executionService;

  public GeraLandingWireframeController(
          ExperimentPipelineGenerationService generationService,
          GeraLandingStageExecutionService executionService) {
    this.generationService = generationService;
    this.executionService = executionService;
  }

  @PostMapping("/wireframe/start")
  public ResponseEntity<Void> startWireframe(
          @PathVariable Long experimentId,
          @Valid @RequestBody GeraLandingStageStartRequest request) {
    executionService.register(experimentId, request);
    generationService.generate(
            experimentId,
            ExperimentPipelineSection.LANDING_PAGE_WIREFRAME,
            new ExperimentPipelineGenerationRequest());
    return ResponseEntity.accepted().build();
  }
}

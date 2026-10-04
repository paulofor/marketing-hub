package com.marketinghub.experiment.run.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.*;
import com.marketinghub.experiment.run.*;
import com.marketinghub.experiment.run.controller.BackendExperimentRunController;
import com.marketinghub.repository.jpa.experiment.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: comprovar bloqueio HTTP antes de qualquer gravação no histórico encerrado. */
class ClosedExperimentPreflightHttpTest {
  /** Conserva provas e recusa comandos encerrados, mesmo sem gates, sem orientar nova tentativa. */
  @Test
  void keepsHistoricalProofsAndRejectsEveryMutation() throws Exception {
    var experiments = mock(ExperimentRepository.class);
    var runs = mock(ExperimentRunRepository.class);
    var gates = mock(ExperimentRunGateResultRepository.class);
    var dossiers = mock(MoisCommercialDossierPreflightService.class);
    var service = new BackendExperimentRunService(experiments, runs, gates, dossiers);
    var experiment = new Experiment();
    experiment.setId(97201L);
    experiment.setStatus(ExperimentStatus.INVALIDATED);
    var run = new ExperimentRun();
    run.setId(97202L);
    run.setExperiment(experiment);
    run.setMode(ExperimentRunMode.PRODUCTION);
    run.setStatus(ExperimentRunStatus.READY_TO_PUBLISH);
    var gate = new ExperimentRunGateResult();
    gate.setGateCode(ExperimentRunGateCodes.LANDING_QUALITY_REVIEW_APPROVED);
    gate.setStatus(ExperimentRunGateStatus.PASS);
    when(experiments.findById(experiment.getId())).thenReturn(Optional.of(experiment));
    when(runs.findById(run.getId())).thenReturn(Optional.of(run));
    when(runs.findForTechnicalHomologationRenewal(run.getId())).thenReturn(Optional.of(run));
    when(gates.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(run.getId()))
        .thenReturn(List.of(gate));
    var http = MockMvcBuilders.standaloneSetup(new BackendExperimentRunController(service)).build();
    String response =
        http.perform(get("/api/experiment-runs/{id}/preflight", run.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hasBlockers").value(true))
            .andExpect(jsonPath("$.canRenewTechnicalHomologation").value(false))
            .andExpect(
                jsonPath("$.executionBlockReason")
                    .value(org.hamcrest.Matchers.containsString("não renove Plutus")))
            .andExpect(jsonPath("$.gates[0].status").value("PASS"))
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    for (String path :
        List.of(
            "/api/experiments/97201/runs",
            "/api/experiment-runs/97202/preflight",
            "/api/experiment-runs/97202/technical-homologation-renewal",
            "/api/experiment-runs/97202/homologation-results")) {
      http.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{}"))
          .andExpect(status().isConflict());
    }
    when(gates.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(run.getId()))
        .thenReturn(List.of());
    http.perform(
            post("/api/experiment-runs/{id}/homologation-results", run.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isConflict());
    assertThat(run.getStatus()).isEqualTo(ExperimentRunStatus.READY_TO_PUBLISH);
    assertThat(gate.getStatus()).isEqualTo(ExperimentRunGateStatus.PASS);
    verify(runs, never()).save(any());
    verify(gates, never()).deleteByExperimentRunId(anyLong());
    verify(gates, never()).saveAll(any());
    verifyNoInteractions(dossiers);
    String output = System.getProperty("closed-preflight.fixture-output");
    if (output != null)
      Files.writeString(Path.of(output), new ObjectMapper().readTree(response).toPrettyString());
  }
}

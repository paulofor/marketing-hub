package com.marketinghub.experiment.run.controller;

import com.marketinghub.experiment.run.service.BackendExperimentRunService;
import com.marketinghub.experiment.run.service.create.CreateExperimentRunRequest;
import com.marketinghub.experiment.run.service.get.ExperimentRunResponse;
import com.marketinghub.experiment.run.service.preflight.ExperimentRunPreflightResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expõe endpoints administrativos para criação e consulta de execuções de experimento.
 */
@RestController
@RequestMapping("/api")
public class BackendExperimentRunController {
    private final BackendExperimentRunService service;

    /** Inicializa o controller com o serviço canônico de runs de experimento. */
    public BackendExperimentRunController(BackendExperimentRunService service) {
        this.service = service;
    }

    /** Cria uma nova tentativa operacional sequencial para o experimento informado. */
    @PostMapping("/experiments/{experimentId}/runs")
    public ExperimentRunResponse create(@PathVariable Long experimentId,
                                        @RequestBody(required = false) CreateExperimentRunRequest request) {
        return service.create(experimentId, request);
    }

    /** Lista as tentativas operacionais já criadas para um experimento. */
    @GetMapping("/experiments/{experimentId}/runs")
    public List<ExperimentRunResponse> listByExperiment(@PathVariable Long experimentId) {
        return service.listByExperiment(experimentId);
    }

    /** Consulta o resultado atual de preflight do run. */
    @GetMapping("/experiment-runs/{runId}/preflight")
    public ExperimentRunPreflightResponse getPreflight(@PathVariable Long runId) {
        return service.getPreflight(runId);
    }

    /** Executa a avaliação determinística inicial de preflight do run. */
    @PostMapping("/experiment-runs/{runId}/preflight")
    public ExperimentRunPreflightResponse runPreflight(@PathVariable Long runId) {
        return service.runPreflight(runId);
    }

    /** Retorna os detalhes de uma tentativa operacional pelo identificador do run. */
    @GetMapping("/experiment-runs/{runId}")
    public ExperimentRunResponse get(@PathVariable Long runId) {
        return service.get(runId);
    }
}

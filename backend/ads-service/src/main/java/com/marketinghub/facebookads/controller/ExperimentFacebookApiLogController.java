package com.marketinghub.facebookads.controller;

import com.marketinghub.facebookads.playbook.dto.ExperimentFacebookApiLogDto;
import com.marketinghub.facebookads.playbook.dto.ExperimentFacebookApiLogIngestionRequest;
import com.marketinghub.facebookads.playbook.service.ExperimentFacebookApiLogService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agrupa endpoints de ingestão e consulta dos logs da API Facebook por experimento.
 */
@RestController
@RequestMapping("/api/experiments/{experimentId}/facebook-api-logs")
public class ExperimentFacebookApiLogController {

    private final ExperimentFacebookApiLogService apiLogService;

    // Executa a operação ExperimentFacebookApiLogController da integração Facebook Ads.
    public ExperimentFacebookApiLogController(ExperimentFacebookApiLogService apiLogService) {
        this.apiLogService = apiLogService;
    }

    @GetMapping
    public List<ExperimentFacebookApiLogDto> getLogs(@PathVariable Long experimentId,
                                                     @RequestParam(name = "limit", defaultValue = "100") int limit) {
        return apiLogService.findLogs(experimentId, limit);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void registerLogs(@PathVariable Long experimentId,
                             @RequestBody(required = false) ExperimentFacebookApiLogIngestionRequest request) {
        apiLogService.registerLogs(experimentId, request);
    }
}

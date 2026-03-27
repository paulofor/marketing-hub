package com.marketinghub.experiment.learning.web;

import com.marketinghub.experiment.learning.ExperimentLearningStatus;
import com.marketinghub.experiment.learning.dto.ExperimentLearningPayloadDto;
import com.marketinghub.experiment.learning.dto.ExperimentLearningRequestDetailDto;
import com.marketinghub.experiment.learning.dto.UpdateExperimentLearningRequest;
import com.marketinghub.experiment.learning.mapper.ExperimentLearningRequestMapper;
import com.marketinghub.experiment.learning.service.ExperimentLearningRequestService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API consumida pelo worker para buscar e atualizar solicitações pendentes.
 */
@RestController
@RequestMapping("/api/experiment-learning-requests")
public class ExperimentLearningRequestProcessingController {

    private final ExperimentLearningRequestService service;
    private final ExperimentLearningRequestMapper mapper;

    public ExperimentLearningRequestProcessingController(ExperimentLearningRequestService service,
                                                         ExperimentLearningRequestMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<ExperimentLearningRequestDetailDto> list(@RequestParam(name = "status", required = false) List<ExperimentLearningStatus> statuses) {
        return service.listByStatus(statuses).stream()
                .map(mapper::toDetailDto)
                .toList();
    }

    @PatchMapping("/{id}")
    public ExperimentLearningRequestDetailDto update(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateExperimentLearningRequest request) {
        ExperimentLearningPayloadDto payload = request.payload();
        experimentValidation(request.status(), payload);
        return mapper.toDetailDto(service.updateStatus(id, request.status(), payload, request.failureReason()));
    }

    private void experimentValidation(ExperimentLearningStatus status, ExperimentLearningPayloadDto payload) {
        if (status == ExperimentLearningStatus.READY && payload == null) {
            throw new IllegalArgumentException("Payload é obrigatório quando o status é READY");
        }
    }
}

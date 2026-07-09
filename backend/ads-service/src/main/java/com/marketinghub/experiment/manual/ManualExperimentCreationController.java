package com.marketinghub.experiment.manual;

import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.experiment.mapper.ExperimentMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expõe a porta de entrada manual para criação de experimentos sem execução de IA.
 */
@RestController
@RequestMapping("/api/manual-experiments")
public class ManualExperimentCreationController {
    private final ManualExperimentCreationService service;
    private final ExperimentMapper mapper;

    /** Inicializa o controller com o serviço manual e o mapper de experimentos. */
    public ManualExperimentCreationController(ManualExperimentCreationService service, ExperimentMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    /** Cria um experimento oficial marcado como originado pelo fluxo manual. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExperimentDto create(@RequestBody ManualExperimentCreationRequest request) {
        return mapper.toDto(service.create(request));
    }
}

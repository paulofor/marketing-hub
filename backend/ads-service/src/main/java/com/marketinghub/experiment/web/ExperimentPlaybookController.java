package com.marketinghub.experiment.web;

import com.marketinghub.experiment.ExperimentStage;
import com.marketinghub.experiment.dto.ExperimentPlaybookStageDto;
import com.marketinghub.experiment.service.ExperimentPlaybookService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the experiment playbook so the frontend can guide operators when defining stage/variables.
 */
@RestController
@RequestMapping("/api/experiment-playbook")
public class ExperimentPlaybookController {
    private final ExperimentPlaybookService service;

    public ExperimentPlaybookController(ExperimentPlaybookService service) {
        this.service = service;
    }

    @GetMapping
    public List<ExperimentPlaybookStageDto> list() {
        return service.list();
    }

    @GetMapping("/{stage}")
    public ExperimentPlaybookStageDto get(@PathVariable ExperimentStage stage) {
        return service.get(stage);
    }
}

package com.marketinghub.hypothesis.web;

import com.marketinghub.hypothesis.dto.HypothesisDto;
import com.marketinghub.hypothesis.dto.HypothesisFrameworkGenerationRequest;
import com.marketinghub.hypothesis.dto.internal.HypothesisFrameworkGenerationJobDto;
import com.marketinghub.hypothesis.framework.HypothesisFrameworkSection;
import com.marketinghub.hypothesis.service.HypothesisFrameworkGenerationService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/hypotheses/{id}/framework")
public class HypothesisFrameworkController {
    private final HypothesisFrameworkGenerationService generationService;

    public HypothesisFrameworkController(HypothesisFrameworkGenerationService generationService) {
        this.generationService = generationService;
    }

    @PostMapping("/{section}/generate")
    public HypothesisDto generate(@PathVariable UUID id,
                                  @PathVariable String section,
                                  @RequestBody(required = false) HypothesisFrameworkGenerationRequest request) {
        HypothesisFrameworkSection parsed;
        try {
            parsed = HypothesisFrameworkSection.fromPath(section);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        HypothesisFrameworkGenerationRequest payload = request != null ? request : new HypothesisFrameworkGenerationRequest();
        return generationService.generate(id, parsed, payload);
    }

    @GetMapping("/jobs")
    public List<HypothesisFrameworkGenerationJobDto> listJobs(@PathVariable UUID id,
                                                              @RequestParam(value = "size", defaultValue = "30") Integer size) {
        return generationService.listJobs(id, size != null ? size : 30);
    }
}

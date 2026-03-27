package com.marketinghub.niche.web;

import com.marketinghub.niche.dto.BacklogRecommendationDto;
import com.marketinghub.niche.dto.NicheLearningDictionaryDto;
import com.marketinghub.niche.service.NicheLearningService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposição do banco de aprendizados consolidados por nicho.
 */
@RestController
@RequestMapping("/api/niches/{nicheId}/learning")
public class NicheLearningController {

    private final NicheLearningService service;

    public NicheLearningController(NicheLearningService service) {
        this.service = service;
    }

    @GetMapping("/dictionary")
    public NicheLearningDictionaryDto dictionary(@PathVariable Long nicheId) {
        return service.summarize(nicheId);
    }

    @GetMapping("/recommendations")
    public List<BacklogRecommendationDto> recommendations(@PathVariable Long nicheId) {
        return service.backlog(nicheId);
    }
}

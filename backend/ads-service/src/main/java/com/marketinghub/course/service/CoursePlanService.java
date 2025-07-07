package com.marketinghub.course.service;

import com.marketinghub.course.CoursePlan;
import com.marketinghub.course.client.KajabiClient;
import com.marketinghub.course.dto.CreateCoursePlanRequest;
import com.marketinghub.course.repository.CoursePlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Service layer for course plan generation using Kajabi AI.
 */
@Service
public class CoursePlanService {
    private final CoursePlanRepository repository;
    private final KajabiClient client;

    public CoursePlanService(CoursePlanRepository repository, KajabiClient client) {
        this.repository = repository;
        this.client = client;
    }

    /**
     * Creates a course plan by delegating to Kajabi AI.
     */
    @Transactional
    public CoursePlan createPlan(CreateCoursePlanRequest request) {
        CoursePlan plan = CoursePlan.builder()
                .targetAudience(request.getTargetAudience())
                .transformation(request.getTransformation())
                .macroTopics(request.getMacroTopics())
                .build();
        repository.save(plan);

        Map<String, Object> resp = client.proposeCoursePlan(Map.of(
                "audience", request.getTargetAudience(),
                "transformation", request.getTransformation(),
                "topics", request.getMacroTopics()
        ));

        plan.setModules(String.valueOf(resp.get("modules")));
        plan.setObjectives(String.valueOf(resp.get("objectives")));
        plan.setResources(String.valueOf(resp.get("resources")));
        return repository.save(plan);
    }

    public CoursePlan getPlan(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public Iterable<CoursePlan> listPlans() {
        return repository.findAll();
    }
}

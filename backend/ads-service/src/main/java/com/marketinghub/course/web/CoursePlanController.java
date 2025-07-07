package com.marketinghub.course.web;

import com.marketinghub.course.dto.CoursePlanDto;
import com.marketinghub.course.dto.CreateCoursePlanRequest;
import com.marketinghub.course.mapper.CoursePlanMapper;
import com.marketinghub.course.service.CoursePlanService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * REST controller for course plans.
 */
@RestController
@RequestMapping("/api/courses")
public class CoursePlanController {
    private final CoursePlanService service;
    private final CoursePlanMapper mapper;

    public CoursePlanController(CoursePlanService service, CoursePlanMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public CoursePlanDto create(@RequestBody CreateCoursePlanRequest request) {
        return mapper.toDto(service.createPlan(request));
    }

    @GetMapping("/{id}")
    public CoursePlanDto get(@PathVariable Long id) {
        return mapper.toDto(service.getPlan(id));
    }

    @GetMapping
    public List<CoursePlanDto> list() {
        return StreamSupport.stream(service.listPlans().spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }
}

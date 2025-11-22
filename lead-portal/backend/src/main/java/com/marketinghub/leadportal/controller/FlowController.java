package com.marketinghub.leadportal.controller;

import com.marketinghub.leadportal.dto.FlowResponse;
import com.marketinghub.leadportal.dto.UpsertFlowRequest;
import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.model.FlowQuestion;
import com.marketinghub.leadportal.service.FlowService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/flows")
@CrossOrigin
@Validated
public class FlowController {

    private final FlowService flowService;

    public FlowController(FlowService flowService) {
        this.flowService = flowService;
    }

    @PutMapping("/{slug}")
    public FlowResponse upsertFlow(@PathVariable("slug") String slug,
                                   @Valid @RequestBody UpsertFlowRequest request) {
        Flow flow = new Flow(
                slug,
                request.getName(),
                request.getDescription(),
                request.getModel(),
                request.getPrompt(),
                request.getQuestions().stream().map(this::toQuestion).toList());
        return FlowResponse.from(flowService.save(flow));
    }

    @GetMapping("/{slug}")
    public FlowResponse getFlow(@PathVariable("slug") String slug) {
        return FlowResponse.from(flowService.getAndTrackAccess(slug));
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> deleteFlow(@PathVariable("slug") String slug) {
        flowService.delete(slug);
        return ResponseEntity.noContent().build();
    }

    private FlowQuestion toQuestion(com.marketinghub.leadportal.dto.FlowQuestionRequest request) {
        List<String> options = request.getOptions() == null
                ? List.of()
                : request.getOptions().stream().map(String::trim).filter(value -> !value.isEmpty()).collect(Collectors.toList());
        return new FlowQuestion(
                request.getTitle(),
                request.getDataKey(),
                request.getType(),
                request.isRequired(),
                request.getDescription(),
                request.getPlaceholder(),
                options);
    }
}

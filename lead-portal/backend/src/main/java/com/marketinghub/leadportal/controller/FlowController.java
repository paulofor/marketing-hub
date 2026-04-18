package com.marketinghub.leadportal.controller;

import com.marketinghub.leadportal.dto.FlowResponse;
import com.marketinghub.leadportal.dto.UpsertFlowRequest;
import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.model.FlowQuestion;
import com.marketinghub.leadportal.model.FlowAccessMetadata;
import com.marketinghub.leadportal.model.SimpleFormStyle;
import com.marketinghub.leadportal.service.FlowService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
        List<com.marketinghub.leadportal.dto.FlowQuestionRequest> questionRequests =
                request.getQuestions() == null ? List.of() : request.getQuestions();

        Flow flow = new Flow(
                slug,
                request.getName(),
                request.getDescription(),
                request.getCustomFormHtml(),
                request.getModel(),
                request.getPrompt(),
                request.getImagePromptModel(),
                request.getImagePromptTemplate(),
                request.getImageBatchSize(),
                questionRequests.stream().map(this::toQuestion).toList(),
                mapStyle(request.getSimpleFormStyle()),
                request.getFacebookPixelId(),
                request.getFacebookPixelCode(),
                request.getFacebookPixelCreatedAt());
        return FlowResponse.from(flowService.save(flow));
    }

    @GetMapping("/{slug}")
    public FlowResponse getFlow(@PathVariable("slug") String slug, HttpServletRequest request) {
        FlowAccessMetadata accessMetadata = FlowAccessMetadata.from(request);
        return FlowResponse.from(flowService.getAndTrackAccess(slug, accessMetadata));
    }

    @GetMapping(value = "/{slug}/page", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getStandaloneFlowPage(
            @PathVariable("slug") String slug,
            HttpServletRequest request) {
        FlowAccessMetadata accessMetadata = FlowAccessMetadata.from(request);
        Flow flow = flowService.getAndTrackAccess(slug, accessMetadata);
        String html = flow.customFormHtml();
        if (!isStandaloneHtmlDocument(html)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Fluxo não possui HTML standalone.");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> deleteFlow(@PathVariable("slug") String slug) {
        flowService.delete(slug);
        return ResponseEntity.noContent().build();
    }

    private SimpleFormStyle mapStyle(UpsertFlowRequest.SimpleFormStylePayload payload) {
        if (payload == null) {
            return null;
        }
        return new SimpleFormStyle(payload.getSlug(), payload.getName(), payload.getDefinition());
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

    private boolean isStandaloneHtmlDocument(String html) {
        if (html == null) {
            return false;
        }
        String normalized = html.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("<!doctype")
                || normalized.startsWith("<html")
                || normalized.startsWith("<body");
    }
}

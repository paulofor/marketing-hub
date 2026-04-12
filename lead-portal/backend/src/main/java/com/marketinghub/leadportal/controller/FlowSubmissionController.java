package com.marketinghub.leadportal.controller;

import com.marketinghub.leadportal.controller.support.FormDataSubmissionRequestFactory;
import com.marketinghub.leadportal.dto.FlowSubmissionRequest;
import com.marketinghub.leadportal.dto.FlowSubmissionResponse;
import com.marketinghub.leadportal.model.FlowSubmission;
import com.marketinghub.leadportal.service.FlowSubmissionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.net.URI;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/flows")
@CrossOrigin
@Validated
public class FlowSubmissionController {

    private final FlowSubmissionService submissionService;
    private final FormDataSubmissionRequestFactory formDataFactory;
    private final Validator validator;
    private final ObjectMapper objectMapper;

    public FlowSubmissionController(
            FlowSubmissionService submissionService,
            FormDataSubmissionRequestFactory formDataFactory,
            Validator validator,
            ObjectMapper objectMapper) {
        this.submissionService = submissionService;
        this.formDataFactory = formDataFactory;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/{slug}/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FlowSubmissionResponse> submit(
            @PathVariable("slug") String slug,
            @RequestPart(value = "payload", required = false) String payloadRaw,
            @RequestParam MultiValueMap<String, String> formFields,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        FlowSubmissionRequest request = resolveRequest(slug, payloadRaw, formFields);
        FlowSubmission submission = submissionService.create(slug, request, imageFile);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(submission.id())
                .toUri();

        String imageUrl = buildImageUrl(submission);
        return ResponseEntity.created(location).body(FlowSubmissionResponse.from(submission, imageUrl));
    }

    @GetMapping("/submissions/{id}/image")
    public ResponseEntity<Resource> downloadImage(@PathVariable("id") UUID id) {
        FlowSubmission submission = submissionService.get(id);
        if (submission.storedFileName() == null) {
            return ResponseEntity.notFound().build();
        }

        Resource file = submissionService.loadImage(submission.storedFileName());

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (submission.contentType() != null) {
            mediaType = MediaType.parseMediaType(submission.contentType());
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + submission.originalFileName())
                .contentType(mediaType)
                .body(file);
    }

    private FlowSubmissionRequest fromFormData(String slug, MultiValueMap<String, String> formFields) {
        FlowSubmissionRequest request = formDataFactory.fromFormData(slug, formFields);
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return request;
    }

    private String buildImageUrl(FlowSubmission submission) {
        if (submission.storedFileName() == null) {
            return null;
        }
        return submissionService
                .resolvePublicUrl(submission.storedFileName())
                .orElseGet(() -> ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/flows/submissions/")
                        .path(submission.id().toString())
                        .path("/image")
                        .toUriString());
    }

    private FlowSubmissionRequest resolveRequest(
            String slug,
            String payloadRaw,
            MultiValueMap<String, String> formFields) {
        if (payloadRaw == null || payloadRaw.isBlank()) {
            return fromFormData(slug, formFields);
        }
        try {
            FlowSubmissionRequest request = objectMapper.readValue(payloadRaw, FlowSubmissionRequest.class);
            var violations = validator.validate(request);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
            return request;
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Payload inválido para submissão.", ex);
        }
    }
}

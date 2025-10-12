package com.marketinghub.ads;

import com.marketinghub.ads.dto.CreateFacebookInstantFormRequest;
import com.marketinghub.ads.dto.FacebookInstantFormDto;
import com.marketinghub.ads.dto.FacebookInstantFormPublicationDto;
import com.marketinghub.ads.dto.UpdateFacebookInstantFormApprovalRequest;
import com.marketinghub.ads.dto.UpdateFacebookInstantFormPublicationRequest;
import com.marketinghub.ads.mapper.FacebookInstantFormMapper;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class FacebookInstantFormController {
    private final FacebookInstantFormRepository repository;
    private final HypothesisRepository hypothesisRepository;
    private final FacebookPageRepository pageRepository;
    private final FacebookInstantFormMapper mapper;
    private final ExperimentRepository experimentRepository;

    public FacebookInstantFormController(FacebookInstantFormRepository repository,
                                         HypothesisRepository hypothesisRepository,
                                         FacebookPageRepository pageRepository,
                                         FacebookInstantFormMapper mapper,
                                         ExperimentRepository experimentRepository) {
        this.repository = repository;
        this.hypothesisRepository = hypothesisRepository;
        this.pageRepository = pageRepository;
        this.mapper = mapper;
        this.experimentRepository = experimentRepository;
    }

    @GetMapping("/hypotheses/{hypothesisId}/instant-forms")
    public List<FacebookInstantFormDto> list(@PathVariable UUID hypothesisId) {
        if (!hypothesisRepository.existsById(hypothesisId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "hypothesis not found");
        }
        return repository.findByHypothesisId(hypothesisId).stream()
                .map(mapper::toDto)
                .sorted((a, b) -> {
                    Instant aInstant = a.updatedTime() != null ? a.updatedTime() : a.createdTime();
                    Instant bInstant = b.updatedTime() != null ? b.updatedTime() : b.createdTime();
                    if (aInstant != null && bInstant != null) {
                        int cmp = bInstant.compareTo(aInstant);
                        if (cmp != 0) {
                            return cmp;
                        }
                    }
                    if (a.createdAt() != null && b.createdAt() != null) {
                        int cmp = b.createdAt().compareTo(a.createdAt());
                        if (cmp != 0) {
                            return cmp;
                        }
                    }
                    return Long.compare(b.id(), a.id());
                })
                .toList();
    }

    @GetMapping("/instant-forms/ready-to-publish")
    public List<FacebookInstantFormPublicationDto> readyToPublish() {
        return repository.findByApprovedTrueAndPublishedFalse().stream()
                .filter(form -> form.getPage() != null)
                .filter(form -> form.getFormId() != null && !form.getFormId().isBlank())
                .map(this::toPublicationDto)
                .toList();
    }

    @PostMapping("/hypotheses/{hypothesisId}/instant-forms")
    @Transactional
    public FacebookInstantFormDto create(@PathVariable UUID hypothesisId,
                                         @RequestBody CreateFacebookInstantFormRequest request) {
        var hypothesis = hypothesisRepository.findById(hypothesisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "hypothesis not found"));
        if (request.facebookPageId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "facebookPageId is required");
        }
        if (request.facebookFormId() == null || request.facebookFormId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "facebookFormId is required");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (request.model() == null || request.model().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "model is required");
        }
        if (request.prompt() == null || request.prompt().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "prompt is required");
        }
        var page = pageRepository.findById(request.facebookPageId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "facebookPageId not found"));

        var entity = FacebookInstantForm.builder()
                .hypothesis(hypothesis)
                .page(page)
                .formId(request.facebookFormId())
                .name(request.name())
                .status(request.status())
                .locale(request.locale())
                .leadsCount(request.leadsCount())
                .createdTime(request.createdTime())
                .updatedTime(request.updatedTime())
                .followUpActionUrl(request.followUpActionUrl())
                .privacyPolicyUrl(request.privacyPolicyUrl())
                .model(request.model())
                .prompt(request.prompt())
                .approved(false)
                .published(false)
                .build();
        return mapper.toDto(repository.save(entity));
    }

    @GetMapping("/instant-forms/{id}")
    public FacebookInstantFormDto get(@PathVariable Long id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "instant form not found"));
    }

    @PatchMapping("/instant-forms/{id}/approval")
    @Transactional
    public FacebookInstantFormDto updateApproval(@PathVariable Long id,
                                                 @RequestBody UpdateFacebookInstantFormApprovalRequest request) {
        FacebookInstantForm form = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "instant form not found"));
        if (request.approved()) {
            form.setApproved(true);
            form.setApprovedAt(Instant.now());
        } else {
            form.setApproved(false);
            form.setApprovedAt(null);
        }
        return mapper.toDto(repository.save(form));
    }

    @PatchMapping("/instant-forms/{id}/publication")
    @Transactional
    public FacebookInstantFormDto updatePublication(@PathVariable Long id,
                                                    @RequestBody UpdateFacebookInstantFormPublicationRequest request) {
        FacebookInstantForm form = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "instant form not found"));
        if (request.published()) {
            form.setPublished(true);
            form.setPublishedAt(request.publishedAt() != null ? request.publishedAt() : Instant.now());
            form.setShareLink(request.shareLink() != null && !request.shareLink().isBlank()
                    ? request.shareLink()
                    : null);
        } else {
            form.setPublished(false);
            form.setPublishedAt(null);
            form.setShareLink(null);
        }
        if (request.status() != null && !request.status().isBlank()) {
            form.setStatus(request.status());
        }
        return mapper.toDto(repository.save(form));
    }

    @DeleteMapping("/instant-forms/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable Long id) {
        FacebookInstantForm form = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "instant form not found"));
        experimentRepository.clearFacebookInstantFormById(id);
        repository.delete(form);
    }

    private FacebookInstantFormPublicationDto toPublicationDto(FacebookInstantForm form) {
        return new FacebookInstantFormPublicationDto(
                form.getId(),
                form.getFormId(),
                form.getName(),
                form.getStatus(),
                form.getPage().getId(),
                form.getPage().getPageId(),
                form.getPage().getName(),
                form.isApproved(),
                form.isPublished(),
                form.getShareLink()
        );
    }
}

package com.marketinghub.ads;

import com.marketinghub.ads.dto.CreateFacebookInstantFormRequest;
import com.marketinghub.ads.dto.FacebookInstantFormDto;
import com.marketinghub.ads.dto.FacebookInstantFormPublicationDto;
import com.marketinghub.ads.dto.UpdateFacebookInstantFormApprovalRequest;
import com.marketinghub.ads.dto.UpdateFacebookInstantFormPublicationRequest;
import com.marketinghub.ads.mapper.FacebookInstantFormMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.settings.GeneralSettingService;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Objects;

@RestController
@RequestMapping("/api")
public class FacebookInstantFormController {
    private final FacebookInstantFormRepository repository;
    private final HypothesisRepository hypothesisRepository;
    private final FacebookPageRepository pageRepository;
    private final FacebookInstantFormMapper mapper;
    private final ExperimentRepository experimentRepository;
    private final GeneralSettingService generalSettingService;

    public FacebookInstantFormController(FacebookInstantFormRepository repository,
                                         HypothesisRepository hypothesisRepository,
                                         FacebookPageRepository pageRepository,
                                         FacebookInstantFormMapper mapper,
                                         ExperimentRepository experimentRepository,
                                         GeneralSettingService generalSettingService) {
        this.repository = repository;
        this.hypothesisRepository = hypothesisRepository;
        this.pageRepository = pageRepository;
        this.mapper = mapper;
        this.experimentRepository = experimentRepository;
        this.generalSettingService = generalSettingService;
    }

    @GetMapping("/hypotheses/{hypothesisId}/instant-forms")
    public List<FacebookInstantFormDto> list(@PathVariable UUID hypothesisId) {
        if (!hypothesisRepository.existsById(hypothesisId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "hypothesis not found");
        }
        return repository.findByHypothesisId(hypothesisId).stream()
                .map(mapper::toDto)
                .map(this::applyDefaults)
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
                .map(this::toPublicationDto)
                .toList();
    }

    @GetMapping("/instant-forms/approved-drafts")
    public List<FacebookInstantFormPublicationDto> approvedDrafts() {
        return repository.findApprovedDraftsWithoutExternalId().stream()
                .filter(form -> form.getPage() != null)
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

        String facebookFormId = request.facebookFormId();
        if (facebookFormId != null) {
            facebookFormId = facebookFormId.trim();
            if (facebookFormId.isEmpty()) {
                facebookFormId = null;
            }
        }

        var entity = FacebookInstantForm.builder()
                .hypothesis(hypothesis)
                .page(page)
                .formId(facebookFormId)
                .name(request.name())
                .status(request.status())
                .locale(request.locale())
                .leadsCount(request.leadsCount())
                .createdTime(request.createdTime())
                .updatedTime(request.updatedTime())
                .followUpActionUrl(request.followUpActionUrl())
                .privacyPolicyUrl(resolvePrivacyPolicyUrl(request.privacyPolicyUrl()))
                .model(request.model())
                .prompt(request.prompt())
                .questions(request.questions())
                .approved(false)
                .published(false)
                .build();
        FacebookInstantForm saved = repository.save(entity);
        return applyDefaults(mapper.toDto(saved));
    }

    @GetMapping("/instant-forms/{id}")
    public FacebookInstantFormDto get(@PathVariable Long id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .map(this::applyDefaults)
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
        FacebookInstantFormDto dto = mapper.toDto(repository.save(form));
        return applyDefaults(dto);
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
            if (request.facebookFormId() != null && !request.facebookFormId().isBlank()) {
                form.setFormId(request.facebookFormId().trim());
            }
        } else {
            form.setPublished(false);
            form.setPublishedAt(null);
            form.setShareLink(null);
        }
        if (request.status() != null && !request.status().isBlank()) {
            form.setStatus(request.status());
        }
        FacebookInstantFormDto dto = mapper.toDto(repository.save(form));
        return applyDefaults(dto);
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

    private FacebookInstantFormDto applyDefaults(FacebookInstantFormDto dto) {
        if (dto == null) {
            return null;
        }
        String resolvedFollowUp = resolveFollowUpActionUrl(dto);
        String resolvedPrivacyPolicyUrl = resolvePrivacyPolicyUrl(dto.privacyPolicyUrl());
        if (Objects.equals(resolvedFollowUp, dto.followUpActionUrl())
                && Objects.equals(resolvedPrivacyPolicyUrl, dto.privacyPolicyUrl())) {
            return dto;
        }
        return new FacebookInstantFormDto(
                dto.id(),
                dto.hypothesisId(),
                dto.facebookPageId(),
                dto.facebookPageExternalId(),
                dto.facebookPageName(),
                dto.facebookFormId(),
                dto.name(),
                dto.status(),
                dto.locale(),
                dto.leadsCount(),
                dto.createdTime(),
                dto.updatedTime(),
                resolvedFollowUp,
                resolvedPrivacyPolicyUrl,
                dto.model(),
                dto.prompt(),
                dto.questions(),
                dto.approved(),
                dto.approvedAt(),
                dto.published(),
                dto.publishedAt(),
                dto.shareLink(),
                dto.createdAt(),
                dto.updatedAt()
        );
    }

    private String resolveFollowUpActionUrl(FacebookInstantFormDto dto) {
        if (dto == null) {
            return null;
        }
        if (StringUtils.hasText(dto.followUpActionUrl())) {
            return dto.followUpActionUrl().trim();
        }
        if (dto.id() == null) {
            return null;
        }
        return experimentRepository.findFirstByFacebookInstantForm_Id(dto.id())
                .map(Experiment::getFollowUpActionUrl)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .orElse(null);
    }

    private String resolvePrivacyPolicyUrl(String candidate) {
        if (StringUtils.hasText(candidate)) {
            return candidate.trim();
        }
        return generalSettingService.getPrivacyPolicyUrl()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .orElse(null);
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

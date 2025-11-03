package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.LeadPortalFlowQuestion;
import com.marketinghub.leadportal.LeadPortalQuestionType;
import com.marketinghub.leadportal.dto.CreateLeadPortalFlowRequest;
import com.marketinghub.leadportal.dto.LeadPortalFlowQuestionRequest;
import com.marketinghub.leadportal.dto.UpdateLeadPortalFlowRequest;
import com.marketinghub.leadportal.repository.LeadPortalFlowRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Handles validation and persistence of {@link LeadPortalFlow} records.
 */
@Service
public class LeadPortalFlowService {
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");
    private static final Pattern DATA_KEY_PATTERN = Pattern.compile("^[a-z][a-z0-9_-]*$");

    private final LeadPortalFlowRepository repository;

    public LeadPortalFlowService(LeadPortalFlowRepository repository) {
        this.repository = repository;
    }

    public List<LeadPortalFlow> listAll() {
        return repository.findAllByOrderByNameAsc();
    }

    public LeadPortalFlow get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lead portal flow not found: " + id));
    }

    @Transactional
    public LeadPortalFlow create(CreateLeadPortalFlowRequest request) {
        String name = normalizeName(request.getName());
        String slug = normalizeSlug(request.getSlug());
        ensureUniqueSlug(slug, null);
        LeadPortalFlow flow = LeadPortalFlow.builder()
                .name(name)
                .slug(slug)
                .description(trimToNull(request.getDescription()))
                .build();
        flow.getQuestions().addAll(buildQuestions(flow, request.getQuestions()));
        return repository.save(flow);
    }

    @Transactional
    public LeadPortalFlow update(Long id, UpdateLeadPortalFlowRequest request) {
        LeadPortalFlow flow = get(id);
        if (request.getName() != null) {
            flow.setName(normalizeName(request.getName()));
        }
        if (request.getSlug() != null) {
            String slug = normalizeSlug(request.getSlug());
            ensureUniqueSlug(slug, id);
            flow.setSlug(slug);
        }
        if (request.getDescription() != null) {
            flow.setDescription(trimToNull(request.getDescription()));
        }
        if (request.getQuestions() != null) {
            flow.getQuestions().clear();
            flow.getQuestions().addAll(buildQuestions(flow, request.getQuestions()));
        }
        return repository.save(flow);
    }

    private List<LeadPortalFlowQuestion> buildQuestions(LeadPortalFlow flow,
                                                        List<LeadPortalFlowQuestionRequest> questionRequests) {
        if (CollectionUtils.isEmpty(questionRequests)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "questions are required");
        }
        List<LeadPortalFlowQuestion> questions = new ArrayList<>();
        Set<String> usedKeys = new HashSet<>();
        for (int index = 0; index < questionRequests.size(); index++) {
            questions.add(toQuestion(flow, questionRequests.get(index), index, usedKeys));
        }
        return questions;
    }

    private LeadPortalFlowQuestion toQuestion(LeadPortalFlow flow,
                                              LeadPortalFlowQuestionRequest request,
                                              int index,
                                              Set<String> usedKeys) {
        String title = normalizeTitle(request.getTitle());
        String dataKey = normalizeDataKey(request.getDataKey());
        if (!usedKeys.add(dataKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "duplicate dataKey in questions: " + dataKey);
        }
        LeadPortalQuestionType type = Objects.requireNonNull(request.getType(), "type");
        List<String> options = sanitizeOptions(request.getOptions(), type);
        return LeadPortalFlowQuestion.builder()
                .flow(flow)
                .title(title)
                .dataKey(dataKey)
                .type(type)
                .required(request.isRequired())
                .description(trimToNull(request.getDescription()))
                .placeholder(trimToNull(request.getPlaceholder()))
                .position(index)
                .options(options)
                .build();
    }

    private List<String> sanitizeOptions(List<String> options, LeadPortalQuestionType type) {
        boolean expectsOptions = type == LeadPortalQuestionType.SINGLE_CHOICE
                || type == LeadPortalQuestionType.MULTIPLE_CHOICE;
        if (CollectionUtils.isEmpty(options)) {
            if (expectsOptions) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "options are required for question type " + type);
            }
            return new ArrayList<>();
        }
        List<String> cleaned = options.stream()
                .map(option -> option == null ? null : option.trim())
                .filter(StringUtils::hasText)
                .toList();
        if (cleaned.isEmpty() && expectsOptions) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "options are required for question type " + type);
        }
        if (!expectsOptions && !cleaned.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "options are not supported for question type " + type);
        }
        return new ArrayList<>(cleaned);
    }

    private void ensureUniqueSlug(String slug, Long currentId) {
        repository.findBySlug(slug)
                .filter(existing -> !Objects.equals(existing.getId(), currentId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "slug already in use: " + slug);
                });
    }

    private String normalizeName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        return name.trim();
    }

    private String normalizeSlug(String slug) {
        if (!StringUtils.hasText(slug)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "slug is required");
        }
        String normalized = slug.trim().toLowerCase(Locale.ROOT);
        if (!SLUG_PATTERN.matcher(normalized).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "slug must contain only lowercase letters, numbers and hyphen");
        }
        return normalized;
    }

    private String normalizeTitle(String title) {
        if (!StringUtils.hasText(title)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
        }
        return title.trim();
    }

    private String normalizeDataKey(String dataKey) {
        if (!StringUtils.hasText(dataKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dataKey is required");
        }
        String normalized = dataKey.trim().toLowerCase(Locale.ROOT);
        if (!DATA_KEY_PATTERN.matcher(normalized).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dataKey must start with a letter and contain only lowercase letters, numbers, hyphen or underscore");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}

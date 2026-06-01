package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.LeadPortalSimpleFormStyle;
import com.marketinghub.leadportal.LeadPortalSimpleFormStyleDefinition;
import com.marketinghub.leadportal.dto.CreateLeadPortalSimpleFormStyleRequest;
import com.marketinghub.leadportal.dto.LeadPortalSimpleFormStyleGenerationResultRequest;
import com.marketinghub.leadportal.dto.UpdateLeadPortalSimpleFormStyleRequest;
import com.marketinghub.repository.jpa.leadportal.LeadPortalSimpleFormStyleRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LeadPortalSimpleFormStyleService {

    public static final String GENERATION_STATUS_PENDING = "PENDING";
    public static final String GENERATION_STATUS_COMPLETED = "COMPLETED";
    public static final String GENERATION_STATUS_FAILED = "FAILED";

    private final LeadPortalSimpleFormStyleRepository repository;

    public LeadPortalSimpleFormStyleService(LeadPortalSimpleFormStyleRepository repository) {
        this.repository = repository;
    }

    public List<LeadPortalSimpleFormStyle> listAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public LeadPortalSimpleFormStyle get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "lead portal simple form style not found: " + id));
    }

    public List<LeadPortalSimpleFormStyle> listPendingForGeneration(int limit) {
        int safeLimit = Math.max(1, limit);
        return repository.findByGenerationStatusOrderByUpdatedAtAscIdAsc(
                GENERATION_STATUS_PENDING,
                PageRequest.of(0, safeLimit));
    }

    @Transactional
    public LeadPortalSimpleFormStyle create(CreateLeadPortalSimpleFormStyleRequest request) {
        String name = normalizeName(request.getName());
        String slug = normalizeSlug(request.getSlug());
        ensureUniqueSlug(slug, null);

        LeadPortalSimpleFormStyle style = LeadPortalSimpleFormStyle.builder()
                .name(name)
                .slug(slug)
                .description(trimToNull(request.getDescription()))
                .previewImageUrl(trimToNull(request.getPreviewImageUrl()))
                .textModel(requireModel(request.getTextModel()))
                .textPrompt(requirePrompt(request.getTextPrompt()))
                .generationStatus(GENERATION_STATUS_PENDING)
                .generationError(null)
                .definition(null)
                .generationCostUsd(null)
                .textParameters(null)
                .build();

        return repository.save(style);
    }

    @Transactional
    public LeadPortalSimpleFormStyle update(Long id, UpdateLeadPortalSimpleFormStyleRequest request) {
        LeadPortalSimpleFormStyle style = get(id);
        if (request.getName() != null) {
            style.setName(normalizeName(request.getName()));
        }
        if (request.getSlug() != null) {
            String slug = normalizeSlug(request.getSlug());
            ensureUniqueSlug(slug, id);
            style.setSlug(slug);
        }
        if (request.getDescription() != null) {
            style.setDescription(trimToNull(request.getDescription()));
        }
        if (request.getPreviewImageUrl() != null) {
            style.setPreviewImageUrl(trimToNull(request.getPreviewImageUrl()));
        }

        String currentModel = style.getTextModel();
        String currentPrompt = style.getTextPrompt();
        String nextModel = request.getTextModel() != null ? trimToNull(request.getTextModel()) : currentModel;
        String nextPrompt = request.getTextPrompt() != null ? trimToNull(request.getTextPrompt()) : currentPrompt;

        boolean modelChanged = request.getTextModel() != null && !Objects.equals(nextModel, currentModel);
        boolean promptChanged = request.getTextPrompt() != null && !Objects.equals(nextPrompt, currentPrompt);
        boolean shouldRegenerate = Boolean.TRUE.equals(request.getRegenerate()) || modelChanged || promptChanged;

        if (request.getTextModel() != null) {
            style.setTextModel(requireModel(nextModel));
        }
        if (request.getTextPrompt() != null) {
            style.setTextPrompt(requirePrompt(nextPrompt));
        }

        if (shouldRegenerate) {
            style.setGenerationStatus(GENERATION_STATUS_PENDING);
            style.setGenerationError(null);
            style.setDefinition(null);
            style.setGenerationCostUsd(null);
            style.setTextParameters(null);
        }

        return repository.save(style);
    }

    @Transactional
    public LeadPortalSimpleFormStyle saveGenerationResult(Long id, LeadPortalSimpleFormStyleGenerationResultRequest request) {
        LeadPortalSimpleFormStyle style = get(id);

        String status = normalizeStatus(request.getStatus());
        style.setGenerationStatus(status);
        style.setGenerationError(trimToNull(request.getGenerationError()));

        if (GENERATION_STATUS_COMPLETED.equals(status)) {
            if (request.getDefinition() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "definition é obrigatória quando status for COMPLETED.");
            }
            style.setDefinition(sanitizeDefinition(request.getDefinition()));
            style.setTextParameters(trimToNull(request.getTextParameters()));
            style.setGenerationCostUsd(request.getGenerationCostUsd());
            style.setGenerationError(null);
        } else {
            style.setDefinition(null);
            style.setGenerationCostUsd(null);
            style.setTextParameters(trimToNull(request.getTextParameters()));
            if (!GENERATION_STATUS_FAILED.equals(status)) {
                style.setGenerationError(null);
            }
        }

        return repository.save(style);
    }

    private LeadPortalSimpleFormStyleDefinition sanitizeDefinition(LeadPortalSimpleFormStyleDefinition definition) {
        String heroLayout = normalizeHeroLayout(definition.heroLayout());
        return new LeadPortalSimpleFormStyleDefinition(
                trimToNull(definition.backgroundColor()),
                trimToNull(definition.backgroundGradient()),
                trimToNull(definition.backgroundPatternUrl()),
                trimToNull(definition.cardBackground()),
                trimToNull(definition.cardBorderColor()),
                trimToNull(definition.cardShadow()),
                trimToNull(definition.headingColor()),
                trimToNull(definition.textColor()),
                trimToNull(definition.mutedTextColor()),
                trimToNull(definition.primaryColor()),
                trimToNull(definition.accentColor()),
                trimToNull(definition.buttonBackground()),
                trimToNull(definition.buttonTextColor()),
                trimToNull(definition.buttonShadow()),
                trimToNull(definition.buttonBorderRadius()),
                trimToNull(definition.highlightBackground()),
                trimToNull(definition.inputBackground()),
                trimToNull(definition.inputBorderColor()),
                heroLayout,
                trimToNull(definition.heroImageUrl()),
                trimToNull(definition.heroImageBlendColor()));
    }

    private String normalizeHeroLayout(String value) {
        if (!StringUtils.hasText(value)) {
            return "image-right";
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "image-left", "image-right", "stacked" -> normalized;
            default -> "image-right";
        };
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "status é obrigatório. Valores válidos: PENDING, COMPLETED, FAILED.");
        }
        String normalized = status.trim().toUpperCase();
        if (!GENERATION_STATUS_PENDING.equals(normalized)
                && !GENERATION_STATUS_COMPLETED.equals(normalized)
                && !GENERATION_STATUS_FAILED.equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "status inválido. Valores válidos: PENDING, COMPLETED, FAILED.");
        }
        return normalized;
    }

    private void ensureUniqueSlug(String slug, Long id) {
        repository.findBySlug(slug)
                .filter(existing -> id == null || !Objects.equals(existing.getId(), id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Já existe um estilo com o slug informado.");
                });
    }

    private String normalizeName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome é obrigatório");
        }
        return name.trim();
    }

    private String normalizeSlug(String slug) {
        if (!StringUtils.hasText(slug)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug é obrigatório");
        }
        return slug.trim();
    }

    private String requireModel(String model) {
        if (!StringUtils.hasText(model)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe o modelo para gerar o estilo.");
        }
        return model.trim();
    }

    private String requirePrompt(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe o prompt para gerar o estilo.");
        }
        return prompt.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

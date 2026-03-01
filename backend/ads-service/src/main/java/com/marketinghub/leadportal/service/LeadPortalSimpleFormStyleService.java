package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.LeadPortalSimpleFormStyle;
import com.marketinghub.leadportal.dto.CreateLeadPortalSimpleFormStyleRequest;
import com.marketinghub.leadportal.dto.UpdateLeadPortalSimpleFormStyleRequest;
import com.marketinghub.leadportal.repository.LeadPortalSimpleFormStyleRepository;
import com.marketinghub.openai.service.OpenAiPricingService;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LeadPortalSimpleFormStyleService {

    private final LeadPortalSimpleFormStyleRepository repository;
    private final LeadPortalSimpleFormStyleGenerator generator;
    private final OpenAiPricingService pricingService;

    public LeadPortalSimpleFormStyleService(LeadPortalSimpleFormStyleRepository repository,
                                            LeadPortalSimpleFormStyleGenerator generator,
                                            OpenAiPricingService pricingService) {
        this.repository = repository;
        this.generator = generator;
        this.pricingService = pricingService;
    }

    public List<LeadPortalSimpleFormStyle> listAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public LeadPortalSimpleFormStyle get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "lead portal simple form style not found: " + id));
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
                .build();

        applyGeneration(style,
                trimToNull(request.getTextModel()),
                trimToNull(request.getTextPrompt()));

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

        if (shouldRegenerate) {
            applyGeneration(style, nextModel, nextPrompt);
        }

        return repository.save(style);
    }

    private void applyGeneration(LeadPortalSimpleFormStyle style, String model, String prompt) {
        if (!StringUtils.hasText(model) || !StringUtils.hasText(prompt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe o modelo e o prompt para gerar o estilo.");
        }
        try {
            LeadPortalSimpleFormStyleGenerator.Generation generation = generator.generate(
                    new LeadPortalSimpleFormStyleGenerator.GenerationCommand(
                            model,
                            prompt,
                            style.getName(),
                            style.getDescription()));
            style.setDefinition(generation.definition());
            style.setTextModel(model);
            style.setTextPrompt(prompt);
            style.setTextParameters(buildAuditTrail(generation));
            style.setGenerationCostUsd(pricingService.estimateBatchCost(model, generation.usage()));
        } catch (LeadPortalStyleGenerationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, ex.getMessage(), ex);
        }
    }

    private String buildAuditTrail(LeadPortalSimpleFormStyleGenerator.Generation generation) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(generation.renderedPrompt())) {
            sb.append("PROMPT:\n").append(generation.renderedPrompt());
        }
        if (StringUtils.hasText(generation.rawResponse())) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append("RAW_RESPONSE:\n").append(generation.rawResponse());
        }
        return sb.length() == 0 ? null : sb.toString();
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

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

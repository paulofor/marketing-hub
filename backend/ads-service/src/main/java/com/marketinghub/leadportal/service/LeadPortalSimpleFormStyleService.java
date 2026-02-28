package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.LeadPortalSimpleFormStyle;
import com.marketinghub.leadportal.LeadPortalSimpleFormStyleDefinition;
import com.marketinghub.leadportal.dto.CreateLeadPortalSimpleFormStyleRequest;
import com.marketinghub.leadportal.dto.UpdateLeadPortalSimpleFormStyleRequest;
import com.marketinghub.leadportal.repository.LeadPortalSimpleFormStyleRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class LeadPortalSimpleFormStyleService {

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

    @Transactional
    public LeadPortalSimpleFormStyle create(CreateLeadPortalSimpleFormStyleRequest request) {
        String name = normalizeName(request.getName());
        String slug = normalizeSlug(request.getSlug());
        ensureUniqueSlug(slug, null);
        LeadPortalSimpleFormStyleDefinition definition = requireDefinition(request.getDefinition());

        LeadPortalSimpleFormStyle style = LeadPortalSimpleFormStyle.builder()
                .name(name)
                .slug(slug)
                .description(trimToNull(request.getDescription()))
                .textModel(trimToNull(request.getTextModel()))
                .textPrompt(trimToNull(request.getTextPrompt()))
                .textParameters(trimToNull(request.getTextParameters()))
                .imageModel(trimToNull(request.getImageModel()))
                .imagePrompt(trimToNull(request.getImagePrompt()))
                .imageNegativePrompt(trimToNull(request.getImageNegativePrompt()))
                .imageParameters(trimToNull(request.getImageParameters()))
                .imageBatchSize(request.getImageBatchSize())
                .imageAspectRatio(trimToNull(request.getImageAspectRatio()))
                .previewImageUrl(trimToNull(request.getPreviewImageUrl()))
                .definition(definition)
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
        if (request.getTextModel() != null) {
            style.setTextModel(trimToNull(request.getTextModel()));
        }
        if (request.getTextPrompt() != null) {
            style.setTextPrompt(trimToNull(request.getTextPrompt()));
        }
        if (request.getTextParameters() != null) {
            style.setTextParameters(trimToNull(request.getTextParameters()));
        }
        if (request.getImageModel() != null) {
            style.setImageModel(trimToNull(request.getImageModel()));
        }
        if (request.getImagePrompt() != null) {
            style.setImagePrompt(trimToNull(request.getImagePrompt()));
        }
        if (request.getImageNegativePrompt() != null) {
            style.setImageNegativePrompt(trimToNull(request.getImageNegativePrompt()));
        }
        if (request.getImageParameters() != null) {
            style.setImageParameters(trimToNull(request.getImageParameters()));
        }
        if (request.getImageBatchSize() != null) {
            style.setImageBatchSize(request.getImageBatchSize());
        }
        if (request.getImageAspectRatio() != null) {
            style.setImageAspectRatio(trimToNull(request.getImageAspectRatio()));
        }
        if (request.getPreviewImageUrl() != null) {
            style.setPreviewImageUrl(trimToNull(request.getPreviewImageUrl()));
        }
        if (request.getDefinition() != null) {
            style.setDefinition(requireDefinition(request.getDefinition()));
        }
        return repository.save(style);
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
        return slug.trim();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private LeadPortalSimpleFormStyleDefinition requireDefinition(LeadPortalSimpleFormStyleDefinition definition) {
        if (definition == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "definition is required");
        }
        return definition;
    }
}

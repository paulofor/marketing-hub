package com.marketinghub.journey.service;

import com.marketinghub.journey.dto.JourneyTemplateRequest;
import com.marketinghub.journey.dto.JourneyTemplateUpdateRequest;
import com.marketinghub.journey.model.JourneyPhase;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.journey.repository.JourneyTemplateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Application service encapsulating template manipulation logic.
 */
@Service
public class JourneyTemplateService {
    private static final List<JourneyPhase> DEFAULT_PHASES = List.of(
            JourneyPhase.ATTENTION,
            JourneyPhase.INTEREST,
            JourneyPhase.DESIRE,
            JourneyPhase.ACTION
    );

    private final JourneyTemplateRepository templateRepository;

    public JourneyTemplateService(JourneyTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Transactional(readOnly = true)
    public Page<JourneyTemplate> list(Pageable pageable) {
        return templateRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public JourneyTemplate get(Long id) {
        return templateRepository.findWithStepsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Journey template not found"));
    }

    @Transactional
    public JourneyTemplate create(JourneyTemplateRequest request) {
        JourneyTemplate template = JourneyTemplate.builder()
                .name(request.name())
                .description(request.description())
                .objective(request.objective())
                .preferredChannel(request.preferredChannel())
                .build();
        applyMutableFields(template, request.phases(), request.tags(), request.metadata());
        return templateRepository.save(template);
    }

    @Transactional
    public JourneyTemplate update(Long id, JourneyTemplateUpdateRequest request) {
        JourneyTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Journey template not found"));

        if (request.name() != null) {
            template.setName(request.name());
        }
        if (request.description() != null) {
            template.setDescription(request.description());
        }
        if (request.objective() != null) {
            template.setObjective(request.objective());
        }
        if (request.preferredChannel() != null) {
            template.setPreferredChannel(request.preferredChannel());
        }
        if (request.phases() != null) {
            template.setPhases(resolvePhases(request.phases()));
        }
        if (request.tags() != null) {
            template.setTags(new LinkedHashSet<>(request.tags()));
        }
        if (request.metadata() != null) {
            template.setMetadata(new LinkedHashMap<>(request.metadata()));
        }
        return templateRepository.save(template);
    }

    @Transactional
    public void delete(Long id) {
        JourneyTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Journey template not found"));
        templateRepository.delete(template);
    }

    private void applyMutableFields(JourneyTemplate template,
                                    List<JourneyPhase> phases,
                                    Set<String> tags,
                                    Map<String, String> metadata) {
        template.setPhases(resolvePhases(phases));
        template.setTags(tags != null ? new LinkedHashSet<>(tags) : new LinkedHashSet<>());
        template.setMetadata(metadata != null ? new LinkedHashMap<>(metadata) : new LinkedHashMap<>());
    }

    private List<JourneyPhase> resolvePhases(List<JourneyPhase> phases) {
        if (phases == null || phases.isEmpty()) {
            return new ArrayList<>(DEFAULT_PHASES);
        }
        return new ArrayList<>(phases);
    }
}

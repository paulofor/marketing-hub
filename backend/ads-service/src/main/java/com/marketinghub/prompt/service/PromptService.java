package com.marketinghub.prompt.service;

import com.marketinghub.prompt.Prompt;
import com.marketinghub.prompt.dto.CreatePromptRequest;
import com.marketinghub.prompt.dto.UpdatePromptRequest;
import com.marketinghub.prompt.repository.PromptRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class PromptService {
    private final PromptRepository repository;

    public PromptService(PromptRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Prompt create(CreatePromptRequest request) {
        validate(request.getName(), request.getDomain(), request.getTemplate());
        Prompt prompt = Prompt.builder()
                .name(request.getName().trim())
                .domain(normalizeDomain(request.getDomain()))
                .template(request.getTemplate())
                .active(Boolean.TRUE.equals(request.getActive()))
                .build();
        Prompt saved = repository.save(prompt);
        if (saved.isActive()) {
            repository.deactivateOthers(saved.getDomain(), saved.getId());
        }
        return saved;
    }

    @Transactional
    public Prompt update(Long id, UpdatePromptRequest request) {
        validate(request.getName(), request.getDomain(), request.getTemplate());
        Prompt prompt = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Prompt not found: " + id));
        String previousDomain = prompt.getDomain();
        boolean wasActive = prompt.isActive();

        prompt.setName(request.getName().trim());
        prompt.setDomain(normalizeDomain(request.getDomain()));
        prompt.setTemplate(request.getTemplate());
        if (request.getActive() != null) {
            prompt.setActive(request.getActive());
        }
        Prompt saved = repository.save(prompt);
        if (saved.isActive()) {
            repository.deactivateOthers(saved.getDomain(), saved.getId());
        } else if (wasActive && !saved.isActive()) {
            // Nothing else to do, another prompt may be activated later.
        }
        // If domain changed and prompt remains active, ensure others in new domain are deactivated
        if (saved.isActive() && !saved.getDomain().equals(previousDomain)) {
            repository.deactivateOthers(saved.getDomain(), saved.getId());
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Prompt> list(String domain) {
        if (StringUtils.hasText(domain)) {
            return repository.findByDomainOrderByUpdatedAtDesc(normalizeDomain(domain));
        }
        return repository.findAllByOrderByUpdatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Prompt get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Prompt not found: " + id));
    }

    @Transactional
    public Prompt activate(Long id) {
        Prompt prompt = get(id);
        prompt.setActive(true);
        Prompt saved = repository.save(prompt);
        repository.deactivateOthers(saved.getDomain(), saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Prompt> findActiveByDomain(String domain) {
        if (!StringUtils.hasText(domain)) {
            return Optional.empty();
        }
        return repository.findFirstByDomainAndActiveTrueOrderByUpdatedAtDesc(normalizeDomain(domain));
    }

    @Transactional(readOnly = true)
    public Prompt getActiveByDomainOrThrow(String domain) {
        return findActiveByDomain(domain)
                .orElseThrow(() -> new EntityNotFoundException("Active prompt not found for domain " + domain));
    }

    private void validate(String name, String domain, String template) {
        if (!StringUtils.hasText(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (!StringUtils.hasText(domain)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "domain is required");
        }
        if (!StringUtils.hasText(template)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "template is required");
        }
    }

    private String normalizeDomain(String domain) {
        return domain == null ? null : domain.trim().toUpperCase(Locale.ROOT);
    }
}

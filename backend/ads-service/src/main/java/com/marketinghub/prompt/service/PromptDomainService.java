package com.marketinghub.prompt.service;

import com.marketinghub.prompt.PromptDomain;
import com.marketinghub.prompt.PromptDomainObjectType;
import com.marketinghub.prompt.dto.CreatePromptDomainRequest;
import com.marketinghub.prompt.dto.PromptDomainDto;
import com.marketinghub.prompt.dto.PromptDomainObjectDto;
import com.marketinghub.prompt.dto.UpdatePromptDomainRequest;
import com.marketinghub.prompt.mapper.PromptDomainMapper;
import com.marketinghub.prompt.repository.PromptDomainRepository;
import com.marketinghub.prompt.repository.PromptRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class PromptDomainService {
    private final PromptDomainRepository repository;
    private final PromptRepository promptRepository;
    private final PromptDomainMapper mapper;
    private final PromptDomainContextFactory contextFactory;

    public PromptDomainService(PromptDomainRepository repository,
                               PromptRepository promptRepository,
                               PromptDomainMapper mapper,
                               PromptDomainContextFactory contextFactory) {
        this.repository = repository;
        this.promptRepository = promptRepository;
        this.mapper = mapper;
        this.contextFactory = contextFactory;
    }

    @Transactional(readOnly = true)
    public List<PromptDomainDto> list() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PromptDomainDto get(Long id) {
        PromptDomain domain = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PromptDomain not found: " + id));
        return enrichDto(domain);
    }

    public PromptDomainDto create(CreatePromptDomainRequest request) {
        validateCreate(request);
        PromptDomain domain = new PromptDomain();
        domain.setCode(normalizeCode(request.getCode()));
        domain.setName(request.getName().trim());
        domain.setDescription(request.getDescription());
        domain.setObjectTypes(parseObjectTypes(request.getObjects()));
        PromptDomain saved = repository.save(domain);
        return enrichDto(saved);
    }

    public PromptDomainDto update(Long id, UpdatePromptDomainRequest request) {
        PromptDomain domain = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PromptDomain not found: " + id));
        if (StringUtils.hasText(request.getName())) {
            domain.setName(request.getName().trim());
        }
        domain.setDescription(request.getDescription());
        if (request.getObjects() != null && !request.getObjects().isEmpty()) {
            domain.setObjectTypes(parseObjectTypes(request.getObjects()));
        }
        PromptDomain saved = repository.save(domain);
        return enrichDto(saved);
    }

    public void delete(Long id) {
        PromptDomain domain = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PromptDomain not found: " + id));
        if (promptRepository.existsByDomainIgnoreCase(domain.getCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível remover o domínio porque existem prompts vinculados.");
        }
        repository.delete(domain);
    }

    @Transactional(readOnly = true)
    public PromptDomain findByCodeOrThrow(String code) {
        if (!StringUtils.hasText(code)) {
            throw new EntityNotFoundException("PromptDomain code is empty");
        }
        return repository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new EntityNotFoundException("PromptDomain not found for code " + code));
    }

    @Transactional(readOnly = true)
    public List<PromptDomainObjectType> getObjectTypes(String code) {
        PromptDomain domain = findByCodeOrThrow(code);
        return domain.getObjectTypes();
    }

    @Transactional(readOnly = true)
    public List<PromptDomainObjectDto> listAvailableObjects() {
        return Arrays.stream(PromptDomainObjectType.values())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> listAvailableVariables(String code) {
        List<PromptDomainObjectType> objects = getObjectTypes(code);
        return contextFactory.availableVariables(objects);
    }

    private PromptDomainDto enrichDto(PromptDomain domain) {
        PromptDomainDto dto = mapper.toDto(domain);
        dto.setAvailableVariables(contextFactory.availableVariables(domain.getObjectTypes()));
        return dto;
    }

    private void validateCreate(CreatePromptDomainRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados obrigatórios não informados");
        }
        if (!StringUtils.hasText(request.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O código do domínio é obrigatório");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome do domínio é obrigatório");
        }
        if (request.getObjects() == null || request.getObjects().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione ao menos um objeto para o domínio");
        }
        String normalizedCode = normalizeCode(request.getCode());
        if (repository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um domínio com este código");
        }
    }

    private List<PromptDomainObjectType> parseObjectTypes(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione ao menos um objeto para o domínio");
        }
        Set<PromptDomainObjectType> set = new LinkedHashSet<>();
        for (String value : raw) {
            PromptDomainObjectType type;
            try {
                type = PromptDomainObjectType.fromValue(value);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Objeto inválido: " + value);
            }
            set.add(type);
        }
        return List.copyOf(set);
    }

    private String normalizeCode(String code) {
        if (!StringUtils.hasText(code)) {
            return code;
        }
        return code.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private PromptDomainObjectDto toDto(PromptDomainObjectType type) {
        PromptDomainObjectDto dto = new PromptDomainObjectDto();
        dto.setType(type.name());
        dto.setSlug(type.getSlug());
        dto.setLabel(type.getLabel());
        dto.setContextKey(type.getContextKey());
        return dto;
    }
}

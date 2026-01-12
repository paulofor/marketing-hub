package com.marketinghub.agent.service;

import com.marketinghub.agent.AgentTheme;
import com.marketinghub.agent.dto.SaveAgentThemeRequest;
import com.marketinghub.agent.repository.AgentThemeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentThemeService {

    private final AgentThemeRepository repository;

    public AgentThemeService(AgentThemeRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AgentTheme create(SaveAgentThemeRequest request) {
        AgentTheme theme = new AgentTheme();
        apply(theme, request);
        return repository.save(theme);
    }

    @Transactional
    public AgentTheme update(Long id, SaveAgentThemeRequest request) {
        AgentTheme theme = repository.findById(id).orElseThrow();
        apply(theme, request);
        return repository.save(theme);
    }

    @Transactional(readOnly = true)
    public List<AgentTheme> list() {
        return repository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public AgentTheme get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    private void apply(AgentTheme theme, SaveAgentThemeRequest request) {
        theme.setName(request.getName());
        theme.setDescription(request.getDescription());
    }
}

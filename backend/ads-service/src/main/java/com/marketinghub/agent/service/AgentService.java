package com.marketinghub.agent.service;

import com.marketinghub.agent.Agent;
import com.marketinghub.agent.AgentInput;
import com.marketinghub.agent.AgentInternalFunction;
import com.marketinghub.agent.AgentOutput;
import com.marketinghub.agent.dto.SaveAgentItemRequest;
import com.marketinghub.agent.dto.SaveAgentRequest;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentService {

    private final AgentRepository repository;
    private final AgentThemeService themeService;

    public AgentService(AgentRepository repository, AgentThemeService themeService) {
        this.repository = repository;
        this.themeService = themeService;
    }

    @Transactional
    public Agent create(SaveAgentRequest request) {
        Agent agent = new Agent();
        apply(agent, request);
        return repository.save(agent);
    }

    @Transactional
    public Agent update(Long id, SaveAgentRequest request) {
        Agent agent = repository.findDetailedById(id).orElseThrow();
        apply(agent, request);
        return repository.save(agent);
    }

    @Transactional(readOnly = true)
    public Agent get(Long id) {
        Agent agent = repository.findDetailedById(id).orElseThrow();
        initialize(agent);
        return agent;
    }

    @Transactional(readOnly = true)
    public List<Agent> list() {
        List<Agent> agents = repository.findAllByOrderByNameAsc();
        agents.forEach(this::initialize);
        return agents;
    }

    private void initialize(Agent agent) {
        agent.getInputs().size();
        agent.getOutputs().size();
        agent.getInternalFunctions().size();
        if (agent.getTheme() != null) {
            agent.getTheme().getName();
        }
    }

    private void apply(Agent agent, SaveAgentRequest request) {
        agent.setName(request.getName());
        agent.setExecutionMode(request.getExecutionMode());
        agent.setDescription(request.getDescription());
        agent.setTheme(themeService.get(request.getThemeId()));

        replaceInputs(agent, request.getInputs());
        replaceOutputs(agent, request.getOutputs());
        replaceFunctions(agent, request.getInternalFunctions());
    }

    private void replaceInputs(Agent agent, List<SaveAgentItemRequest> items) {
        agent.getInputs().clear();
        if (items == null) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            SaveAgentItemRequest item = items.get(i);
            AgentInput input = new AgentInput();
            input.setAgent(agent);
            input.setName(item.getName());
            input.setType(item.getType());
            input.setDescription(item.getDescription());
            input.setOrderIndex(item.getOrderIndex() != null ? item.getOrderIndex() : i);
            agent.getInputs().add(input);
        }
    }

    private void replaceOutputs(Agent agent, List<SaveAgentItemRequest> items) {
        agent.getOutputs().clear();
        if (items == null) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            SaveAgentItemRequest item = items.get(i);
            AgentOutput output = new AgentOutput();
            output.setAgent(agent);
            output.setName(item.getName());
            output.setType(item.getType());
            output.setDescription(item.getDescription());
            output.setOrderIndex(item.getOrderIndex() != null ? item.getOrderIndex() : i);
            agent.getOutputs().add(output);
        }
    }

    private void replaceFunctions(Agent agent, List<SaveAgentItemRequest> items) {
        agent.getInternalFunctions().clear();
        if (items == null) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            SaveAgentItemRequest item = items.get(i);
            AgentInternalFunction function = new AgentInternalFunction();
            function.setAgent(agent);
            function.setName(item.getName());
            function.setType(item.getType());
            function.setDescription(item.getDescription());
            function.setOrderIndex(item.getOrderIndex() != null ? item.getOrderIndex() : i);
            agent.getInternalFunctions().add(function);
        }
    }
}

package com.marketinghub.agent.mapper;

import com.marketinghub.agent.Agent;
import com.marketinghub.agent.AgentInput;
import com.marketinghub.agent.AgentInternalFunction;
import com.marketinghub.agent.AgentOutput;
import com.marketinghub.agent.AgentTheme;
import com.marketinghub.agent.dto.AgentDto;
import com.marketinghub.agent.dto.AgentItemDto;
import com.marketinghub.agent.dto.AgentThemeDto;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AgentMapper {

    public AgentDto toDto(Agent agent) {
        AgentDto dto = new AgentDto();
        dto.setId(agent.getId());
        dto.setName(agent.getName());
        dto.setExecutionMode(agent.getExecutionMode());
        dto.setDescription(agent.getDescription());
        dto.setThemeId(Optional.ofNullable(agent.getTheme()).map(AgentTheme::getId).orElse(null));
        dto.setThemeName(Optional.ofNullable(agent.getTheme()).map(AgentTheme::getName).orElse(null));
        dto.setInputs(mapInputs(agent.getInputs()));
        dto.setOutputs(mapOutputs(agent.getOutputs()));
        dto.setInternalFunctions(mapInternalFunctions(agent.getInternalFunctions()));
        dto.setCreatedAt(agent.getCreatedAt());
        dto.setUpdatedAt(agent.getUpdatedAt());
        return dto;
    }

    public AgentThemeDto toDto(AgentTheme theme) {
        AgentThemeDto dto = new AgentThemeDto();
        dto.setId(theme.getId());
        dto.setName(theme.getName());
        dto.setDescription(theme.getDescription());
        return dto;
    }

    private List<AgentItemDto> mapInputs(List<AgentInput> inputs) {
        return inputs == null
                ? List.of()
                : inputs.stream().sorted(Comparator.comparingInt(AgentInput::getOrderIndex)).map(input -> {
                    AgentItemDto dto = new AgentItemDto();
                    dto.setId(input.getId());
                    dto.setName(input.getName());
                    dto.setType(input.getType());
                    dto.setDescription(input.getDescription());
                    dto.setOrderIndex(input.getOrderIndex());
                    return dto;
                }).toList();
    }

    private List<AgentItemDto> mapOutputs(List<AgentOutput> outputs) {
        return outputs == null
                ? List.of()
                : outputs.stream().sorted(Comparator.comparingInt(AgentOutput::getOrderIndex)).map(output -> {
                    AgentItemDto dto = new AgentItemDto();
                    dto.setId(output.getId());
                    dto.setName(output.getName());
                    dto.setType(output.getType());
                    dto.setDescription(output.getDescription());
                    dto.setOrderIndex(output.getOrderIndex());
                    return dto;
                }).toList();
    }

    private List<AgentItemDto> mapInternalFunctions(List<AgentInternalFunction> functions) {
        return functions == null
                ? List.of()
                : functions.stream()
                        .sorted(Comparator.comparingInt(AgentInternalFunction::getOrderIndex))
                        .map(function -> {
                            AgentItemDto dto = new AgentItemDto();
                            dto.setId(function.getId());
                            dto.setName(function.getName());
                            dto.setType(function.getType());
                            dto.setDescription(function.getDescription());
                            dto.setOrderIndex(function.getOrderIndex());
                            return dto;
                        })
                        .toList();
    }
}

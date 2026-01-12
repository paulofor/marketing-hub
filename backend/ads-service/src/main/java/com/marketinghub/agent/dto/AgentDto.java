package com.marketinghub.agent.dto;

import java.time.Instant;
import java.util.List;
import lombok.Data;

@Data
public class AgentDto {
    private Long id;
    private String name;
    private String executionMode;
    private String description;
    private Long themeId;
    private String themeName;
    private List<AgentItemDto> inputs;
    private List<AgentItemDto> outputs;
    private List<AgentItemDto> internalFunctions;
    private Instant createdAt;
    private Instant updatedAt;
}

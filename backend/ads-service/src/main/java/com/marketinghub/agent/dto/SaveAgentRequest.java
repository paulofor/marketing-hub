package com.marketinghub.agent.dto;

import java.util.List;
import lombok.Data;

@Data
public class SaveAgentRequest {
    private String name;
    private String executionMode;
    private String description;
    private Long themeId;
    private List<SaveAgentItemRequest> inputs;
    private List<SaveAgentItemRequest> outputs;
    private List<SaveAgentItemRequest> internalFunctions;
}

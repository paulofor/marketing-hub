package com.marketinghub.agent.dto;

import lombok.Data;

@Data
public class SaveAgentItemRequest {
    private String name;
    private String type;
    private String description;
    private Integer orderIndex;
}

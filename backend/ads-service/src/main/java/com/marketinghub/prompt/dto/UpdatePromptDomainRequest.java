package com.marketinghub.prompt.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class UpdatePromptDomainRequest {
  private String name;
  private String description;
  private List<String> objects = new ArrayList<>();
}

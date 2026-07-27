package com.marketinghub.deliverable.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;

/** Request payload to create deliverable packages for an experiment. */
@Data
public class CreateDeliverablePackageRequest {
  @JsonAlias({"experimentId"})
  private Long experimentId;

  private UUID hypothesisId;
  private String name;
  private String description;
  private String model;
  private String prompt;
  private List<Long> deliverableIds = new ArrayList<>();
}

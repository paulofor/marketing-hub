package com.marketinghub.deliverable.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** Request payload to update deliverable packages. */
@Data
public class UpdateDeliverablePackageRequest {
  private String name;
  private String description;
  private String model;
  private String prompt;
  private List<Long> deliverableIds = new ArrayList<>();
}

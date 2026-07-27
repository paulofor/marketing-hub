package com.marketinghub.informationsource.dto;

import java.time.Instant;
import lombok.Data;

/** Data transfer object for {@link com.marketinghub.informationsource.InformationSource}. */
@Data
public class InformationSourceDto {
  private Long id;
  private Long marketNicheId;
  private String name;
  private String url;
  private Instant createdAt;
  private Instant updatedAt;
}

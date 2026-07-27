package com.marketinghub.imagedeliverable.dto;

import com.marketinghub.imagedeliverable.ImageDeliverableAccessType;
import java.time.Instant;
import lombok.Data;

/** DTO exposing generated image details. */
@Data
public class ImageDeliverableItemDto {
  private Long id;
  private Long assetId;
  private String assetUrl;
  private ImageDeliverableAccessType accessType;
  private int position;
  private Instant createdAt;
}

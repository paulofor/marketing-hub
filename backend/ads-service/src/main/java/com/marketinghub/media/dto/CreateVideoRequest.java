package com.marketinghub.media.dto;

import com.marketinghub.media.MediaProvider;
import lombok.Data;

/** Responsabilidade: transportar a solicitação e a atribuição comercial de uma geração de vídeo. */
@Data
public class CreateVideoRequest {
  private MediaProvider provider;
  private String avatar;
  private String voice;
  private String script;
  private Long campaignId;
  private Long productId;
  private Long commercialPlanId;
  private Long experimentId;
}

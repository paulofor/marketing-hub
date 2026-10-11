package com.marketinghub.salesvideo.service;

import com.marketinghub.salesvideo.dto.DeliveryExampleDto;
import java.util.List;

/** Porta da biblioteca de entregáveis; amostras não substituem provas técnicas ou vendas. */
public interface VideoDeliveryExampleSource {
  /** Lista somente imagens aprovadas e íntegras pertencentes aos planos do produto. */
  List<DeliveryExampleDto> approved(Long productId);
}

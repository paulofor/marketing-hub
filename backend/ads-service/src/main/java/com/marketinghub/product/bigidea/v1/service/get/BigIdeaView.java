package com.marketinghub.product.bigidea.v1.service.get;

import com.marketinghub.product.bigidea.v1.service.save.BigIdeaContent;
import java.time.Instant;

/** Expõe uma revisão auditável sem serializar conteúdo JSON como texto dentro da API. */
public record BigIdeaView(
    Long id,
    Long productId,
    int revision,
    BigIdeaContent content,
    String changeReason,
    String createdBy,
    Long sourceTaskId,
    Instant createdAt) {}

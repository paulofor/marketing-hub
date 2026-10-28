package com.marketinghub.chatgptads.v1.service.managecampaign;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Contratos imutáveis de preparação, autorização e execução do canal ChatGPT Ads. */
public final class ChatgptAdsContracts {
  /** Impede instâncias de um agrupador de contratos. */
  private ChatgptAdsContracts() {}

  /** Recebe preparação sem autorizar gasto ou publicar recursos externos. */
  public record Draft(
      Long experimentId,
      Long creativeId,
      String name,
      String title,
      String body,
      String imageUrl,
      String targetUrl,
      List<String> contextHints,
      String currency,
      BigDecimal totalBudget,
      BigDecimal maxCpc,
      Instant startsAt,
      Instant endsAt) {}

  /** Registra a decisão humana sobre o snapshot exato do anúncio e seu teto. */
  public record Authorization(String fingerprint, String confirmation, String decidedBy) {}

  /** Projeta os fatos e ações permitidas sem exigir inferências do frontend. */
  public record CampaignView(
      Long id,
      Long experimentId,
      String productName,
      String status,
      Draft draft,
      String fingerprint,
      boolean authorized,
      List<String> blockers,
      boolean canPublish,
      boolean canActivate,
      boolean canPause,
      JsonNode remote,
      JsonNode metrics,
      Instant updatedAt) {}

  /** Entrega a conexão conhecida e as campanhas persistidas. */
  public record Overview(JsonNode connection, List<CampaignView> campaigns) {}

  /** Contrato canônico da única operação pendente entregue ao executor. */
  public record Work(
      Long id,
      Long campaignId,
      String lease,
      String kind,
      String method,
      String path,
      JsonNode body,
      String idempotencyKey,
      String accountId) {}

  /** Preserva a resposta do provedor, sem transportar credenciais. */
  public record Receipt(String lease, int httpStatus, JsonNode response, String errorCode) {}
}

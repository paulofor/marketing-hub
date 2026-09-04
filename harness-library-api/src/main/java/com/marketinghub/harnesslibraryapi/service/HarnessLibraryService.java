package com.marketinghub.harnesslibraryapi.service;

import com.marketinghub.harnesslibraryapi.api.CardListResponse;
import com.marketinghub.harnesslibraryapi.api.CardTransitionRequest;
import com.marketinghub.harnesslibraryapi.api.CardVersionResponse;
import com.marketinghub.harnesslibraryapi.api.RegisterCardRequest;
import com.marketinghub.harnesslibraryapi.client.HarnessBackendClient;
import org.springframework.stereotype.Service;

/** Orquestra o contrato público sem criar persistência ou regra editorial paralela. */
@Service
public class HarnessLibraryService {
  private final HarnessBackendClient backendClient;

  /** Usa exclusivamente o cliente do backend principal como porta de dados. */
  public HarnessLibraryService(HarnessBackendClient backendClient) {
    this.backendClient = backendClient;
  }

  /** Encaminha um novo rascunho com identidade e correlação preservadas. */
  public CardVersionResponse register(
      RegisterCardRequest request, String actor, String idempotencyKey, String requestId) {
    return backendClient.register(request, actor, idempotencyKey, requestId);
  }

  /** Consulta versões filtradas sem pós-processar dados no gateway. */
  public CardListResponse list(
      String status, String collection, int limit, String actor, String requestId) {
    return backendClient.list(status, collection, limit, actor, requestId);
  }

  /** Consulta uma versão diretamente na fonte de verdade. */
  public CardVersionResponse get(String cardKey, int version, String actor, String requestId) {
    return backendClient.get(cardKey, version, actor, requestId);
  }

  /** Encaminha a versão para revisão editorial. */
  public CardVersionResponse submitForReview(
      String cardKey,
      int version,
      CardTransitionRequest request,
      String actor,
      String idempotencyKey,
      String requestId) {
    return backendClient.submitForReview(
        cardKey, version, request, actor, idempotencyKey, requestId);
  }

  /** Ativa uma versão revisada pelo contrato canônico. */
  public CardVersionResponse activate(
      String cardKey,
      int version,
      CardTransitionRequest request,
      String actor,
      String idempotencyKey,
      String requestId) {
    return backendClient.activate(cardKey, version, request, actor, idempotencyKey, requestId);
  }

  /** Arquiva uma versão ativa sem apagá-la. */
  public CardVersionResponse archive(
      String cardKey,
      int version,
      CardTransitionRequest request,
      String actor,
      String idempotencyKey,
      String requestId) {
    return backendClient.archive(cardKey, version, request, actor, idempotencyKey, requestId);
  }
}

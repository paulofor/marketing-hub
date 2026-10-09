package com.marketinghub.communication.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.CommunicationMaterializationContextProvider;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityCompletion;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityRequirement;
import com.marketinghub.product.Product;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Responsabilidade: preparar a decisão de uso privado sem promover peças à biblioteca comercial.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PrivateCreativeSelection {
  private static final String TOKEN = "CONFIRM:creative-production-approval:human";
  private final CommunicationMaterializationContextProvider contexts;
  private final PrivateCommunicationCreativeProof creativeProof;
  private final ObjectMapper json;

  /**
   * Oferece a confirmação privada ou seu bloqueio; os demais contratos conservam o caminho
   * comercial.
   */
  public Optional<HumanProductProcessActivityReadiness> readiness(
      BusinessProcessDefinition process, Product product, String reference) {
    return privateContext(reference)
        .map(
            context -> {
              boolean ready = false;
              String reason;
              String audit = null;
              String token = TOKEN + ":PRIVATE:blocked";
              try {
                var proof = proof(process, product, reference, context);
                ready = true;
                reason =
                    "Íris, Psique e Têmis concluíram a mesma peça privada. Falta seu aceite de uso.";
                audit = auditReference(reference, proof);
                token = confirmationToken(reference, context, proof);
              } catch (RuntimeException ex) {
                log.warn(
                    "Seleção criativa privada bloqueada. processId={} productId={} sourceReference={}",
                    process.getId(),
                    product.getId(),
                    reference,
                    ex);
                reason = ex.getMessage();
              }
              return new HumanProductProcessActivityReadiness(
                  ready,
                  reason,
                  "Aprovar uso privado da peça",
                  "Registra a peça e os pareceres nesta preparação privada. Não publica, cobra, autoriza mídia ou gera vídeo pago.",
                  "Confirmar uso privado",
                  "Aprovo o uso desta peça revisada na preparação privada, sem publicação comercial, cobrança ou mídia.",
                  token,
                  null,
                  null,
                  List.of(
                      new HumanProductProcessActivityRequirement(
                          "APPROVED_PRIVATE_CREATIVE_PIXELS",
                          "Peça privada e pareceres da mesma versão",
                          ready,
                          reason,
                          ready
                              ? "Confira a peça final e confirme seu uso privado."
                              : "Resolva a pendência da versão ou dos pareceres antes de aprovar.")),
                  HumanProductProcessActivityReadiness.REVIEW_AND_ACCEPT,
                  audit);
            });
  }

  /**
   * Revalida contratos e pixels na confirmação e fornece sua auditoria sem escrita em storage
   * público.
   */
  public Optional<HumanProductProcessActivityCompletion> completeApproval(
      BusinessProcessDefinition process,
      Product product,
      String reference,
      String confirmationToken) {
    return privateContext(reference)
        .map(
            context -> {
              var proof = proof(process, product, reference, context);
              require(
                  confirmationToken(reference, context, proof).equals(confirmationToken),
                  "A peça ou seus pareceres mudaram. Confira a versão atual antes de confirmar seu uso.");
              Map<String, Object> evidence = new LinkedHashMap<>();
              evidence.put("evidenceType", "PDE_PRIVATE_CREATIVE_SELECTION_V1");
              evidence.put("scope", "PRIVATE_PREPARATION");
              evidence.put("sourceReference", reference);
              evidence.put("productId", product.getId());
              evidence.put("prototypeVersion", context.path("prototypeVersion").asText());
              evidence.put("creativeProof", json.convertValue(proof, Map.class));
              evidence.put(
                  "selectionSha256",
                  confirmationToken.substring(confirmationToken.lastIndexOf(':') + 1));
              evidence.put("published", false);
              evidence.put("publicationAuthorized", false);
              evidence.put("paymentEnabled", false);
              evidence.put("externalMediaSpendAuthorized", false);
              return HumanProductProcessActivityCompletion.completed(evidence);
            });
  }

  /**
   * Identifica o modo canônico mesmo quando bloqueado, impedindo fallback para importação
   * comercial.
   */
  private Optional<JsonNode> privateContext(String reference) {
    return contexts
        .resolve(reference)
        .map(input -> json.<JsonNode>valueToTree(input))
        .filter(
            context ->
                PrivateCreativePreparationContext.isPrivateMode(context.path("mode").asText()));
  }

  /**
   * Exige contexto aprovado e identidade exata antes de reutilizar a prova compartilhada dos
   * pareceres.
   */
  private JsonNode proof(
      BusinessProcessDefinition process, Product product, String reference, JsonNode context) {
    require(
        "creative-production-approval".equals(process.getProcessCode()) && process.getId() != null,
        "A seleção não pertence ao subprocesso criativo.");
    require(
        "AVAILABLE".equals(context.path("availability").asText())
            && "READY".equals(context.path("inputReadiness").asText()),
        context.path("reason").asText("O gate privado precisa estar aprovado."));
    require(
        reference != null
            && reference.equals(context.path("sourceReference").asText())
            && product.getId().equals(context.path("product").path("id").asLong())
            && !context.path("prototypeVersion").asText().isBlank(),
        "Produto, referência ou versão divergente na seleção privada.");
    var preparation = context.path(PrivateCreativePreparationContext.FIELD);
    require(
        PrivateCreativePreparationContext.isPreparation(preparation, reference)
            && context
                .path("prototypeVersion")
                .asText()
                .equals(preparation.path("prototypeVersion").asText())
            && !context.path("publicationAuthorized").asBoolean(true)
            && !context.path("paymentEnabled").asBoolean(true)
            && !context.path("externalMediaSpendAuthorized").asBoolean(true),
        "O contrato não preserva os limites da preparação privada.");
    return creativeProof.resolveReviews(
        process.getId(), reference, context.path("prototypeVersion").asText());
  }

  /** Correlaciona a decisão de um clique com a produção e os pixels previamente revisados. */
  private String auditReference(String reference, JsonNode proof) {
    return reference
        + ";agent-task:"
        + proof.path("producerTaskId").asLong()
        + ";"
        + proof.path("renderedAssets").findValuesAsText("sha256").stream()
            .sorted()
            .reduce((a, b) -> a + "," + b)
            .orElse("");
  }

  /**
   * Vincula a confirmação ao produto, versão, referência, produção e pareceres mostrados na tela.
   */
  private String confirmationToken(String reference, JsonNode context, JsonNode proof) {
    return TOKEN
        + ":PRIVATE:"
        + IrisCommunicationInputFingerprint.hash(
            json,
            Map.of(
                "sourceReference",
                reference,
                "productId",
                context.path("product").path("id").asLong(),
                "prototypeVersion",
                context.path("prototypeVersion").asText(),
                "creativeProof",
                proof));
  }

  /** Preserva o motivo concreto que bloqueia a decisão sem recorrer a um plano artificial. */
  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalStateException(message);
  }
}

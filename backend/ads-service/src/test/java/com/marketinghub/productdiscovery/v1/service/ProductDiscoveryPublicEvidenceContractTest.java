package com.marketinghub.productdiscovery.v1.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunityDecision;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunityMaturity;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** Protege o gate público contra promoção sem suporte ou duplicação de fontes comportamentais. */
class ProductDiscoveryPublicEvidenceContractTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Aceita relatos rastreáveis sem exigir entrevista ou venda própria. */
  @Test
  void acceptsSupportedPublicEvidence() throws Exception {
    ProductDiscoveryPublicEvidenceContract.validate(901L, candidate(evidence(), true));
  }

  /** Conserva pesquisa insuficiente sem exigir que o modelo fabrique fatos. */
  @Test
  void acceptsOpenGapWithoutApproval() throws Exception {
    var root = evidence();
    root.withObject("/publicEvidenceAssessment").putArray("observations");
    root.withObject("/publicEvidenceAssessment").put("ready", false);
    ProductDiscoveryPublicEvidenceContract.validate(901L, candidate(root, false));
    assertThatThrownBy(
            () -> ProductDiscoveryPublicEvidenceContract.validate(901L, candidate(root, true)))
        .hasMessageContaining("contrato de evidências públicas");
  }

  /** Recusa trechos inexistentes mesmo quando o worker declara suporte suficiente. */
  @Test
  void rejectsFabricatedExcerpt() throws Exception {
    var root = evidence();
    ((ObjectNode) root.at("/publicEvidenceAssessment/observations/0"))
        .put("supportingExcerpt", "Uma compra inexistente relatada pelo modelo");
    assertThatThrownBy(
            () -> ProductDiscoveryPublicEvidenceContract.validate(901L, candidate(root, true)))
        .hasMessageContaining("contrato de evidências públicas");
  }

  /** Não aceita copy de vendedor como confirmação independente de comportamento. */
  @Test
  void rejectsSellerClaimCountedAsCustomer() throws Exception {
    var root = evidence();
    ((ObjectNode) root.at("/publicEvidenceAssessment/observations/0"))
        .put("sourceRole", "SELLER_CLAIM");
    assertThatThrownBy(
            () -> ProductDiscoveryPublicEvidenceContract.validate(902L, candidate(root, true)))
        .hasMessageContaining("contrato de evidências públicas");
  }

  /** Monta fontes sintéticas com suporte independente, sem dados pessoais. */
  private ObjectNode evidence() throws Exception {
    var root = json.createObjectNode();
    var ids = root.putObject("candidateEvidence").putArray("evidenceIds");
    var sources = root.putObject("referencedEvidence").putArray("publicEvidence");
    var assessment =
        root.putObject("publicEvidenceAssessment")
            .put("policy", "PUBLIC_SOURCES_V1")
            .put("ready", true);
    var observations = assessment.putArray("observations");
    for (int i = 0; i < 2; i++) {
      String id = "P" + i;
      String url = "https://community.example" + i + ".org/report";
      String text = "Relato sintético de uso: tentei resolver e ainda tive dificuldade.";
      String hash =
          HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(text.getBytes(StandardCharsets.UTF_8)));
      ids.add(id);
      sources
          .addObject()
          .put("evidenceId", id)
          .put("url", url)
          .put("snippet", text)
          .put("retrievedAt", "2026-09-24T12:00:00Z");
      observations
          .addObject()
          .put("evidenceId", id)
          .put("url", url)
          .put("retrievedAt", "2026-09-24T12:00:00Z")
          .put("sourceRole", "PUBLIC_CUSTOMER_REPORT")
          .put("reportedAction", "USE_REPORTED")
          .put("supportingExcerpt", text)
          .put("limitation", "Relato não conciliado")
          .put("evidenceScope", "SEARCH_EXCERPT_ONLY")
          .put("classification", "MODEL_CLASSIFIED")
          .put("sourceSha256", hash);
    }
    return root;
  }

  /** Declara o resultado funcional sem vincular a fixture a uma candidata produtiva. */
  private ProductDiscoveryOpportunityResultRequest candidate(ObjectNode root, boolean approve) {
    return new ProductDiscoveryOpportunityResultRequest(
        "Candidata sintética",
        "Público",
        "Dificuldade",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        root.toString(),
        BigDecimal.TEN,
        approve
            ? ProductDiscoveryOpportunityMaturity.DOSSIER_READY
            : ProductDiscoveryOpportunityMaturity.RESEARCHABLE,
        approve
            ? ProductDiscoveryOpportunityDecision.APPROVE
            : ProductDiscoveryOpportunityDecision.RESEARCH_MORE);
  }
}

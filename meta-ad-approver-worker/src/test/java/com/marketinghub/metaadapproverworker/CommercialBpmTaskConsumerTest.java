package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Responsabilidade: proteger o contrato funcional do gate BPM de Têmis. */
class CommercialBpmTaskConsumerTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Aceita gate aprovado somente com justificativa e evidência comercial. */
  @Test
  void acceptsCompleteCommercialReview() throws Exception {
    CommercialBpmTaskConsumer.validate(
        json.readTree(
            "{\"decision\":\"APPROVED\",\"commercialRationale\":\"Jornada coerente\",\"evidence\":[\"Preço consistente\"],\"requiredChanges\":[]}"));
  }

  /** Rejeita autoaprovação vazia que liberaria o processo sem gate real. */
  @Test
  void rejectsCommercialReviewWithoutEvidence() throws Exception {
    var result =
        json.readTree(
            "{\"decision\":\"APPROVED\",\"commercialRationale\":\"\",\"evidence\":[],\"requiredChanges\":[]}");
    assertThatThrownBy(() -> CommercialBpmTaskConsumer.validate(result))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /** Rejeita aprovação que interprete equivocadamente a escala percentual de clareza do preço. */
  @Test
  void rejectsApprovedReviewWithLowPriceClarityScore() throws Exception {
    var result =
        json.readTree(
            "{\"decision\":\"APPROVED\",\"commercialRationale\":\"Preço supostamente claro\",\"priceClarityScore\":10,\"evidence\":[\"Preço de R$ 67\"],\"requiredChanges\":[]}");

    assertThatThrownBy(() -> CommercialBpmTaskConsumer.validate(result))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("80/100");
  }

  /** Exige prompt e schema próprios para o gate comercial do criativo. */
  @Test
  void selectsVersionedCreativeContract() throws Exception {
    org.assertj.core.api.Assertions.assertThat(
            CommercialBpmTaskConsumer.promptResourceFor("creative-production-approval"))
        .isEqualTo("prompts/bpm/creative-commercial-review.md");
    org.assertj.core.api.Assertions.assertThat(
            CommercialBpmTaskConsumer.schemaResourceFor("creative-production-approval"))
        .isEqualTo("prompts/bpm/creative-commercial-review-schema.json");
    org.assertj.core.api.Assertions.assertThat(read("prompts/bpm/creative-commercial-review.md"))
        .contains("PRODUCT_PROOF", "Só aplique limites da Meta", "contato direto consentido")
        .doesNotContain("demonstra inequivocamente o kit digital");
  }

  /** Mantém o polling de Têmis alinhado aos identificadores publicados no processo v6. */
  @Test
  void supportsPublishedCreativeProductionActivities() {
    org.assertj.core.api.Assertions.assertThat(
            CommercialBpmTaskConsumer.supportsContract("creative-production-approval", "route"))
        .isTrue();
    org.assertj.core.api.Assertions.assertThat(
            CommercialBpmTaskConsumer.supportsContract("creative-production-approval", "produce"))
        .isTrue();
    org.assertj.core.api.Assertions.assertThat(
            CommercialBpmTaskConsumer.supportsContract("creative-production-approval", "generate"))
        .isFalse();
  }

  /** Seleciona o contrato independente de revisão dos entregáveis do PDE. */
  @Test
  void selectsVersionedPdeDeliverablesContract() {
    org.assertj.core.api.Assertions.assertThat(
            CommercialBpmTaskConsumer.promptResourceFor("pde-construction-approval"))
        .isEqualTo("prompts/bpm/pde-deliverables-review.md");
    org.assertj.core.api.Assertions.assertThat(
            CommercialBpmTaskConsumer.schemaResourceFor("pde-construction-approval"))
        .isEqualTo("prompts/bpm/pde-deliverables-review-schema.json");
  }

  /** Seleciona a tradução estratégica em comunicação sem reutilizar prompt de landing. */
  @Test
  void selectsVersionedPdeCommunicationContract() throws Exception {
    org.assertj.core.api.Assertions.assertThat(
            CommercialBpmTaskConsumer.promptResourceFor("pde-communication-sales-journey"))
        .isEqualTo("prompts/bpm/pde-communication-translation-v2.md");
    org.assertj.core.api.Assertions.assertThat(
            CommercialBpmTaskConsumer.schemaResourceFor("pde-communication-sales-journey"))
        .isEqualTo("prompts/bpm/pde-communication-translation-v2-schema.json");

    String prompt = read("prompts/bpm/pde-communication-translation-v2.md");
    String schema = read("prompts/bpm/pde-communication-translation-v2-schema.json");
    org.assertj.core.api.Assertions.assertThat(prompt)
        .contains(
            "transformar a estratégia aprovada de Atena",
            "não redefine mercado",
            "exatamente três traduções",
            "Hermes produz separadamente distribuição",
            "não altera preço",
            "80–100");
    org.assertj.core.api.Assertions.assertThat(schema)
        .contains(
            "strategicContractReference",
            "communicationAlternatives",
            "communicationContract",
            "priceClarityScore",
            "commercialRationale",
            "requiredChanges");
  }

  /** Rejeita aprovação que indique necessidade de mudar a estratégia de Atena. */
  @Test
  void rejectsCommunicationApprovalThatRequiresStrategicRevision() throws Exception {
    var result =
        json.readTree(
            """
            {
              "decision":"APPROVED",
              "commercialRationale":"Mensagem aparentemente pronta.",
              "strategicContractReference":{"contentHash":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","strategyPreserved":true,"revisionRequired":true},
              "communicationAlternatives":[{},{},{}],
              "communicationContract":{"creativeBrief":"Briefing completo para o criativo."},
              "priceClarityScore":90,
              "evidence":["Contrato consultado"],
              "requiredChanges":[]
            }
            """);

    assertThatThrownBy(
            () ->
                CommercialBpmTaskConsumer.validate(
                    result,
                    "pde-communication-sales-journey",
                    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
        .hasMessageContaining("não preserva a estratégia");
  }

  /** Rejeita tradução que devolva identidade diferente da estratégia recebida. */
  @Test
  void rejectsDifferentAtenaContractHash() throws Exception {
    var result =
        json.readTree(
            """
            {
              "decision":"ADJUST",
              "commercialRationale":"Mensagem precisa de ajuste.",
              "strategicContractReference":{"contentHash":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","strategyPreserved":true,"revisionRequired":false},
              "communicationAlternatives":[{},{},{}],
              "communicationContract":{"creativeBrief":"Briefing completo para o criativo."},
              "priceClarityScore":90,
              "evidence":["Contrato consultado"],
              "requiredChanges":["Ajustar prova"]
            }
            """);

    assertThatThrownBy(
            () ->
                CommercialBpmTaskConsumer.validate(
                    result,
                    "pde-communication-sales-journey",
                    "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"))
        .hasMessageContaining("não preserva a estratégia");
  }

  /** Exige contrato íntegro antes de Têmis consumir o modelo para traduzir a estratégia. */
  @Test
  void validatesMarketStrategyBeforeCommunicationModel() throws Exception {
    var valid =
        json.readTree(
            """
            {
              "availability":"AVAILABLE",
              "contractVersion":"MARKET_STRATEGY_V2",
              "contentHash":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
              "contract":{
                "contractVersion":"MARKET_STRATEGY_V2",
                "status":"READY_FOR_OPERATION",
                "operatorBoundary":"ATENA_DEFINES_STRATEGY_HERMES_OPERATES_GROWTH"
              }
            }
            """);
    var malformedHash = valid.deepCopy();
    ((com.fasterxml.jackson.databind.node.ObjectNode) malformedHash)
        .put("contentHash", "z".repeat(64));

    org.assertj.core.api.Assertions.assertThat(
            CommercialBpmTaskConsumer.isReadyMarketStrategicContract(valid))
        .isTrue();
    org.assertj.core.api.Assertions.assertThat(
            CommercialBpmTaskConsumer.isReadyMarketStrategicContract(malformedHash))
        .isFalse();
  }

  /** Seleciona o gate independente que antecede o preflight e a autorização humana. */
  @Test
  void selectsVersionedPdeCommercialHomologationContract() throws Exception {
    org.assertj.core.api.Assertions.assertThat(
            CommercialBpmTaskConsumer.promptResourceFor("pde-commercial-homologation-activation"))
        .isEqualTo("prompts/bpm/pde-commercial-homologation-independent-review.md");
    org.assertj.core.api.Assertions.assertThat(
            CommercialBpmTaskConsumer.schemaResourceFor("pde-commercial-homologation-activation"))
        .isEqualTo("prompts/bpm/pde-commercial-homologation-independent-review-schema.json");

    String prompt = read("prompts/bpm/pde-commercial-homologation-independent-review.md");
    String schema = read("prompts/bpm/pde-commercial-homologation-independent-review-schema.json");
    org.assertj.core.api.Assertions.assertThat(prompt)
        .contains(
            "READY_FOR_PREFLIGHT",
            "SHA-256",
            "não autoriza `RUNNING`",
            "canal efetivo proposto",
            "QA excluído",
            "Nunca use escala de 0 a 10",
            "nota mínima de 80");
    org.assertj.core.api.Assertions.assertThat(schema)
        .contains("activationRecommendation", "gateChecks", "priceClarityScore");
  }

  /** Mantém Têmis na coerência comercial da landing sem antecipar pagamento e acesso. */
  @Test
  void acceptsCanonicalCheckoutBindingAsLandingEvidence() throws Exception {
    String prompt = read("prompts/bpm/landing-commercial-review.md");
    String normalizedPrompt = prompt.replaceAll("\\s+", " ");

    org.assertj.core.api.Assertions.assertThat(normalizedPrompt)
        .contains(
            "VALIDATED_FROM_PERSISTED_CANONICAL_BINDING",
            "não exija aqui captura ou pagamento no provedor externo",
            "Integração de canal, checkout, acesso e eventos",
            "approvedCreativeEvidence.status",
            "adCopy` ou `adImageBriefing`");
  }

  /** Exige Flex no gate de IA para manter custo e contrato operacional auditáveis. */
  @Test
  void usesFlexServiceTier() {
    org.assertj.core.api.Assertions.assertThat(CommercialBpmTaskConsumer.serviceTier())
        .isEqualTo("flex");
    org.assertj.core.api.Assertions.assertThat(CommercialBpmTaskConsumer.effectiveServiceTier())
        .isEqualTo("STANDARD");
  }

  /** Lê integralmente um contrato versionado do classpath. */
  private String read(String resource) throws Exception {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Lê entrada, cache e saída cumulativos do gate executado pelo Codex. */
  @Test
  void readsTaskTokenUsageFromCodexJsonl() throws Exception {
    Path output = Files.createTempFile("temis-bpm-usage-", ".jsonl");
    Files.writeString(
        output,
        """
        mensagem operacional
        {"type":"turn.completed","usage":{"input_tokens":2400,"cached_input_tokens":1500,"output_tokens":450}}
        """);

    CommercialBpmTaskConsumer.TokenUsage usage =
        CommercialBpmTaskConsumer.readTokenUsage(json, output);

    org.assertj.core.api.Assertions.assertThat(usage.inputTokens()).isEqualTo(2400L);
    org.assertj.core.api.Assertions.assertThat(usage.cachedInputTokens()).isEqualTo(1500L);
    org.assertj.core.api.Assertions.assertThat(usage.outputTokens()).isEqualTo(450L);
    Files.deleteIfExists(output);
  }

  /** Preserva contexto e ausência de efeitos externos inclusive quando Têmis bloqueia. */
  @Test
  void buildsGovernedFailureEvidence() {
    Map<String, Object> evidence =
        CommercialBpmTaskConsumer.evidenceFields(
            "Têmis",
            "gpt-5.6-sol",
            Map.of("sourceReference", "experiment:88", "activityId", "commercial"));

    org.assertj.core.api.Assertions.assertThat(evidence)
        .containsEntry("sourceReference", "experiment:88")
        .containsEntry("activityId", "commercial")
        .containsEntry("accessMode", "READ_ONLY")
        .containsEntry("externalSideEffects", false)
        .containsEntry("requestedServiceTier", "FLEX")
        .containsEntry("effectiveServiceTier", "STANDARD")
        .containsKey("serviceTierException");
  }
}

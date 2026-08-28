package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o contrato funcional da revisão BPM de Psique. */
class CustomerBpmTaskConsumerTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Aceita parecer aprovado somente quando há perspectiva e evidência verificável. */
  @Test
  void acceptsCompleteCustomerReview() throws Exception {
    CustomerBpmTaskConsumer.validate(
        json.readTree(
            """
            {
              "decision":"APPROVED",
              "customerPerspective":"Oferta clara",
              "behavioralResponse":{
                "firstImpulse":"Curiosidade e alívio",
                "belongingAdmirationLove":"Promete reconhecimento profissional sem pressionar insegurança",
                "sensoryExperience":{
                  "evidenceAvailable":true,
                  "availableModalities":["VISUAL"],
                  "pleasureByModality":[{"modality":"VISUAL","pleasureScore":4,"evidence":"Hierarquia clara"}],
                  "processingFluency":5,
                  "sensoryCongruence":4,
                  "overloadRisk":1,
                  "embodiedAnticipation":"Imagino usar o produto sem esforço",
                  "dominantCue":"Demonstração visual do resultado",
                  "evidenceBoundary":"Somente screenshot fornecido"
                }
              },
              "evidence":["CTA visível"],
              "requiredChanges":[]
            }
            """));
  }

  /** Rejeita aprovação vazia que liberaria Têmis sem avaliação real da cliente. */
  @Test
  void rejectsReviewWithoutEvidence() throws Exception {
    var result =
        json.readTree(
            "{\"decision\":\"APPROVED\",\"customerPerspective\":\"\",\"evidence\":[],\"requiredChanges\":[]}");
    assertThatThrownBy(() -> CustomerBpmTaskConsumer.validate(result))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /** Rejeita decisão aprovada que ainda preserve gate funcional em ajuste. */
  @Test
  void rejectsApprovedReviewWithAdjustedGate() throws Exception {
    var result =
        json.readTree(
            "{\"decision\":\"APPROVED\",\"customerPerspective\":\"Oferta clara e utilizável\",\"behavioralResponse\":{\"firstImpulse\":\"Curiosidade segura\",\"belongingAdmirationLove\":\"Desejo sem pressão\",\"sensoryExperience\":{\"evidenceAvailable\":false,\"availableModalities\":[],\"pleasureByModality\":[],\"processingFluency\":0,\"sensoryCongruence\":0,\"overloadRisk\":0,\"embodiedAnticipation\":\"Não observável\",\"dominantCue\":\"Não observado\",\"evidenceBoundary\":\"Sem evidência sensorial\"}},\"gateChecks\":[{\"status\":\"ADJUST\"}],\"evidence\":[\"Jornada comprovada\"],\"requiredChanges\":[]}");

    assertThatThrownBy(() -> CustomerBpmTaskConsumer.validate(result))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("gate não aprovado");
  }

  /** Rejeita um parecer racionalmente correto que omita impulso e valor relacional. */
  @Test
  void rejectsFullyRationalReviewWithoutHumanBehavior() throws Exception {
    var result =
        json.readTree(
            """
            {
              "decision":"APPROVED",
              "customerPerspective":"Oferta clara",
              "evidence":["CTA visível"],
              "requiredChanges":[]
            }
            """);

    assertThatThrownBy(() -> CustomerBpmTaskConsumer.validate(result))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /** Exige prompt e schema próprios para a percepção do criativo. */
  @Test
  void selectsVersionedCreativeContract() throws Exception {
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.promptResourceFor("creative-production-approval"))
        .isEqualTo("prompts/bpm/v2/creative-customer-review.md");
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.schemaResourceFor("creative-production-approval"))
        .isEqualTo("prompts/bpm/v2/creative-customer-review-schema.json");
    String prompt =
        Files.readString(Path.of("src/main/resources/prompts/bpm/v2/creative-customer-review.md"));
    org.assertj.core.api.Assertions.assertThat(prompt)
        .contains("formato e canal declarados", "PRODUCT_PROOF", "dois primeiros segundos")
        .doesNotContain("nail designer", "posts e stories prontos");
  }

  /** Seleciona o contrato de revisão integral da experiência do PDE. */
  @Test
  void selectsVersionedPdeExperienceContract() {
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.promptResourceFor("pde-construction-approval"))
        .isEqualTo("prompts/bpm/v2/pde-experience-review.md");
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.schemaResourceFor("pde-construction-approval"))
        .isEqualTo("prompts/bpm/v2/pde-experience-review-schema.json");
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.supportsContract(
                "pde-construction-approval", "humanExperienceReview"))
        .isTrue();
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.supportsContract("pde-construction-approval", "review"))
        .isFalse();
  }

  /** Seleciona o gate específico da cliente para homologação comercial do PDE. */
  @Test
  void selectsVersionedPdeCommercialHomologationContract() throws Exception {
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.promptResourceFor("pde-commercial-homologation-activation"))
        .isEqualTo("prompts/bpm/v2/pde-commercial-homologation-customer-review.md");
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.schemaResourceFor("pde-commercial-homologation-activation"))
        .isEqualTo("prompts/bpm/v2/pde-commercial-homologation-customer-review-schema.json");
    String prompt =
        Files.readString(
            Path.of(
                "src/main/resources/prompts/bpm/v2/pde-commercial-homologation-customer-review.md"));
    org.assertj.core.api.Assertions.assertThat(prompt)
        .contains(
            "taskTarget",
            "UPDATED_CANDIDATE",
            "fronteira externa esperada",
            "Use `ADJUST` somente para defeito corrigível na candidata local",
            "todos os itens de `gateChecks` em `PASS`");
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.supportsContract(
                "pde-commercial-homologation-activation", "humanExperienceReview"))
        .isTrue();
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.supportsContract(
                "pde-commercial-homologation-activation", "pdeGate"))
        .isFalse();
  }

  /** Mantém Psique no escopo da landing sem antecipar o preflight do subprocesso seguinte. */
  @Test
  void acceptsCanonicalCheckoutBindingAsLandingEvidence() throws Exception {
    String prompt =
        Files.readString(Path.of("src/main/resources/prompts/bpm/v2/landing-customer-review.md"));
    String normalizedPrompt = prompt.replaceAll("\\s+", " ");

    org.assertj.core.api.Assertions.assertThat(normalizedPrompt)
        .contains(
            "VALIDATED_FROM_PERSISTED_CANONICAL_BINDING",
            "EVIDENCE_TRANSPORT",
            "Não peça reconstrução da landing",
            "Não bloqueie apenas porque a tela do provedor externo não pôde ser aberta",
            "Integração de canal, checkout, acesso e eventos",
            "approvedCreativeEvidence.status",
            "adCopy` ou `adImageBriefing` legados");
    String schema =
        Files.readString(
            Path.of("src/main/resources/prompts/bpm/v2/landing-customer-review-schema.json"));
    org.assertj.core.api.Assertions.assertThat(schema)
        .contains("remediationTarget", "LANDING_CONTENT", "CANONICAL_CONTRACT");
  }

  /** Exige Flex no gate de IA para manter custo e contrato operacional auditáveis. */
  @Test
  void usesFlexServiceTier() {
    org.assertj.core.api.Assertions.assertThat(CustomerBpmTaskConsumer.serviceTier())
        .isEqualTo("flex");
    org.assertj.core.api.Assertions.assertThat(CustomerBpmTaskConsumer.effectiveServiceTier())
        .isEqualTo("STANDARD");
  }

  /** Exige o núcleo afetivo, social e sensorial em todos os contratos BPM atuais. */
  @Test
  void requiresSharedBehavioralCoreInEveryBpmReview() throws Exception {
    String core =
        Files.readString(Path.of("src/main/resources/prompts/psique/behavioral-core-v3.md"));
    String creative =
        Files.readString(
            Path.of("src/main/resources/prompts/bpm/v2/creative-customer-review-schema.json"));
    String landing =
        Files.readString(
            Path.of("src/main/resources/prompts/bpm/v2/landing-customer-review-schema.json"));
    String pde =
        Files.readString(
            Path.of("src/main/resources/prompts/bpm/v2/pde-experience-review-schema.json"));
    String commercialHomologation =
        Files.readString(
            Path.of(
                "src/main/resources/prompts/bpm/v2/pde-commercial-homologation-customer-review-schema.json"));

    org.assertj.core.api.Assertions.assertThat(core)
        .contains("reação afetiva rápida")
        .contains("faixa de novidade segura")
        .contains("amada")
        .contains("prazer sensorial")
        .contains("Não recomende explorar vergonha");
    org.assertj.core.api.Assertions.assertThat(
            java.util.List.of(creative, landing, pde, commercialHomologation))
        .allSatisfy(
            schema ->
                org.assertj.core.api.Assertions.assertThat(schema)
                    .contains(
                        "behavioralResponse", "belongingAdmirationLove", "sensoryExperience"));
  }

  /** Lê os contadores cumulativos e a parcela de cache informados pelo Codex. */
  @Test
  void readsTaskTokenUsageFromCodexJsonl() throws Exception {
    Path output = Files.createTempFile("psique-bpm-usage-", ".jsonl");
    Files.writeString(
        output,
        """
        WARN inicialização
        {"type":"turn.completed","usage":{"input_tokens":1200,"input_tokens_details":{"cached_tokens":700},"output_tokens":300}}
        """);

    CustomerBpmTaskConsumer.TokenUsage usage = CustomerBpmTaskConsumer.readTokenUsage(json, output);

    org.assertj.core.api.Assertions.assertThat(usage.inputTokens()).isEqualTo(1200L);
    org.assertj.core.api.Assertions.assertThat(usage.cachedInputTokens()).isEqualTo(700L);
    org.assertj.core.api.Assertions.assertThat(usage.outputTokens()).isEqualTo(300L);
    Files.deleteIfExists(output);
  }

  /** Preserva contexto e ausência de efeitos externos inclusive quando Psique bloqueia. */
  @Test
  void buildsGovernedFailureEvidence() {
    Map<String, Object> evidence =
        CustomerBpmTaskConsumer.evidenceFields(
            "Psique",
            "gpt-5.6-sol",
            Map.of(
                "sourceReference",
                "experiment:88",
                "activityId",
                "customer",
                "taskTarget",
                Map.of("productId", 9L, "productSlug", "kit-whatsapp-pronto")));

    org.assertj.core.api.Assertions.assertThat(evidence)
        .containsEntry("sourceReference", "experiment:88")
        .containsEntry("activityId", "customer")
        .containsEntry("accessMode", "READ_ONLY")
        .containsEntry("externalSideEffects", false)
        .containsEntry("requestedServiceTier", "FLEX")
        .containsEntry("effectiveServiceTier", "STANDARD")
        .containsKey("taskTarget")
        .containsKey("serviceTierException");
  }
}

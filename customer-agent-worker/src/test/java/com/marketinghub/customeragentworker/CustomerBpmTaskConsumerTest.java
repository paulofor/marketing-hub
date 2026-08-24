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
                "belongingAdmirationLove":"Promete reconhecimento profissional sem pressionar insegurança"
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
        .isEqualTo("prompts/bpm/creative-customer-review.md");
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.schemaResourceFor("creative-production-approval"))
        .isEqualTo("prompts/bpm/creative-customer-review-schema.json");
    String prompt =
        Files.readString(Path.of("src/main/resources/prompts/bpm/creative-customer-review.md"));
    org.assertj.core.api.Assertions.assertThat(prompt)
        .contains("formato e canal declarados", "PRODUCT_PROOF", "dois primeiros segundos")
        .doesNotContain("nail designer", "posts e stories prontos");
  }

  /** Seleciona o contrato de revisão integral da experiência do PDE. */
  @Test
  void selectsVersionedPdeExperienceContract() {
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.promptResourceFor("pde-construction-approval"))
        .isEqualTo("prompts/bpm/pde-experience-review.md");
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.schemaResourceFor("pde-construction-approval"))
        .isEqualTo("prompts/bpm/pde-experience-review-schema.json");
  }

  /** Seleciona o gate específico da cliente para homologação comercial do PDE. */
  @Test
  void selectsVersionedPdeCommercialHomologationContract() {
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.promptResourceFor("pde-commercial-homologation-activation"))
        .isEqualTo("prompts/bpm/pde-commercial-homologation-customer-review.md");
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.schemaResourceFor("pde-commercial-homologation-activation"))
        .isEqualTo("prompts/bpm/pde-commercial-homologation-customer-review-schema.json");
  }

  /** Exige Flex no gate de IA para manter custo e contrato operacional auditáveis. */
  @Test
  void usesFlexServiceTier() {
    org.assertj.core.api.Assertions.assertThat(CustomerBpmTaskConsumer.serviceTier())
        .isEqualTo("flex");
    org.assertj.core.api.Assertions.assertThat(CustomerBpmTaskConsumer.effectiveServiceTier())
        .isEqualTo("STANDARD");
  }

  /** Exige o núcleo afetivo, a surpresa segura e o desejo de amor nos contratos BPM. */
  @Test
  void requiresSharedBehavioralCoreInEveryBpmReview() throws Exception {
    String core =
        Files.readString(Path.of("src/main/resources/prompts/psique/behavioral-core-v2.md"));
    String creative =
        Files.readString(
            Path.of("src/main/resources/prompts/bpm/creative-customer-review-schema.json"));
    String landing =
        Files.readString(
            Path.of("src/main/resources/prompts/bpm/landing-customer-review-schema.json"));
    String pde =
        Files.readString(
            Path.of("src/main/resources/prompts/bpm/pde-experience-review-schema.json"));
    String commercialHomologation =
        Files.readString(
            Path.of(
                "src/main/resources/prompts/bpm/pde-commercial-homologation-customer-review-schema.json"));

    org.assertj.core.api.Assertions.assertThat(core)
        .contains("reação afetiva rápida")
        .contains("faixa de novidade segura")
        .contains("amada")
        .contains("Não recomende explorar vergonha");
    org.assertj.core.api.Assertions.assertThat(
            java.util.List.of(creative, landing, pde, commercialHomologation))
        .allSatisfy(
            schema ->
                org.assertj.core.api.Assertions.assertThat(schema)
                    .contains("behavioralResponse", "belongingAdmirationLove"));
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
            Map.of("sourceReference", "experiment:88", "activityId", "customer"));

    org.assertj.core.api.Assertions.assertThat(evidence)
        .containsEntry("sourceReference", "experiment:88")
        .containsEntry("activityId", "customer")
        .containsEntry("accessMode", "READ_ONLY")
        .containsEntry("externalSideEffects", false)
        .containsEntry("requestedServiceTier", "FLEX")
        .containsEntry("effectiveServiceTier", "STANDARD")
        .containsKey("serviceTierException");
  }
}

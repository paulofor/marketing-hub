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

  /** Bloqueia Têmis antes de reservar tarefas quando o raciocínio não está configurado. */
  @Test
  void rejectsMissingReasoningEffortBeforeModel() {
    MetaAdApproverProperties properties = new MetaAdApproverProperties();
    properties.setReasoningEffort(" ");

    assertThatThrownBy(
            () ->
                new CommercialBpmTaskConsumer(
                    properties, "codex", "gpt-5.6-sol", "/workspace", "", json))
        .hasMessageContaining("obrigatório para auditar Têmis");
  }

  /** Aceita gate aprovado somente com justificativa e evidência comercial. */
  @Test
  void acceptsCompleteCommercialReview() throws Exception {
    CommercialBpmTaskConsumer.validate(
        json.readTree(
            "{\"decision\":\"APPROVED\",\"commercialRationale\":\"Jornada coerente\",\"evidence\":[\"Preço consistente\"],\"requiredChanges\":[]}"));
  }

  /** Exige todos os controles comerciais verdadeiros para aprovar a validação privada. */
  @Test
  void validatesStructuredPrivateCommercialReview() throws Exception {
    var result =
        json.readTree(
            """
            {
              "decision":"APPROVED",
              "commercialRationale":"As duas leituras sustentam a integridade comercial privada.",
              "evidence":["Evidência própria preservada"],
              "requiredChanges":[],
              "privateValidationChecks":{
                "sameProductAndVersion":true,
                "criteriaPredeclared":true,
                "twoDistinctParticipants":true,
                "fiveSignalsPassedTwice":true,
                "firstPartyEvents":true,
                "privateAndUnpublished":true,
                "paymentDisabled":true,
                "zeroMediaSpend":true,
                "privacyPreserved":true
              }
            }
            """);

    CommercialBpmTaskConsumer.validate(result, "pde-construction-approval");
    ((com.fasterxml.jackson.databind.node.ObjectNode) result.path("privateValidationChecks"))
        .put("paymentDisabled", false);
    assertThatThrownBy(
            () -> CommercialBpmTaskConsumer.validate(result, "pde-construction-approval"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("check reprovado");
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
        .contains(
            "PRODUCT_PROOF",
            "Só aplique limites da Meta",
            "contato direto consentido",
            "researchIntelligence",
            "cardId")
        .doesNotContain("demonstra inequivocamente o kit digital");
  }

  /** Mantém o polling de Têmis restrito ao gate comercial publicado no processo v7. */
  @Test
  void supportsPublishedCreativeProductionActivities() {
    org.assertj.core.api.Assertions.assertThat(
            CommercialBpmTaskConsumer.supportsContract("creative-production-approval", "route"))
        .isFalse();
    org.assertj.core.api.Assertions.assertThat(
            CommercialBpmTaskConsumer.supportsContract("creative-production-approval", "produce"))
        .isFalse();
    org.assertj.core.api.Assertions.assertThat(
            CommercialBpmTaskConsumer.supportsContract(
                "creative-production-approval", "commercial"))
        .isTrue();
  }

  /** Seleciona o contrato independente da validação privada do PDE. */
  @Test
  void selectsVersionedPdeDeliverablesContract() {
    org.assertj.core.api.Assertions.assertThat(
            CommercialBpmTaskConsumer.promptResourceFor("pde-construction-approval"))
        .isEqualTo("prompts/bpm/pde-private-validation-review-v2.md");
    org.assertj.core.api.Assertions.assertThat(
            CommercialBpmTaskConsumer.schemaResourceFor("pde-construction-approval"))
        .isEqualTo("prompts/bpm/pde-private-validation-review-v2-schema.json");
  }

  /** Usa somente o contexto privado da tarefa e não carrega entregáveis globais de outro PDE. */
  @Test
  void composesPrivateValidationPromptWithoutGlobalProductArtifacts() throws Exception {
    MetaAdApproverProperties properties = new MetaAdApproverProperties();
    CommercialBpmTaskConsumer consumer =
        new CommercialBpmTaskConsumer(
            properties,
            "codex",
            "gpt-5.6-sol",
            "/workspace-inexistente",
            "/workspace-inexistente",
            json);
    Map<String, Object> task =
        Map.of(
            "taskId",
            402L,
            "processCode",
            "pde-construction-approval",
            "activityId",
            "commercialIntegrityReview",
            "sourceReference",
            "product:19@private-validation-v1",
            "taskTarget",
            Map.of(
                "productId",
                19L,
                "productSlug",
                "pde-planejado-301",
                "experienceVersion",
                "private-validation-v1"),
            "processContext",
            Map.of(
                "completedHumanActivities",
                java.util.List.of(
                    Map.of(
                        "activityId",
                        "privateReading2",
                        "participantReference",
                        "PV-1A2B3C4D5E6F"))));

    String prompt = consumer.prompt(task);

    org.assertj.core.api.Assertions.assertThat(prompt)
        .contains(
            "PV-1A2B3C4D5E6F",
            "critérios precisam ter sido declarados antes do uso",
            "não são venda ou receita")
        .doesNotContain(
            "versionedArtifactEvidence", "Kit Manual de Atendimento", "15 respostas", "Rigel");
  }

  /** Impede que Têmis consuma novamente a atividade histórica de autoria da comunicação. */
  @Test
  void rejectsRetiredPdeCommunicationContract() throws Exception {
    org.assertj.core.api.Assertions.assertThat(
            CommercialBpmTaskConsumer.supportsContract(
                "pde-communication-sales-journey", "contract"))
        .isFalse();
    org.assertj.core.api.Assertions.assertThat(
            read("prompts/bpm/pde-communication-translation-v2.md"))
        .contains("HISTÓRICO DESATIVADO", "nenhum processo novo pode carregar este prompt");
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
            "taskTarget",
            "UPDATED_CANDIDATE",
            "não autoriza `RUNNING`",
            "canal efetivo proposto",
            "QA excluído",
            "Nunca use escala de 0 a 10",
            "nota mínima de 80",
            "ATTESTED_REFERENCE",
            "Não tente reler por shell");
    org.assertj.core.api.Assertions.assertThat(schema)
        .contains("activationRecommendation", "gateChecks", "priceClarityScore");
  }

  /** Mantém o prompt real dentro do limite e comprova a atestação incremental vigente. */
  @Test
  void composesBoundedCommercialPromptFromReadOnlyEvidenceWorkspace() throws Exception {
    Path moduleDirectory = Path.of("").toAbsolutePath().normalize();
    Path repository =
        moduleDirectory.getFileName().toString().equals("meta-ad-approver-worker")
            ? moduleDirectory.getParent()
            : moduleDirectory;
    MetaAdApproverProperties properties = new MetaAdApproverProperties();
    CommercialBpmTaskConsumer consumer =
        new CommercialBpmTaskConsumer(
            properties, "codex", "gpt-5.6-sol", repository.toString(), repository.toString(), json);
    Map<String, Object> task =
        Map.of(
            "taskId",
            275L,
            "processCode",
            "pde-commercial-homologation-activation",
            "activityId",
            "commercialIntegrityReview",
            "taskTarget",
            Map.of(
                "experimentId",
                89L,
                "productId",
                9L,
                "productSlug",
                "kit-whatsapp-pronto",
                "experienceVersion",
                "kit-whatsapp-pronto-pde-v2"));

    String prompt = consumer.prompt(task);

    org.assertj.core.api.Assertions.assertThat(prompt)
        .contains(
            "kit-whatsapp-tasting-homologation-v5.json",
            "kit-whatsapp-tasting-homologation-v4.json",
            "ATTESTED_REFERENCE",
            "reviewSummary",
            "pde-platform/backend/src/main/java/com/marketinghub/pde/service/RigelCommercialContractPolicy.java")
        .doesNotContain("VERSIONED_FILESYSTEM");
    org.assertj.core.api.Assertions.assertThat(prompt.length())
        .isLessThan(850_000)
        .isLessThan(CommercialBpmTaskConsumer.promptCharacterLimit());
  }

  /** Mantém o prompt real da Vega abaixo do teto e comprova manifesto vigente e baseline. */
  @Test
  void composesBoundedVegaCommercialPromptFromReadOnlyEvidenceWorkspace() throws Exception {
    Path moduleDirectory = Path.of("").toAbsolutePath().normalize();
    Path repository =
        moduleDirectory.getFileName().toString().equals("meta-ad-approver-worker")
            ? moduleDirectory.getParent()
            : moduleDirectory;
    MetaAdApproverProperties properties = new MetaAdApproverProperties();
    CommercialBpmTaskConsumer consumer =
        new CommercialBpmTaskConsumer(
            properties, "codex", "gpt-5.6-sol", repository.toString(), repository.toString(), json);
    Map<String, Object> task =
        Map.of(
            "taskId",
            257L,
            "processCode",
            "pde-commercial-homologation-activation",
            "activityId",
            "commercialIntegrityReview",
            "taskTarget",
            Map.of(
                "experimentId",
                90L,
                "productId",
                4L,
                "productSlug",
                "metodo-musa-7-dias",
                "experienceVersion",
                "musa-pde-entry-v7-espelho-antes-de-sair"));

    String prompt = consumer.prompt(task);

    org.assertj.core.api.Assertions.assertThat(prompt)
        .contains(
            "musa-v7-commercial-homologation-v5.json",
            "musa-v7-commercial-homologation-v4.json",
            "ATTESTED_REFERENCE",
            "reviewSummary",
            "https://go.pepper.com.br/owm6x");
    org.assertj.core.api.Assertions.assertThat(prompt.length())
        .isLessThan(850_000)
        .isLessThan(CommercialBpmTaskConsumer.promptCharacterLimit());
  }

  /** Rejeita localmente uma entrada sem margem antes de abrir processo ou consumir modelo. */
  @Test
  void rejectsPromptAbovePreventiveCharacterLimit() {
    assertThatThrownBy(
            () ->
                CommercialBpmTaskConsumer.validatePromptSize(
                    "x".repeat(CommercialBpmTaskConsumer.promptCharacterLimit() + 1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("limite preventivo");
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

  /** Repete inatividade real com margem de inicialização e conclui sem processo órfão. */
  @Test
  void retriesInactiveModelAttemptAndCompletesSecondAttempt() throws Exception {
    Path counter = Files.createTempFile("temis-attempt-counter-", ".txt");
    Files.writeString(counter, "0");
    Path executable = Files.createTempFile("temis-fake-codex-", ".sh");
    Files.writeString(
        executable,
        """
        #!/bin/sh
        output=""
        previous=""
        for argument in "$@"; do
          if [ "$previous" = "--output-last-message" ]; then output="$argument"; fi
          previous="$argument"
        done
        cat >/dev/null
        attempt=$(($(cat '%s') + 1))
        printf '%%s' "$attempt" > '%s'
        if [ "$attempt" -eq 1 ]; then
          printf '%%s\n' '{"type":"turn.completed","usage":{"input_tokens":10,"cached_input_tokens":1,"output_tokens":2}}'
          sleep 30
        fi
        printf '%%s' '{"decision":"APPROVED","commercialRationale":"Contrato coerente","evidence":["Prova íntegra"],"requiredChanges":[]}' > "$output"
        printf '%%s\n' '{"type":"turn.completed","usage":{"input_tokens":20,"cached_input_tokens":2,"output_tokens":3}}'
        """
            .formatted(counter, counter));
    executable.toFile().setExecutable(true);
    MetaAdApproverProperties properties = new MetaAdApproverProperties();
    CodexProcessSupervisor supervisor =
        new CodexProcessSupervisor(
            java.time.Duration.ofSeconds(1),
            java.time.Duration.ofSeconds(5),
            java.time.Duration.ofMillis(20));
    CommercialBpmTaskConsumer consumer =
        new CommercialBpmTaskConsumer(
            properties,
            executable.toString(),
            "gpt-5.6-sol",
            "/workspace",
            "",
            json,
            supervisor,
            2);

    CommercialBpmTaskConsumer.BpmExecution execution =
        consumer.execute(
            Map.of(
                "taskId",
                273L,
                "processCode",
                "landing-page-generation",
                "activityId",
                "commercial",
                "taskTarget",
                Map.of("productId", 9L)));

    org.assertj.core.api.Assertions.assertThat(execution.result().path("decision").asText())
        .isEqualTo("APPROVED");
    org.assertj.core.api.Assertions.assertThat(execution.usage().inputTokens()).isEqualTo(30L);
    org.assertj.core.api.Assertions.assertThat(execution.usage().cachedInputTokens()).isEqualTo(3L);
    org.assertj.core.api.Assertions.assertThat(execution.usage().outputTokens()).isEqualTo(5L);
    org.assertj.core.api.Assertions.assertThat(Files.readString(counter)).isEqualTo("2");
    Files.deleteIfExists(executable);
    Files.deleteIfExists(counter);
  }

  /** Preserva contexto e ausência de efeitos externos inclusive quando Têmis bloqueia. */
  @Test
  void buildsGovernedFailureEvidence() {
    Map<String, Object> evidence =
        CommercialBpmTaskConsumer.evidenceFields(
            "Têmis",
            "gpt-5.6-sol",
            Map.of(
                "sourceReference",
                "experiment:88",
                "activityId",
                "commercial",
                "taskTarget",
                Map.of("productId", 9L, "productSlug", "produto-a")));

    org.assertj.core.api.Assertions.assertThat(evidence)
        .containsEntry("sourceReference", "experiment:88")
        .containsEntry("activityId", "commercial")
        .containsEntry("accessMode", "READ_ONLY")
        .containsEntry("externalSideEffects", false)
        .containsEntry("requestedServiceTier", "FLEX")
        .containsEntry("effectiveServiceTier", "STANDARD")
        .containsEntry("taskTarget", Map.of("productId", 9L, "productSlug", "produto-a"))
        .containsKey("serviceTierException");
  }
}

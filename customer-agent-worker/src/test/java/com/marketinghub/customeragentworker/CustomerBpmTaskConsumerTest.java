package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o contrato funcional da revisão BPM de Psique. */
class CustomerBpmTaskConsumerTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Bloqueia Psique antes de reservar tarefas quando o raciocínio não está configurado. */
  @Test
  void rejectsMissingReasoningEffortBeforeModel() {
    assertThatThrownBy(
            () ->
                new CustomerBpmTaskConsumer(
                    "http://backend:8000", "codex", "gpt-5.6-sol", " ", "/workspace", "", json))
        .hasMessageContaining("obrigatório para auditar Psique");
  }

  /** Aceita parecer aprovado somente quando há perspectiva e evidência verificável. */
  @Test
  void acceptsCompleteCustomerReview() throws Exception {
    var result =
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
                  "evidenceBoundary":"Somente screenshot fornecido",
                  "visualComposition":{
                    "applicable":true,"archetype":"PERSUASIVE_LANDING",
                    "imageTextBalance":{"score":4,"evidence":"Texto e imagem possuem funções complementares"},
                    "mediaVariety":{"score":4,"evidence":"Demonstração e prova cumprem funções distintas"},
                    "visualRhythm":{"score":4,"evidence":"Densidade e respiro alternam ao longo da página"},
                    "colorStrategy":{"score":4,"evidence":"Cor orienta a atenção para conteúdo e ação"},
                    "typographicHierarchy":{"score":4,"evidence":"Título, apoio, corpo e ação são distinguíveis"},
                    "densityAndBreathingRoom":{"score":4,"evidence":"Os blocos permanecem escaneáveis no mobile"},
                    "noveltyFamiliarity":{"score":4,"evidence":"Padrões familiares sustentam novidade segura"},
                    "humanConnection":{"peopleObserved":false,"functionalRole":"NONE","appropriatenessScore":4,"absenceImpact":"LOW","evidence":"A demonstração do produto comunica melhor que uma foto genérica"},
                    "strongestPattern":"A hierarquia conduz a pessoa da promessa até a ação",
                    "criticalDeficitPresent":false,
                    "criticalDeficit":"Nenhum déficit visual crítico foi observado"
                  }
                }
              },
              "purchaseEmotion":{
                "acquisitionExpectation":"Espero ganhar clareza prática para atender melhor",
                "acquisitionAnxiety":"Receio comprar e receber algo genérico ou trabalhoso",
                "expectedPostDeliveryFeeling":"Imagino sentir alívio e controle depois de aplicar",
                "emotionalTension":"Desejo o resultado, mas temo perder tempo e dinheiro",
                "evidenceBoundary":"Reação simulada pela persona e pelo snapshot, não cliente real"
              },
              "privateExperienceChecks":{
                "sameProductAndVersion":true,
                "twoDistinctParticipants":true,
                "fiveSignalsPassedTwice":true,
                "firstPartyEvents":true,
                "lowEffortReadyResult":true,
                "desktopAndMobileUsable":true,
                "consentAndPrivacyPreserved":true,
                "noMaterialHarm":true
              },
              "evidence":["CTA visível"],
              "requiredChanges":[]
            }
            """);

    CustomerBpmTaskConsumer.validate(result);
    CustomerBpmTaskConsumer.validate(result, "pde-construction-approval");
    ((com.fasterxml.jackson.databind.node.ObjectNode) result.path("privateExperienceChecks"))
        .put("lowEffortReadyResult", false);
    assertThatThrownBy(() -> CustomerBpmTaskConsumer.validate(result, "pde-construction-approval"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("check reprovado");
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
            """
            {
              "decision":"APPROVED","customerPerspective":"Oferta clara e utilizável",
              "behavioralResponse":{
                "firstImpulse":"Curiosidade segura","belongingAdmirationLove":"Desejo sem pressão",
                "sensoryExperience":{
                  "evidenceAvailable":false,"availableModalities":[],"pleasureByModality":[],
                  "processingFluency":0,"sensoryCongruence":0,"overloadRisk":0,
                  "embodiedAnticipation":"Não observável","dominantCue":"Não observado",
                  "evidenceBoundary":"Sem evidência sensorial",
                  "visualComposition":{
                    "applicable":false,"archetype":"NOT_APPLICABLE",
                    "imageTextBalance":{"score":0,"evidence":"Composição visual não está disponível"},
                    "mediaVariety":{"score":0,"evidence":"Composição visual não está disponível"},
                    "visualRhythm":{"score":0,"evidence":"Composição visual não está disponível"},
                    "colorStrategy":{"score":0,"evidence":"Composição visual não está disponível"},
                    "typographicHierarchy":{"score":0,"evidence":"Composição visual não está disponível"},
                    "densityAndBreathingRoom":{"score":0,"evidence":"Composição visual não está disponível"},
                    "noveltyFamiliarity":{"score":0,"evidence":"Composição visual não está disponível"},
                    "humanConnection":{"peopleObserved":false,"functionalRole":"NONE","appropriatenessScore":0,"absenceImpact":"NONE","evidence":"Conexão humana visual não está disponível"},
                    "strongestPattern":"Composição visual não aplicável à evidência recebida",
                    "criticalDeficitPresent":false,
                    "criticalDeficit":"Composição visual não aplicável à evidência recebida"
                  }
                }
              },
              "purchaseEmotion":{"acquisitionExpectation":"Espero obter o resultado prometido","acquisitionAnxiety":"Receio perder dinheiro e tempo","expectedPostDeliveryFeeling":"Imagino sentir alívio após aplicar","emotionalTension":"Desejo versus receio da compra","evidenceBoundary":"Simulação baseada na persona"},
              "gateChecks":[{"status":"ADJUST"}],"evidence":["Jornada comprovada"],"requiredChanges":[]
            }
            """);

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

  /** Rejeita parecer que omite expectativa, ansiedade ou sentimento imaginado pós-entrega. */
  @Test
  void rejectsReviewWithoutCompletePurchaseEmotion() throws Exception {
    var result =
        json.readTree(
            """
            {
              "decision":"ADJUST",
              "customerPerspective":"Oferta clara, mas ainda sem antecipação emocional",
              "behavioralResponse":{
                "firstImpulse":"Curiosidade",
                "belongingAdmirationLove":"Valor relacional sem pressão",
                "sensoryExperience":{
                  "evidenceAvailable":false,"availableModalities":[],"pleasureByModality":[],
                  "processingFluency":0,"sensoryCongruence":0,"overloadRisk":0,
                  "embodiedAnticipation":"Indisponível","dominantCue":"Indisponível",
                  "evidenceBoundary":"Sem pixels"
                }
              },
              "purchaseEmotion":{"acquisitionExpectation":"Espero clareza"},
              "evidence":["Contexto comercial"],
              "requiredChanges":["Completar emoção"]
            }
            """);

    assertThatThrownBy(() -> CustomerBpmTaskConsumer.validate(result))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("evidências suficientes");
  }

  /** Exige prompt e schema próprios para a percepção do criativo. */
  @Test
  void selectsVersionedCreativeContract() throws Exception {
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.promptResourceFor("creative-production-approval"))
        .isEqualTo("prompts/bpm/v3/creative-customer-review.md");
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.schemaResourceFor("creative-production-approval"))
        .isEqualTo("prompts/bpm/v3/creative-customer-review-schema.json");
    String prompt =
        Files.readString(Path.of("src/main/resources/prompts/bpm/v3/creative-customer-review.md"));
    org.assertj.core.api.Assertions.assertThat(prompt)
        .contains(
            "formato e canal declarados",
            "PRODUCT_PROOF",
            "dois primeiros segundos",
            "purchaseEmotion",
            "researchIntelligence",
            "cardId")
        .doesNotContain("nail designer", "posts e stories prontos");
  }

  /** Seleciona o contrato de revisão integral da experiência do PDE. */
  @Test
  void selectsVersionedPdeExperienceContract() {
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.promptResourceFor("pde-construction-approval"))
        .isEqualTo("prompts/bpm/v4/pde-private-validation-experience-review.md");
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.schemaResourceFor("pde-construction-approval"))
        .isEqualTo("prompts/bpm/v4/pde-private-validation-experience-review-schema.json");
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.supportsContract(
                "pde-construction-approval", "humanExperienceReview"))
        .isTrue();
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.supportsContract("pde-construction-approval", "review"))
        .isFalse();
  }

  /** Usa somente o alvo e as leituras da tarefa privada, sem herdar provas de outro PDE. */
  @Test
  void composesPrivateValidationPromptWithoutGlobalProductEvidence() throws Exception {
    CustomerBpmTaskConsumer consumer =
        new CustomerBpmTaskConsumer(
            "http://backend:8000",
            "codex",
            "gpt-5.6-sol",
            "high",
            "/workspace-inexistente",
            "/workspace-inexistente",
            json);
    Map<String, Object> task =
        Map.of(
            "taskId",
            401L,
            "processCode",
            "pde-construction-approval",
            "activityId",
            "humanExperienceReview",
            "sourceReference",
            "product:19@private-validation-v1",
            "taskTarget",
            Map.of(
                "productId",
                19L,
                "productSlug",
                "pde-planejado-301",
                "experienceVersion",
                "private-validation-v1",
                "publicUrl",
                "https://private.local/prototype"),
            "processContext",
            Map.of(
                "completedHumanActivities",
                List.of(
                    Map.of(
                        "activityId",
                        "privateReading1",
                        "participantReference",
                        "PV-A1B2C3D4E5F6"))));

    String prompt = consumer.prompt(task, List.of());

    org.assertj.core.api.Assertions.assertThat(prompt)
        .contains(
            "PV-A1B2C3D4E5F6",
            "duas leituras humanas persistidas",
            "checkout de teste ou parecer de agente não são venda")
        .doesNotContain("versionedExperienceEvidence", "Kit Manual de Atendimento", "Rigel");
  }

  /** Seleciona o gate específico da cliente para homologação comercial do PDE. */
  @Test
  void selectsVersionedPdeCommercialHomologationContract() throws Exception {
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.promptResourceFor("pde-commercial-homologation-activation"))
        .isEqualTo("prompts/bpm/v3/pde-commercial-homologation-customer-review.md");
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.schemaResourceFor("pde-commercial-homologation-activation"))
        .isEqualTo("prompts/bpm/v3/pde-commercial-homologation-customer-review-schema.json");
    String prompt =
        Files.readString(
            Path.of(
                "src/main/resources/prompts/bpm/v3/pde-commercial-homologation-customer-review.md"));
    org.assertj.core.api.Assertions.assertThat(prompt)
        .contains(
            "taskTarget",
            "UPDATED_CANDIDATE",
            "fronteira externa esperada",
            "Use `ADJUST` somente para defeito corrigível na candidata local",
            "todos os itens de `gateChecks` em `PASS`",
            "ATTESTED_REFERENCE",
            "Não tente reler por shell",
            "visualEvidence",
            "todas as capturas `FOLD`",
            "purchaseEmotion");
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.supportsContract(
                "pde-commercial-homologation-activation", "humanExperienceReview"))
        .isTrue();
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.supportsContract(
                "pde-commercial-homologation-activation", "pdeGate"))
        .isFalse();
  }

  /** Mantém o prompt real da Vega com margem e comprova manifesto vigente e baseline. */
  @Test
  void composesBoundedVegaCommercialPromptFromReadOnlyEvidenceWorkspace() throws Exception {
    Path moduleDirectory = Path.of("").toAbsolutePath().normalize();
    Path repository =
        moduleDirectory.getFileName().toString().equals("customer-agent-worker")
            ? moduleDirectory.getParent()
            : moduleDirectory;
    CustomerBpmTaskConsumer consumer =
        new CustomerBpmTaskConsumer(
            "http://backend:8000",
            "codex",
            "gpt-5.6-sol",
            "high",
            repository.toString(),
            repository.toString(),
            json);
    Map<String, Object> task =
        Map.of(
            "taskId",
            299L,
            "sourceReference",
            "experiment:90",
            "processCode",
            "pde-commercial-homologation-activation",
            "activityId",
            "humanExperienceReview",
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

    String prompt = consumer.prompt(task, List.of());

    org.assertj.core.api.Assertions.assertThat(prompt)
        .contains(
            "musa-v7-commercial-homologation-v4.json",
            "musa-v7-commercial-homologation-v3.json",
            "ATTESTED_REFERENCE",
            "reviewSummary",
            "pde-platform/backend/src/main/resources/contracts/musa-v7-product-v1.json",
            "https://go.pepper.com.br/owm6x");
    org.assertj.core.api.Assertions.assertThat(prompt.length())
        .isLessThan(850_000)
        .isLessThan(CustomerBpmTaskConsumer.promptCharacterLimit());
  }

  /** Rejeita localmente uma entrada sem margem antes de abrir processo ou consumir modelo. */
  @Test
  void rejectsPromptAbovePreventiveCharacterLimit() {
    assertThatThrownBy(
            () ->
                CustomerBpmTaskConsumer.validatePromptSize(
                    "x".repeat(CustomerBpmTaskConsumer.promptCharacterLimit() + 1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("limite preventivo");
  }

  /** Mantém Psique no escopo da landing sem antecipar o preflight do subprocesso seguinte. */
  @Test
  void acceptsCanonicalCheckoutBindingAsLandingEvidence() throws Exception {
    String prompt =
        Files.readString(Path.of("src/main/resources/prompts/bpm/v3/landing-customer-review.md"));
    String normalizedPrompt = prompt.replaceAll("\\s+", " ");

    org.assertj.core.api.Assertions.assertThat(normalizedPrompt)
        .contains(
            "VALIDATED_FROM_PERSISTED_CANONICAL_BINDING",
            "EVIDENCE_TRANSPORT",
            "Não peça reconstrução da landing",
            "Não bloqueie apenas porque a tela do provedor externo não pôde ser aberta",
            "Integração de canal, checkout, acesso e eventos",
            "approvedCreativeEvidence.status",
            "adCopy` ou `adImageBriefing` legados",
            "`localPath` informados",
            "visualAudit",
            "purchaseEmotion");
    String schema =
        Files.readString(
            Path.of("src/main/resources/prompts/bpm/v3/landing-customer-review-schema.json"));
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

  /** Anexa cada captura ao turno sem remover a sandbox somente leitura do agente. */
  @Test
  void attachesEveryVisualEvidenceToCodexCommand() throws Exception {
    Path fullPage = Files.createTempFile("psique-full-page-", ".png");
    Path fold = Files.createTempFile("psique-fold-", ".png");
    try {
      CustomerBpmTaskConsumer consumer =
          new CustomerBpmTaskConsumer(
              "http://backend:8000", "codex", "gpt-5.6-sol", "high", "/workspace", "", json);

      List<String> command =
          consumer.command(
              Path.of("/tmp/result.json"),
              Path.of("/tmp/schema.json"),
              List.of(
                  visualEvidenceAt(901L, "FULL_PAGE", null, fullPage),
                  visualEvidenceAt(902L, "FOLD", 1, fold)));

      org.assertj.core.api.Assertions.assertThat(command)
          .contains("--sandbox", "read-only")
          .containsSequence("--image", fullPage.toAbsolutePath().toString())
          .containsSequence("--image", fold.toAbsolutePath().toString());
      org.assertj.core.api.Assertions.assertThat(command).filteredOn("--image"::equals).hasSize(2);
    } finally {
      Files.deleteIfExists(fullPage);
      Files.deleteIfExists(fold);
    }
  }

  /** Impede iniciar Psique quando um anexo persistido já não está disponível localmente. */
  @Test
  void rejectsMissingVisualAttachmentBeforeCodexStarts() throws Exception {
    Path directory = Files.createTempDirectory("psique-missing-attachment-");
    Path missing = directory.resolve("missing.png");
    try {
      CustomerBpmTaskConsumer consumer =
          new CustomerBpmTaskConsumer(
              "http://backend:8000", "codex", "gpt-5.6-sol", "high", "/workspace", "", json);

      assertThatThrownBy(
              () ->
                  consumer.command(
                      Path.of("/tmp/result.json"),
                      Path.of("/tmp/schema.json"),
                      List.of(visualEvidenceAt(901L, "FOLD", 1, missing))))
          .isInstanceOf(BpmVisualEvidenceRunner.VisualEvidenceException.class)
          .hasMessageContaining("não foi encontrado");
    } finally {
      Files.deleteIfExists(directory);
    }
  }

  /** Exige que prompts visuais usem os anexos sem depender de bubblewrap no container. */
  @Test
  void instructsModelToInspectAttachedPixelsWithoutFilesystemTool() throws Exception {
    String core =
        Files.readString(Path.of("src/main/resources/prompts/psique/behavioral-core-v4.md"));
    String prompt =
        Files.readString(
            Path.of(
                "src/main/resources/prompts/bpm/v3/pde-commercial-homologation-customer-review.md"));

    org.assertj.core.api.Assertions.assertThat(core.replaceAll("\\s+", " "))
        .contains("imagem anexada diretamente a este turno", "não tente reabrir o arquivo");
    org.assertj.core.api.Assertions.assertThat(prompt)
        .contains("anexada diretamente a este", "sem tentar reabrir o", "filesystem");
  }

  /** Exige o núcleo afetivo, social, sensorial e estético em todos os contratos BPM atuais. */
  @Test
  void requiresSharedBehavioralCoreInEveryBpmReview() throws Exception {
    String core =
        Files.readString(Path.of("src/main/resources/prompts/psique/behavioral-core-v4.md"));
    String creative =
        Files.readString(
            Path.of("src/main/resources/prompts/bpm/v3/creative-customer-review-schema.json"));
    String landing =
        Files.readString(
            Path.of("src/main/resources/prompts/bpm/v3/landing-customer-review-schema.json"));
    String pde =
        Files.readString(
            Path.of("src/main/resources/prompts/bpm/v3/pde-experience-review-schema.json"));
    String commercialHomologation =
        Files.readString(
            Path.of(
                "src/main/resources/prompts/bpm/v3/pde-commercial-homologation-customer-review-schema.json"));

    org.assertj.core.api.Assertions.assertThat(core.replaceAll("\\s+", " "))
        .contains("reação afetiva rápida")
        .contains("faixa de novidade segura")
        .contains("amada")
        .contains("prazer sensorial")
        .contains("Não recomende explorar vergonha")
        .contains("expectativa ao considerar adquirir")
        .contains("todas as dobras numeradas")
        .contains("Trate pessoas e rostos como pistas sociais fortes")
        .contains("Paleta contida pode ser excelente");
    org.assertj.core.api.Assertions.assertThat(
            java.util.List.of(creative, landing, pde, commercialHomologation))
        .allSatisfy(
            schema ->
                org.assertj.core.api.Assertions.assertThat(schema)
                    .contains(
                        "behavioralResponse",
                        "belongingAdmirationLove",
                        "sensoryExperience",
                        "visualComposition",
                        "humanConnection",
                        "purchaseEmotion"));
  }

  /** Exige captura por dobra nos processos com tela e preserva criativo no contrato próprio. */
  @Test
  void requiresVisualEvidenceOnlyForScreenJourneys() {
    org.assertj.core.api.Assertions.assertThat(
            java.util.List.of(
                "landing-page-generation",
                "pde-commercial-homologation-activation",
                "pde-construction-approval"))
        .allSatisfy(
            processCode ->
                org.assertj.core.api.Assertions.assertThat(
                        CustomerBpmTaskConsumer.requiresVisualAudit(processCode))
                    .isTrue());
    org.assertj.core.api.Assertions.assertThat(
            CustomerBpmTaskConsumer.requiresVisualAudit("creative-production-approval"))
        .isFalse();
  }

  /** Aceita somente quando full-page e cada dobra persistida aparecem uma vez na análise. */
  @Test
  void validatesExactVisualCoverageByArtifactId() throws Exception {
    var visualEvidence =
        java.util.List.of(
            visualEvidence(901L, "FULL_PAGE", null),
            visualEvidence(902L, "FOLD", 1),
            visualEvidence(903L, "FOLD", 2));
    var result =
        json.readTree(
            """
            {
              "visualAudit":{
                "captureSessionId":"capture-abc",
                "mobileFirst":true,
                "fullPageEvidenceIds":[901],
                "fullPageContinuity":"A página mantém narrativa contínua entre as dobras.",
                "overallAestheticAssessment":"A composição é consistente e adequada à persona.",
                "foldAnalyses":[
                  {"artifactId":902,"deviceProfile":"IPHONE_15_PRO","pageNumber":1,"foldNumber":1,"aestheticAssessment":"Abertura limpa","visualHierarchy":"Título domina","legibility":"Texto legível","emotionEvoked":"Curiosidade segura","ctaVisibility":"CTA principal visível"},
                  {"artifactId":903,"deviceProfile":"IPHONE_15_PRO","pageNumber":1,"foldNumber":2,"aestheticAssessment":"Prova equilibrada","visualHierarchy":"Benefício antes dos detalhes","legibility":"Contraste adequado","emotionEvoked":"Confiança crescente","ctaVisibility":"CTA de continuidade visível"}
                ]
              }
            }
            """);

    org.assertj.core.api.Assertions.assertThatCode(
            () -> CustomerBpmTaskConsumer.validateVisualAudit(result, visualEvidence))
        .doesNotThrowAnyException();
  }

  /** Bloqueia sucesso quando Psique deixa uma dobra persistida sem análise estética. */
  @Test
  void rejectsVisualAuditThatOmitsPersistedFold() throws Exception {
    var visualEvidence =
        java.util.List.of(
            visualEvidence(901L, "FULL_PAGE", null),
            visualEvidence(902L, "FOLD", 1),
            visualEvidence(903L, "FOLD", 2));
    var result =
        json.readTree(
            """
            {
              "visualAudit":{
                "captureSessionId":"capture-abc","mobileFirst":true,
                "fullPageEvidenceIds":[901],
                "fullPageContinuity":"Jornada contínua entre as dobras.",
                "overallAestheticAssessment":"Estética coerente com a oferta.",
                "foldAnalyses":[
                  {"artifactId":902,"deviceProfile":"IPHONE_15_PRO","pageNumber":1,"foldNumber":1,"aestheticAssessment":"Limpa","visualHierarchy":"Clara","legibility":"Boa","emotionEvoked":"Curiosidade","ctaVisibility":"Visível"}
                ]
              }
            }
            """);

    assertThatThrownBy(() -> CustomerBpmTaskConsumer.validateVisualAudit(result, visualEvidence))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("todas as dobras");
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

  /** Registra apenas URLs de navegação real e ignora endereços que estavam somente no prompt. */
  @Test
  void extractsOnlyActuallyAccessedUrlsFromCodexEvents() throws Exception {
    Path output = Files.createTempFile("psique-bpm-urls-", ".jsonl");
    Files.writeString(
        output,
        """
        {"type":"turn.started","prompt":{"publicUrl":"https://nao-acessada.example/produto"}}
        {"type":"item.started","item":{"id":"item_1","type":"web_search","query":"fonte","action":{"type":"open_page","url":"https://ainda-nao-confirmada.example/artigo"}}}
        {"type":"item.completed","item":{"id":"item_1","type":"web_search","query":"fonte","action":{"type":"open_page","url":"https://fonte.example/artigo"}}}
        {"type":"item.completed","item":{"id":"item_2","type":"web_search","query":"checkout","action":{"type":"find_in_page","url":"https://loja.example/checkout","pattern":"comprar"}}}
        {"type":"item.completed","item":{"id":"item_3","type":"command_execution","command":"echo https://inventada.example","aggregated_output":"{\\"requestedUrl\\":\\"https://nao-comprovada.example\\"}","status":"completed"}}
        """);

    var urls = CustomerBpmTaskConsumer.readAccessedUrls(json, output);

    org.assertj.core.api.Assertions.assertThat(urls)
        .extracting(value -> value.get("url"))
        .containsExactly("https://fonte.example/artigo", "https://loja.example/checkout");
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

  /** Monta uma evidência já persistida no backend para validar o parecer visual. */
  private BpmVisualEvidenceBackendClient.UploadedVisualEvidence visualEvidence(
      Long id, String evidenceType, Integer foldNumber) {
    return new BpmVisualEvidenceBackendClient.UploadedVisualEvidence(
        id,
        "capture-abc",
        evidenceType.toLowerCase() + "-" + id,
        evidenceType,
        foldNumber == null ? "Página 1 · visão completa" : "Página 1 · dobra " + foldNumber,
        "IPHONE_15_PRO",
        1,
        foldNumber,
        393,
        852,
        1704,
        foldNumber == null ? 0 : (foldNumber - 1) * 852,
        "https://rigel.example/jornada",
        "https://rigel.example/jornada",
        "/api/agent-tasks/258/visual-evidence/" + id + "/content",
        1200L,
        "a".repeat(64),
        java.time.Instant.parse("2026-08-29T10:00:00Z"),
        "/tmp/visual-" + id + ".png");
  }

  /** Monta um snapshot real em disco para validar os argumentos multimodais do comando. */
  private BpmVisualEvidenceBackendClient.UploadedVisualEvidence visualEvidenceAt(
      Long id, String evidenceType, Integer foldNumber, Path localPath) {
    return new BpmVisualEvidenceBackendClient.UploadedVisualEvidence(
        id,
        "capture-abc",
        evidenceType.toLowerCase() + "-" + id,
        evidenceType,
        foldNumber == null ? "Página 1 · visão completa" : "Página 1 · dobra " + foldNumber,
        "IPHONE_15_PRO",
        1,
        foldNumber,
        393,
        852,
        1704,
        foldNumber == null ? 0 : (foldNumber - 1) * 852,
        "https://rigel.example/jornada",
        "https://rigel.example/jornada",
        "/api/agent-tasks/265/visual-evidence/" + id + "/content",
        1200L,
        "a".repeat(64),
        Instant.parse("2026-08-29T10:00:00Z"),
        localPath.toString());
  }
}

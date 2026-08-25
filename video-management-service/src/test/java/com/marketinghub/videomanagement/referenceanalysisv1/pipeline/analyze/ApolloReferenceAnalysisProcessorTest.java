package com.marketinghub.videomanagement.referenceanalysisv1.pipeline.analyze;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.referenceanalysisv1.pipeline.ReferenceAnalysisStageContext;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Valida a receita, os gates de direitos e a responsabilidade de Apolo. */
class ApolloReferenceAnalysisProcessorTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ReferenceMediaInspector inspector;
    private ReferenceAnalysisAiClient aiClient;
    private ApolloReferenceAnalysisProcessor processor;

    /** Configura dependências simuladas sem chamar IA ou provider pago. */
    @BeforeEach
    void setUp() {
        VideoManagementProperties properties = new VideoManagementProperties();
        inspector = mock(ReferenceMediaInspector.class);
        aiClient = mock(ReferenceAnalysisAiClient.class);
        processor = new ApolloReferenceAnalysisProcessor(properties, objectMapper, inspector, aiClient);
    }

    /** Converte evidência e resposta válida em resultado importável e auditável. */
    @Test
    void shouldBuildImportableApolloRecipe() throws Exception {
        ReferenceAnalysisStageContext context = context();
        ObjectNode artifacts = artifacts();
        ReferenceMediaInspector.Evidence evidence =
                new ReferenceMediaInspector.Evidence(artifacts, List.of("data:image/jpeg;base64,AA=="));
        ObjectNode request = objectMapper.createObjectNode().put("service_tier", "flex");
        JsonNode response = response(output("EXTEND_APOLLO"));
        given(inspector.inspect(context)).willReturn(evidence);
        given(aiClient.analyze(context, evidence))
                .willReturn(new ReferenceAnalysisAiClient.AiInteraction(request, response));

        var result = processor.process(context);

        assertThat(result.decision()).isEqualTo("NEEDS_PROVIDER_HOMOLOGATION");
        assertThat(result.costUsd()).isNull();
        assertThat(result.summaryMarkdown()).contains("24 frames-chave", "EXTEND_APOLLO", "execução #").doesNotContain("NEW_AGENT");
        assertThat(result.output().path("productionBlueprint").path("scenePlan")).hasSize(4);
    }

    /** Bloqueia criação injustificada de outro agente para um estilo audiovisual. */
    @Test
    void shouldRejectNewAgentDecisionForStyleOnly() throws Exception {
        ReferenceAnalysisStageContext context = context();
        ReferenceMediaInspector.Evidence evidence =
                new ReferenceMediaInspector.Evidence(artifacts(), List.of("data:image/jpeg;base64,AA=="));
        given(inspector.inspect(context)).willReturn(evidence);
        given(aiClient.analyze(context, evidence)).willReturn(
                new ReferenceAnalysisAiClient.AiInteraction(objectMapper.createObjectNode(), response(output("NEW_AGENT"))));

        assertThatThrownBy(() -> processor.process(context))
                .isInstanceOf(ReferenceAnalysisFailureException.class)
                .hasRootCauseMessage("A referência não justificou um papel diferente da direção criativa de Apolo");
    }

    /** Preserva request, resposta de erro e artefatos quando a integração multimodal falha. */
    @Test
    void shouldPreserveAvailableAuditOnAiFailure() throws Exception {
        ReferenceAnalysisStageContext context = context();
        ObjectNode artifacts = artifacts();
        ReferenceMediaInspector.Evidence evidence =
                new ReferenceMediaInspector.Evidence(artifacts, List.of("data:image/jpeg;base64,AA=="));
        ObjectNode request = objectMapper.createObjectNode().put("service_tier", "flex");
        ObjectNode response = objectMapper.createObjectNode().put("status", 429);
        given(inspector.inspect(context)).willReturn(evidence);
        given(aiClient.analyze(context, evidence)).willThrow(
                new ReferenceAnalysisAiClient.AiFailure(
                        "limite externo", new IllegalStateException("429"), request, response));

        assertThatThrownBy(() -> processor.process(context))
                .isInstanceOfSatisfying(ReferenceAnalysisFailureException.class, failure -> {
                    assertThat(failure.artifacts()).isSameAs(artifacts);
                    assertThat(failure.rawRequest()).isSameAs(request);
                    assertThat(failure.rawResponse()).isSameAs(response);
                    assertThat(failure.model()).isEqualTo("gpt-5.6");
                });
    }

    /** Preserva request, resposta bruta e artefatos quando o modelo viola o contrato funcional. */
    @Test
    void shouldPreserveAuditWhenStructuredOutputIsInvalid() throws Exception {
        ReferenceAnalysisStageContext context = context();
        ObjectNode artifacts = artifacts();
        ReferenceMediaInspector.Evidence evidence =
                new ReferenceMediaInspector.Evidence(artifacts, List.of("data:image/jpeg;base64,AA=="));
        ObjectNode request = objectMapper.createObjectNode().put("service_tier", "flex");
        JsonNode rawResponse = objectMapper.readTree("""
                {"output":[{"content":[{"type":"output_text","text":"{json-invalido"}]}]}
                """);
        given(inspector.inspect(context)).willReturn(evidence);
        given(aiClient.analyze(context, evidence)).willReturn(
                new ReferenceAnalysisAiClient.AiInteraction(request, rawResponse));

        assertThatThrownBy(() -> processor.process(context))
                .isInstanceOfSatisfying(ReferenceAnalysisFailureException.class, failure -> {
                    assertThat(failure.artifacts()).isSameAs(artifacts);
                    assertThat(failure.rawRequest()).isSameAs(request);
                    assertThat(failure.rawResponse()).isSameAs(rawResponse);
                    assertThat(failure.model()).isEqualTo("gpt-5.6");
                });
    }

    /** Monta contexto imutável para os cenários de processor. */
    private ReferenceAnalysisStageContext context() throws Exception {
        return new ReferenceAnalysisStageContext(91L, 3L, "default", 1, "producer-91",
                objectMapper.readTree("{\"title\":\"Madonna\",\"sourceUrl\":\"https://example/video.mp4\"}"),
                Instant.now());
    }

    /** Monta evidência técnica já extraída pelo ffmpeg e ffprobe. */
    private ObjectNode artifacts() {
        ObjectNode value = objectMapper.createObjectNode();
        value.put("sha256", "abc");
        value.put("durationSeconds", 63.7);
        value.put("width", 576);
        value.put("height", 1024);
        value.put("videoCodec", "h264");
        value.put("audioCodec", "aac");
        value.put("sceneChangeCount", 10);
        value.put("sceneChangeThreshold", 0.22);
        value.put("integratedLoudnessLufs", -14.1);
        value.put("truePeakDbfs", -0.6);
        return value;
    }

    /** Monta response da API com usage e texto estruturado. */
    private JsonNode response(JsonNode output) throws Exception {
        return objectMapper.readTree("""
                {"usage":{"input_tokens":1000,"input_tokens_details":{"cached_tokens":200},"output_tokens":300},
                 "output":[{"content":[{"type":"output_text","text":%s}]}]}
                """.formatted(objectMapper.writeValueAsString(output.toString())));
    }

    /** Monta análise com sequência, direitos e receita suficientes para o gate. */
    private JsonNode output(String capability) throws Exception {
        return objectMapper.readTree("""
                {
                  "commercialDiagnosis":"A referência usa performance, alternância de planos e recompensa visual para sustentar atenção.",
                  "hook":"Abrir com uma ação visual original e reconhecível.",
                  "narrativePattern":"Gancho, progressão, recompensa, prova visual e encerramento.",
                  "visualDirection":"Luz original, personagem fictícia e alternância de planos.",
                  "continuityStrategy":"Bíblia visual fixa para personagem, cenário, figurino e paleta.",
                  "audioStrategy":"Voz e trilha originais ou licenciadas com mixagem mobile.",
                  "captionStrategy":"Legendas curtas sincronizadas e área segura vertical.",
                  "sequence":[
                    {"startSeconds":0,"endSeconds":5,"role":"HOOK","visualAction":"Ação um","audioFunction":"Ataque","editing":"Corte um"},
                    {"startSeconds":5,"endSeconds":20,"role":"SETUP","visualAction":"Ação dois","audioFunction":"Contexto","editing":"Corte dois"},
                    {"startSeconds":20,"endSeconds":50,"role":"PAYOFF","visualAction":"Ação três","audioFunction":"Recompensa","editing":"Corte três"},
                    {"startSeconds":50,"endSeconds":63,"role":"CTA","visualAction":"Ação quatro","audioFunction":"Fecho","editing":"Corte quatro"}
                  ],
                  "reusableLearnings":["Alternar plano geral e detalhe.","Manter recompensa visual progressiva.","Usar legenda sincronizada original."],
                  "salesApplications":{"campaign":"Criativo de atenção com produto original.","product":"Abertura premium de uma aula ou entrega.","organic":"Conteúdo narrativo seriado e autoral."},
                  "rightsRisks":["Não copiar artista, voz, música, letra ou gravação da referência."],
                  "productionBlueprint":{
                    "archetype":"Performance narrativa original","targetDurationSeconds":60,"format":"VERTICAL_9_16",
                    "hook":"Ação visual original","story":"Uma personagem fictícia progride por quatro atos até uma recompensa coerente.",
                    "scenePlan":["Cena um com localização, ação, câmera e gancho.","Cena dois com localização, ação, câmera e progressão.","Cena três com localização, ação, câmera e recompensa.","Cena quatro com localização, ação, câmera e encerramento."],
                    "characterBible":"Personagem fictícia com aparência e voz originais.","environmentBible":"Cenário autoral com mapa e luz consistentes.",
                    "objectBible":"Objetos originais sem marcas reconhecíveis.","visualStyleGuide":"Paleta autoral, contraste mobile e textura cinematográfica.",
                    "imageGenerationPlan":"Gerar frames mestres antes dos clipes.","continuityRules":"Preservar rosto, figurino, luz e objetos.",
                    "voiceoverPlan":"Voz original licenciada e dirigida.","soundtrackPlan":"Trilha original ou licenciada com prova.",
                    "captionPlan":"Legendas temporizadas na área segura.","providerPlan":"Homologar lip-sync ou performance antes de gerar.",
                    "editingNotes":"Alternar escalas e finalizar deterministicamente.","qualityGate":"Bloquear sem direitos, preço, continuidade, áudio e revisão humana.",
                    "estimatedGeneratedClips":8,"requiresLipSync":true,"requiresLicensedMusic":true,
                    "apolloCapability":"%s","capabilityGaps":["Homologar performance e lip-sync"]
                  },
                  "operationalDecision":"NEEDS_PROVIDER_HOMOLOGATION"
                }
                """.formatted(capability));
    }
}

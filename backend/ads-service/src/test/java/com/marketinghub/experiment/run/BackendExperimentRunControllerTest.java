package com.marketinghub.experiment.run;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.creative.label.Angle;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignObjective;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.run.service.QuartzoPreflightEvidenceScopeService;
import com.marketinghub.experiment.run.service.create.CreateExperimentRunRequest;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.OfferType;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.creative.label.AngleRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunGateResultRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunRepository;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.repository.jpa.mois.dossieproduto.PipelineDossieProdutoRepository;
import com.marketinghub.repository.jpa.mois.dossieproduto.entity.PipelineDossieProduto;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Valida contratos HTTP, persistência e recuperação de execuções operacionais de experimentos. */
@SpringBootTest(classes = AdsServiceApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:com.marketinghub.experiment.run.BackendExperimentRunControllerTest-${random.uuid};MODE=MySQL;DB_CLOSE_DELAY=0;DB_CLOSE_ON_EXIT=FALSE",
      "spring.datasource.driverClassName=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create",
      "spring.liquibase.enabled=false"
    })
class BackendExperimentRunControllerTest {
  @org.springframework.boot.test.mock.mockito.MockBean
  private QuartzoPreflightEvidenceScopeService quartzoEvidence;

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private ExperimentRepository experimentRepository;
  @Autowired private ExperimentRunRepository experimentRunRepository;
  @Autowired private ExperimentRunGateResultRepository gateResultRepository;
  @Autowired private MarketNicheRepository marketNicheRepository;
  @Autowired private HypothesisRepository hypothesisRepository;
  @Autowired private AngleRepository angleRepository;
  @Autowired private PipelineDossieProdutoRepository pipelineDossieProdutoRepository;

  /** Limpa dados persistidos para manter a ordem sequencial dos runs previsível. */
  @BeforeEach
  void cleanDb() {
    gateResultRepository.deleteAll();
    experimentRunRepository.deleteAll();
    experimentRepository.deleteAll();
    hypothesisRepository.deleteAll();
    angleRepository.deleteAll();
    marketNicheRepository.deleteAll();
    pipelineDossieProdutoRepository.deleteAll();
  }

  /** Concorrência renova uma tentativa pausada, mas preserva sem mutação a referência encerrada. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.EnumSource(
      value = ExperimentStatus.class,
      names = {"PAUSED", "INVALIDATED"})
  void serializesTechnicalRenewalAndPreservesHistoricApproval(ExperimentStatus state)
      throws Exception {
    Long experimentId = createExperiment();
    var configured = experimentRepository.findById(experimentId).orElseThrow();
    configured.setCampaignObjective(ExperimentCampaignObjective.SALES);
    configured.setUnitPrice(new BigDecimal("67"));
    configured.setSampleSize(100);
    configured.setTargetCvr(new BigDecimal("0.05"));
    experimentRepository.saveAndFlush(configured);
    createRelevantCommercialDossier();
    Long previousId = createRun(experimentId);
    mockMvc
        .perform(post("/api/experiment-runs/{id}/preflight", previousId))
        .andExpect(status().isOk());
    var previous = experimentRunRepository.findById(previousId).orElseThrow();
    previous.setStatus(ExperimentRunStatus.COMPLETED);
    previous.setDataQualityStatus(ExperimentRunDataQualityStatus.VALID);
    previous.setEvidenceValidity(ExperimentEvidenceValidity.COMMERCIALLY_VALID);
    experimentRunRepository.saveAndFlush(previous);
    var oldGates =
        gateResultRepository.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(previousId);
    oldGates.forEach(gate -> gate.setStatus(ExperimentRunGateStatus.PASS));
    gateResultRepository.saveAllAndFlush(oldGates);
    var experiment = experimentRepository.findById(experimentId).orElseThrow();
    experiment.setStatus(state);
    experimentRepository.saveAndFlush(experiment);
    org.mockito.Mockito.when(quartzoEvidence.applies(org.mockito.ArgumentMatchers.any()))
        .thenReturn(true);
    org.mockito.Mockito.when(quartzoEvidence.requiredReference(org.mockito.ArgumentMatchers.any()))
        .thenReturn("publication:93001;page-sha256:" + "a".repeat(64));
    org.mockito.Mockito.when(quartzoEvidence.hasCurrentEvidence(org.mockito.ArgumentMatchers.any()))
        .thenReturn(false);
    var barrier = new java.util.concurrent.CyclicBarrier(2);
    try (var threads = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var command =
          (java.util.concurrent.Callable<Long>)
              () -> {
                barrier.await(30, java.util.concurrent.TimeUnit.SECONDS);
                var performed =
                    mockMvc.perform(
                        post(
                            "/api/experiment-runs/{id}/technical-homologation-renewal",
                            previousId));
                if (state == ExperimentStatus.INVALIDATED) {
                  performed.andExpect(status().isConflict());
                  return previousId;
                }
                var response =
                    performed
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.hasBlockers").value(true))
                        .andExpect(jsonPath("$.canRenewTechnicalHomologation").value(false))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
                return objectMapper.readTree(response).path("runId").asLong();
              };
      var first = threads.submit(command);
      var second = threads.submit(command);
      Long newId = first.get(30, java.util.concurrent.TimeUnit.SECONDS);
      assertThat(second.get(30, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(newId);
      var all = experimentRunRepository.findByExperimentIdOrderByRunNumberAsc(experimentId);
      if (state == ExperimentStatus.INVALIDATED) {
        assertThat(all).hasSize(1);
        assertThat(all.getFirst().getStatus()).isEqualTo(ExperimentRunStatus.COMPLETED);
        assertThat(
                gateResultRepository.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(
                    previousId))
            .allMatch(gate -> gate.getStatus() == ExperimentRunGateStatus.PASS);
        return;
      }
      assertThat(newId).isNotEqualTo(previousId);
      assertThat(all).hasSize(2);
      assertThat(all.getLast().getCreatedBy())
          .isEqualTo("technical-homologation-renewal:" + previousId);
      assertThat(all.getLast().getStatus()).isEqualTo(ExperimentRunStatus.PREFLIGHT_PENDING);
      assertThat(experimentRepository.findById(experimentId).orElseThrow().getStatus())
          .isEqualTo(state);
      assertThat(experimentRunRepository.findById(previousId).orElseThrow().getStatus())
          .isEqualTo(ExperimentRunStatus.COMPLETED);
      assertThat(
              gateResultRepository.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(previousId))
          .allMatch(gate -> gate.getStatus() == ExperimentRunGateStatus.PASS);
      assertThat(gateResultRepository.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(newId))
          .anyMatch(gate -> gate.getStatus() == ExperimentRunGateStatus.PENDING);
      String output = System.getProperty("preflight.renewal.fixture-output");
      if (output != null) {
        String body =
            mockMvc
                .perform(get("/api/experiment-runs/{id}/preflight", newId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        java.nio.file.Files.writeString(java.nio.file.Path.of(output), body);
      }
    }
  }

  /** Tentativa sem evidência vencida não pode criar renovação ou preencher aprovação histórica. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.EnumSource(ExperimentRunMode.class)
  void rejectsRenewalWithoutBackendPermission(ExperimentRunMode mode) throws Exception {
    Long experimentId = createExperiment();
    String created =
        mockMvc
            .perform(
                post("/api/experiments/{id}/runs", experimentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateExperimentRunRequest(
                                mode, ExperimentRunStopPolicy.MANUAL_ONLY, "local-test"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    Long runId = objectMapper.readTree(created).path("id").asLong();
    mockMvc
        .perform(post("/api/experiment-runs/{id}/technical-homologation-renewal", runId))
        .andExpect(status().isConflict());
    assertThat(experimentRunRepository.findByExperimentIdOrderByRunNumberAsc(experimentId))
        .hasSize(1);
  }

  /** Deve criar runs sequenciais com validade inicial neutra e sem alterar o experimento legado. */
  @Test
  void createSequentialRuns() throws Exception {
    Long experimentId = createExperiment();
    CreateExperimentRunRequest request =
        new CreateExperimentRunRequest(
            ExperimentRunMode.TEST, ExperimentRunStopPolicy.MANUAL_ONLY, "codex");

    mockMvc
        .perform(
            post("/api/experiments/{experimentId}/runs", experimentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.experimentId").value(experimentId))
        .andExpect(jsonPath("$.runNumber").value(1))
        .andExpect(jsonPath("$.mode").value("TEST"))
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andExpect(jsonPath("$.evidenceValidity").value("NOT_EVALUATED"))
        .andExpect(jsonPath("$.dataQualityStatus").value("UNKNOWN"));

    mockMvc
        .perform(
            post("/api/experiments/{experimentId}/runs", experimentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.runNumber").value(2));
  }

  /** Deve listar e consultar runs já persistidos para alimentar o frontend. */
  @Test
  void listAndGetRuns() throws Exception {
    Long experimentId = createExperiment();
    String response =
        mockMvc
            .perform(
                post("/api/experiments/{experimentId}/runs", experimentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateExperimentRunRequest(
                                ExperimentRunMode.PRODUCTION,
                                ExperimentRunStopPolicy.FIRST_VALID_LEAD_STANDBY,
                                "operador"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Long runId = objectMapper.readTree(response).get("id").asLong();

    mockMvc
        .perform(get("/api/experiments/{experimentId}/runs", experimentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(runId))
        .andExpect(jsonPath("$[0].stopPolicy").value("FIRST_VALID_LEAD_STANDBY"));

    mockMvc
        .perform(get("/api/experiment-runs/{runId}", runId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(runId))
        .andExpect(jsonPath("$.experimentId").value(experimentId));
  }

  /** Deve manter o run pendente enquanto os quatro gates funcionais não tiverem evidências. */
  @Test
  void runPreflightWithoutBlockers() throws Exception {
    Long experimentId = createExperiment();
    createRelevantCommercialDossier();
    Long runId = createRun(experimentId);

    mockMvc
        .perform(post("/api/experiment-runs/{runId}/preflight", runId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.runStatus").value("PREFLIGHT_PENDING"))
        .andExpect(jsonPath("$.hasBlockers").value(true))
        .andExpect(
            jsonPath("$.gates[?(@.gateCode == 'MOIS_COMMERCIAL_DOSSIER_PREFLIGHT')].status")
                .value(hasItem("PASS")))
        .andExpect(
            jsonPath(
                    "$.gates[?(@.gateCode =="
                        + " 'MOIS_COMMERCIAL_DOSSIER_PREFLIGHT')].evidenceReference")
                .value(hasItem(org.hamcrest.Matchers.containsString("mois-dossiers:"))))
        .andExpect(
            jsonPath("$.gates[?(@.gateCode == 'PRIMARY_VARIABLE_DEFINED')].status")
                .value(hasItem("PASS")))
        .andExpect(
            jsonPath("$.gates[?(@.gateCode == 'FORM_CAN_BE_SUBMITTED')].status")
                .value(hasItem("PENDING")));

    mockMvc
        .perform(get("/api/experiment-runs/{runId}/preflight", runId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.runStatus").value("PREFLIGHT_PENDING"));
  }

  /** Deve liberar um run somente depois de persistir quatro evidencias funcionais aprovadas. */
  @Test
  void homologationEvidenceCompletesPendingPreflight() throws Exception {
    Long experimentId = createExperiment();
    createRelevantCommercialDossier();
    Long runId = createRun(experimentId);
    mockMvc
        .perform(post("/api/experiment-runs/{runId}/preflight", runId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.runStatus").value("PREFLIGHT_PENDING"));

    String evidence =
        """
        {
          "gates":[
            {"gateCode":"LANDING_QUALITY_REVIEW_APPROVED","status":"PASS","summary":"Desktop e mobile aprovados","evidenceReference":"e2e://landing/round-1"},
            {"gateCode":"FORM_CAN_BE_SUBMITTED","status":"PASS","summary":"Compra de teste e entrega concluidas","evidenceReference":"e2e://journey/round-1"},
            {"gateCode":"META_EFFECTIVE_STATUS_CONFIRMED","status":"PASS","summary":"Contrato Meta confirmado sem publicar campanha","evidenceReference":"e2e://meta/round-1"},
            {"gateCode":"DATA_FRESHNESS_VALID","status":"PASS","summary":"Eventos segregados e deduplicados","evidenceReference":"e2e://measurement/round-1"}
          ]
        }
        """;

    mockMvc
        .perform(
            post("/api/experiment-runs/{runId}/homologation-results", runId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(evidence))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.runStatus").value("READY_TO_PUBLISH"))
        .andExpect(jsonPath("$.hasBlockers").value(false))
        .andExpect(
            jsonPath("$.gates[?(@.gateCode == 'FORM_CAN_BE_SUBMITTED')].status")
                .value(hasItem("PASS")))
        .andExpect(
            jsonPath("$.gates[?(@.gateCode == 'FORM_CAN_BE_SUBMITTED')].evidenceReference")
                .value(hasItem("e2e://journey/round-1")));
  }

  /** Libera venda direta somente com checkout, entrega, canal consentido e métricas comprovados. */
  @Test
  void directSalesPreflightUsesCommercialJourneyAndDistributionGates() throws Exception {
    Long experimentId = createDirectSalesExperiment();
    createRelevantCommercialDossier();
    Long runId = createRun(experimentId);

    mockMvc
        .perform(post("/api/experiment-runs/{runId}/preflight", runId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.runStatus").value("PREFLIGHT_PENDING"))
        .andExpect(
            jsonPath("$.gates[?(@.gateCode == 'SALES_VALIDATION_TARGET_DEFINED')].status")
                .value(hasItem("PASS")))
        .andExpect(
            jsonPath("$.gates[?(@.gateCode == 'CHECKOUT_AND_DELIVERY_CAN_BE_COMPLETED')].status")
                .value(hasItem("PENDING")))
        .andExpect(
            jsonPath("$.gates[?(@.gateCode == 'DIRECT_CHANNEL_READINESS_CONFIRMED')].status")
                .value(hasItem("PENDING")))
        .andExpect(jsonPath("$.gates[?(@.gateCode == 'KPI_TARGET_CPL_VALID')]").isEmpty())
        .andExpect(jsonPath("$.gates[?(@.gateCode == 'FORM_CAN_BE_SUBMITTED')]").isEmpty())
        .andExpect(
            jsonPath("$.gates[?(@.gateCode == 'META_EFFECTIVE_STATUS_CONFIRMED')]").isEmpty());

    String evidence =
        """
        {
          "gates":[
            {"gateCode":"LANDING_QUALITY_REVIEW_APPROVED","status":"PASS","summary":"Oferta responsiva aprovada","evidenceReference":"e2e://musa-v7/landing"},
            {"gateCode":"CHECKOUT_AND_DELIVERY_CAN_BE_COMPLETED","status":"PASS","summary":"Pagamento de teste, acesso e entrega concluídos","evidenceReference":"e2e://musa-v7/journey"},
            {"gateCode":"DIRECT_CHANNEL_READINESS_CONFIRMED","status":"PASS","summary":"Canal consentido sem gasto ou disparo automático","evidenceReference":"contract://musa-v7/direct-channel"},
            {"gateCode":"DATA_FRESHNESS_VALID","status":"PASS","summary":"QA segregado, correlacionado e deduplicado","evidenceReference":"db://musa-v7/measurement"}
          ]
        }
        """;

    mockMvc
        .perform(
            post("/api/experiment-runs/{runId}/homologation-results", runId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(evidence))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.runStatus").value("READY_TO_PUBLISH"))
        .andExpect(jsonPath("$.hasBlockers").value(false));
  }

  /** Recusa como HTTP 400 o uso de NOT_APPLICABLE para ignorar a Meta em produção. */
  @Test
  void homologationRejectsMetaNotApplicableInProduction() throws Exception {
    Long experimentId = createExperiment();
    createRelevantCommercialDossier();
    Long runId = createRun(experimentId);
    mockMvc.perform(post("/api/experiment-runs/{runId}/preflight", runId));
    String evidence =
        """
        {"gates":[
          {"gateCode":"LANDING_QUALITY_REVIEW_APPROVED","status":"PASS","summary":"ok","evidenceReference":"e2e://landing"},
          {"gateCode":"FORM_CAN_BE_SUBMITTED","status":"PASS","summary":"ok","evidenceReference":"e2e://journey"},
          {"gateCode":"META_EFFECTIVE_STATUS_CONFIRMED","status":"NOT_APPLICABLE","summary":"ignorado","evidenceReference":"e2e://meta"},
          {"gateCode":"DATA_FRESHNESS_VALID","status":"PASS","summary":"ok","evidenceReference":"e2e://measurement"}
        ]}
        """;

    mockMvc
        .perform(
            post("/api/experiment-runs/{runId}/homologation-results", runId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(evidence))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("NOT_APPLICABLE só é permitido para Meta em run técnico de teste"));
  }

  /**
   * Rejeita texto extenso como erro de entrada e permite corrigir o mesmo run sem mutação parcial.
   */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "summary,0",
    "summary,3",
    "evidenceReference,0",
    "evidenceReference,3"
  })
  void rejectsOversizedEvidenceAndRecoversOnSameRun(String field, int gateIndex) throws Exception {
    Long experimentId = createExperiment();
    createRelevantCommercialDossier();
    Long runId = createRun(experimentId);
    mockMvc.perform(post("/api/experiment-runs/{id}/preflight", runId)).andExpect(status().isOk());
    String before =
        mockMvc
            .perform(get("/api/experiment-runs/{id}/preflight", runId))
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    var evidence = validHomologationRequest();
    var gate =
        (com.fasterxml.jackson.databind.node.ObjectNode) evidence.withArray("gates").get(gateIndex);
    gate.put(field, "a".repeat(513));
    mockMvc
        .perform(
            post("/api/experiment-runs/{id}/homologation-results", runId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(evidence)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("512")));
    String after =
        mockMvc
            .perform(get("/api/experiment-runs/{id}/preflight", runId))
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    assertThat(objectMapper.readTree(after)).isEqualTo(objectMapper.readTree(before));
    org.mockito.Mockito.verify(quartzoEvidence, org.mockito.Mockito.never())
        .validateAndBind(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    gate.put(field, "a".repeat(512));
    mockMvc
        .perform(
            post("/api/experiment-runs/{id}/homologation-results", runId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(evidence)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.runStatus").value("READY_TO_PUBLISH"))
        .andExpect(jsonPath("$.hasBlockers").value(false));
    assertThat(experimentRunRepository.findByExperimentIdOrderByRunNumberAsc(experimentId))
        .hasSize(1);
    var saved =
        gateResultRepository.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(runId).stream()
            .filter(g -> g.getGateCode().equals(gate.path("gateCode").asText()))
            .findFirst()
            .orElseThrow();
    assertThat(field.equals("summary") ? saved.getSummary() : saved.getEvidenceReference())
        .isEqualTo("a".repeat(512));
  }

  /** Entradas funcionais inválidas devolvem HTTP 400 e conservam todos os gates pendentes. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "missing",
        "duplicate",
        "unknown",
        "pending",
        "empty",
        "nullGate",
        "nullCode",
        "missingCode",
        "nullGates"
      })
  void rejectsInvalidEvidenceWithoutClassifyingItAsTechnicalFailure(String scenario)
      throws Exception {
    Long experimentId = createExperiment();
    createRelevantCommercialDossier();
    Long runId = createRun(experimentId);
    mockMvc.perform(post("/api/experiment-runs/{id}/preflight", runId)).andExpect(status().isOk());
    var evidence = validHomologationRequest();
    var gates = evidence.withArray("gates");
    var first = (com.fasterxml.jackson.databind.node.ObjectNode) gates.get(0);
    switch (scenario) {
      case "missing" -> gates.remove(3);
      case "duplicate" -> gates.add(first.deepCopy());
      case "unknown" -> first.put("gateCode", "UNKNOWN");
      case "pending" -> first.put("status", "PENDING");
      case "empty" -> first.put("summary", " ");
      case "nullGate" -> gates.addNull();
      case "nullCode" -> first.putNull("gateCode");
      case "missingCode" -> first.remove("gateCode");
      case "nullGates" -> evidence.putNull("gates");
      default -> throw new IllegalArgumentException("Cenário de teste desconhecido");
    }
    mockMvc
        .perform(
            post("/api/experiment-runs/{id}/homologation-results", runId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(evidence)))
        .andExpect(status().isBadRequest());
    assertThat(experimentRunRepository.findById(runId).orElseThrow().getStatus())
        .isEqualTo(ExperimentRunStatus.PREFLIGHT_PENDING);
    assertThat(gateResultRepository.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(runId))
        .filteredOn(
            g ->
                java.util.Set.of(
                        "LANDING_QUALITY_REVIEW_APPROVED",
                        "FORM_CAN_BE_SUBMITTED",
                        "META_EFFECTIVE_STATUS_CONFIRMED",
                        "DATA_FRESHNESS_VALID")
                    .contains(g.getGateCode()))
        .hasSize(4)
        .allMatch(g -> g.getStatus() == ExperimentRunGateStatus.PENDING);
  }

  /** Monta quatro provas locais válidas sem fixar produto, versão ou identidade de execução. */
  private com.fasterxml.jackson.databind.node.ObjectNode validHomologationRequest() {
    var request = objectMapper.createObjectNode();
    var gates = request.putArray("gates");
    for (String code :
        java.util.List.of(
            "LANDING_QUALITY_REVIEW_APPROVED",
            "FORM_CAN_BE_SUBMITTED",
            "META_EFFECTIVE_STATUS_CONFIRMED",
            "DATA_FRESHNESS_VALID")) {
      gates
          .addObject()
          .put("gateCode", code)
          .put("status", "PASS")
          .put("summary", "Contrato funcional local comprovado.")
          .put("evidenceReference", "fixture://local/" + code);
    }
    return request;
  }

  /** Deve bloquear preflight quando não existir dossiê MOIS aderente à hipótese. */
  @Test
  void runPreflightBlocksWithoutCommercialDossier() throws Exception {
    Long experimentId = createExperiment();
    Long runId = createRun(experimentId);

    mockMvc
        .perform(post("/api/experiment-runs/{runId}/preflight", runId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.runStatus").value("PREFLIGHT_FAILED"))
        .andExpect(jsonPath("$.hasBlockers").value(true))
        .andExpect(
            jsonPath("$.gates[?(@.gateCode == 'MOIS_COMMERCIAL_DOSSIER_PREFLIGHT')].status")
                .value(hasItem("FAIL")))
        .andExpect(
            jsonPath(
                    "$.gates[?(@.gateCode == 'MOIS_COMMERCIAL_DOSSIER_PREFLIGHT')].remediationCode")
                .value(hasItem("GENERATE_MOIS_COMMERCIAL_DOSSIER")));
  }

  /** Deve bloquear preflight quando desenho experimental ou persona estiverem incompletos. */
  @Test
  void runPreflightWithStrategicBlockers() throws Exception {
    Long experimentId = createExperimentWithMissingDesign();
    createRelevantCommercialDossier();
    Long runId = createRun(experimentId);

    mockMvc
        .perform(post("/api/experiment-runs/{runId}/preflight", runId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.runStatus").value("PREFLIGHT_FAILED"))
        .andExpect(jsonPath("$.hasBlockers").value(true))
        .andExpect(
            jsonPath("$.gates[?(@.gateCode == 'PERSONA_MINIMUM_COMPLETE')].status")
                .value(hasItem("FAIL")))
        .andExpect(
            jsonPath("$.gates[?(@.gateCode == 'PRIMARY_METRIC_DEFINED')].status")
                .value(hasItem("FAIL")))
        .andExpect(
            jsonPath("$.gates[?(@.gateCode == 'KPI_TARGET_CPL_VALID')].remediationCode")
                .value(hasItem("DEFINE_KPI_TARGET_CPL")));
  }

  /** Reavalia o mesmo run bloqueado sem colidir nem duplicar os gates persistidos. */
  @Test
  void rerunFailedPreflightReplacesGatesWithoutDuplicates() throws Exception {
    Long experimentId = createExperimentWithMissingDesign();
    createRelevantCommercialDossier();
    Long runId = createRun(experimentId);

    mockMvc
        .perform(post("/api/experiment-runs/{runId}/preflight", runId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.runStatus").value("PREFLIGHT_FAILED"));

    mockMvc
        .perform(post("/api/experiment-runs/{runId}/preflight", runId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.runStatus").value("PREFLIGHT_FAILED"));

    var gates = gateResultRepository.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(runId);
    assertThat(gates).hasSize(11);
    assertThat(gates).extracting(ExperimentRunGateResult::getGateCode).doesNotHaveDuplicates();
  }

  /** Cria o experimento mínimo necessário para vincular runs nos testes. */
  private Long createExperiment() {
    MarketNiche niche =
        marketNicheRepository.save(MarketNiche.builder().name("Doces Finos Premium").build());
    Angle angle = angleRepository.save(Angle.builder().name("Ângulo Run").build());
    Hypothesis hypothesis =
        hypothesisRepository.save(
            Hypothesis.builder()
                .marketNiche(niche)
                .title("Especialista em doces finos")
                .premiseAngle(angle)
                .promise("Cobrar mais por doces finos premium")
                .problem("Confeiteiras trabalham muito e cobram pouco")
                .persona("Confeiteira autônoma")
                .offerType(OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .mechanism("Reposicionamento premium")
                .entrega("Plano de precificação")
                .build());
    Experiment experiment =
        Experiment.builder()
            .niche(niche)
            .name("Experimento Doces Finos")
            .hypothesisRef(hypothesis)
            .hypothesis("Doces finos premium")
            .status(ExperimentStatus.PLANNED)
            .platform(ExperimentPlatform.FACEBOOK)
            .primaryVariable("Ângulo de dor")
            .primaryMetric("Envio de formulário")
            .kpiTargetCpl(new BigDecimal("45"))
            .build();
    return experimentRepository.save(experiment).getId();
  }

  /** Cria um experimento de venda direta que não depende de formulário, Meta ou CPL. */
  private Long createDirectSalesExperiment() {
    MarketNiche niche =
        marketNicheRepository.save(MarketNiche.builder().name("Doces Finos Venda Direta").build());
    Angle angle = angleRepository.save(Angle.builder().name("Ângulo Venda Direta").build());
    Hypothesis hypothesis =
        hypothesisRepository.save(
            Hypothesis.builder()
                .marketNiche(niche)
                .title("Venda direta de doces finos")
                .premiseAngle(angle)
                .promise("Cobrar mais por doces finos premium")
                .problem("Confeiteiras trabalham muito e cobram pouco")
                .persona("Confeiteira autônoma")
                .offerType(OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .mechanism("Reposicionamento premium")
                .entrega("Plano de precificação")
                .build());
    Experiment experiment =
        Experiment.builder()
            .niche(niche)
            .name("Experimento Venda Direta")
            .hypothesisRef(hypothesis)
            .hypothesis("Venda direta de doces finos premium")
            .status(ExperimentStatus.PLANNED)
            .platform(ExperimentPlatform.DIRECT_ONE_TO_ONE)
            .campaignObjective(ExperimentCampaignObjective.SALES)
            .primaryVariable("Enquadramento da oferta")
            .primaryMetric("Venda líquida reconciliada")
            .unitPrice(new BigDecimal("67"))
            .sampleSize(100)
            .targetCvr(new BigDecimal("0.80"))
            .build();
    return experimentRepository.save(experiment).getId();
  }

  /** Cria dossiê concluído aderente ao experimento para liberar o gate comercial. */
  private void createRelevantCommercialDossier() {
    PipelineDossieProduto dossier = new PipelineDossieProduto();
    dossier.setIdExterno("101");
    dossier.setCodigoEtapa("dossier-synthesis");
    dossier.setStatus("CONCLUIDO");
    dossier.setPipelineCode("warmupecosystem.v1");
    dossier.setVersaoPipeline("v1");
    dossier.setDataHora(Instant.now());
    dossier.setJobId("job-doces-finos");
    dossier.setRespostaFinal(
        """
        Produto quente de doces finos premium: confeiteira autônoma usa reposicionamento premium
        para cobrar mais, trabalhar menos e vender oferta com autoridade e prova social.
        """);
    pipelineDossieProdutoRepository.save(dossier);
  }

  /** Cria um run e retorna seu identificador para os testes de preflight. */
  private Long createRun(Long experimentId) throws Exception {
    String response =
        mockMvc
            .perform(
                post("/api/experiments/{experimentId}/runs", experimentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateExperimentRunRequest(
                                ExperimentRunMode.PRODUCTION,
                                ExperimentRunStopPolicy.MANUAL_ONLY,
                                "teste"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(response).get("id").asLong();
  }

  /** Cria experimento com lacunas estratégicas para validar bloqueios de preflight. */
  private Long createExperimentWithMissingDesign() {
    MarketNiche niche =
        marketNicheRepository.save(MarketNiche.builder().name("Nicho Bloqueado").build());
    Angle angle = angleRepository.save(Angle.builder().name("Ângulo Bloqueado").build());
    Hypothesis hypothesis =
        hypothesisRepository.save(
            Hypothesis.builder()
                .marketNiche(niche)
                .title("Hipótese Bloqueada")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("teste")
                .offerType(OfferType.LEAD)
                .kpiTargetCpl(BigDecimal.ZERO)
                .build());
    Experiment experiment =
        Experiment.builder()
            .niche(niche)
            .name("Experimento Bloqueado")
            .hypothesisRef(hypothesis)
            .hypothesis("Resumo")
            .status(ExperimentStatus.PLANNED)
            .platform(ExperimentPlatform.FACEBOOK)
            .kpiTargetCpl(BigDecimal.ZERO)
            .build();
    return experimentRepository.save(experiment).getId();
  }
}

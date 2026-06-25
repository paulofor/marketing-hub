package com.marketinghub.experiment.run;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.creative.label.Angle;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.run.service.create.CreateExperimentRunRequest;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.OfferType;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.creative.label.AngleRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunGateResultRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunRepository;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Valida a API administrativa inicial de execuções operacionais de experimentos.
 */
@SpringBootTest(classes = AdsServiceApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:experiment_run_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=false"
})
class BackendExperimentRunControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ExperimentRepository experimentRepository;
    @Autowired
    private ExperimentRunRepository experimentRunRepository;
    @Autowired
    private ExperimentRunGateResultRepository gateResultRepository;
    @Autowired
    private MarketNicheRepository marketNicheRepository;
    @Autowired
    private HypothesisRepository hypothesisRepository;
    @Autowired
    private AngleRepository angleRepository;

    /** Limpa dados persistidos para manter a ordem sequencial dos runs previsível. */
    @BeforeEach
    void cleanDb() {
        gateResultRepository.deleteAll();
        experimentRunRepository.deleteAll();
        experimentRepository.deleteAll();
        hypothesisRepository.deleteAll();
        angleRepository.deleteAll();
        marketNicheRepository.deleteAll();
    }

    /** Deve criar runs sequenciais com validade inicial neutra e sem alterar o experimento legado. */
    @Test
    void createSequentialRuns() throws Exception {
        Long experimentId = createExperiment();
        CreateExperimentRunRequest request = new CreateExperimentRunRequest(
                ExperimentRunMode.TEST,
                ExperimentRunStopPolicy.MANUAL_ONLY,
                "codex");

        mockMvc.perform(post("/api/experiments/{experimentId}/runs", experimentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experimentId").value(experimentId))
                .andExpect(jsonPath("$.runNumber").value(1))
                .andExpect(jsonPath("$.mode").value("TEST"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.evidenceValidity").value("NOT_EVALUATED"))
                .andExpect(jsonPath("$.dataQualityStatus").value("UNKNOWN"));

        mockMvc.perform(post("/api/experiments/{experimentId}/runs", experimentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runNumber").value(2));
    }

    /** Deve listar e consultar runs já persistidos para alimentar o frontend. */
    @Test
    void listAndGetRuns() throws Exception {
        Long experimentId = createExperiment();
        String response = mockMvc.perform(post("/api/experiments/{experimentId}/runs", experimentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExperimentRunRequest(
                                ExperimentRunMode.PRODUCTION,
                                ExperimentRunStopPolicy.FIRST_VALID_LEAD_STANDBY,
                                "operador"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long runId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/experiments/{experimentId}/runs", experimentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(runId))
                .andExpect(jsonPath("$[0].stopPolicy").value("FIRST_VALID_LEAD_STANDBY"));

        mockMvc.perform(get("/api/experiment-runs/{runId}", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(runId))
                .andExpect(jsonPath("$.experimentId").value(experimentId));
    }



    /** Deve executar preflight determinístico e liberar run sem bloqueadores estratégicos iniciais. */
    @Test
    void runPreflightWithoutBlockers() throws Exception {
        Long experimentId = createExperiment();
        Long runId = createRun(experimentId);

        mockMvc.perform(post("/api/experiment-runs/{runId}/preflight", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runStatus").value("READY_TO_PUBLISH"))
                .andExpect(jsonPath("$.hasBlockers").value(false))
                .andExpect(jsonPath("$.gates[?(@.gateCode == 'PRIMARY_VARIABLE_DEFINED')].status").value(hasItem("PASS")))
                .andExpect(jsonPath("$.gates[?(@.gateCode == 'FORM_CAN_BE_SUBMITTED')].status").value(hasItem("PENDING")));

        mockMvc.perform(get("/api/experiment-runs/{runId}/preflight", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runStatus").value("READY_TO_PUBLISH"));
    }

    /** Deve bloquear preflight quando desenho experimental ou persona estiverem incompletos. */
    @Test
    void runPreflightWithStrategicBlockers() throws Exception {
        Long experimentId = createExperimentWithMissingDesign();
        Long runId = createRun(experimentId);

        mockMvc.perform(post("/api/experiment-runs/{runId}/preflight", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runStatus").value("PREFLIGHT_FAILED"))
                .andExpect(jsonPath("$.hasBlockers").value(true))
                .andExpect(jsonPath("$.gates[?(@.gateCode == 'PERSONA_MINIMUM_COMPLETE')].status").value(hasItem("FAIL")))
                .andExpect(jsonPath("$.gates[?(@.gateCode == 'PRIMARY_METRIC_DEFINED')].status").value(hasItem("FAIL")))
                .andExpect(jsonPath("$.gates[?(@.gateCode == 'KPI_TARGET_CPL_VALID')].remediationCode").value(hasItem("DEFINE_KPI_TARGET_CPL")));
    }

    /** Cria o experimento mínimo necessário para vincular runs nos testes. */
    private Long createExperiment() {
        MarketNiche niche = marketNicheRepository.save(MarketNiche.builder().name("Nicho Run").build());
        Angle angle = angleRepository.save(Angle.builder().name("Ângulo Run").build());
        Hypothesis hypothesis = hypothesisRepository.save(Hypothesis.builder()
                .marketNiche(niche)
                .title("Hipótese Run")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .offerType(OfferType.LEAD)
                .kpiTargetCpl(new BigDecimal("1"))
                .mechanism("Mecanismo")
                .entrega("Entrega")
                .build());
        Experiment experiment = Experiment.builder()
                .niche(niche)
                .name("Experimento Run")
                .hypothesisRef(hypothesis)
                .hypothesis("Resumo")
                .status(ExperimentStatus.PLANNED)
                .platform(ExperimentPlatform.FACEBOOK)
                .primaryVariable("Ângulo de dor")
                .primaryMetric("Envio de formulário")
                .kpiTargetCpl(new BigDecimal("45"))
                .build();
        return experimentRepository.save(experiment).getId();
    }

    /** Cria um run e retorna seu identificador para os testes de preflight. */
    private Long createRun(Long experimentId) throws Exception {
        String response = mockMvc.perform(post("/api/experiments/{experimentId}/runs", experimentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExperimentRunRequest(
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
        MarketNiche niche = marketNicheRepository.save(MarketNiche.builder().name("Nicho Bloqueado").build());
        Angle angle = angleRepository.save(Angle.builder().name("Ângulo Bloqueado").build());
        Hypothesis hypothesis = hypothesisRepository.save(Hypothesis.builder()
                .marketNiche(niche)
                .title("Hipótese Bloqueada")
                .premiseAngle(angle)
                .promise("Promessa")
                .problem("Problema")
                .persona("teste")
                .offerType(OfferType.LEAD)
                .kpiTargetCpl(BigDecimal.ZERO)
                .build());
        Experiment experiment = Experiment.builder()
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

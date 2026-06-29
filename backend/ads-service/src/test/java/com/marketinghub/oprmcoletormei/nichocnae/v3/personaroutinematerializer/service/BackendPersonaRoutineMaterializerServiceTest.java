package com.marketinghub.oprmcoletormei.nichocnae.v3.personaroutinematerializer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.openai.service.OpenAiPricingService;
import com.marketinghub.oprm.market.OprmCnpjCnaeDim;
import com.marketinghub.oprmcoletormei.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.oprmcoletormei.nichocnae.v3.OprmNichoCnaeV3StageExecutionStatus;
import com.marketinghub.oprmcoletormei.nichocnae.v3.personaroutinematerializer.gateway.PersonaRoutineMaterializerNicheGateway;
import com.marketinghub.repository.jpa.oprm.market.OprmCnpjCnaeDimRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.PipelineNichoCnaeRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Testa a materialização final do NichoCNAE v3 em dados reutilizáveis do nicho. */
@ExtendWith(MockitoExtension.class)
class BackendPersonaRoutineMaterializerServiceTest {
    @Mock
    private OprmNichoCnaeV3StageExecutionRepository repository;

    @Mock
    private OprmCnpjCnaeDimRepository cnaeRepository;

    @Mock
    private PersonaRoutineMaterializerNicheGateway nicheGateway;

    @Mock
    private PipelineNichoCnaeRepository pipelineNichoCnaeRepository;

    @Mock
    private OpenAiPricingService openAiPricingService;

    private BackendPersonaRoutineMaterializerService service;

    @BeforeEach
    void setUp() {
        service = new BackendPersonaRoutineMaterializerService(
                repository, cnaeRepository, pipelineNichoCnaeRepository, openAiPricingService, nicheGateway, new ObjectMapper());
    }

    /** Garante que a etapa final grava MarketNiche e perfil enriquecido para uso por outros pipelines. */
    @Test
    void completeMaterializesFinalPayloadIntoNicheData() {
        OprmNichoCnaeV3StageExecution execution = execution();
        String outputPayload = """
                {
                  "neutralNicheName":"Lojistas de roupas infantis com rotina de reposição",
                  "cnaeDescription":"Comércio varejista de artigos do vestuário",
                  "routineSummary":"Compram, organizam vitrines e atendem famílias diariamente.",
                  "personaDailyTasks":["repor peças", "responder clientes"],
                  "routineEvidenceScore":82,
                  "difficultyEvidenceScore":77,
                  "sourceDiversityScore":68
                }
                """;
        when(repository.findById(19L)).thenReturn(Optional.of(execution));
        when(repository.save(any(OprmNichoCnaeV3StageExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(nicheGateway.findPersonaRoutineMaterializedNiche(
                eq("4781400"), eq("cnae 4781400 — comércio varejista de artigos do vestuário")))
                .thenReturn(Optional.empty());
        when(nicheGateway.materialize(any(PersonaRoutineMaterializerNicheGateway.MarketNicheDraft.class), any(PersonaRoutineMaterializerNicheGateway.EnrichedNicheProfileDraft.class)))
                .thenReturn(new PersonaRoutineMaterializerNicheGateway.NicheMaterializationResult(300L, 400L, Instant.now()));
        OprmCnpjCnaeDim cnae = new OprmCnpjCnaeDim();
        cnae.setCnaeCode("4781400");
        cnae.setNichocnaeCurrentStageCode("persona-routine-materializer");
        cnae.setNichocnaePipelineStatus("INICIADO");
        when(cnaeRepository.findById("4781400")).thenReturn(Optional.of(cnae));

        service.complete(19L, outputPayload, "");

        ArgumentCaptor<PersonaRoutineMaterializerNicheGateway.MarketNicheDraft> nicheCaptor = ArgumentCaptor.forClass(PersonaRoutineMaterializerNicheGateway.MarketNicheDraft.class);
        ArgumentCaptor<PersonaRoutineMaterializerNicheGateway.EnrichedNicheProfileDraft> profileCaptor = ArgumentCaptor.forClass(PersonaRoutineMaterializerNicheGateway.EnrichedNicheProfileDraft.class);
        verify(nicheGateway).materialize(nicheCaptor.capture(), profileCaptor.capture());
        assertThat(nicheCaptor.getValue().name()).isEqualTo("CNAE 4781400 — Comércio varejista de artigos do vestuário");
        assertThat(nicheCaptor.getValue().sourceCnaeCode()).isEqualTo("4781400");
        assertThat(nicheCaptor.getValue().sourceCnaeDescription()).isEqualTo("Comércio varejista de artigos do vestuário");
        assertThat(nicheCaptor.getValue().description()).contains("Compram, organizam vitrines");
        assertThat(profileCaptor.getValue().researchCycleId()).isEqualTo(19L);
        assertThat(profileCaptor.getValue().sourceRoutineCardId()).isEqualTo(19L);
        assertThat(profileCaptor.getValue().routineEvidenceScore()).isEqualTo(82);
        assertThat(profileCaptor.getValue().difficultyEvidenceScore()).isEqualTo(77);
        assertThat(profileCaptor.getValue().sourceDiversityScore()).isEqualTo(68);
        assertThat(profileCaptor.getValue().routineSummary()).contains("Compram");
        assertThat(profileCaptor.getValue().personaDailyTasks()).contains("repor peças");
        assertThat(cnae.getNichocnaePipelineStatus()).isEqualTo("CONCLUIDO");
    }

    /** Garante que o start marca o status do pipeline e a etapa atual no cadastro do CNAE. */
    @Test
    void startUpdatesCnaePipelineStatusAndCurrentStage() {
        OprmCnpjCnaeDim cnae = new OprmCnpjCnaeDim();
        cnae.setCnaeCode("4781400");
        cnae.setDescription("Comércio varejista de artigos do vestuário");
        cnae.setActive(true);
        cnae.setUpdatedAt(Instant.parse("2026-06-26T00:00:00Z"));
        when(cnaeRepository.findById("4781400")).thenReturn(Optional.of(cnae));
        when(repository.save(any(OprmNichoCnaeV3StageExecution.class))).thenAnswer(invocation -> {
            OprmNichoCnaeV3StageExecution saved = invocation.getArgument(0);
            saved.setId(88L);
            return saved;
        });

        var response = service.start("4781400");

        assertThat(UUID.fromString(response.jobId()).toString()).isEqualTo(response.jobId());
        assertThat(cnae.getNichocnaePipelineStatus()).isEqualTo("INICIADO");
        assertThat(cnae.getNichocnaeCurrentStageCode()).isEqualTo("persona-routine-materializer");
        assertThat(cnae.getNichocnaePipelineUpdatedAt()).isNotNull();
        verify(cnaeRepository).save(cnae);
    }


    /** Garante que o pending lê CNAEs iniciados da etapa corrente pelo cadastro canônico de CNAE. */
    @Test
    void pendingListsStartedCnaesFromCurrentStage() {
        OprmCnpjCnaeDim cnae = new OprmCnpjCnaeDim();
        cnae.setCnaeCode("4781400");
        cnae.setNichocnaeCurrentStageCode("persona-routine-materializer");
        cnae.setNichocnaePipelineStatus("INICIADO");
        OprmNichoCnaeV3StageExecution execution = execution();
        execution.setInputPayload("{\"origin\":\"stage-execution\"}");
        execution.setAttemptNumber(2);
        execution.setKnowledgeVersion(3);
        when(cnaeRepository.findByNichocnaeCurrentStageCodeAndNichocnaePipelineStatusOrderByNichocnaePipelineUpdatedAtAsc(
                eq("persona-routine-materializer"), eq("INICIADO"), eq(PageRequest.of(0, 10))))
                .thenReturn(List.of(cnae));
        when(repository.findTop1ByCnaeCodeAndStageCodeOrderByCreatedAtDesc("4781400", "persona-routine-materializer"))
                .thenReturn(Optional.of(execution));

        var pending = service.pending();

        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().stageExecutionId()).isEqualTo(19L);
        assertThat(pending.getFirst().jobId()).isEqualTo("nichocnae-v3-4781400-1782411268024");
        assertThat(pending.getFirst().cnaeCode()).isEqualTo("4781400");
        assertThat(pending.getFirst().inputPayload()).isEqualTo("{\"origin\":\"stage-execution\"}");
        assertThat(pending.getFirst().attemptNumber()).isEqualTo(2);
        assertThat(pending.getFirst().knowledgeVersion()).isEqualTo(3);
    }

    /** Monta uma execução final pendente para o callback de conclusão. */
    private OprmNichoCnaeV3StageExecution execution() {
        OprmNichoCnaeV3StageExecution execution = new OprmNichoCnaeV3StageExecution();
        execution.setId(19L);
        execution.setJobId("nichocnae-v3-4781400-1782411268024");
        execution.setCnaeCode("4781400");
        execution.setStageCode("persona-routine-materializer");
        execution.setStatus(OprmNichoCnaeV3StageExecutionStatus.PENDING);
        execution.setCreatedAt(Instant.parse("2026-06-25T21:08:56Z"));
        execution.setUpdatedAt(Instant.parse("2026-06-25T21:08:56Z"));
        return execution;
    }
}

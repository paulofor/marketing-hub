package com.marketinghub.oprm.nichocnae.v3.personaroutinematerializer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionStatus;
import com.marketinghub.oprm.nichocnae.v3.personaroutinematerializer.gateway.PersonaRoutineMaterializerNicheGateway;
import com.marketinghub.repository.jpa.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionRepository;
import java.time.Instant;
import java.util.Optional;
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
    private PersonaRoutineMaterializerNicheGateway nicheGateway;

    private BackendPersonaRoutineMaterializerService service;

    @BeforeEach
    void setUp() {
        service = new BackendPersonaRoutineMaterializerService(repository, nicheGateway, new ObjectMapper());
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
                  "painsSummary":"Perdem tempo conciliando estoque e atendimento.",
                  "resultsSummary":"Querem previsibilidade de vendas e menos ruptura.",
                  "mechanismOpportunitiesSummary":"Padronização de rotina e priorização de reposição.",
                  "routineEvidenceScore":82,
                  "difficultyEvidenceScore":77,
                  "sourceDiversityScore":68
                }
                """;
        when(repository.findById(19L)).thenReturn(Optional.of(execution));
        when(repository.save(any(OprmNichoCnaeV3StageExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(nicheGateway.findPersonaRoutineMaterializedNiche(
                eq("4781400"), eq("lojistas de roupas infantis com rotina de reposição")))
                .thenReturn(Optional.empty());
        when(nicheGateway.materialize(any(PersonaRoutineMaterializerNicheGateway.MarketNicheDraft.class), any(PersonaRoutineMaterializerNicheGateway.EnrichedNicheProfileDraft.class)))
                .thenReturn(new PersonaRoutineMaterializerNicheGateway.NicheMaterializationResult(300L, 400L, Instant.now()));

        service.complete(19L, outputPayload, "");

        ArgumentCaptor<PersonaRoutineMaterializerNicheGateway.MarketNicheDraft> nicheCaptor = ArgumentCaptor.forClass(PersonaRoutineMaterializerNicheGateway.MarketNicheDraft.class);
        ArgumentCaptor<PersonaRoutineMaterializerNicheGateway.EnrichedNicheProfileDraft> profileCaptor = ArgumentCaptor.forClass(PersonaRoutineMaterializerNicheGateway.EnrichedNicheProfileDraft.class);
        verify(nicheGateway).materialize(nicheCaptor.capture(), profileCaptor.capture());
        assertThat(nicheCaptor.getValue().name()).isEqualTo("Lojistas de roupas infantis com rotina de reposição");
        assertThat(nicheCaptor.getValue().description()).contains("Compram, organizam vitrines");
        assertThat(profileCaptor.getValue().researchCycleId()).isEqualTo(19L);
        assertThat(profileCaptor.getValue().sourceRoutineCardId()).isEqualTo(19L);
        assertThat(profileCaptor.getValue().routineEvidenceScore()).isEqualTo(82);
        assertThat(profileCaptor.getValue().difficultyEvidenceScore()).isEqualTo(77);
        assertThat(profileCaptor.getValue().sourceDiversityScore()).isEqualTo(68);
        assertThat(profileCaptor.getValue().routineSummary()).contains("Compram");
        assertThat(profileCaptor.getValue().personaDailyTasks()).contains("repor peças");
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

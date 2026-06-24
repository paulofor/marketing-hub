package com.marketinghub.targeting.service;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.repository.jpa.targeting.TargetingElementRepository;
import com.marketinghub.targeting.TargetingElementSource;
import com.marketinghub.targeting.TargetingElementStatus;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.targeting.dto.CreateTargetingElementRequest;
import com.marketinghub.targeting.dto.generation.TargetingElementGenerationFailureRequest;
import com.marketinghub.targeting.dto.generation.TargetingElementGenerationResultRequest;
import com.marketinghub.targeting.dto.generation.TargetingElementGenerationPendingDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes dos contratos internos de geração de públicos consumidos pelo AI Worker.
 */
@ExtendWith(MockitoExtension.class)
class TargetingElementGenerationServiceTest {
    @Mock
    TargetingElementRepository elementRepository;

    @Mock
    MarketNicheRepository nicheRepository;

    @Mock
    HypothesisRepository hypothesisRepository;

    @InjectMocks
    TargetingElementService service;

    /** Garante que o backend publica pendências de cargos com contexto do nicho para o worker. */
    @Test
    void listPendingGenerationShouldExposeJobTitleRequests() {
        MarketNiche niche = MarketNiche.builder()
                .id(23L)
                .name("Comércio varejista")
                .description("Lojas de roupas")
                .jobTitlesToGenerate(1)
                .jobTitleModel("gpt-5.5")
                .roleCategory("Varejo")
                .build();
        when(nicheRepository.findAllToGenerateInterests()).thenReturn(List.of());
        when(nicheRepository.findAllToGenerateJobTitles()).thenReturn(List.of(niche));

        List<TargetingElementGenerationPendingDto> pending = service.listPendingGeneration(10);

        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).nicheId()).isEqualTo(23L);
        assertThat(pending.get(0).type()).isEqualTo(TargetingElementType.JOB_TITLE);
        assertThat(pending.get(0).quantity()).isEqualTo(1);
        assertThat(pending.get(0).model()).isEqualTo("gpt-5.5");
    }

    /** Garante que o backend persiste itens retornados pelo worker e zera apenas a pendência processada. */
    @Test
    void saveGeneratedElementsShouldPersistItemsAndResetCounter() {
        MarketNiche niche = MarketNiche.builder().id(23L).jobTitlesToGenerate(1).build();
        when(nicheRepository.findById(23L)).thenReturn(Optional.of(niche));
        when(elementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        CreateTargetingElementRequest item = new CreateTargetingElementRequest();
        item.setTerm("Gerente de loja");
        item.setDescription("Responsável pela operação da loja");
        item.setSource(TargetingElementSource.AI);
        item.setStatus(TargetingElementStatus.NEEDS_REVIEW);

        service.saveGeneratedElements(
                23L,
                TargetingElementType.JOB_TITLE,
                new TargetingElementGenerationResultRequest(List.of(item)));

        ArgumentCaptor<com.marketinghub.targeting.TargetingElement> captor =
                ArgumentCaptor.forClass(com.marketinghub.targeting.TargetingElement.class);
        verify(elementRepository).save(captor.capture());
        assertThat(captor.getValue().getNiche()).isEqualTo(niche);
        assertThat(captor.getValue().getType()).isEqualTo(TargetingElementType.JOB_TITLE);
        assertThat(niche.getJobTitlesToGenerate()).isZero();
        verify(nicheRepository).save(niche);
    }


    /** Garante que cargo só pode ser aprovado quando já possui ID oficial da Meta. */
    @Test
    void createShouldRejectApprovedJobTitleWithoutMetaId() {
        MarketNiche niche = MarketNiche.builder().id(23L).build();
        when(nicheRepository.findById(23L)).thenReturn(Optional.of(niche));
        CreateTargetingElementRequest item = new CreateTargetingElementRequest();
        item.setMarketNicheId(23L);
        item.setType(TargetingElementType.JOB_TITLE);
        item.setTerm("Gerente de loja");
        item.setStatus(TargetingElementStatus.APPROVED);

        assertThatThrownBy(() -> service.create(item))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID oficial da Meta");
    }

    /** Garante que cargo com ID oficial da Meta pode ser aprovado para uso operacional. */
    @Test
    void createShouldApproveJobTitleWithMetaId() {
        MarketNiche niche = MarketNiche.builder().id(23L).build();
        when(nicheRepository.findById(23L)).thenReturn(Optional.of(niche));
        when(elementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        CreateTargetingElementRequest item = new CreateTargetingElementRequest();
        item.setMarketNicheId(23L);
        item.setType(TargetingElementType.JOB_TITLE);
        item.setTerm("Gerente de loja");
        item.setMetaId("6000000000001");
        item.setStatus(TargetingElementStatus.APPROVED);

        var saved = service.create(item);

        assertThat(saved.getStatus()).isEqualTo(TargetingElementStatus.APPROVED);
        assertThat(saved.getMetaId()).isEqualTo("6000000000001");
    }

    /** Garante que uma falha reportada pelo worker libera a pendência para evitar loop infinito. */
    @Test
    void markGenerationFailureShouldResetCounter() {
        MarketNiche niche = MarketNiche.builder().id(23L).behaviorsToGenerate(1).build();
        when(nicheRepository.findById(23L)).thenReturn(Optional.of(niche));

        service.markGenerationFailure(
                23L,
                TargetingElementType.BEHAVIOR,
                new TargetingElementGenerationFailureRequest("erro"));

        assertThat(niche.getBehaviorsToGenerate()).isZero();
        verify(nicheRepository).save(niche);
    }
}

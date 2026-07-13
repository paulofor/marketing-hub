package com.marketinghub.experiment.salespagetype.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.salespagetype.ExperimentSalesPageTypeSelection;
import com.marketinghub.experiment.salespagetype.SalesPageType;
import com.marketinghub.experiment.salespagetype.service.updateselection.UpdateExperimentSalesPageTypeSelectionItem;
import com.marketinghub.experiment.salespagetype.service.updateselection.UpdateExperimentSalesPageTypeSelectionRequest;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.salespagetype.ExperimentSalesPageTypeSelectionRepository;
import com.marketinghub.repository.jpa.experiment.salespagetype.SalesPageTypeRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/** Valida a selecao de tipos de pagina de venda por experimento. */
@ExtendWith(MockitoExtension.class)
class SalesPageTypeServiceTest {
    @Mock
    private SalesPageTypeRepository typeRepository;
    @Mock
    private ExperimentSalesPageTypeSelectionRepository selectionRepository;
    @Mock
    private ExperimentRepository experimentRepository;

    private SalesPageTypeService service;

    /** Inicializa o servico com dependencias simuladas. */
    @BeforeEach
    void setUp() {
        service = new SalesPageTypeService(typeRepository, selectionRepository, experimentRepository);
    }

    /** Garante que o tipo chat IA pode ser salvo como variante de teste. */
    @Test
    void shouldSelectAiChatDigitalBaitAsSalesPageType() {
        Experiment experiment = Experiment.builder().id(64L).name("MUSA-H001-E003").build();
        SalesPageType chatType = chatType();
        given(experimentRepository.findById(64L)).willReturn(Optional.of(experiment));
        given(typeRepository.findById("AI_CHAT_DIGITAL_BAIT")).willReturn(Optional.of(chatType));
        given(selectionRepository.saveAll(anyList())).willAnswer(invocation -> {
            List<ExperimentSalesPageTypeSelection> selections = invocation.getArgument(0);
            selections.get(0).setId(1L);
            return selections;
        });

        var response = service.replaceExperimentSelections(
                64L,
                new UpdateExperimentSalesPageTypeSelectionRequest(List.of(
                        new UpdateExperimentSalesPageTypeSelectionItem(
                                "AI_CHAT_DIGITAL_BAIT",
                                "A",
                                new BigDecimal("100.00"),
                                true,
                                "Coletar dados e entregar imagem no chat"))));

        assertThat(response).hasSize(1);
        assertThat(response.get(0).typeCode()).isEqualTo("AI_CHAT_DIGITAL_BAIT");
        assertThat(response.get(0).variantKey()).isEqualTo("A");
        assertThat(response.get(0).type().digitalBaitDelivery())
                .contains("imagem personalizada");
        verify(selectionRepository).deleteByExperimentId(64L);
    }

    /** Garante que o mesmo tipo nao pode ser duplicado na selecao A/B. */
    @Test
    void shouldRejectDuplicatedTypeCode() {
        Experiment experiment = Experiment.builder().id(64L).build();
        given(experimentRepository.findById(64L)).willReturn(Optional.of(experiment));
        given(typeRepository.findById("AI_CHAT_DIGITAL_BAIT")).willReturn(Optional.of(chatType()));

        assertThatThrownBy(() -> service.replaceExperimentSelections(
                64L,
                new UpdateExperimentSalesPageTypeSelectionRequest(List.of(
                        new UpdateExperimentSalesPageTypeSelectionItem(
                                "AI_CHAT_DIGITAL_BAIT", "A", BigDecimal.TEN, true, null),
                        new UpdateExperimentSalesPageTypeSelectionItem(
                                "AI_CHAT_DIGITAL_BAIT", "B", BigDecimal.TEN, true, null)))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("duplicated typeCode");
    }

    /** Garante que o teste A/B comercial nao aceita terceira variante selecionada. */
    @Test
    void shouldRejectMoreThanTwoSalesPageTypeSelections() {
        Experiment experiment = Experiment.builder().id(64L).build();
        given(experimentRepository.findById(64L)).willReturn(Optional.of(experiment));

        assertThatThrownBy(() -> service.replaceExperimentSelections(
                64L,
                new UpdateExperimentSalesPageTypeSelectionRequest(List.of(
                        new UpdateExperimentSalesPageTypeSelectionItem(
                                "TRADITIONAL_LONG_FORM", "A", BigDecimal.TEN, true, null),
                        new UpdateExperimentSalesPageTypeSelectionItem(
                                "HUMAN_VIDEO_SALES_PAGE", "B", BigDecimal.TEN, true, null),
                        new UpdateExperimentSalesPageTypeSelectionItem(
                                "AI_CHAT_DIGITAL_BAIT", "C", BigDecimal.TEN, true, null)))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("at most 2 variants");
    }

    /** Cria o tipo chat IA usado nos testes. */
    private SalesPageType chatType() {
        return SalesPageType.builder()
                .code("AI_CHAT_DIGITAL_BAIT")
                .name("Chat IA com isca imediata")
                .description("Experiencia chat-first com coleta de dados e entrega de isca.")
                .commercialMechanism("Gera valor percebido antes da venda.")
                .leadCaptureStrategy("Coleta dados em conversa curta.")
                .digitalBaitDelivery("Entrega imagem personalizada diretamente no chat.")
                .defaultForAbTest(true)
                .active(true)
                .build();
    }
}

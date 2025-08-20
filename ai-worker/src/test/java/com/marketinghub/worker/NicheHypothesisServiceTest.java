package com.marketinghub.worker;

import com.marketinghub.creative.label.Angle;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.OfferType;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NicheHypothesisServiceTest {

    @Mock
    MarketNicheRepository nicheRepository;
    @Mock
    HypothesisRepository hypothesisRepository;
    @Mock
    HypothesisChatGptClient chatGptClient;

    @InjectMocks
    NicheHypothesisService service;

    @Test
    void generateHypothesesSavesResults() {
        MarketNiche niche = MarketNiche.builder().id(1L).name("Niche").hypothesesToGenerate(2).build();
        when(nicheRepository.findAll()).thenReturn(List.of(niche));

        Hypothesis h1 = baseHypothesis("h1");
        Hypothesis h2 = baseHypothesis("h2");
        when(chatGptClient.generate(niche, 2)).thenReturn(List.of(h1, h2));

        service.generateHypotheses();

        ArgumentCaptor<Hypothesis> captor = ArgumentCaptor.forClass(Hypothesis.class);
        verify(hypothesisRepository, times(2)).save(captor.capture());
        List<Hypothesis> saved = captor.getAllValues();
        assertThat(saved).hasSize(2);
        assertThat(saved).allMatch(h -> h.getMarketNiche().equals(niche));
    }

    private Hypothesis baseHypothesis(String title) {
        return Hypothesis.builder()
                .title(title)
                .premiseAngle(Angle.builder().id(1L).name("a").build())
                .promise("p")
                .problem("pr")
                .persona("pe")
                .offerType(OfferType.LEAD)
                .kpiTargetCpl(BigDecimal.ONE)
                .build();
    }
}


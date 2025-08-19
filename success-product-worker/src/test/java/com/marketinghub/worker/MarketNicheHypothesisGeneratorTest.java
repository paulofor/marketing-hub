package com.marketinghub.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.marketinghub.creative.label.Angle;
import com.marketinghub.creative.label.repository.AngleRepository;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.OfferType;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketNicheHypothesisGeneratorTest {

    @Mock
    MarketNicheRepository nicheRepository;
    @Mock
    HypothesisRepository hypothesisRepository;
    @Mock
    HypothesisChatGptClient client;
    @Mock
    AngleRepository angleRepository;

    @Test
    void generatesHypothesesAndResetsCounter() {
        MarketNiche niche = MarketNiche.builder().id(1L).name("Saúde").hypothesesToGenerate(2).build();
        when(nicheRepository.findAll()).thenReturn(List.of(niche));
        Hypothesis h1 = Hypothesis.builder()
                .title("A")
                .promise("p1")
                .problem("pr1")
                .persona("pe1")
                .offerType(OfferType.LEAD)
                .price(BigDecimal.ONE)
                .kpiTargetCpl(BigDecimal.TEN)
                .build();
        Hypothesis h2 = Hypothesis.builder()
                .title("B")
                .promise("p2")
                .problem("pr2")
                .persona("pe2")
                .offerType(OfferType.TRIPWIRE)
                .price(BigDecimal.ONE)
                .kpiTargetCpl(BigDecimal.TEN)
                .build();
        when(client.generate(niche, 2)).thenReturn(List.of(h1, h2));
        Angle angle = Angle.builder().id(5L).name("Default").build();
        when(angleRepository.findAll()).thenReturn(List.of(angle));

        MarketNicheHypothesisGenerator generator =
                new MarketNicheHypothesisGenerator(
                        nicheRepository, hypothesisRepository, client, angleRepository);
        generator.generateForNiches();

        ArgumentCaptor<Hypothesis> captor = ArgumentCaptor.forClass(Hypothesis.class);
        verify(hypothesisRepository, times(2)).save(captor.capture());
        for (Hypothesis h : captor.getAllValues()) {
            assertThat(h.getMarketNiche()).isEqualTo(niche);
            assertThat(h.getPremiseAngle()).isEqualTo(angle);
        }
        assertThat(niche.getHypothesesToGenerate()).isZero();
        verify(nicheRepository).save(niche);
    }
}

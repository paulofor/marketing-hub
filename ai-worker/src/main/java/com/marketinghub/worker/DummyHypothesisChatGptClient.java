package com.marketinghub.worker;

import com.marketinghub.creative.label.Angle;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.OfferType;
import com.marketinghub.niche.MarketNiche;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("dummy")
public class DummyHypothesisChatGptClient implements HypothesisChatGptClient {
    @Override
    public List<Hypothesis> generate(MarketNiche niche, int quantity) {
        List<Hypothesis> results = new ArrayList<>();
        for (int i = 1; i <= quantity; i++) {
            results.add(
                    Hypothesis.builder()
                            .title("Hipotese " + i + " for " + niche.getName())
                            .premiseAngle(Angle.builder().id(1L).name("Default Angle").build())
                            .promise("Promise " + i)
                            .problem("Problem " + i)
                            .persona("Persona " + i)
                            .offerType(OfferType.LEAD)
                            .kpiTargetCpl(BigDecimal.ONE)
                            .build()
            );
        }
        return results;
    }
}


package com.marketinghub.worker;

import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.OfferType;
import com.marketinghub.niche.MarketNiche;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dummy")
public class DummyHypothesisChatGptClient implements HypothesisChatGptClient {
    private static final Logger log = LoggerFactory.getLogger(DummyHypothesisChatGptClient.class);

    @Override
    public List<Hypothesis> generate(MarketNiche niche, int quantity) {
        log.debug("Generating {} dummy hypotheses for niche {}", quantity, niche.getId());
        List<Hypothesis> list = new ArrayList<>();
        for (int i = 1; i <= quantity; i++) {
            list.add(Hypothesis.builder()
                    .title("Hypothesis " + i)
                    .promise("Promise " + i)
                    .problem("Problem " + i)
                    .persona("Persona " + i)
                    .offerType(OfferType.LEAD)
                    .price(BigDecimal.ZERO)
                    .kpiTargetCpl(BigDecimal.ONE)
                    .build());
        }
        return list;
    }
}

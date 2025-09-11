package com.marketinghub.worker.successproduct;

import com.marketinghub.successproduct.SuccessProduct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Dummy implementation used in tests to avoid real API calls.
 */
@Component
@Profile({"test", "dummy"})
public class DummyChatGptClient implements ChatGptClient {
    @Override
    public NicheHypothesis extract(SuccessProduct product) {
        return new NicheHypothesis(
                "Saude",
                "Nicho de saude",
                "Hipotese A",
                "Persona A",
                "Problema A",
                "Promessa A",
                "Mecanismo A");
    }
}

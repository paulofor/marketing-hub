package com.marketinghub.worker.successproduct;

import com.marketinghub.successproduct.SuccessProduct;

/**
 * Client for querying ChatGPT to extract market niche and hypothesis
 * information from a {@link SuccessProduct} description.
 */
public interface ChatGptClient {
    NicheHypothesis extract(SuccessProduct product);

    /**
     * Combined result containing the generated niche and hypothesis data.
     */
    record NicheHypothesis(
            String nicheName,
            String nicheDescription,
            String hypothesisTitle,
            String persona,
            String problem,
            String promise,
            String uniqueMechanism) {}
}

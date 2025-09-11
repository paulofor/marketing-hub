package com.marketinghub.worker.successproduct;

import com.marketinghub.niche.dto.CreateMarketNicheRequest;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.service.MarketNicheService;
import com.marketinghub.hypothesis.dto.CreateHypothesisRequest;
import com.marketinghub.hypothesis.service.HypothesisService;
import com.marketinghub.worker.WorkerSuccessProductRepository;
import com.marketinghub.successproduct.SuccessProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Generates {@link com.marketinghub.niche.MarketNiche} and
 * {@link com.marketinghub.hypothesis.Hypothesis} entities from existing
 * {@link SuccessProduct} records.
 *
 * <p>This performs a simple reverse engineering by copying relevant fields
 * from the success product to the new entities.</p>
 */
@Service
public class SuccessProductNicheHypothesisService {
    private static final Logger log = LoggerFactory.getLogger(SuccessProductNicheHypothesisService.class);
    private final WorkerSuccessProductRepository productRepository;
    private final MarketNicheService marketNicheService;
    private final HypothesisService hypothesisService;

    public SuccessProductNicheHypothesisService(WorkerSuccessProductRepository productRepository,
                                               MarketNicheService marketNicheService,
                                               HypothesisService hypothesisService) {
        this.productRepository = productRepository;
        this.marketNicheService = marketNicheService;
        this.hypothesisService = hypothesisService;
    }

    /**
     * Process all success products marked as not new and create corresponding
     * niche and hypothesis entries.
     */
    @Transactional
    public void generate() {
        List<SuccessProduct> products = productRepository.findByNovoFalse();
        log.info("Processing {} success products for niche/hypothesis generation", products.size());
        for (SuccessProduct product : products) {
            if (product.getNiche() == null || product.getNiche().isBlank() ||
                product.getName() == null || product.getName().isBlank()) {
                log.debug("Skipping product {} due to missing niche or name", product.getId());
                continue;
            }
            CreateMarketNicheRequest nicheReq = new CreateMarketNicheRequest();
            nicheReq.setName(product.getNiche());
            nicheReq.setDescription(product.getAvatar());
            MarketNiche niche = marketNicheService.create(nicheReq);

            CreateHypothesisRequest hypReq = new CreateHypothesisRequest();
            hypReq.setMarketNicheId(niche.getId());
            hypReq.setTitle(product.getName());
            hypReq.setPersona(product.getAvatar());
            hypReq.setProblem(product.getExplicitPain());
            hypReq.setPromise(product.getPromise());
            hypReq.setUniqueMechanism(product.getUniqueMechanism());
            hypothesisService.create(hypReq);
        }
    }
}


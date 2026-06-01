package com.marketinghub.worker;

import com.marketinghub.successproduct.SuccessProduct;
import com.marketinghub.repository.jpa.successproduct.SuccessProductRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Worker-specific repository for {@link SuccessProduct} queries.
 *
 * <p>Extends the shared {@link SuccessProductRepository} from the backend and
 * adds convenience methods used only by the AI Worker.</p>
 */
@Primary
@Repository
public interface WorkerSuccessProductRepository extends SuccessProductRepository {
    /**
     * Retrieves products marked as new in the database.
     */
    List<SuccessProduct> findByNovoTrue();

    /**
     * Retrieves products already processed (novo = false).
     */
    List<SuccessProduct> findByNovoFalse();

    /**
     * Retrieves products flagged for niche/hypothesis generation.
     */
    List<SuccessProduct> findByGenerateNicheHypothesisTrue();
}

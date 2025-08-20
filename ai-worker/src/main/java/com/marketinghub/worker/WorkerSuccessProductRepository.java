package com.marketinghub.worker;

import com.marketinghub.successproduct.SuccessProduct;
import com.marketinghub.successproduct.repository.SuccessProductRepository;

import java.util.List;

/**
 * Worker-specific repository for {@link SuccessProduct} queries.
 *
 * <p>Extends the shared {@link SuccessProductRepository} from the backend and
 * adds convenience methods used only by the AI Worker.</p>
 */
public interface WorkerSuccessProductRepository extends SuccessProductRepository {
    /**
     * Retrieves products marked as new in the database.
     */
    List<SuccessProduct> findByNovoTrue();
}

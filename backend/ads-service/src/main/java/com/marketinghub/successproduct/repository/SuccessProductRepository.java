package com.marketinghub.successproduct.repository;

import com.marketinghub.successproduct.SuccessProduct;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for {@link SuccessProduct} entities.
 */
public interface SuccessProductRepository extends JpaRepository<SuccessProduct, Long> {
}

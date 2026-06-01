package com.marketinghub.repository.jpa.product;

import com.marketinghub.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for {@link Product} entities.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {
}

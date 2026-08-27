package com.marketinghub.repository.jpa.product;

import com.marketinghub.product.ProductAutomaticExecutionControlEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir a trilha append-only das mudanças PLAY/STOP dos produtos. */
public interface ProductAutomaticExecutionControlEventRepository
    extends JpaRepository<ProductAutomaticExecutionControlEvent, Long> {}

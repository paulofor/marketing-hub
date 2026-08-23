package com.marketinghub.repository.jpa.producttype;

import com.marketinghub.producttype.ProductTypeDefinition;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir e listar as definições do catálogo de tipos de produto. */
public interface ProductTypeDefinitionRepository
    extends JpaRepository<ProductTypeDefinition, Long> {
  /** Lista o catálogo em ordem estável e legível. */
  List<ProductTypeDefinition> findAllByOrderByNameAsc();
}

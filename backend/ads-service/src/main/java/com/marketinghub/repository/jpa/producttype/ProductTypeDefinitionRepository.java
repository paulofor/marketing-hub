package com.marketinghub.repository.jpa.producttype;

import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.producttype.ProductTypeStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir e listar as definições do catálogo de tipos de produto. */
public interface ProductTypeDefinitionRepository
    extends JpaRepository<ProductTypeDefinition, Long> {
  /** Serializa revisões de modelos financeiros para o mesmo tipo sem sobrescrever histórico. */
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query(
      "SELECT t FROM ProductTypeDefinition t WHERE t.id=:id")
  Optional<ProductTypeDefinition> findLockedById(
      @org.springframework.data.repository.query.Param("id") Long id);

  /** Lista o catálogo em ordem estável e legível. */
  List<ProductTypeDefinition> findAllByOrderByNameAsc();

  /** Lista somente os tipos que Atena pode escolher para um produto novo. */
  List<ProductTypeDefinition> findAllByStatusOrderByNameAsc(ProductTypeStatus status);

  /** Localiza um tipo pelo código estável usado pelas materializações automáticas. */
  Optional<ProductTypeDefinition> findByCode(String code);
}

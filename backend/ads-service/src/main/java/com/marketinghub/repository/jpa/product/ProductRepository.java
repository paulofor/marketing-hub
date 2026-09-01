package com.marketinghub.repository.jpa.product;

import com.marketinghub.product.Product;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositório JPA responsável pela persistência de produtos digitais. */
public interface ProductRepository extends JpaRepository<Product, Long> {
  /** Bloqueia o produto durante a troca atômica do estado operacional PLAY/STOP. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT product FROM Product product WHERE product.id = :productId")
  Optional<Product> findLockedById(@Param("productId") Long productId);

  /** Busca um produto pelo slug comercial público. */
  Optional<Product> findBySlug(String slug);

  /** Projeta a identidade e o estado atual sem carregar os campos comerciais extensos. */
  @Query(
      """
      SELECT new com.marketinghub.repository.jpa.product.ProductValueChainSummaryProduct(
        product.id, product.name, product.internalName, product.commercialStatus)
      FROM Product product
      WHERE product.id = :productId
      """)
  Optional<ProductValueChainSummaryProduct> findValueChainSummaryById(
      @Param("productId") Long productId);

  /** Busca o produto operacional mais recente vinculado ao nicho informado. */
  Optional<Product> findFirstByMarketNiche_IdOrderByCreatedAtDesc(Long marketNicheId);

  /** Lista produtos do nicho para impedir que agentes misturem mapas quando houver ambiguidade. */
  List<Product> findAllByMarketNiche_Id(Long marketNicheId);

  /** Lista produtos vinculados a um tipo para manter o nome legado sincronizado. */
  List<Product> findAllByProductTypeDefinition_Id(Long productTypeId);

  /** Conta produtos classificados por um tipo sem recomputação no frontend. */
  long countByProductTypeDefinition_Id(Long productTypeId);

  /** Pesquisa o catálogo por nome comercial, nome interno, apelido ou slug. */
  @Query(
      """
      SELECT DISTINCT product
      FROM Product product
      LEFT JOIN product.aliases alias
      WHERE LOWER(COALESCE(product.name, '')) LIKE LOWER(CONCAT('%', :identityQuery, '%'))
         OR LOWER(COALESCE(product.internalName, '')) LIKE LOWER(CONCAT('%', :identityQuery, '%'))
         OR LOWER(COALESCE(product.slug, '')) LIKE LOWER(CONCAT('%', :identityQuery, '%'))
         OR LOWER(COALESCE(alias, '')) LIKE LOWER(CONCAT('%', :identityQuery, '%'))
      ORDER BY product.updatedAt DESC, product.id DESC
      """)
  List<Product> searchByIdentity(@Param("identityQuery") String identityQuery);

  /** Lista produtos em PLAY, incluindo legados sem decisão persistida que equivalem a PLAY. */
  @Query(
      """
      SELECT product
      FROM Product product
      WHERE product.automaticExecutionEnabled IS NULL OR product.automaticExecutionEnabled = TRUE
      ORDER BY product.updatedAt DESC, product.id DESC
      """)
  List<Product> findAllInPlayState();

  /** Pesquisa produtos em PLAY sem expor itens em STOP na visão operacional. */
  @Query(
      """
      SELECT DISTINCT product
      FROM Product product
      LEFT JOIN product.aliases alias
      WHERE (product.automaticExecutionEnabled IS NULL OR product.automaticExecutionEnabled = TRUE)
        AND (
          LOWER(COALESCE(product.name, '')) LIKE LOWER(CONCAT('%', :identityQuery, '%'))
          OR LOWER(COALESCE(product.internalName, '')) LIKE LOWER(CONCAT('%', :identityQuery, '%'))
          OR LOWER(COALESCE(product.slug, '')) LIKE LOWER(CONCAT('%', :identityQuery, '%'))
          OR LOWER(COALESCE(alias, '')) LIKE LOWER(CONCAT('%', :identityQuery, '%'))
        )
      ORDER BY product.updatedAt DESC, product.id DESC
      """)
  List<Product> searchByIdentityInPlayState(@Param("identityQuery") String identityQuery);

  /** Conta conflitos exatos de identidade em outro produto do catálogo. */
  @Query(
      """
      SELECT COUNT(DISTINCT product.id)
      FROM Product product
      LEFT JOIN product.aliases alias
      WHERE (:productId IS NULL OR product.id <> :productId)
        AND (
          LOWER(COALESCE(product.name, '')) = LOWER(:identity)
          OR LOWER(COALESCE(product.internalName, '')) = LOWER(:identity)
          OR LOWER(COALESCE(product.slug, '')) = LOWER(:identity)
          OR LOWER(COALESCE(alias, '')) = LOWER(:identity)
        )
      """)
  long countIdentityOnAnotherProduct(
      @Param("productId") Long productId, @Param("identity") String identity);

  /** Atualiza somente o nome interno sem regravar os demais campos comerciais do produto. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      UPDATE Product product
         SET product.internalName = :internalName
       WHERE product.id = :productId
      """)
  int updateInternalName(
      @Param("productId") Long productId, @Param("internalName") String internalName);
}

package com.marketinghub.repository.jpa.product;

import com.marketinghub.product.ProductProcessCommit;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: persistir e consultar commits vinculados a produtos e processos. */
public interface ProductProcessCommitRepository extends JpaRepository<ProductProcessCommit, Long> {
  /** Lista o histórico de commits de um produto do registro mais recente para o mais antigo. */
  List<ProductProcessCommit> findByProductIdOrderByRecordedAtDescIdDesc(Long productId);

  /** Localiza um commit sem permitir leitura cruzada entre produtos. */
  Optional<ProductProcessCommit> findByIdAndProductId(Long id, Long productId);

  /** Serializa registros concorrentes do mesmo produto para preservar idempotência. */
  @Query(value = "SELECT id FROM product WHERE id = :productId FOR UPDATE", nativeQuery = true)
  Long lockProductForCommitRegistration(@Param("productId") Long productId);

  /** Localiza um vínculo idempotente pelo produto, processo, repositório e SHA. */
  Optional<ProductProcessCommit> findByProductIdAndProcessDefinitionIdAndRepositoryNameAndCommitSha(
      Long productId, Long processDefinitionId, String repositoryName, String commitSha);
}

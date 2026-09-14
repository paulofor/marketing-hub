package com.marketinghub.repository.jpa.productexecution;

import com.marketinghub.product.executionprofile.v1.ExecutionProfileBinding;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: localizar vínculos imutáveis de ficha por produto e referência de execução. */
public interface ExecutionProfileBindingRepository
    extends JpaRepository<ExecutionProfileBinding, Long> {
  /** Consulta o vínculo exato, impedindo herança da revisão mais recente por engano. */
  Optional<ExecutionProfileBinding> findByProductIdAndSourceReference(
      Long productId, String reference);

  /** Localiza a referência canônica global, cuja propriedade foi validada ao vincular. */
  Optional<ExecutionProfileBinding> findBySourceReference(String sourceReference);

  /** Lista as referências que adotaram uma determinada ficha. */
  List<ExecutionProfileBinding> findByProfileIdOrderByIdAsc(Long profileId);

  /** Identifica se o produto já adotou o controle de execução versionado. */
  boolean existsByProductId(Long productId);
}

package com.marketinghub.repository.jpa.pde;

import com.marketinghub.pde.paidvisual.v1.PaidVisualOrder;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: persistir direitos pagos e serializar alterações do mesmo pedido. */
public interface PaidVisualOrderRepository extends JpaRepository<PaidVisualOrder, String> {
  /** Reserva o pedido para callback, acesso ou geração idempotente. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from PaidVisualOrder o where o.paymentId = :id")
  Optional<PaidVisualOrder> lockById(@Param("id") String id);

  /** Localiza somente o direito associado ao bearer opaco. */
  Optional<PaidVisualOrder> findByAccessHash(String accessHash);

  /** Localiza a última compra da mesma pessoa e oferta sem expor existência ao navegador. */
  Optional<PaidVisualOrder> findFirstByExperimentIdAndBuyerEmailOrderByPaidAtDesc(
      Long experimentId, String email);
}

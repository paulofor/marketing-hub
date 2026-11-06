package com.marketinghub.repository.jpa.learningcycle;

import com.marketinghub.businessprocesschain.learningcycle.v1.SingleVideoSpend;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/** Responsabilidade: consultar reservas vinculadas ao recibo sem perder estados incertos. */
public interface SingleVideoSpendRepository extends JpaRepository<SingleVideoSpend, Long> {
  /** Lista o consumo do mesmo teto em todos os executores e ciclos de produção. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<SingleVideoSpend> findByReceiptIdOrderByIdAsc(Long receiptId);

  /** Lê relatório sem lock de escrita, inclusive em transação administrativa somente leitura. */
  @org.springframework.data.jpa.repository.Query(
      "select s from SingleVideoSpend s where s.receiptId = :receiptId order by s.id")
  List<SingleVideoSpend> report(
      @org.springframework.data.repository.query.Param("receiptId") Long receiptId);

  /** Reutiliza a operação original, impedindo nova chamada em uma retomada. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<SingleVideoSpend> findByReceiptIdAndOperationKey(Long receiptId, String operationKey);
}

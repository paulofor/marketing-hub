package com.marketinghub.repository.jpa.productexecution;

import com.marketinghub.product.executionprofile.v1.ExecutionProfileConsumption;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: consultar reservas e custos segregados por vínculo e unidade de entrega. */
public interface ExecutionProfileConsumptionRepository
    extends JpaRepository<ExecutionProfileConsumption, Long> {
  /** Recupera a operação para impedir execução externa repetida. */
  Optional<ExecutionProfileConsumption> findByBindingIdAndOperationKey(Long bindingId, String key);

  /** Filtra o pacote e o ambiente antes de calcular consumo. */
  List<ExecutionProfileConsumption> findByBindingIdAndUsageKeyAndTestData(
      Long bindingId, String key, boolean testData);

  /** Soma toda a produção privada do vínculo, impedindo multiplicar o teto por pacote. */
  List<ExecutionProfileConsumption> findByBindingIdAndTestData(Long bindingId, boolean testData);

  /** Lista auditorias do vínculo para o relatório administrativo. */
  List<ExecutionProfileConsumption> findByBindingIdOrderByIdAsc(Long bindingId);

  /** Impede multiplicar um sobrecusto em novos pacotes da mesma operação. */
  boolean existsByBindingIdAndTestDataAndStatusIn(
      Long bindingId, boolean testData, java.util.Collection<String> statuses);
}

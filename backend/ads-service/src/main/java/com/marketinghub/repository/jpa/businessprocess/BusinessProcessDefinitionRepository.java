package com.marketinghub.repository.jpa.businessprocess;

import com.marketinghub.businessprocess.BusinessProcessDefinition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir e consultar as versões dos processos de negócio. */
public interface BusinessProcessDefinitionRepository
    extends JpaRepository<BusinessProcessDefinition, Long> {
  /** Lista o catálogo ordenado por processo e versão mais recente. */
  List<BusinessProcessDefinition> findAllByOrderByNameAscVersionNumberDesc();

  /** Localiza uma versão específica para impedir duplicidade. */
  Optional<BusinessProcessDefinition> findByProcessCodeAndVersionNumber(
      String processCode, Integer versionNumber);

  /** Lista versões do mesmo processo para governar a publicação. */
  List<BusinessProcessDefinition> findAllByProcessCodeOrderByVersionNumberDesc(String processCode);

  /** Localiza a versão vigente de um processo pai para validar composição sem ambiguidade. */
  Optional<BusinessProcessDefinition> findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
      String processCode, String status);

  /** Lista os subprocessos vigentes de um processo de valor para expor sua composição oficial. */
  List<BusinessProcessDefinition>
      findAllByParentProcessCodeAndStatusOrderByNameAscVersionNumberDesc(
          String parentProcessCode, String status);

  /** Lista processos publicados por escopos aceitos no cockpit de execução independente. */
  List<BusinessProcessDefinition> findAllByStatusAndExecutionScopeInOrderByNameAscVersionNumberDesc(
      String status, List<String> executionScopes);
}

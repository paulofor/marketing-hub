package com.marketinghub.businessprocesscomposition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesscomposition.service.BusinessProcessCompositionService;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: comprovar a leitura oficial da hierarquia de processos e subprocessos. */
class BusinessProcessCompositionServiceTest {

  /** Lista subprocessos publicados com contagem e dados suficientes para explicar a composição. */
  @Test
  void returnsPublishedSubprocessesForValueProcess() {
    var repository = mock(BusinessProcessDefinitionRepository.class);
    BusinessProcessDefinition parent =
        process(40L, "communication", "Comunicação", "VALUE_PROCESS");
    BusinessProcessDefinition creative =
        process(17L, "creative-production", "Criação de criativos", "SUBPROCESS");
    creative.setParentProcessCode("communication");
    BusinessProcessDefinition landing =
        process(18L, "landing-generation", "Geração de landing", "SUBPROCESS");
    landing.setParentProcessCode("communication");
    when(repository.findById(40L)).thenReturn(Optional.of(parent));
    when(repository.findAllByParentProcessCodeAndStatusOrderByNameAscVersionNumberDesc(
            "communication", "PUBLISHED"))
        .thenReturn(List.of(creative, landing));
    var service = new BusinessProcessCompositionService(repository);

    var result = service.getComposition(40L);

    assertThat(result.parentProcess()).isNull();
    assertThat(result.subprocessCount()).isEqualTo(2);
    assertThat(result.subprocesses())
        .extracting(item -> item.name())
        .containsExactly("Criação de criativos", "Geração de landing");
  }

  /** Expõe o pai publicado de um subprocesso sem atribuir filhos a ele. */
  @Test
  void returnsPublishedParentForSubprocess() {
    var repository = mock(BusinessProcessDefinitionRepository.class);
    BusinessProcessDefinition parent =
        process(40L, "communication", "Comunicação", "VALUE_PROCESS");
    BusinessProcessDefinition child =
        process(17L, "creative-production", "Criação de criativos", "SUBPROCESS");
    child.setParentProcessCode("communication");
    when(repository.findById(17L)).thenReturn(Optional.of(child));
    when(repository.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            "communication", "PUBLISHED"))
        .thenReturn(Optional.of(parent));
    var service = new BusinessProcessCompositionService(repository);

    var result = service.getComposition(17L);

    assertThat(result.parentProcess().name()).isEqualTo("Comunicação");
    assertThat(result.subprocessCount()).isZero();
    assertThat(result.subprocesses()).isEmpty();
  }

  /** Responde 404 quando a definição solicitada não existe. */
  @Test
  void rejectsUnknownProcess() {
    var repository = mock(BusinessProcessDefinitionRepository.class);
    when(repository.findById(99L)).thenReturn(Optional.empty());
    var service = new BusinessProcessCompositionService(repository);

    assertThatThrownBy(() -> service.getComposition(99L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Processo não encontrado");
  }

  /** Monta a menor definição publicada necessária aos cenários de composição. */
  private BusinessProcessDefinition process(Long id, String code, String name, String type) {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(id);
    process.setProcessCode(code);
    process.setName(name);
    process.setPurpose("Entregar valor especializado.");
    process.setOwnerName("Operação");
    process.setVersionNumber(1);
    process.setStatus("PUBLISHED");
    process.setProcessType(type);
    return process;
  }
}

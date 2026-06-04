package com.marketinghub.oprm.nichocnae.signalextractor.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.oprm.nichocnae.signalextractor.service.BackendSignalExtractorService;
import com.marketinghub.oprm.nichocnae.signalextractor.service.pending.RecordSignalExtractorPending;
import com.marketinghub.oprm.nichocnae.signalextractor.web.BackendSignalExtractorController;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmExtractedSignalRepository;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Valida a aderência arquitetural da etapa cinco OPRM NichoCNAE ao padrão backend por etapa. */
class OprmSignalExtractorArchitectureTest {
  private static final String WEB_PACKAGE = "com.marketinghub.oprm.nichocnae.signalextractor.web";
  private static final String SERVICE_PACKAGE = "com.marketinghub.oprm.nichocnae.signalextractor.service";
  private static final String REPOSITORY_PACKAGE = "com.marketinghub.repository.jpa.oprm.nichocnae";

  /** Garante que o controller da etapa cinco fica no pacote web direto e expõe a fila pending canônica. */
  @Test
  void controllerShouldFollowBackendStageContract() throws NoSuchMethodException {
    Class<BackendSignalExtractorController> controller = BackendSignalExtractorController.class;

    assertThat(controller.getPackageName())
        .as("[ARQUITETURA] BackendSignalExtractorController deve ficar no pacote web direto da etapa")
        .isEqualTo(WEB_PACKAGE);
    assertThat(controller.isAnnotationPresent(RestController.class))
        .as("[ARQUITETURA] BackendSignalExtractorController deve possuir @RestController")
        .isTrue();
    assertThat(controller.getAnnotation(RequestMapping.class).value())
        .as("[ARQUITETURA] BackendSignalExtractorController deve possuir @RequestMapping(\"/api\")")
        .containsExactly("/api");

    Type pendingReturnType = controller.getMethod("pending").getGenericReturnType();
    assertThat(pendingReturnType)
        .as("[ARQUITETURA] pending deve retornar List<RecordSignalExtractorPending>")
        .isInstanceOf(ParameterizedType.class);
    ParameterizedType parameterizedType = (ParameterizedType) pendingReturnType;
    assertThat(parameterizedType.getRawType())
        .as("[ARQUITETURA] pending deve retornar List<RecordSignalExtractorPending>")
        .isEqualTo(List.class);
    assertThat(parameterizedType.getActualTypeArguments())
        .as("[ARQUITETURA] pending deve retornar List<RecordSignalExtractorPending>")
        .containsExactly(RecordSignalExtractorPending.class);
  }

  /** Garante que o service canônico da etapa cinco é único na raiz service e anotado como serviço Spring. */
  @Test
  void serviceShouldFollowBackendStageContract() {
    Class<BackendSignalExtractorService> service = BackendSignalExtractorService.class;

    assertThat(service.getPackageName())
        .as("[ARQUITETURA] BackendSignalExtractorService deve ficar no pacote service direto da etapa")
        .isEqualTo(SERVICE_PACKAGE);
    assertThat(service.isAnnotationPresent(Service.class))
        .as("[ARQUITETURA] BackendSignalExtractorService deve possuir @Service")
        .isTrue();
  }

  /** Garante que todos os DTOs de borda da etapa cinco são records imutáveis por operação. */
  @Test
  void serviceOperationDtosShouldBeRecords() {
    List<Class<?>> operationDtos = List.of(
        com.marketinghub.oprm.nichocnae.signalextractor.service.completeStageExecution.CompleteSignalExtractorRequest.class,
        com.marketinghub.oprm.nichocnae.signalextractor.service.completeStageExecution.CompleteSignalExtractorResponse.class,
        com.marketinghub.oprm.nichocnae.signalextractor.service.completeStageExecution.ExtractedSignalResponse.class,
        com.marketinghub.oprm.nichocnae.signalextractor.service.completeStageExecution.SignalExtractionItemRequest.class,
        com.marketinghub.oprm.nichocnae.signalextractor.service.detailStageExecution.SignalExtractorDetailResponse.class,
        com.marketinghub.oprm.nichocnae.signalextractor.service.failStageExecution.FailSignalExtractorRequest.class,
        RecordSignalExtractorPending.class);

    assertThat(operationDtos)
        .as("[ARQUITETURA] DTOs de service da etapa cinco devem ser records")
        .allSatisfy(dto -> assertThat(dto.isRecord())
            .as("[ARQUITETURA] DTO de borda deve ser record: " + dto.getName())
            .isTrue());
  }

  /** Garante que a persistência da etapa cinco permanece centralizada em com.marketinghub.repository.jpa. */
  @Test
  void repositoryShouldRemainInCanonicalJpaRepositoryPackage() {
    assertThat(OprmExtractedSignalRepository.class.getPackageName())
        .as("[ARQUITETURA] OprmExtractedSignalRepository deve ficar em com.marketinghub.repository.jpa")
        .isEqualTo(REPOSITORY_PACKAGE);
    assertThat(JpaRepository.class.isAssignableFrom(OprmExtractedSignalRepository.class))
        .as("[ARQUITETURA] OprmExtractedSignalRepository deve ser o repository JPA canônico da etapa")
        .isTrue();
  }
}

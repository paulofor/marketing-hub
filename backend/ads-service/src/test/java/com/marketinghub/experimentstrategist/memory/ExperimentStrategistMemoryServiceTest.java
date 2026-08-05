package com.marketinghub.experimentstrategist.memory;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experimentstrategist.memory.ExperimentStrategistMemoryService.CreateMemoryRequest;
import com.marketinghub.repository.jpa.experimentstrategist.ExperimentStrategistMemoryArtifactRepository;
import com.marketinghub.repository.jpa.experimentstrategist.ExperimentStrategistMemoryRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;

/** Responsabilidade: validar gates de evidencia da memoria comportamental. */
class ExperimentStrategistMemoryServiceTest {
  /** Impede confirmar uma explicacao causal usando apenas observacao. */
  @Test
  void shouldRejectConfirmationWithoutSubsequentResult() {
    ExperimentStrategistMemoryService service =
        new ExperimentStrategistMemoryService(
            mock(ExperimentStrategistMemoryRepository.class),
            mock(ExperimentStrategistMemoryArtifactRepository.class),
            new ExperimentStrategistMemoryProperties(),
            new ExperimentStrategistAnonymizer(),
            new ObjectMapper(),
            mock(S3Client.class));
    CreateMemoryRequest request =
        new CreateMemoryRequest(
            1L,
            null,
            "UNCERTAINTY",
            "A demora sugere incerteza.",
            "OBSERVATION",
            "LOW",
            "CONFIRMED",
            List.of(),
            null,
            180);

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Confirmacao exige resultado humano ou comercial");
  }
}

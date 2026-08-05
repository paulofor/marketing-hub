package com.marketinghub.customeragent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.marketinghub.customeragent.CustomerAgentMemoryMotivation;
import com.marketinghub.customeragent.CustomerDigitalObservation;
import com.marketinghub.customeragent.CustomerPersona;
import com.marketinghub.repository.jpa.customeragent.CustomerAgentMemoryMotivationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: proteger escala e separacao de origem dos vetores motivacionais. */
@ExtendWith(MockitoExtension.class)
class CustomerAgentMotivationServiceTest {
  @Mock private CustomerAgentMemoryMotivationRepository repository;
  private CustomerAgentMotivationService service;
  private CustomerDigitalObservation observation;

  /** Prepara uma observacao auditavel e persistencia isolada. */
  @BeforeEach
  void setUp() {
    service = new CustomerAgentMotivationService(repository);
    CustomerPersona persona = new CustomerPersona();
    persona.setId(7L);
    observation = new CustomerDigitalObservation();
    observation.setId(11L);
    observation.setPersona(persona);
    lenient()
        .when(repository.save(any()))
        .thenAnswer(
            invocation -> {
              CustomerAgentMemoryMotivation value = invocation.getArgument(0);
              value.setId(13L);
              return value;
            });
  }

  /** Confirma que a hipotese simulada nunca nasce como aprendizado humano. */
  @Test
  void recordsSimulatedVectorWithExplicitOrigin() {
    var response = service.recordSimulated(observation, validVector());

    assertThat(response.originType()).isEqualTo("SIMULATED_HYPOTHESIS");
    assertThat(response.motivationalDirection()).isEqualTo("MIXED");
    assertThat(response.painIntensity()).isEqualTo(4);
    assertThat(response.pleasureIntensity()).isEqualTo(3);
  }

  /** Confirma que resultado humano cria origem separada da simulacao. */
  @Test
  void recordsHumanVectorWithConfirmedOrigin() {
    var response = service.recordHumanConfirmed(observation, validVector());

    assertThat(response.originType()).isEqualTo("HUMAN_CONFIRMED");
  }

  /** Rejeita falsa precisao fora da escala canônica. */
  @Test
  void rejectsWeightOutsideZeroToFive() {
    var invalid =
        new CustomerAgentContracts.MotivationalVectorRequest(
            "MIXED", 6, 3, 2, 4, 3, 2, 3, 2, 1, 4, 3, "https://example.com", "Trecho");

    assertThatThrownBy(() -> service.recordSimulated(observation, invalid))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Pesos devem estar entre 0 e 5");
  }

  /** Cria um vetor completo com fonte e justificativa verificaveis. */
  private CustomerAgentContracts.MotivationalVectorRequest validVector() {
    return new CustomerAgentContracts.MotivationalVectorRequest(
        "MIXED", 4, 3, 2, 4, 3, 2, 3, 2, 1, 4, 3, "https://example.com", "CTA observado");
  }
}

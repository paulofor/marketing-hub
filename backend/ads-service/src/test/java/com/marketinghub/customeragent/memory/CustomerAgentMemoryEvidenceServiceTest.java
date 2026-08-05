package com.marketinghub.customeragent.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.customeragent.CustomerAgentMemoryEvidence;
import com.marketinghub.customeragent.CustomerPersona;
import com.marketinghub.repository.jpa.customeragent.CustomerAgentMemoryEvidenceRepository;
import com.marketinghub.repository.jpa.customeragent.CustomerDigitalObservationRepository;
import com.marketinghub.repository.jpa.customeragent.CustomerPersonaRepository;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

/** Responsabilidade: validar deduplicacao e persistencia híbrida da memoria do Agente Cliente. */
@ExtendWith(MockitoExtension.class)
class CustomerAgentMemoryEvidenceServiceTest {
  @Mock private S3Client s3;
  @Mock private CustomerAgentMemoryEvidenceRepository evidenceRepository;
  @Mock private CustomerPersonaRepository personaRepository;
  @Mock private CustomerDigitalObservationRepository observationRepository;
  private CustomerAgentMemoryEvidenceService service;

  /** Prepara configuração privada de bucket e dependencias isoladas. */
  @BeforeEach
  void setUp() {
    CustomerAgentMemoryProperties properties = new CustomerAgentMemoryProperties();
    properties.setBucket("customer-agent-test");
    service =
        new CustomerAgentMemoryEvidenceService(
            properties, s3, evidenceRepository, personaRepository, observationRepository);
  }

  /** Confirma upload no S3 e metadados canônicos separados no MySQL. */
  @Test
  void storesEvidenceWithChecksumAndLayer() throws Exception {
    CustomerPersona persona = new CustomerPersona();
    persona.setId(7L);
    when(personaRepository.findById(7L)).thenReturn(Optional.of(persona));
    when(evidenceRepository.findByPersonaIdAndMemoryLayerAndSha256(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(evidenceRepository.save(any()))
        .thenAnswer(
            invocation -> {
              CustomerAgentMemoryEvidence value = invocation.getArgument(0);
              value.setId(9L);
              return value;
            });

    var response =
        service.store(
            7L,
            null,
            "EXTERNAL_OBSERVATION",
            "https://example.com/public",
            new MockMultipartFile(
                "file", "page.html", "text/html", "evidencia".getBytes(StandardCharsets.UTF_8)));

    assertThat(response.id()).isEqualTo(9L);
    assertThat(response.memoryLayer()).isEqualTo("EXTERNAL_OBSERVATION");
    assertThat(response.sha256()).hasSize(64);
    verify(s3)
        .putObject(
            any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class),
            any(software.amazon.awssdk.core.sync.RequestBody.class));
  }

  /** Confirma que conteúdo repetido não gera novo objeto nem novo registro. */
  @Test
  void returnsExistingEvidenceWithoutDuplicateUpload() throws Exception {
    CustomerPersona persona = new CustomerPersona();
    persona.setId(7L);
    CustomerAgentMemoryEvidence existing = new CustomerAgentMemoryEvidence();
    existing.setId(5L);
    existing.setPersona(persona);
    existing.setMemoryLayer("EXTERNAL_OBSERVATION");
    existing.setContentType("text/html");
    existing.setSizeBytes(9L);
    existing.setSha256("a".repeat(64));
    when(evidenceRepository.findByPersonaIdAndMemoryLayerAndSha256(any(), any(), any()))
        .thenReturn(Optional.of(existing));

    var response =
        service.store(
            7L,
            null,
            "EXTERNAL_OBSERVATION",
            null,
            new MockMultipartFile(
                "file", "page.html", "text/html", "evidencia".getBytes(StandardCharsets.UTF_8)));

    assertThat(response.id()).isEqualTo(5L);
    verify(s3, never())
        .putObject(
            any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class),
            any(software.amazon.awssdk.core.sync.RequestBody.class));
    verify(evidenceRepository, never()).save(any());
  }
}

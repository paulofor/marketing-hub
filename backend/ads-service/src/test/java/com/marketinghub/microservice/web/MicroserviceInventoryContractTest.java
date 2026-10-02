package com.marketinghub.microservice.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.microservice.VpsHostInventory;
import com.marketinghub.microservice.exception.service.MicroserviceExceptionService;
import com.marketinghub.microservice.mapper.MicroserviceMapper;
import com.marketinghub.microservice.service.MicroserviceDiscoveryService;
import com.marketinghub.microservice.service.MicroserviceService;
import com.marketinghub.repository.jpa.microservice.VpsHostInventoryRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Responsabilidade: comprovar leitura e edição do inventário pelo contrato HTTP sem VPS ou banco
 * real.
 */
class MicroserviceInventoryContractTest {
  private MockMvc mvc;

  /** Monta controller e serviço reais com persistência isolada e fonte embarcada de deploy. */
  @BeforeEach
  void setUp() {
    VpsHostInventoryRepository repository = mock(VpsHostInventoryRepository.class);
    AtomicReference<VpsHostInventory> saved = new AtomicReference<>();
    when(repository.findByHost("test-vps.example"))
        .thenAnswer(invocation -> Optional.ofNullable(saved.get()));
    when(repository.save(any(VpsHostInventory.class)))
        .thenAnswer(
            invocation -> {
              saved.set(invocation.getArgument(0));
              return saved.get();
            });
    when(repository.findAllByOrderByHostAsc())
        .thenAnswer(invocation -> saved.get() == null ? List.of() : List.of(saved.get()));
    MicroserviceDiscoveryService discovery =
        new MicroserviceDiscoveryService(
            "absent-runtime-compose.yml", "absent-workflows", "/health", repository);
    mvc =
        MockMvcBuilders.standaloneSetup(
                new MicroserviceController(
                    mock(MicroserviceService.class),
                    mock(MicroserviceMapper.class),
                    mock(MicroserviceExceptionService.class),
                    discovery))
            .build();
  }

  /**
   * Deve entregar serviços no endpoint real sem depender do checkout do repositório em produção.
   */
  @Test
  void shouldExposePackagedInventoryThroughHttp() throws Exception {
    mvc.perform(get("/api/microservices/operational-inventory"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.services[?(@.serviceName == 'frontend')].hostPort").value(5173))
        .andExpect(jsonPath("$.deployments").isNotEmpty())
        .andExpect(jsonPath("$.hosts").isNotEmpty());
  }

  /**
   * Deve preservar referência pública e custo desconhecido após gravação, leitura e consolidação.
   */
  @Test
  void shouldKeepUnknownCostAndPublicReferenceAfterEditing() throws Exception {
    mvc.perform(
            put("/api/microservices/operational-inventory/hosts/test-vps.example")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "cpu": "3 vCPU",
                      "memoryGb": 4,
                      "diskGb": 71,
                      "monthlyCostBrl": null,
                      "costEvidence": "Referência pública; contrato não informado",
                      "physicalSpecsEvidence": "Fonte operacional com data"
                    }
                    """))
        .andExpect(status().isOk());
    mvc.perform(get("/api/microservices/operational-inventory/hosts/test-vps.example"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cpu").value("3 vCPU"))
        .andExpect(jsonPath("$.monthlyCostBrl").isEmpty())
        .andExpect(jsonPath("$.costEvidence").value("Referência pública; contrato não informado"));
    mvc.perform(get("/api/microservices/operational-inventory"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hosts[?(@.host == 'test-vps.example')].diskGb").value(71));
  }
}

package com.marketinghub.pde.infrastructure.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.infrastructure.PdeVpsStatus;
import com.marketinghub.pde.infrastructure.service.PdeVpsInfrastructureService;
import com.marketinghub.pde.infrastructure.service.listVps.PdeVpsServerResponse;
import com.marketinghub.pde.infrastructure.service.listVps.PdeVpsSummaryResponse;
import com.marketinghub.pde.infrastructure.service.saveVps.SavePdeVpsServerRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: validar o contrato REST de gestão de VPS dos PDEs. */
@ExtendWith(MockitoExtension.class)
class PdeVpsInfrastructureControllerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private MockMvc mockMvc;

  @Mock private PdeVpsInfrastructureService service;

  /** Monta o controller isolado para validar rotas administrativas de VPS PDE. */
  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new PdeVpsInfrastructureController(service)).build();
  }

  /** Deve listar VPS e custo fixo mensal consolidado dos PDEs. */
  @Test
  void listServers() throws Exception {
    when(service.listServers())
        .thenReturn(
            new PdeVpsSummaryResponse(
                new BigDecimal("49.90"),
                1,
                1,
                List.of(
                    response(
                        10L,
                        "DokeHost PDE principal",
                        "DokeHost",
                        "163.245.200.7",
                        new BigDecimal("49.90"),
                        PdeVpsStatus.ACTIVE))));

    mockMvc
        .perform(get("/api/pde/vps"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalMonthlyCostBrl").value(49.90))
        .andExpect(jsonPath("$.activeServers").value(1))
        .andExpect(jsonPath("$.servers[0].provider").value("DokeHost"))
        .andExpect(jsonPath("$.servers[0].ipAddress").value("163.245.200.7"));
  }

  /** Deve cadastrar VPS nova para controlar custo fixo de produto PDE. */
  @Test
  void createServer() throws Exception {
    SavePdeVpsServerRequest request =
        new SavePdeVpsServerRequest(
            "DokeHost PDE principal",
            "DokeHost",
            "163.245.200.7",
            "VPS Linux",
            "Brasil",
            2,
            4,
            80,
            new BigDecimal("49.90"),
            "metodo-musa-7-dias",
            "production",
            "v6.clubemusa.com.br",
            PdeVpsStatus.ACTIVE,
            "Produção inicial");
    when(service.createServer(any(SavePdeVpsServerRequest.class)))
        .thenReturn(
            response(
                10L,
                "DokeHost PDE principal",
                "DokeHost",
                "163.245.200.7",
                new BigDecimal("49.90"),
                PdeVpsStatus.ACTIVE));

    mockMvc
        .perform(
            post("/api/pde/vps")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(10L))
        .andExpect(jsonPath("$.monthlyCostBrl").value(49.90));
  }

  /** Deve atualizar VPS existente pelo identificador administrativo. */
  @Test
  void updateServer() throws Exception {
    SavePdeVpsServerRequest request =
        new SavePdeVpsServerRequest(
            "Locaweb PDE escala",
            "Locaweb",
            "191.252.181.168",
            "Cloud Large",
            "Brasil",
            2,
            8,
            160,
            new BigDecimal("160.00"),
            "metodo-musa-7-dias",
            "production",
            "clubemusa.com.br",
            PdeVpsStatus.ACTIVE,
            "Escala");
    when(service.updateServer(eq(10L), any(SavePdeVpsServerRequest.class)))
        .thenReturn(
            response(
                10L,
                "Locaweb PDE escala",
                "Locaweb",
                "191.252.181.168",
                new BigDecimal("160.00"),
                PdeVpsStatus.ACTIVE));

    mockMvc
        .perform(
            put("/api/pde/vps/{id}", 10L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.provider").value("Locaweb"))
        .andExpect(jsonPath("$.monthlyCostBrl").value(160.00));
  }

  /** Deve remover VPS pelo identificador administrativo. */
  @Test
  void deleteServer() throws Exception {
    doNothing().when(service).deleteServer(10L);

    mockMvc.perform(delete("/api/pde/vps/{id}", 10L)).andExpect(status().isNoContent());
  }

  /** Cria resposta de VPS para os testes de contrato HTTP. */
  private PdeVpsServerResponse response(
      Long id,
      String name,
      String provider,
      String ipAddress,
      BigDecimal monthlyCost,
      PdeVpsStatus status) {
    return new PdeVpsServerResponse(
        id,
        name,
        provider,
        ipAddress,
        "VPS Linux",
        "Brasil",
        2,
        4,
        80,
        monthlyCost,
        "metodo-musa-7-dias",
        "production",
        "v6.clubemusa.com.br",
        status,
        "Operação PDE",
        Instant.parse("2026-07-29T00:00:00Z"),
        Instant.parse("2026-07-29T00:00:00Z"));
  }
}

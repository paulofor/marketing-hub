package com.marketinghub.pde.mira.privateprototype.v1;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.marketinghub.pde.mira.privateprototype.v1.controller.MiraPrivateController;
import com.marketinghub.pde.mira.privateprototype.v1.service.MiraPrivateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: bloquear acesso interno indevido e produtos opcionais incompletos na API. */
class MiraPrivateControllerTest {
  private MockMvc mvc;
  private MiraPrivateService service;

  /** Monta a API real com serviço isolado e segredo exclusivamente sintético. */
  @BeforeEach
  void setup() {
    service = mock(MiraPrivateService.class);
    mvc =
        MockMvcBuilders.standaloneSetup(new MiraPrivateController(service, "test-only-secret"))
            .build();
  }

  /** Rejeita campos faltantes em qualquer item da lista antes de gravar ou gerar. */
  @Test
  void validatesEachIncludedProduct() throws Exception {
    mvc.perform(
            put("/api/pde/mira/candidate/v1/input")
                .header("X-Mira-Session", "test-only-session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"objective\":\"Organizar cuidados\",\"products\":[{\"name\":\"Produto\",\"labelDirections\":\"Enxaguar\"},{\"name\":\"\",\"labelDirections\":\"\"}]}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  /**
   * Garante que relatório e criação não aceitam ausência ou credencial de sessão como acesso
   * interno.
   */
  @Test
  void protectsPrivateReports() throws Exception {
    mvc.perform(get("/api/pde/mira/candidate/v1/internal/cycles/7006/report"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            get("/api/pde/mira/candidate/v1/internal/cycles/7006/report")
                .header("X-PDE-Internal-Token", "wrong-session"))
        .andExpect(status().isUnauthorized());
    verifyNoInteractions(service);
  }
}

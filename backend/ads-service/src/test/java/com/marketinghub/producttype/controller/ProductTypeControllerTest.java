package com.marketinghub.producttype.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.producttype.ProductTypeStatus;
import com.marketinghub.producttype.service.ProductTypeService;
import com.marketinghub.producttype.service.catalog.ProductTypeCatalogItemResponse;
import com.marketinghub.producttype.service.catalog.SaveProductTypeRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: validar o contrato HTTP do catálogo de tipos de produto. */
@ExtendWith(MockitoExtension.class)
class ProductTypeControllerTest {
  private final ObjectMapper objectMapper = new ObjectMapper();
  private MockMvc mockMvc;

  @Mock private ProductTypeService service;

  /** Monta o controller isolado antes de cada cenário HTTP. */
  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new ProductTypeController(service)).build();
  }

  /** Deve listar inclusive tipos históricos quando a tela solicitar. */
  @Test
  void listProductTypes() throws Exception {
    var item =
        new ProductTypeCatalogItemResponse(
            1L,
            "PDE",
            "Produto Digital Experiencial",
            "Jornada de valor.",
            List.of("Experiência guiada"),
            ProductTypeStatus.ACTIVE,
            4,
            null,
            null);
    when(service.list("guiada", true)).thenReturn(List.of(item));

    mockMvc
        .perform(
            get("/api/product-types")
                .queryParam("query", "guiada")
                .queryParam("includeRetired", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("PDE"))
        .andExpect(jsonPath("$[0].aliases[0]").value("Experiência guiada"))
        .andExpect(jsonPath("$[0].productCount").value(4));
  }

  /** Deve validar o nome obrigatório antes de criar um tipo. */
  @Test
  void rejectBlankName() throws Exception {
    mockMvc
        .perform(
            post("/api/product-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"aliases\":[],\"status\":\"PROPOSED\"}"))
        .andExpect(status().isBadRequest());
  }

  /** Deve atualizar uma classificação pelo endpoint canônico. */
  @Test
  void updateProductType() throws Exception {
    SaveProductTypeRequest request =
        new SaveProductTypeRequest(
            "PDE",
            "Produto Digital Experiencial",
            "Jornada de valor.",
            List.of("Experiência guiada"),
            ProductTypeStatus.ACTIVE);
    var response =
        new ProductTypeCatalogItemResponse(
            1L,
            request.code(),
            request.name(),
            request.description(),
            request.aliases(),
            request.status(),
            4,
            null,
            null);
    when(service.update(eq(1L), any(SaveProductTypeRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            put("/api/product-types/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }
}

package com.marketinghub.product.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: validar os aliases públicos PDE do cadastro canônico de produtos. */
@ExtendWith(MockitoExtension.class)
class PdePublicProductControllerTest {
  private MockMvc mockMvc;

  @Mock private ProductService productService;

  /** Monta o controller isolado para validar o contrato público PDE. */
  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new PdePublicProductController(productService)).build();
  }

  /** Deve expor o mesmo contrato PDE canônico pela rota pública usada pelo Clube MUSA. */
  @Test
  void getPublicPdeProduct() throws Exception {
    when(productService.getPublicPdeExperienceJson("metodo-musa-7-dias"))
        .thenReturn("{\"slug\":\"metodo-musa-7-dias\",\"missions\":[]}");

    mockMvc
        .perform(get("/api/pde/products/{productSlug}", "metodo-musa-7-dias"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.slug").value("metodo-musa-7-dias"));
  }
}

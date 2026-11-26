package com.marketinghub.product.fichas.v1;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.product.fichas.v1.controller.FichasController;
import com.marketinghub.product.fichas.v1.service.FichasService;
import com.marketinghub.repository.jpa.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: comprovar isolamento, histórico e ausência de efeitos no piloto. */
class FichasTest {
  /** Consulta os dois resultados, preserva revisões e impede vazamento entre contextos. */
  @Test
  void historicoIdempotentePorProdutoECiclo() throws Exception {
    var products = mock(ProductRepository.class);
    when(products.existsById(7L)).thenReturn(true);
    when(products.existsById(8L)).thenReturn(true);
    var service = new FichasService(products, new ObjectMapper());
    var mvc = MockMvcBuilders.standaloneSetup(new FichasController(service)).build();
    mvc.perform(get("/api/products/7/fichas/v1?ciclo=20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.despachoPermitido").value(false))
        .andExpect(jsonPath("$.fichas.length()").value(4))
        .andExpect(jsonPath("$.fichas[2].execucao.estado").value("BLOCKED"))
        .andExpect(jsonPath("$.fichas[3].execucao.estado").value("COMPLETED"));
    assertEquals(service.list(7L, 20L), service.list(7L, 20L));
    assertTrue(service.list(8L, 20L).fichas().isEmpty());
    assertTrue(service.list(7L, 21L).fichas().isEmpty());
    mvc.perform(get("/api/products/999/fichas/v1")).andExpect(status().isNotFound());
    verify(products, never()).save(any());
  }

  /** Comprova que o piloto não fica disponível sem habilitação explícita. */
  @Test
  void desabilitadoPorPadrao() {
    var context =
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
            .withBean(ProductRepository.class, () -> mock(ProductRepository.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withUserConfiguration(FichasService.class, FichasController.class);
    context.run(c -> assertFalse(c.containsBean("fichasController")));
    context
        .withPropertyValues("marketinghub.fichas.homologacao=true")
        .run(c -> assertNotNull(c.getBean(FichasController.class)));
  }

  /** Impede que recursos do mesmo produto, mas sem vínculo ao ciclo, apareçam como prova. */
  @Test
  void excluiRecursosNaoVinculadosAoCiclo() throws Exception {
    var products = mock(ProductRepository.class);
    when(products.existsById(7L)).thenReturn(true);
    var real = new ObjectMapper();
    var cards =
        (com.fasterxml.jackson.databind.node.ArrayNode)
            real.readTree(getClass().getResourceAsStream("/fichas/v1/exemplo-bloqueado.json"));
    var unrelated = cards.get(1).deepCopy();
    ((com.fasterxml.jackson.databind.node.ObjectNode) unrelated).put("id", "recurso-outro-ciclo");
    cards.add(unrelated);
    var mapper = mock(ObjectMapper.class);
    when(mapper.readTree(any(java.io.InputStream.class))).thenReturn(cards, real.createArrayNode());
    var view = new FichasService(products, mapper).list(7L, 20L);
    assertEquals(3, view.fichas().size());
    assertTrue(
        view.fichas().stream().noneMatch(c -> c.path("id").asText().equals("recurso-outro-ciclo")));
  }
}

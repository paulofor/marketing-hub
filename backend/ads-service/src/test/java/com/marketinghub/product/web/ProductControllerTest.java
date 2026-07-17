package com.marketinghub.product.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.product.Product;
import com.marketinghub.product.dto.CreateProductRequest;
import com.marketinghub.product.dto.ProductDto;
import com.marketinghub.product.mapper.ProductMapper;
import com.marketinghub.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Responsabilidade: validar o contrato REST do cadastro comercial de produtos. */
@ExtendWith(MockitoExtension.class)
class ProductControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @Mock
    private ProductService service;

    @Mock
    private ProductMapper mapper;

    /** Monta o controller isolado para validar o contrato HTTP de produto. */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(service, mapper)).build();
    }

    /** Deve aceitar atualização de dados comerciais pelo endpoint canônico de produto. */
    @Test
    void updateProduct() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Método MUSA - Presença Elegante em 7 Dias");
        request.setMarketNicheId(10L);
        request.setCurrentPriceBrl(new BigDecimal("47.00"));

        Product product = Product.builder().id(1L).name(request.getName()).build();
        ProductDto response = new ProductDto();
        response.setId(1L);
        response.setName(request.getName());
        response.setCurrentPriceBrl(request.getCurrentPriceBrl());

        when(service.updateProduct(eq(1L), any(CreateProductRequest.class))).thenReturn(product);
        when(mapper.toDto(product)).thenReturn(response);

        mockMvc.perform(put("/api/products/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value(request.getName()))
                .andExpect(jsonPath("$.currentPriceBrl").value(47.00));
    }

    /** Deve expor a definição pública de mercado do produto como Markdown. */
    @Test
    void getPublicMarketingDefinitionMarkdown() throws Exception {
        String markdown = "# Definição de Produto para Mercado — Método MUSA\n\n## 1. Identidade do produto\n";

        when(service.buildPublicMarketingDefinitionMarkdown("metodo-musa-7-dias")).thenReturn(markdown);

        mockMvc.perform(get("/api/products/public/{productCode}/marketing-definition.md", "metodo-musa-7-dias"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/markdown;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition",
                        "inline; filename=\"produto-metodo-musa-7-dias-definicao-mercado.md\""))
                .andExpect(content().string(markdown));
    }

    /** Deve expor a definição pública de mercado do produto como HTML formatado. */
    @Test
    void getPublicMarketingDefinitionHtml() throws Exception {
        String markdown = "# Definição de Produto para Mercado — Método MUSA\n\n"
                + "> Documento público de posicionamento comercial do produto.\n\n"
                + "## 1. Identidade do produto\n\n"
                + "- **Nome comercial:** Método MUSA\n";

        when(service.buildPublicMarketingDefinitionMarkdown("metodo-musa-7-dias")).thenReturn(markdown);

        mockMvc.perform(get("/api/products/public/{productCode}/marketing-definition", "metodo-musa-7-dias"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(containsString("<h1>Definição de Produto para Mercado — Método MUSA</h1>")))
                .andExpect(content().string(containsString("<blockquote>Documento público de posicionamento comercial do produto.</blockquote>")))
                .andExpect(content().string(containsString("<li><strong>Nome comercial:</strong> Método MUSA</li>")));
    }
}

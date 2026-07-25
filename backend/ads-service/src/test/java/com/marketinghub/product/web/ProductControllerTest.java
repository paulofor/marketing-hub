package com.marketinghub.product.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotDto;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.pde.service.PdeProductionSlotService;
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
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Mock
    private PdeProductionSlotService pdeProductionSlotService;

    /** Monta o controller isolado para validar o contrato HTTP de produto. */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(service, mapper, pdeProductionSlotService)).build();
    }

    /** Deve aceitar atualização de dados comerciais pelo endpoint canônico de produto. */
    @Test
    void updateProduct() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Método MUSA - Presença Elegante em 7 Dias");
        request.setMarketNicheId(10L);
        request.setCurrentPriceBrl(new BigDecimal("67.00"));

        Product product = Product.builder().id(1L).name(request.getName()).build();
        ProductDto response = new ProductDto();
        response.setId(1L);
        response.setName(request.getName());
        response.setLogoUrl("https://clubemusa.com.br/assets/logo-musa.svg");
        response.setCurrentPriceBrl(request.getCurrentPriceBrl());

        when(service.updateProduct(eq(1L), any(CreateProductRequest.class))).thenReturn(product);
        when(mapper.toDto(product)).thenReturn(response);

        mockMvc.perform(put("/api/products/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value(request.getName()))
                .andExpect(jsonPath("$.logoUrl").value("https://clubemusa.com.br/assets/logo-musa.svg"))
                .andExpect(jsonPath("$.currentPriceBrl").value(67.00));
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

    /** Deve expor o contrato JSON da experiência PDE publicado pelo Marketing Hub. */
    @Test
    void getPublicPdeExperience() throws Exception {
        String json = "{\"slug\":\"metodo-musa-7-dias\",\"missions\":[]}";

        when(service.getPublicPdeExperienceJson("metodo-musa-7-dias")).thenReturn(json);

        mockMvc.perform(get("/api/products/public/{productCode}/pde-experience", "metodo-musa-7-dias"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.slug").value("metodo-musa-7-dias"));
    }

    /** Deve acionar a inserção da jornada persuasiva interativa no produto. */
    @Test
    void applyDefaultPdePersuasiveJourney() throws Exception {
        Product product = Product.builder().id(1L).name("Método MUSA").build();
        ProductDto response = new ProductDto();
        response.setId(1L);
        response.setName("Método MUSA");
        response.setPdeExperienceJson("{\"persuasiveJourney\":{\"framework\":\"Funil experiencial PDE\"}}");

        when(service.applyDefaultPdePersuasiveJourney(1L)).thenReturn(product);
        when(mapper.toDto(product)).thenReturn(response);

        mockMvc.perform(post("/api/products/{id}/pde-persuasive-journey/default", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.pdeExperienceJson").value("{\"persuasiveJourney\":{\"framework\":\"Funil experiencial PDE\"}}"));
    }

    /** Deve listar versões produtivas PDE pelo endpoint canônico do produto. */
    @Test
    void listPdeProductionSlots() throws Exception {
        Product product = Product.builder().id(1L).slug("metodo-musa-7-dias").build();
        when(service.getProduct(1L)).thenReturn(product);
        when(pdeProductionSlotService.listProductionSlotsForProduct("metodo-musa-7-dias"))
                .thenReturn(List.of(new PostDeployPdeProductionSlotDto(
                        2L,
                        "v2",
                        "metodo-musa-7-dias",
                        "v2.clubemusa.com.br",
                        "https://v2.clubemusa.com.br",
                        null,
                        "musa-pde-entry-v5-estrada-desejo",
                        "production-v2",
                        PdeProductionSlotStatus.PLANNED,
                        71L,
                        "Hipotese 2",
                        Instant.parse("2026-07-24T10:00:00Z"),
                        Instant.parse("2026-07-24T10:00:00Z"))));

        mockMvc.perform(get("/api/products/{id}/pde-production-slots", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slotCode").value("v2"))
                .andExpect(jsonPath("$[0].experienceVersion").value("musa-pde-entry-v5-estrada-desejo"));
    }

    /** Deve expor a jornada persuasiva PDE como contrato JSON público. */
    @Test
    void getPublicPdePersuasiveJourney() throws Exception {
        var journey = objectMapper.readTree("{\"framework\":\"Funil experiencial PDE\",\"steps\":[]}");

        when(service.getPublicPdePersuasiveJourney("metodo-musa-7-dias")).thenReturn(journey);

        mockMvc.perform(get("/api/products/public/{productCode}/pde-persuasive-journey", "metodo-musa-7-dias"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.framework").value("Funil experiencial PDE"));
    }
}

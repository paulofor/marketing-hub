package com.marketinghub.product.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotDto;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.pde.service.PdeProductionSlotService;
import com.marketinghub.pde.service.versionvideos.PdeProductionSlotVideoAssetDto;
import com.marketinghub.pde.service.versionvideos.PdeProductionSlotVideoPanelDto;
import com.marketinghub.product.Product;
import com.marketinghub.product.ProductVideoSeedImageReviewStatus;
import com.marketinghub.product.dto.CreateProductRequest;
import com.marketinghub.product.dto.ProductDto;
import com.marketinghub.product.dto.ProductScientificArticleDto;
import com.marketinghub.product.dto.ProductVideoProviderAvatarDto;
import com.marketinghub.product.dto.SaveProductScientificArticleRequest;
import com.marketinghub.product.mapper.ProductMapper;
import com.marketinghub.product.service.ProductScientificArticleService;
import com.marketinghub.product.service.ProductService;
import com.marketinghub.product.service.adlibrary.ProductAdLibraryItemResponse;
import com.marketinghub.product.service.adlibrary.ProductAdLibraryResponse;
import com.marketinghub.product.service.experimentcomparison.ProductExperimentComparisonExperimentResponse;
import com.marketinghub.product.service.experimentcomparison.ProductExperimentComparisonFunnelStageResponse;
import com.marketinghub.product.service.experimentcomparison.ProductExperimentComparisonResponse;
import com.marketinghub.product.service.financialsummary.ProductFinancialAmountResponse;
import com.marketinghub.product.service.financialsummary.ProductFinancialLineResponse;
import com.marketinghub.product.service.financialsummary.ProductFinancialMonthlyResultResponse;
import com.marketinghub.product.service.financialsummary.ProductFinancialSummaryResponse;
import com.marketinghub.product.service.organicvideoplan.ProductOrganicVideoDecisionRuleResponse;
import com.marketinghub.product.service.organicvideoplan.ProductOrganicVideoPlanItemResponse;
import com.marketinghub.product.service.organicvideoplan.ProductOrganicVideoPlanResponse;
import com.marketinghub.product.service.updateInternalName.UpdateProductInternalNameRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: validar o contrato REST do cadastro comercial de produtos. */
@ExtendWith(MockitoExtension.class)
class ProductControllerTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  private MockMvc mockMvc;

  @Mock private ProductService service;

  @Mock private ProductScientificArticleService scientificArticleService;

  @Mock private ProductMapper mapper;

  @Mock private PdeProductionSlotService pdeProductionSlotService;

  /** Monta o controller isolado para validar o contrato HTTP de produto. */
  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new ProductController(
                    service, scientificArticleService, mapper, pdeProductionSlotService))
            .build();
  }

  /** Deve aceitar atualização de dados comerciais pelo endpoint canônico de produto. */
  @Test
  void updateProduct() throws Exception {
    CreateProductRequest request = new CreateProductRequest();
    request.setName("Método MUSA - Presença Elegante em 7 Dias");
    request.setInternalName("MUSA desejo v7");
    request.setAliases(List.of("MUSA v7", "Vídeos orientados ao desejo"));
    request.setMarketNicheId(10L);
    request.setCurrentPriceBrl(new BigDecimal("67.00"));

    Product product = Product.builder().id(1L).name(request.getName()).build();
    ProductDto response = new ProductDto();
    response.setId(1L);
    response.setName(request.getName());
    response.setInternalName(request.getInternalName());
    response.setAliases(request.getAliases());
    response.setLogoUrl("https://clubemusa.com.br/assets/logo-musa.svg");
    response.setCurrentPriceBrl(request.getCurrentPriceBrl());

    when(service.updateProduct(eq(1L), any(CreateProductRequest.class))).thenReturn(product);
    when(mapper.toDto(product)).thenReturn(response);

    mockMvc
        .perform(
            put("/api/products/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.name").value(request.getName()))
        .andExpect(jsonPath("$.internalName").value("MUSA desejo v7"))
        .andExpect(jsonPath("$.aliases[0]").value("MUSA v7"))
        .andExpect(jsonPath("$.logoUrl").value("https://clubemusa.com.br/assets/logo-musa.svg"))
        .andExpect(jsonPath("$.currentPriceBrl").value(67.00));
  }

  /** Deve atualizar o nome interno sem exigir o contrato comercial completo. */
  @Test
  void updateProductInternalName() throws Exception {
    UpdateProductInternalNameRequest request = new UpdateProductInternalNameRequest("Vega");
    Product product = Product.builder().id(1L).internalName("Vega").build();
    ProductDto response = new ProductDto();
    response.setId(1L);
    response.setInternalName("Vega");

    when(service.updateInternalName(eq(1L), any(UpdateProductInternalNameRequest.class)))
        .thenReturn(product);
    when(mapper.toDto(product)).thenReturn(response);

    mockMvc
        .perform(
            patch("/api/products/{id}/internal-name", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.internalName").value("Vega"));
  }

  /** Deve rejeitar nome interno vazio antes de executar a atualização. */
  @Test
  void rejectBlankProductInternalName() throws Exception {
    mockMvc
        .perform(
            patch("/api/products/{id}/internal-name", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"internalName\":\"   \"}"))
        .andExpect(status().isBadRequest());
  }

  /** Deve encaminhar a pesquisa por apelido ao serviço canônico de produtos. */
  @Test
  void listProductsByInternalAlias() throws Exception {
    Product product =
        Product.builder()
            .id(4L)
            .name("Método MUSA")
            .internalName("MUSA desejo v7")
            .aliases(Set.of("MUSA v7"))
            .build();
    ProductDto response = new ProductDto();
    response.setId(4L);
    response.setName("Método MUSA");
    response.setInternalName("MUSA desejo v7");
    response.setAliases(List.of("MUSA v7"));
    when(service.listProducts("MUSA v7")).thenReturn(List.of(product));
    when(mapper.toDto(product)).thenReturn(response);

    mockMvc
        .perform(get("/api/products").queryParam("query", "MUSA v7"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(4L))
        .andExpect(jsonPath("$[0].internalName").value("MUSA desejo v7"))
        .andExpect(jsonPath("$[0].aliases[0]").value("MUSA v7"));
  }

  /** Deve rejeitar campos maiores que a coluna antes de chegar ao banco. */
  @Test
  void rejectProductDeliveryModeLargerThanSchema() throws Exception {
    CreateProductRequest request = new CreateProductRequest();
    request.setName("PDE seguro");
    request.setDeliveryMode("x".repeat(65));

    mockMvc
        .perform(
            put("/api/products/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  /** Deve expor anúncios reutilizáveis vinculados ao produto. */
  @Test
  void getAdLibrary() throws Exception {
    var response =
        new ProductAdLibraryResponse(
            1L,
            "Método MUSA",
            "metodo-musa-7-dias",
            "VALIDACAO_COMERCIAL",
            "Priorize anúncios prontos como controle criativo.",
            List.of(
                new ProductAdLibraryItemResponse(
                    12L,
                    null,
                    1,
                    true,
                    74L,
                    "MUSA-H001-E009",
                    "RUNNING",
                    "IMAGE",
                    "READY",
                    "Elegância visível em 7 dias",
                    "Descubra quais escolhas deixam sua presença mais elegante.",
                    "Criativo para primeira dobra.",
                    "LEARN_MORE",
                    "https://clubemusa.com.br",
                    "https://cdn.example.com/musa-ad.png",
                    null,
                    null,
                    "Pode ser reaproveitado em novos experimentos.",
                    Instant.parse("2026-07-28T12:00:00Z"),
                    null,
                    null,
                    null,
                    null,
                    null,
                    0,
                    null)));

    when(service.getAdLibrary(1L)).thenReturn(response);

    mockMvc
        .perform(get("/api/products/{id}/ads", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.productId").value(1L))
        .andExpect(
            jsonPath("$.mainRecommendation")
                .value("Priorize anúncios prontos como controle criativo."))
        .andExpect(jsonPath("$.ads[0].creativeId").value(12L))
        .andExpect(jsonPath("$.ads[0].experimentId").value(74L))
        .andExpect(jsonPath("$.ads[0].headline").value("Elegância visível em 7 dias"))
        .andExpect(jsonPath("$.ads[0].imageUrl").value("https://cdn.example.com/musa-ad.png"));
  }

  /** Deve expor os anúncios do produto que estão em uso no experimento. */
  @Test
  void getExperimentAdsInUse() throws Exception {
    var response =
        new ProductAdLibraryResponse(
            1L,
            "Método MUSA",
            "metodo-musa-7-dias",
            "VALIDACAO_COMERCIAL",
            "O experimento já usa anúncio aprovado do produto.",
            List.of(
                new ProductAdLibraryItemResponse(
                    253L,
                    null,
                    1,
                    true,
                    76L,
                    "MUSA-H001-E011",
                    "PLANNED",
                    "IMAGE",
                    "READY",
                    "Presença elegante em 7 dias",
                    "Descubra o ajuste que muda sua imagem hoje.",
                    "Controle criativo do produto.",
                    "LEARN_MORE",
                    "https://clubemusa.com.br",
                    "https://cdn.example.com/musa-76.png",
                    null,
                    null,
                    "Pode ser reaproveitado em novos experimentos.",
                    Instant.parse("2026-07-29T12:00:00Z"),
                    "APPROVED",
                    "{\"summary\":\"Peça clara\"}",
                    "gpt-test",
                    Instant.parse("2026-07-29T11:00:00Z"),
                    "PROCESSING",
                    1,
                    null)));

    when(service.getExperimentAdsInUse(76L)).thenReturn(response);

    mockMvc
        .perform(get("/api/products/experiments/{experimentId}/ads-in-use", 76L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.productId").value(1L))
        .andExpect(jsonPath("$.productSlug").value("metodo-musa-7-dias"))
        .andExpect(jsonPath("$.ads[0].creativeId").value(253L))
        .andExpect(jsonPath("$.ads[0].versionNumber").value(1))
        .andExpect(jsonPath("$.ads[0].finalCandidate").value(true))
        .andExpect(jsonPath("$.ads[0].experimentId").value(76L))
        .andExpect(jsonPath("$.ads[0].status").value("READY"))
        .andExpect(jsonPath("$.ads[0].agentReviewStatus").value("APPROVED"))
        .andExpect(jsonPath("$.ads[0].agentImprovementStatus").value("PROCESSING"))
        .andExpect(jsonPath("$.ads[0].agentImprovementAttempts").value(1));
  }

  /** Deve expor o resumo financeiro do produto no contrato canônico. */
  @Test
  void getFinancialSummary() throws Exception {
    var response =
        new ProductFinancialSummaryResponse(
            1L,
            "Método MUSA",
            "metodo-musa-7-dias",
            new BigDecimal("5.00"),
            Instant.parse("2026-07-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z"),
            List.of(
                new ProductFinancialMonthlyResultResponse(
                    Instant.parse("2026-07-01T00:00:00Z"),
                    "Julho 2026",
                    new ProductFinancialAmountResponse(
                        new BigDecimal("75.00"), new BigDecimal("15.00")),
                    new ProductFinancialAmountResponse(
                        new BigDecimal("67.00"), new BigDecimal("13.40")),
                    new ProductFinancialAmountResponse(
                        new BigDecimal("-8.00"), new BigDecimal("-1.60")))),
            List.of(
                new ProductFinancialLineResponse(
                    "MEDIA",
                    "Mídia paga",
                    new ProductFinancialAmountResponse(
                        new BigDecimal("25.00"), new BigDecimal("5.00")),
                    new ProductFinancialAmountResponse(
                        new BigDecimal("250.00"), new BigDecimal("50.00")),
                    "Métricas de campanha")),
            new ProductFinancialLineResponse(
                "SALES",
                "Receitas de vendas",
                new ProductFinancialAmountResponse(
                    new BigDecimal("67.00"), new BigDecimal("13.40")),
                new ProductFinancialAmountResponse(
                    new BigDecimal("670.00"), new BigDecimal("134.00")),
                "Vendas aprovadas"),
            new ProductFinancialLineResponse(
                "PROFIT",
                "Lucro",
                new ProductFinancialAmountResponse(new BigDecimal("42.00"), new BigDecimal("8.40")),
                new ProductFinancialAmountResponse(
                    new BigDecimal("420.00"), new BigDecimal("84.00")),
                "Receita menos custos"));

    when(service.getFinancialSummary(1L)).thenReturn(response);

    mockMvc
        .perform(get("/api/products/{id}/financial-summary", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.productId").value(1L))
        .andExpect(jsonPath("$.exchangeRateBrlPerUsd").value(5.00))
        .andExpect(jsonPath("$.monthlyResults[0].monthLabel").value("Julho 2026"))
        .andExpect(jsonPath("$.monthlyResults[0].cost.brl").value(75.00))
        .andExpect(jsonPath("$.monthlyResults[0].profit.usd").value(-1.60))
        .andExpect(jsonPath("$.costs[0].type").value("MEDIA"))
        .andExpect(jsonPath("$.revenue.monthly.brl").value(67.00))
        .andExpect(jsonPath("$.profit.annual.usd").value(84.00));
  }

  /** Deve expor o painel comparativo de experimentos por produto. */
  @Test
  void getExperimentComparison() throws Exception {
    var response =
        new ProductExperimentComparisonResponse(
            1L,
            "Método MUSA",
            "metodo-musa-7-dias",
            "VALIDACAO_COMERCIAL",
            "GUIDED_PROGRAM",
            "HYBRID",
            "ONE_TIME_PURCHASE",
            "7 dias concluídos",
            "SATISFACTION",
            "v1",
            "Priorizar correção da ativação/funil antes de comparar novos criativos ou públicos.",
            List.of(
                new ProductExperimentComparisonExperimentResponse(
                    74L,
                    "MUSA-H001-E009",
                    "RUNNING",
                    "ACTIVE",
                    "SALES",
                    "PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL",
                    java.time.LocalDate.parse("2026-07-27"),
                    java.time.LocalDate.parse("2026-08-03"),
                    new BigDecimal("25.00"),
                    new BigDecimal("67.00"),
                    1000L,
                    900L,
                    20L,
                    0L,
                    new BigDecimal("12.50"),
                    new BigDecimal("0.62"),
                    BigDecimal.ZERO,
                    3L,
                    3L,
                    List.of(
                        new ProductExperimentComparisonFunnelStageResponse(
                            "ACESSO_FORM_LEAD", "Acesso ao formulário de lead", 5)),
                    "Público amplo Meta",
                    "Elegância possível em 7 dias",
                    "Clique barato, mas ativação precisa melhorar.",
                    "Corrigir ativação pós-clique: o anúncio gera interesse, mas o funil não registra entrada.",
                    Instant.parse("2026-07-27T12:00:00Z"))));

    when(service.getExperimentComparison(1L)).thenReturn(response);

    mockMvc
        .perform(get("/api/products/{id}/experiment-comparison", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.productId").value(1L))
        .andExpect(jsonPath("$.productFormat").value("GUIDED_PROGRAM"))
        .andExpect(jsonPath("$.valueEvidenceMetric").value("SATISFACTION"))
        .andExpect(
            jsonPath("$.mainRecommendation")
                .value(
                    "Priorizar correção da ativação/funil antes de comparar novos criativos ou públicos."))
        .andExpect(jsonPath("$.experiments[0].experimentId").value(74L))
        .andExpect(jsonPath("$.experiments[0].campaignStatus").value("ACTIVE"))
        .andExpect(
            jsonPath("$.experiments[0].funnelStages[0].stageLabel")
                .value("Acesso ao formulário de lead"));
  }

  /** Deve expor o playbook de vídeos orgânicos do produto pelo contrato canônico. */
  @Test
  void getOrganicVideoPlan() throws Exception {
    var response =
        new ProductOrganicVideoPlanResponse(
            1L,
            "Método MUSA",
            "metodo-musa-7-dias",
            "9 vídeos em 7 dias",
            "Validar atenção antes de aumentar CTA.",
            "7 dias",
            "TikTok + Reels",
            "6 vídeos de dor, 2 educativos e 1 direto.",
            List.of(
                new ProductOrganicVideoPlanItemResponse(
                    1,
                    1,
                    "ENTRETENIMENTO_DOR",
                    "Desconhecido -> relevante",
                    "Isso acontece comigo.",
                    "TikTok + Reels",
                    "POV: você já trocou de roupa 4 vezes e nenhuma parece você.",
                    "Cena no espelho.",
                    "Falta intenção visual.",
                    "Faça o diagnóstico.",
                    "Retenção e comentários.",
                    List.of("Legenda grande."))),
            List.of(
                new ProductOrganicVideoDecisionRuleResponse(
                    "Dor cotidiana",
                    "Vídeos de dor geram retenção.",
                    "Aumentar CTA.",
                    "A audiência reconheceu o problema.")),
            List.of("Começar por situação reconhecível."));

    when(service.getOrganicVideoPlan(1L)).thenReturn(response);

    mockMvc
        .perform(get("/api/products/{id}/organic-video-plan", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.strategyName").value("9 vídeos em 7 dias"))
        .andExpect(jsonPath("$.videos[0].category").value("ENTRETENIMENTO_DOR"))
        .andExpect(jsonPath("$.decisionRules[0].decision").value("Aumentar CTA."));
  }

  /** Deve expor os artigos científicos usados no mecanismo do produto. */
  @Test
  void listScientificArticles() throws Exception {
    var response =
        new ProductScientificArticleDto(
            8L,
            1L,
            "https://doi.org/10.1016/j.jesp.2012.02.008",
            "Enclothed cognition",
            "Cognição vestida",
            "Resumo operacional.",
            "Aplicação no mecanismo MUSA.",
            Instant.parse("2026-07-27T00:00:00Z"),
            Instant.parse("2026-07-27T00:00:00Z"));

    when(scientificArticleService.listArticles(1L)).thenReturn(List.of(response));

    mockMvc
        .perform(get("/api/products/{id}/scientific-articles", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(8L))
        .andExpect(jsonPath("$[0].originalTitle").value("Enclothed cognition"))
        .andExpect(jsonPath("$[0].portugueseTitle").value("Cognição vestida"))
        .andExpect(jsonPath("$[0].mechanismApplication").value("Aplicação no mecanismo MUSA."));
  }

  /** Deve cadastrar artigo científico pelo contrato canônico do produto. */
  @Test
  void createScientificArticle() throws Exception {
    var response =
        new ProductScientificArticleDto(
            9L,
            1L,
            "https://doi.org/10.1177/1948550615579462",
            "The Cognitive Consequences of Formal Clothing",
            "As consequências cognitivas da roupa formal",
            "Resumo.",
            "Aplicação.",
            null,
            null);
    var request =
        new SaveProductScientificArticleRequest(
            response.link(),
            response.originalTitle(),
            response.portugueseTitle(),
            response.summary(),
            response.mechanismApplication());

    when(scientificArticleService.createArticle(
            eq(1L), any(SaveProductScientificArticleRequest.class)))
        .thenReturn(response);

    mockMvc
        .perform(
            post("/api/products/{id}/scientific-articles", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(9L))
        .andExpect(jsonPath("$.link").value(response.link()));
  }

  /** Deve atualizar artigo científico do produto pelo contrato canônico. */
  @Test
  void updateScientificArticle() throws Exception {
    var response =
        new ProductScientificArticleDto(
            9L,
            1L,
            "https://doi.org/10.1177/1948550615579462",
            "The Cognitive Consequences of Formal Clothing",
            "Roupa formal e cognição",
            "Resumo revisado.",
            "Aplicação revisada.",
            null,
            null);
    var request =
        new SaveProductScientificArticleRequest(
            response.link(),
            response.originalTitle(),
            response.portugueseTitle(),
            response.summary(),
            response.mechanismApplication());

    when(scientificArticleService.updateArticle(
            eq(1L), eq(9L), any(SaveProductScientificArticleRequest.class)))
        .thenReturn(response);

    mockMvc
        .perform(
            put("/api/products/{id}/scientific-articles/{articleId}", 1L, 9L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.portugueseTitle").value("Roupa formal e cognição"))
        .andExpect(jsonPath("$.summary").value("Resumo revisado."));
  }

  /** Deve remover artigo científico do produto sem retornar corpo. */
  @Test
  void deleteScientificArticle() throws Exception {
    mockMvc
        .perform(delete("/api/products/{id}/scientific-articles/{articleId}", 1L, 9L))
        .andExpect(status().isNoContent());
  }

  /** Deve expor a definição pública de mercado do produto como Markdown. */
  @Test
  void getPublicMarketingDefinitionMarkdown() throws Exception {
    String markdown =
        "# Definição de Produto para Mercado — Método MUSA\n\n## 1. Identidade do produto\n";

    when(service.buildPublicMarketingDefinitionMarkdown("metodo-musa-7-dias")).thenReturn(markdown);

    mockMvc
        .perform(
            get("/api/products/public/{productCode}/marketing-definition.md", "metodo-musa-7-dias"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("text/markdown;charset=UTF-8"))
        .andExpect(
            header()
                .string(
                    "Content-Disposition",
                    "inline; filename=\"produto-metodo-musa-7-dias-definicao-mercado.md\""))
        .andExpect(content().string(markdown));
  }

  /** Deve expor a definição pública de mercado do produto como HTML formatado. */
  @Test
  void getPublicMarketingDefinitionHtml() throws Exception {
    String markdown =
        "# Definição de Produto para Mercado — Método MUSA\n\n"
            + "> Documento público de posicionamento comercial do produto.\n\n"
            + "## 1. Identidade do produto\n\n"
            + "- **Nome comercial:** Método MUSA\n";

    when(service.buildPublicMarketingDefinitionMarkdown("metodo-musa-7-dias")).thenReturn(markdown);

    mockMvc
        .perform(
            get("/api/products/public/{productCode}/marketing-definition", "metodo-musa-7-dias"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("text/html;charset=UTF-8"))
        .andExpect(
            content()
                .string(containsString("<h1>Definição de Produto para Mercado — Método MUSA</h1>")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "<blockquote>Documento público de posicionamento comercial do produto.</blockquote>")))
        .andExpect(
            content()
                .string(containsString("<li><strong>Nome comercial:</strong> Método MUSA</li>")));
  }

  /** Deve expor o contrato JSON da experiência PDE publicado pelo Marketing Hub. */
  @Test
  void getPublicPdeExperience() throws Exception {
    String json = "{\"slug\":\"metodo-musa-7-dias\",\"missions\":[]}";

    when(service.getPublicPdeExperienceJson("metodo-musa-7-dias")).thenReturn(json);

    mockMvc
        .perform(get("/api/products/public/{productCode}/pde-experience", "metodo-musa-7-dias"))
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
    response.setPdeExperienceJson(
        "{\"persuasiveJourney\":{\"framework\":\"Funil experiencial PDE\"}}");

    when(service.applyDefaultPdePersuasiveJourney(1L)).thenReturn(product);
    when(mapper.toDto(product)).thenReturn(response);

    mockMvc
        .perform(post("/api/products/{id}/pde-persuasive-journey/default", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(
            jsonPath("$.pdeExperienceJson")
                .value("{\"persuasiveJourney\":{\"framework\":\"Funil experiencial PDE\"}}"));
  }

  /** Deve aprovar a imagem semente de vídeo do produto pelo endpoint canônico. */
  @Test
  void updateVideoSeedImage() throws Exception {
    Product product = Product.builder().id(1L).name("Método MUSA").build();
    ProductDto response = new ProductDto();
    response.setId(1L);
    response.setName("Método MUSA");
    response.setVideoSeedImageAssetId(99L);
    response.setVideoSeedCharacterName("Sofia MUSA");
    response.setVideoSeedReviewStatus(ProductVideoSeedImageReviewStatus.APPROVED);

    when(service.updateVideoSeedImage(eq(1L), any())).thenReturn(product);
    when(mapper.toDto(product)).thenReturn(response);

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                    "/api/products/{id}/video-seed-image", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "assetId": 99,
                                  "characterName": "Sofia MUSA",
                                  "reviewStatus": "APPROVED",
                                  "reviewNotes": "Aprovada como imagem-mestre",
                                  "reviewedBy": "marketing@hub.local"
                                }
                                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.videoSeedImageAssetId").value(99L))
        .andExpect(jsonPath("$.videoSeedCharacterName").value("Sofia MUSA"))
        .andExpect(jsonPath("$.videoSeedReviewStatus").value("APPROVED"));
  }

  /** Deve registrar avatar de vídeo do produto para uso futuro por provider. */
  @Test
  void registerVideoProviderAvatar() throws Exception {
    var response =
        new ProductVideoProviderAvatarDto(
            10L,
            4L,
            1927L,
            "HEYGEN",
            "Sofia MUSA",
            "281a1e5b526841b0865ea466dfb33ab9",
            "3952e73a14d94871b8130274e27287ee",
            "processing",
            "https://cdn.example/musa.png",
            true,
            "Avatar criado por API HeyGen.",
            Instant.parse("2026-07-25T14:53:43Z"),
            Instant.parse("2026-07-25T14:53:43Z"));

    when(service.registerVideoProviderAvatar(eq(4L), any())).thenReturn(response);

    mockMvc
        .perform(
            post("/api/products/{id}/video-provider-avatars", 4L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "sourceAssetId": 1927,
                                  "provider": "HEYGEN",
                                  "characterName": "Sofia MUSA",
                                  "providerAvatarId": "281a1e5b526841b0865ea466dfb33ab9",
                                  "providerAvatarGroupId": "3952e73a14d94871b8130274e27287ee",
                                  "providerStatus": "processing",
                                  "supportsReusableAvatar": true
                                }
                                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.provider").value("HEYGEN"))
        .andExpect(jsonPath("$.characterName").value("Sofia MUSA"))
        .andExpect(jsonPath("$.providerAvatarId").value("281a1e5b526841b0865ea466dfb33ab9"))
        .andExpect(jsonPath("$.supportsReusableAvatar").value(true));
  }

  /** Deve listar avatars de vídeo disponíveis para o produto. */
  @Test
  void listVideoProviderAvatars() throws Exception {
    when(service.listVideoProviderAvatars(4L))
        .thenReturn(
            List.of(
                new ProductVideoProviderAvatarDto(
                    10L,
                    4L,
                    1927L,
                    "HEYGEN",
                    "Sofia MUSA",
                    "281a1e5b526841b0865ea466dfb33ab9",
                    "3952e73a14d94871b8130274e27287ee",
                    "processing",
                    "https://cdn.example/musa.png",
                    true,
                    "Avatar criado por API HeyGen.",
                    Instant.parse("2026-07-25T14:53:43Z"),
                    Instant.parse("2026-07-25T14:53:43Z"))));

    mockMvc
        .perform(get("/api/products/{id}/video-provider-avatars", 4L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].provider").value("HEYGEN"))
        .andExpect(jsonPath("$[0].characterName").value("Sofia MUSA"));
  }

  /** Deve listar versões produtivas PDE pelo endpoint canônico do produto. */
  @Test
  void listPdeProductionSlots() throws Exception {
    Product product = Product.builder().id(1L).slug("metodo-musa-7-dias").build();
    when(service.getProduct(1L)).thenReturn(product);
    when(pdeProductionSlotService.listProductionSlotsForProduct("metodo-musa-7-dias"))
        .thenReturn(
            List.of(
                new PostDeployPdeProductionSlotDto(
                    2L,
                    "v2",
                    "metodo-musa-7-dias",
                    "v2.clubemusa.com.br",
                    "https://v2.clubemusa.com.br",
                    null,
                    "musa-pde-entry-v5-estrada-desejo",
                    "estrada-desejo",
                    "production-v2",
                    PdeProductionSlotStatus.PLANNED,
                    71L,
                    "Hipotese 2",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Instant.parse("2026-07-24T10:00:00Z"),
                    Instant.parse("2026-07-24T10:00:00Z"))));

    mockMvc
        .perform(get("/api/products/{id}/pde-production-slots", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].slotCode").value("v2"))
        .andExpect(jsonPath("$[0].experienceVersion").value("musa-pde-entry-v5-estrada-desejo"));
  }

  /** Deve listar vídeos HLS já resolvidos por versão PDE pelo backend. */
  @Test
  void listPdeVersionVideos() throws Exception {
    Product product = Product.builder().id(1L).slug("metodo-musa-7-dias").build();
    when(service.getProduct(1L)).thenReturn(product);
    when(pdeProductionSlotService.listProductionSlotVideosForProduct("metodo-musa-7-dias"))
        .thenReturn(
            List.of(
                new PdeProductionSlotVideoPanelDto(
                    new PostDeployPdeProductionSlotDto(
                        4L,
                        "v6",
                        "metodo-musa-7-dias",
                        "v6.clubemusa.com.br",
                        "https://v6.clubemusa.com.br",
                        null,
                        "musa-pde-entry-v6-video-motivacional",
                        "video-motivacional",
                        "production-v6",
                        PdeProductionSlotStatus.ACTIVE,
                        76L,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Instant.parse("2026-07-24T10:00:00Z"),
                        Instant.parse("2026-07-24T10:00:00Z")),
                    List.of(
                        new PdeProductionSlotVideoAssetDto(
                            23L,
                            68L,
                            "VERSION_TOKEN",
                            "Microexperiência visível",
                            "DIAGNOSTIC_STARTED",
                            "HEYGEN",
                            "avatar",
                            ExperimentVideoStatus.READY,
                            ExperimentVideoReviewStatus.APPROVED,
                            "https://cdn.example/video.mp4",
                            "/assets/hls/musa-v6-microexperiencia-visivel/index.m3u8",
                            null,
                            46,
                            35L,
                            20462L,
                            null,
                            null)),
                    List.of("Vídeo #23 pertence ao experimento 68, mas foi exibido na v6."))));

    mockMvc
        .perform(get("/api/products/{id}/pde-videos", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].slot.slotCode").value("v6"))
        .andExpect(jsonPath("$[0].videos[0].id").value(23))
        .andExpect(jsonPath("$[0].videos[0].assignmentSource").value("VERSION_TOKEN"))
        .andExpect(jsonPath("$[0].alerts[0]").value(containsString("experimento 68")));
  }

  /** Deve expor a jornada persuasiva PDE como contrato JSON público. */
  @Test
  void getPublicPdePersuasiveJourney() throws Exception {
    var journey = objectMapper.readTree("{\"framework\":\"Funil experiencial PDE\",\"steps\":[]}");

    when(service.getPublicPdePersuasiveJourney("metodo-musa-7-dias")).thenReturn(journey);

    mockMvc
        .perform(
            get("/api/products/public/{productCode}/pde-persuasive-journey", "metodo-musa-7-dias"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.framework").value("Funil experiencial PDE"));
  }
}

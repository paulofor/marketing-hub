package com.marketinghub.product.service;

import com.marketinghub.ads.InstagramAccount;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.product.Product;
import com.marketinghub.product.dto.CreateProductRequest;
import com.marketinghub.repository.jpa.ads.InstagramAccountRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: gerenciar o cadastro comercial de produtos digitais.
 */
@Service
public class ProductService {
    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");

    private final ProductRepository repository;
    private final InstagramAccountRepository accountRepository;
    private final MarketNicheRepository marketNicheRepository;

    /** Inicializa o serviço com os repositórios necessários para cadastro de produtos. */
    public ProductService(
            ProductRepository repository,
            InstagramAccountRepository accountRepository,
            MarketNicheRepository marketNicheRepository) {
        this.repository = repository;
        this.accountRepository = accountRepository;
        this.marketNicheRepository = marketNicheRepository;
    }

    /** Cria e persiste um produto comercial com seus atributos de venda e entrega. */
    @Transactional
    public Product createProduct(CreateProductRequest request) {
        Product product = Product.builder().build();
        applyRequest(product, request);
        return repository.save(product);
    }

    /** Atualiza um produto comercial existente com os dados informados pela tela. */
    @Transactional
    public Product updateProduct(Long id, CreateProductRequest request) {
        Product product = getProduct(id);
        applyRequest(product, request);
        return repository.save(product);
    }

    /** Aplica os campos editáveis do cadastro comercial ao produto informado. */
    private void applyRequest(Product product, CreateProductRequest request) {
        product.setSlug(request.getSlug());
        product.setName(request.getName());
        product.setPublicUrl(request.getPublicUrl());
        product.setLogoUrl(request.getLogoUrl());
        product.setColorPalette(request.getColorPalette());
        product.setTargetAudience(request.getTargetAudience());
        product.setLanguageStyle(request.getLanguageStyle());
        product.setCodeModules(request.getCodeModules());
        product.setProductType(request.getProductType());
        product.setCommercialStatus(request.getCommercialStatus());
        product.setCurrentPriceBrl(request.getCurrentPriceBrl());
        product.setPrimaryHypothesisId(request.getPrimaryHypothesisId());
        product.setPrimaryHypothesis(request.getPrimaryHypothesis());
        product.setAssociatedExperiments(request.getAssociatedExperiments());
        product.setCommercialNotes(request.getCommercialNotes());
        product.setSevenDayJourney(request.getSevenDayJourney());
        product.setSupportMaterialPositioning(request.getSupportMaterialPositioning());
        product.setPrimaryCta(request.getPrimaryCta());
        product.setNiche(request.getNiche());
        product.setAvatar(request.getAvatar());
        product.setInstagramAccount(resolveAccount(request.getInstagramAccountId()));
        product.setMarketNiche(resolveNiche(request.getMarketNicheId()));
        product.setExplicitPain(request.getExplicitPain());
        product.setPromise(request.getPromise());
        product.setUniqueMechanism(request.getUniqueMechanism());
        product.setTripwire(request.getTripwire());
        product.setRiskReversal(request.getRiskReversal());
        product.setSocialProof(request.getSocialProof());
        product.setScientificEvidencePack(request.getScientificEvidencePack());
        product.setCheckoutMonetization(request.getCheckoutMonetization());
        product.setFunnel(request.getFunnel());
        product.setCreativeVolume(request.getCreativeVolume());
        product.setStorytelling(request.getStorytelling());
        product.setAiCost(request.getAiCost());
    }

    /** Resolve a conta do Instagram quando ela for informada no cadastro. */
    private InstagramAccount resolveAccount(Long id) {
        if (id == null) {
            return null;
        }
        return accountRepository.findById(id).orElseThrow();
    }

    /** Resolve o nicho de mercado obrigatório para produtos cadastrados pela tela atual. */
    private MarketNiche resolveNiche(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("marketNicheId is required");
        }
        return marketNicheRepository.findById(id).orElseThrow();
    }

    /** Busca um produto pelo identificador interno. */
    public Product getProduct(Long id) {
        return repository.findById(id).orElseThrow();
    }

    /** Lista todos os produtos cadastrados para uso operacional no Marketing Hub. */
    public Iterable<Product> listProducts() {
        return repository.findAll();
    }

    /** Monta a definição pública de mercado do produto em Markdown. */
    @Transactional(readOnly = true)
    public String buildPublicMarketingDefinitionMarkdown(String productCode) {
        Product product = findProductByCode(productCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado"));

        StringBuilder markdown = new StringBuilder();
        appendTitle(markdown, product);
        appendSection(markdown, "1. Identidade do produto",
                line("Nome comercial", product.getName()),
                line("Código do produto", product.getSlug()),
                line("Tipo de produto", product.getProductType()),
                line("Status comercial", product.getCommercialStatus()),
                line("Preço atual", formatPrice(product.getCurrentPriceBrl())),
                line("URL pública", product.getPublicUrl()),
                optionalLine("Logo", product.getLogoUrl()));
        appendSection(markdown, "2. Mercado e nicho",
                line("Nicho", resolveNiche(product)),
                line("Público alvo", product.getTargetAudience()),
                line("Avatar", product.getAvatar()));
        appendSection(markdown, "3. Hipótese comercial",
                paragraph(product.getPrimaryHypothesis()));
        appendSection(markdown, "4. Dor, resultado e mecanismo",
                line("Dor principal", product.getExplicitPain()),
                line("Resultado prometido", product.getPromise()),
                line("Mecanismo único", product.getUniqueMechanism()));
        appendSection(markdown, "5. Estilo de comunicação",
                line("Linguagem", product.getLanguageStyle()),
                line("Storytelling", product.getStorytelling()),
                line("Paleta visual completa", product.getColorPalette()));
        appendSection(markdown, "6. Oferta e monetização",
                line("Oferta", product.getTripwire()),
                line("Reversão de risco", product.getRiskReversal()),
                line("Prova", product.getSocialProof()),
                optionalLine("Base científica operacional", product.getScientificEvidencePack()),
                optionalLine("Material de apoio", product.getSupportMaterialPositioning()),
                optionalLine("CTA principal recomendado", product.getPrimaryCta()),
                line("Checkout e monetização", product.getCheckoutMonetization()));
        appendSection(markdown, "7. Jornada de 7 dias",
                paragraph(product.getSevenDayJourney()));
        appendSection(markdown, "8. Funil de aquisição e venda",
                paragraph(product.getFunnel()));
        appendSection(markdown, "9. Criativos e escala",
                line("Volume criativo esperado", product.getCreativeVolume()),
                line("Experimentos associados", product.getAssociatedExperiments()));
        appendSection(markdown, "10. Aprendizados e próximos ajustes de marketing",
                paragraph(product.getCommercialNotes()));
        return markdown.toString();
    }

    /** Busca o produto por slug público ou por identificador interno numérico. */
    private Optional<Product> findProductByCode(String productCode) {
        if (productCode == null || productCode.isBlank()) {
            return Optional.empty();
        }
        String normalizedCode = productCode.trim();
        Optional<Product> bySlug = repository.findBySlug(normalizedCode);
        if (bySlug.isPresent()) {
            return bySlug;
        }
        if (!normalizedCode.matches("\\d+")) {
            return Optional.empty();
        }
        return repository.findById(Long.valueOf(normalizedCode));
    }

    /** Adiciona o título principal do documento. */
    private void appendTitle(StringBuilder markdown, Product product) {
        markdown.append("# Definição de Produto para Mercado — ")
                .append(valueOrFallback(product.getName()))
                .append("\n\n");
        markdown.append("> Documento público de posicionamento comercial do produto. Não inclui detalhes técnicos de implementação.\n\n");
    }

    /** Adiciona uma seção com linhas ou parágrafos já formatados. */
    private void appendSection(StringBuilder markdown, String title, String... entries) {
        markdown.append("## ").append(title).append("\n\n");
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            markdown.append(entry);
            if (!entry.endsWith("\n")) {
                markdown.append("\n");
            }
        }
        markdown.append("\n");
    }

    /** Formata uma linha de definição de negócio. */
    private String line(String label, String value) {
        return "- **" + label + ":** " + valueOrFallback(value) + "\n";
    }

    /** Formata uma linha somente quando o campo comercial foi cadastrado. */
    private String optionalLine(String label, String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return "- **" + label + ":** " + value.trim() + "\n";
    }

    /** Formata um parágrafo livre preservando um fallback quando não houver dado cadastrado. */
    private String paragraph(String value) {
        return valueOrFallback(value) + "\n";
    }

    /** Resolve o nicho priorizando o relacionamento canônico e usando o campo legado como fallback. */
    private String resolveNiche(Product product) {
        if (product.getMarketNiche() != null && product.getMarketNiche().getName() != null) {
            return product.getMarketNiche().getName();
        }
        return product.getNiche();
    }

    /** Formata o preço comercial em reais quando ele estiver cadastrado. */
    private String formatPrice(BigDecimal price) {
        if (price == null) {
            return null;
        }
        return NumberFormat.getCurrencyInstance(BRAZIL).format(price);
    }

    /** Retorna um texto padrão para campos comerciais ainda não definidos. */
    private String valueOrFallback(String value) {
        if (value == null || value.isBlank()) {
            return "Não definido";
        }
        return value.trim();
    }
}

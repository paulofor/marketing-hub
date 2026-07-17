package com.marketinghub.product.service;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.product.Product;
import com.marketinghub.product.dto.CreateProductRequest;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.ads.InstagramAccountRepository;
import com.marketinghub.ads.InstagramAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsabilidade: gerenciar o cadastro comercial de produtos digitais.
 */
@Service
public class ProductService {
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
}

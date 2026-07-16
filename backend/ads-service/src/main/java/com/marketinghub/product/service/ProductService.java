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

    /**
     * Cria e persiste um produto comercial com seus atributos de venda e entrega.
     */
    @Transactional
    public Product createProduct(CreateProductRequest request) {
        Product product = Product.builder()
                .slug(request.getSlug())
                .name(request.getName())
                .publicUrl(request.getPublicUrl())
                .colorPalette(request.getColorPalette())
                .targetAudience(request.getTargetAudience())
                .languageStyle(request.getLanguageStyle())
                .codeModules(request.getCodeModules())
                .productType(request.getProductType())
                .commercialStatus(request.getCommercialStatus())
                .currentPriceBrl(request.getCurrentPriceBrl())
                .primaryHypothesisId(request.getPrimaryHypothesisId())
                .primaryHypothesis(request.getPrimaryHypothesis())
                .associatedExperiments(request.getAssociatedExperiments())
                .commercialNotes(request.getCommercialNotes())
                .niche(request.getNiche())
                .avatar(request.getAvatar())
                .instagramAccount(resolveAccount(request.getInstagramAccountId()))
                .marketNiche(resolveNiche(request.getMarketNicheId()))
                .explicitPain(request.getExplicitPain())
                .promise(request.getPromise())
                .uniqueMechanism(request.getUniqueMechanism())
                .tripwire(request.getTripwire())
                .riskReversal(request.getRiskReversal())
                .socialProof(request.getSocialProof())
                .checkoutMonetization(request.getCheckoutMonetization())
                .funnel(request.getFunnel())
                .creativeVolume(request.getCreativeVolume())
                .storytelling(request.getStorytelling())
                .aiCost(request.getAiCost())
                .build();
        return repository.save(product);
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

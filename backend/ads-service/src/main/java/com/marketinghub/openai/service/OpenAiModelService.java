package com.marketinghub.openai.service;

import com.marketinghub.modelos.openai.catalogo.v1.dto.OpenAiModelCatalogResponse;
import com.marketinghub.modelos.openai.catalogo.v1.service.OpenAiModelCatalogV1Service;
import com.marketinghub.openai.OpenAiModel;
import com.marketinghub.openai.OpenAiModelPricing;
import com.marketinghub.openai.OpenAiPricingPageClient;
import com.marketinghub.openai.dto.CreateOpenAiModelRequest;
import com.marketinghub.repository.jpa.openai.OpenAiModelRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Responsabilidade: manter o catálogo administrativo de modelos OpenAI e suas capacidades. */
@Service
public class OpenAiModelService {
    private static final Logger log = LoggerFactory.getLogger(OpenAiModelService.class);
    private static final String PRICING_SOURCE = "https://developers.openai.com/api/docs/pricing";
    private static final BigDecimal ZERO_PRICE = BigDecimal.ZERO.setScale(5);

    private final OpenAiModelRepository repository;
    private final OpenAiModelCatalogV1Service catalogService;
    private final OpenAiPricingPageClient pricingPageClient;

    /** Inicializa o serviço com repositórios e clientes oficiais usados para manter o catálogo de modelos OpenAI. */
    public OpenAiModelService(
            OpenAiModelRepository repository,
            OpenAiModelCatalogV1Service catalogService,
            OpenAiPricingPageClient pricingPageClient) {
        this.repository = repository;
        this.catalogService = catalogService;
        this.pricingPageClient = pricingPageClient;
    }

    /** Cria ou atualiza um modelo OpenAI buscando código, preços e capacidade nas fontes oficiais da OpenAI. */
    @Transactional
    public OpenAiModel create(CreateOpenAiModelRequest request) {
        String requestedName = normalizeRequiredName(request);
        try {
            OpenAiModelCatalogResponse catalog = catalogService.fetchAndPersistCatalog();
            String code = resolveCatalogCode(requestedName, catalog);
            boolean imageModel = catalog.imageModels().contains(code);
            OpenAiModel model = repository.findByCode(code).orElseGet(OpenAiModel::new);
            applyOfficialData(model, requestedName, code, imageModel);
            return repository.save(model);
        } catch (RuntimeException ex) {
            log.error(
                    "Falha ao criar modelo OpenAI via fontes oficiais; operation=openai-model-create name={}",
                    requestedName,
                    ex);
            throw ex;
        }
    }

    /** Busca um modelo OpenAI pelo identificador para edição ou detalhe. */
    @Transactional(readOnly = true)
    public OpenAiModel get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    /** Lista modelos OpenAI cadastrados ordenados por nome para seleção nas telas. */
    @Transactional(readOnly = true)
    public List<OpenAiModel> list() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    /** Atualiza preços e capacidades de um modelo OpenAI existente pelos campos administrativos editáveis. */
    @Transactional
    public OpenAiModel update(Long id, CreateOpenAiModelRequest request) {
        OpenAiModel model = repository.findById(id).orElseThrow();
        apply(model, request);
        return repository.save(model);
    }

    /** Valida e normaliza o nome informado na tela de criação do modelo. */
    private String normalizeRequiredName(CreateOpenAiModelRequest request) {
        String name = request == null ? null : request.getName();
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Informe o nome do modelo OpenAI.");
        }
        return name.trim();
    }

    /** Resolve o código canônico retornado pela API /models da OpenAI a partir do nome digitado pelo usuário. */
    private String resolveCatalogCode(String requestedName, OpenAiModelCatalogResponse catalog) {
        Set<String> officialCodes = Stream.concat(catalog.textModels().stream(), catalog.imageModels().stream())
                .collect(Collectors.toSet());
        for (String candidate : buildCodeCandidates(requestedName)) {
            if (officialCodes.contains(candidate)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Modelo OpenAI não encontrado na API oficial /models: " + requestedName);
    }

    /** Monta variações simples para aceitar tanto código canônico quanto nome legível digitado pelo usuário. */
    private List<String> buildCodeCandidates(String requestedName) {
        String normalized = requestedName.trim().toLowerCase(Locale.ROOT);
        String withoutExtraSpaces = normalized.replaceAll("\\s+", " ");
        String dashed = withoutExtraSpaces.replace(' ', '-');
        String gptDashed = withoutExtraSpaces.replaceFirst("^gpt ", "gpt-").replace(' ', '-');
        return Stream.of(normalized, withoutExtraSpaces, dashed, gptDashed)
                .map(value -> value.replace('–', '-').replace('—', '-'))
                .map(value -> value.replaceAll("-+", "-"))
                .distinct()
                .toList();
    }

    /** Aplica na entidade persistida os dados oficiais resolvidos a partir da API e da página de preços da OpenAI. */
    private void applyOfficialData(OpenAiModel model, String requestedName, String code, boolean imageModel) {
        Optional<OpenAiModelPricing> pricing = pricingPageClient.fetchTextModelPricing().stream()
                .filter(price -> Objects.equals(price.code(), code))
                .findFirst();
        model.setName(toDisplayName(requestedName, code));
        model.setCode(code);
        if (pricing.isPresent()) {
            applyPricing(model, pricing.get());
        } else {
            applyZeroPricing(model);
        }
        model.setAcceptsImageInput(imageModel);
        model.setPricingSource(PRICING_SOURCE);
        model.setLastPricingSyncAt(Instant.now());
    }

    /** Aplica os preços oficiais de texto na entidade usada para cálculo financeiro. */
    private void applyPricing(OpenAiModel model, OpenAiModelPricing pricing) {
        model.setPriceInputStandard(pricing.priceInputStandard());
        model.setPriceInputCachedStandard(pricing.priceInputCachedStandard());
        model.setPriceOutputStandard(pricing.priceOutputStandard());
        model.setPriceInputBatch(pricing.priceInputBatch());
        model.setPriceInputCachedBatch(pricing.priceInputCachedBatch());
        model.setPriceOutputBatch(pricing.priceOutputBatch());
    }

    /** Preenche preços zerados quando a fonte oficial de tokens não expõe preço compatível para o modelo encontrado. */
    private void applyZeroPricing(OpenAiModel model) {
        model.setPriceInputStandard(ZERO_PRICE);
        model.setPriceInputCachedStandard(ZERO_PRICE);
        model.setPriceOutputStandard(ZERO_PRICE);
        model.setPriceInputBatch(ZERO_PRICE);
        model.setPriceInputCachedBatch(ZERO_PRICE);
        model.setPriceOutputBatch(ZERO_PRICE);
    }

    /** Gera um nome legível quando a fonte de preço não fornece nome textual para o código do modelo. */
    private String toDisplayName(String requestedName, String code) {
        if (!requestedName.equalsIgnoreCase(code)) {
            return requestedName;
        }
        return code.toUpperCase(Locale.ROOT).replace('-', ' ');
    }

    /** Aplica os campos editáveis do request na entidade persistida. */
    private void apply(OpenAiModel model, CreateOpenAiModelRequest request) {
        model.setName(request.getName());
        model.setCode(request.getCode());
        model.setPriceInputStandard(request.getPriceInputStandard());
        model.setPriceInputCachedStandard(request.getPriceInputCachedStandard());
        model.setPriceOutputStandard(request.getPriceOutputStandard());
        model.setPriceInputBatch(request.getPriceInputBatch());
        model.setPriceInputCachedBatch(request.getPriceInputCachedBatch());
        model.setPriceOutputBatch(request.getPriceOutputBatch());
        model.setAcceptsImageInput(request.isAcceptsImageInput());
    }
}

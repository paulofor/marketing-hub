package com.marketinghub.productai.service;

import com.marketinghub.deliverable.Deliverable;
import com.marketinghub.deliverable.DeliverablePackage;
import com.marketinghub.experiment.ExperimentCampaignObjective;
import com.marketinghub.experiment.ExperimentStage;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.OfferType;
import com.marketinghub.productai.ProductAiSubtype;
import com.marketinghub.productai.dto.PersonalizedSamplePreparationDto;
import com.marketinghub.productai.dto.ProductAiExperimentPreparationDto;
import com.marketinghub.repository.jpa.deliverable.DeliverablePackageRepository;
import com.marketinghub.repository.jpa.deliverable.DeliverableRepository;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: preparar e validar hipóteses Produto IA antes da criação de experimento. */
@Service
public class ProductAiExperimentPreparationService {
    private static final String SAMPLE_DESCRIPTION_BLOCKER = "Descrição da amostra/entrega personalizada";
    private static final BigDecimal DEFAULT_PERSONALIZED_SAMPLE_PRICE = new BigDecimal("27.00");
    private static final BigDecimal DEFAULT_VISUAL_PREVIEW_PRICE = new BigDecimal("9.90");

    private final HypothesisRepository hypothesisRepository;
    private final DeliverableRepository deliverableRepository;
    private final DeliverablePackageRepository deliverablePackageRepository;

    /** Inicializa o serviço com o repositório de hipóteses. */
    public ProductAiExperimentPreparationService(
            HypothesisRepository hypothesisRepository,
            DeliverableRepository deliverableRepository,
            DeliverablePackageRepository deliverablePackageRepository) {
        this.hypothesisRepository = hypothesisRepository;
        this.deliverableRepository = deliverableRepository;
        this.deliverablePackageRepository = deliverablePackageRepository;
    }

    /** Retorna o diagnóstico de preparo de Produto IA para uma hipótese. */
    @Transactional(readOnly = true)
    public ProductAiExperimentPreparationDto prepare(UUID hypothesisId) {
        Hypothesis hypothesis = hypothesisRepository.findById(hypothesisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "hypothesis not found"));
        return buildPreparation(hypothesis);
    }

    /** Bloqueia criação de experimento Produto IA quando a hipótese ainda não tem rastreabilidade mínima. */
    @Transactional(readOnly = true)
    public void assertReadyForExperiment(UUID hypothesisId) {
        Hypothesis hypothesis = hypothesisRepository.findById(hypothesisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "hypothesis not found"));
        assertReadyForExperiment(hypothesisId, hypothesis.getProductAiSubtype());
    }

    /** Bloqueia criação de experimento quando a hipótese preparada tem subtipo diferente do experimento. */
    @Transactional(readOnly = true)
    public void assertReadyForExperiment(UUID hypothesisId, ProductAiSubtype expectedSubtype) {
        ProductAiExperimentPreparationDto preparation = prepare(hypothesisId);
        if (!preparation.ready()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Produto IA incompleto para experimento: " + String.join(", ", preparation.blockers()));
        }
        if (expectedSubtype != null && preparation.productAiSubtype() != expectedSubtype) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Hipótese preparada para " + preparation.productAiSubtype()
                            + ", mas o experimento solicitou " + expectedSubtype);
        }
    }

    /** Completa uma hipótese rastreada do sistema para o MVP de amostra personalizada sem criar experimento manual. */
    @Transactional
    public PersonalizedSamplePreparationDto preparePersonalizedSampleHypothesis(UUID hypothesisId) {
        return prepareProductAiHypothesis(hypothesisId, ProductAiSubtype.AI_PERSONALIZED_SAMPLE);
    }

    /** Prepara a hipótese original ou uma variante paralela para o subtipo Produto IA informado. */
    @Transactional
    public PersonalizedSamplePreparationDto prepareProductAiHypothesis(
            UUID hypothesisId,
            ProductAiSubtype requestedSubtype) {
        Hypothesis hypothesis = hypothesisRepository.findById(hypothesisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "hypothesis not found"));
        validateTraceableHypothesis(hypothesis);
        ProductAiSubtype subtype = requestedSubtype != null ? requestedSubtype : ProductAiSubtype.AI_PERSONALIZED_SAMPLE;
        Hypothesis target = resolvePreparationTarget(hypothesis, subtype);

        target.setProductAiSubtype(subtype);
        if (target.getOfferType() == null) {
            target.setOfferType(OfferType.TRIPWIRE);
        }
        if (target.getPrice() == null) {
            target.setPrice(defaultPrice(subtype));
        }
        if (!StringUtils.hasText(target.getEntrega())) {
            target.setEntrega(buildDeliveryDescription(target, subtype));
        }

        DeliverablePackage offerPackage = ensureOfferPackage(target, subtype);
        target.setOfferPackage(offerPackage);
        Hypothesis saved = hypothesisRepository.save(target);
        ProductAiExperimentPreparationDto preparation = buildPreparation(saved);
        Deliverable deliverable = offerPackage.getDeliverables().iterator().next();

        return new PersonalizedSamplePreparationDto(
                saved.getId(),
                saved.getTitle(),
                saved.getProductAiSubtype(),
                saved.getPrice(),
                offerPackage.getId(),
                offerPackage.getName(),
                deliverable.getId(),
                deliverable.getTitle(),
                preparation);
    }

    /** Monta o diagnóstico e o rascunho aplicável a partir dos campos persistidos da hipótese. */
    private ProductAiExperimentPreparationDto buildPreparation(Hypothesis hypothesis) {
        var blockers = new ArrayList<String>();
        ProductAiSubtype subtype = hypothesis.getProductAiSubtype();
        require(blockers, subtype != null, "Subtipo Produto IA");
        require(blockers, hypothesis.getMarketNiche() != null, "Nicho/contexto");
        requireText(blockers, hypothesis.getProblem(), "Dor principal");
        requireText(blockers, hypothesis.getPersona(), "Persona");
        requireText(blockers, hypothesis.getPromise(), "Promessa");
        requireText(blockers, resolveMechanism(hypothesis), "Mecanismo");
        require(blockers, hypothesis.getPrice() != null, "Preço");
        require(blockers, hypothesis.getOfferPackage() != null, "Pacote de oferta");
        require(blockers, hasDeliverables(hypothesis), "Entregáveis do pacote");
        requireText(blockers, hypothesis.getEntrega(), SAMPLE_DESCRIPTION_BLOCKER);

        boolean ready = blockers.isEmpty();
        ProductAiExperimentPreparationDto.ProductAiExperimentDraftDto draft = ready
                ? new ProductAiExperimentPreparationDto.ProductAiExperimentDraftDto(
                        ExperimentType.LOW_TICKET_PRODUCT,
                        subtype,
                        subtype == ProductAiSubtype.AI_PERSONALIZED_SAMPLE
                                ? ExperimentStage.SAMPLE
                                : ExperimentStage.AD,
                        ExperimentCampaignObjective.SALES,
                        primaryVariable(subtype),
                        primaryMetric(subtype),
                        hypothesis.getPrice())
                : null;

        return new ProductAiExperimentPreparationDto(
                hypothesis.getId(),
                hypothesis.getTitle(),
                hypothesis.getProductAiSubtype(),
                ready,
                blockers,
                draft);
    }

    /** Retorna o mecanismo mais específico disponível na hipótese. */
    private String resolveMechanism(Hypothesis hypothesis) {
        return StringUtils.hasText(hypothesis.getUniqueMechanism())
                ? hypothesis.getUniqueMechanism()
                : hypothesis.getMechanism();
    }

    /** Verifica se a hipótese possui pacote com ao menos um entregável persistido. */
    private boolean hasDeliverables(Hypothesis hypothesis) {
        return hypothesis.getOfferPackage() != null
                && hypothesis.getOfferPackage().getDeliverables() != null
                && !hypothesis.getOfferPackage().getDeliverables().isEmpty();
    }

    /** Bloqueia preparo quando a hipótese ainda não foi gerada com base comercial suficiente pelo sistema. */
    private void validateTraceableHypothesis(Hypothesis hypothesis) {
        var blockers = new ArrayList<String>();
        require(blockers, hypothesis.getMarketNiche() != null, "Nicho/contexto");
        requireText(blockers, hypothesis.getProblem(), "Dor principal");
        requireText(blockers, hypothesis.getPersona(), "Persona");
        requireText(blockers, hypothesis.getPromise(), "Promessa");
        requireText(blockers, resolveMechanism(hypothesis), "Mecanismo");
        if (!blockers.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Hipótese sem rastreabilidade mínima para Produto IA: " + String.join(", ", blockers));
        }
    }

    /** Resolve se o preparo atualiza a hipótese original ou cria uma variante paralela para preservar o funil atual. */
    private Hypothesis resolvePreparationTarget(Hypothesis source, ProductAiSubtype subtype) {
        if (source.getProductAiSubtype() == null || source.getProductAiSubtype() == subtype) {
            return source;
        }
        return hypothesisRepository.findByMarketNicheId(source.getMarketNiche().getId()).stream()
                .filter(candidate -> candidate.getProductAiSubtype() == subtype)
                .filter(candidate -> sameText(candidate.getProblem(), source.getProblem()))
                .filter(candidate -> sameText(candidate.getPromise(), source.getPromise()))
                .findFirst()
                .orElseGet(() -> createVariantHypothesis(source, subtype));
    }

    /** Cria uma hipótese variante com a mesma base comercial e subtipo diferente para comparação de funis. */
    private Hypothesis createVariantHypothesis(Hypothesis source, ProductAiSubtype subtype) {
        return Hypothesis.builder()
                .marketNiche(source.getMarketNiche())
                .title(buildVariantTitle(source, subtype))
                .premiseAngle(source.getPremiseAngle())
                .promise(source.getPromise())
                .problem(source.getProblem())
                .persona(source.getPersona())
                .mechanism(source.getMechanism())
                .uniqueMechanism(source.getUniqueMechanism())
                .entrega(buildDeliveryDescription(source, subtype))
                .prompt(source.getPrompt())
                .frameworkJson(source.getFrameworkJson())
                .model(source.getModel())
                .offerType(OfferType.TRIPWIRE)
                .price(defaultPrice(subtype))
                .kpiTargetCpl(source.getKpiTargetCpl())
                .productAiSubtype(subtype)
                .status(source.getStatus())
                .generatedAt(source.getGeneratedAt())
                .build();
    }

    /** Monta um título claro para a hipótese variante sem sobrescrever a hipótese original. */
    private String buildVariantTitle(Hypothesis source, ProductAiSubtype subtype) {
        String suffix = subtype == ProductAiSubtype.AI_VISUAL_PREVIEW
                ? " - Prévia paga"
                : " - " + subtype.name();
        String base = StringUtils.hasText(source.getTitle()) ? source.getTitle().trim() : "Produto IA";
        return base.endsWith(suffix) ? base : base + suffix;
    }

    /** Garante um pacote de oferta mínimo vinculado à hipótese e ao nicho já existentes. */
    private DeliverablePackage ensureOfferPackage(Hypothesis hypothesis, ProductAiSubtype subtype) {
        if (hasDeliverables(hypothesis)) {
            return hypothesis.getOfferPackage();
        }
        Deliverable deliverable = deliverableRepository.save(Deliverable.builder()
                .niche(hypothesis.getMarketNiche())
                .title(deliverableTitle(subtype))
                .description(buildDeliveryDescription(hypothesis, subtype))
                .content(buildDeliveryContent(hypothesis, subtype))
                .build());
        if (hypothesis.getOfferPackage() != null) {
            DeliverablePackage existingPackage = hypothesis.getOfferPackage();
            existingPackage.setDeliverables(new LinkedHashSet<>(List.of(deliverable)));
            if (!StringUtils.hasText(existingPackage.getDescription())) {
                existingPackage.setDescription(packageDescription(subtype));
            }
            return deliverablePackageRepository.save(existingPackage);
        }
        DeliverablePackage offerPackage = deliverablePackageRepository.save(DeliverablePackage.builder()
                .hypothesis(hypothesis)
                .name(packageName(subtype))
                .description(packageDescription(subtype))
                .deliverables(new LinkedHashSet<>(List.of(deliverable)))
                .build());
        return offerPackage;
    }

    /** Retorna o preço inicial recomendado para cada subtipo Produto IA preparado pelo sistema. */
    private BigDecimal defaultPrice(ProductAiSubtype subtype) {
        return subtype == ProductAiSubtype.AI_VISUAL_PREVIEW
                ? DEFAULT_VISUAL_PREVIEW_PRICE
                : DEFAULT_PERSONALIZED_SAMPLE_PRICE;
    }

    /** Define a variável primária canônica do experimento conforme o subtipo preparado. */
    private String primaryVariable(ProductAiSubtype subtype) {
        return subtype == ProductAiSubtype.AI_VISUAL_PREVIEW
                ? "Prévia visual paga"
                : "Amostra visual personalizada";
    }

    /** Define a métrica primária canônica do experimento conforme o subtipo preparado. */
    private String primaryMetric(ProductAiSubtype subtype) {
        return subtype == ProductAiSubtype.AI_VISUAL_PREVIEW
                ? "Compra aprovada da prévia e clique no checkout"
                : "Compra aprovada e custo de IA por compra";
    }

    /** Retorna o nome do pacote mínimo conforme a estratégia do funil Produto IA. */
    private String packageName(ProductAiSubtype subtype) {
        return subtype == ProductAiSubtype.AI_VISUAL_PREVIEW
                ? "Pacote inicial de prévia paga"
                : "Pacote inicial de amostra personalizada";
    }

    /** Retorna a descrição operacional do pacote mínimo conforme a estratégia do funil Produto IA. */
    private String packageDescription(ProductAiSubtype subtype) {
        return subtype == ProductAiSubtype.AI_VISUAL_PREVIEW
                ? "Pacote mínimo para testar Produto IA com venda de entrada antes da personalização completa."
                : "Pacote mínimo para testar Produto IA com amostra visual personalizada antes da compra.";
    }

    /** Retorna o título do entregável mínimo conforme a estratégia do funil Produto IA. */
    private String deliverableTitle(ProductAiSubtype subtype) {
        return subtype == ProductAiSubtype.AI_VISUAL_PREVIEW
                ? "Prévia visual personalizada"
                : "Amostra visual personalizada";
    }

    /** Descreve a entrega personalizada a partir da dor, promessa e mecanismo já persistidos. */
    private String buildDeliveryDescription(Hypothesis hypothesis, ProductAiSubtype subtype) {
        if (subtype == ProductAiSubtype.AI_VISUAL_PREVIEW) {
            return "Prévia visual personalizada paga para mostrar ao comprador, depois do checkout, uma direção concreta da promessa: "
                    + compact(hypothesis.getPromise()) + ".";
        }
        return "Amostra visual personalizada para mostrar ao lead, antes da compra, uma prévia concreta da promessa: "
                + compact(hypothesis.getPromise()) + ".";
    }

    /** Registra o conteúdo funcional mínimo do entregável para manter rastreabilidade da oferta. */
    private String buildDeliveryContent(Hypothesis hypothesis, ProductAiSubtype subtype) {
        return String.join("\n",
                "Dor: " + compact(hypothesis.getProblem()),
                "Persona: " + compact(hypothesis.getPersona()),
                "Promessa: " + compact(hypothesis.getPromise()),
                "Mecanismo: " + compact(resolveMechanism(hypothesis)),
                "Entrega: " + deliverableTitle(subtype)
                        + " gerada por IA para tangibilizar a transformação prometida.");
    }

    /** Compara textos normalizados para localizar variante já existente da mesma base comercial. */
    private boolean sameText(String first, String second) {
        return compact(first).equalsIgnoreCase(compact(second));
    }

    /** Normaliza texto usado no pacote mínimo sem remover o significado comercial. */
    private String compact(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    /** Registra bloqueio quando a condição obrigatória não foi atendida. */
    private void require(ArrayList<String> blockers, boolean condition, String label) {
        if (!condition) {
            blockers.add(label);
        }
    }

    /** Registra bloqueio quando o texto obrigatório está ausente. */
    private void requireText(ArrayList<String> blockers, String value, String label) {
        require(blockers, StringUtils.hasText(value), label);
    }
}

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Responsabilidade: validar se uma hipótese Produto IA tem insumos rastreáveis para virar experimento. */
@Service
public class ProductAiExperimentPreparationService {
    private static final String SAMPLE_DESCRIPTION_BLOCKER = "Descrição da amostra/entrega personalizada";
    private static final BigDecimal DEFAULT_PERSONALIZED_SAMPLE_PRICE = new BigDecimal("27.00");

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
        ProductAiExperimentPreparationDto preparation = prepare(hypothesisId);
        if (!preparation.ready()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Produto IA incompleto para experimento: " + String.join(", ", preparation.blockers()));
        }
    }

    /** Completa uma hipótese rastreada do sistema para o MVP de amostra personalizada sem criar experimento manual. */
    @Transactional
    public PersonalizedSamplePreparationDto preparePersonalizedSampleHypothesis(UUID hypothesisId) {
        Hypothesis hypothesis = hypothesisRepository.findById(hypothesisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "hypothesis not found"));
        validateTraceableHypothesis(hypothesis);

        hypothesis.setProductAiSubtype(ProductAiSubtype.AI_PERSONALIZED_SAMPLE);
        if (hypothesis.getOfferType() == null) {
            hypothesis.setOfferType(OfferType.TRIPWIRE);
        }
        if (hypothesis.getPrice() == null) {
            hypothesis.setPrice(DEFAULT_PERSONALIZED_SAMPLE_PRICE);
        }
        if (!StringUtils.hasText(hypothesis.getEntrega())) {
            hypothesis.setEntrega(buildSampleDescription(hypothesis));
        }

        DeliverablePackage offerPackage = ensureOfferPackage(hypothesis);
        hypothesis.setOfferPackage(offerPackage);
        Hypothesis saved = hypothesisRepository.save(hypothesis);
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
        require(blockers, hypothesis.getProductAiSubtype() == ProductAiSubtype.AI_PERSONALIZED_SAMPLE,
                "Subtipo AI_PERSONALIZED_SAMPLE");
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
                        ProductAiSubtype.AI_PERSONALIZED_SAMPLE,
                        ExperimentStage.SAMPLE,
                        ExperimentCampaignObjective.SALES,
                        "Amostra visual personalizada",
                        "Compra aprovada e custo de IA por compra",
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

    /** Garante um pacote de oferta mínimo vinculado à hipótese e ao nicho já existentes. */
    private DeliverablePackage ensureOfferPackage(Hypothesis hypothesis) {
        if (hasDeliverables(hypothesis)) {
            return hypothesis.getOfferPackage();
        }
        Deliverable deliverable = deliverableRepository.save(Deliverable.builder()
                .niche(hypothesis.getMarketNiche())
                .title("Amostra visual personalizada")
                .description(buildSampleDescription(hypothesis))
                .content(buildSampleContent(hypothesis))
                .build());
        if (hypothesis.getOfferPackage() != null) {
            DeliverablePackage existingPackage = hypothesis.getOfferPackage();
            existingPackage.setDeliverables(new LinkedHashSet<>(List.of(deliverable)));
            if (!StringUtils.hasText(existingPackage.getDescription())) {
                existingPackage.setDescription(
                        "Pacote mínimo para testar Produto IA com amostra visual personalizada antes da compra.");
            }
            return deliverablePackageRepository.save(existingPackage);
        }
        DeliverablePackage offerPackage = deliverablePackageRepository.save(DeliverablePackage.builder()
                .hypothesis(hypothesis)
                .name("Pacote inicial de amostra personalizada")
                .description("Pacote mínimo para testar Produto IA com amostra visual personalizada antes da compra.")
                .deliverables(new LinkedHashSet<>(List.of(deliverable)))
                .build());
        return offerPackage;
    }

    /** Descreve a amostra personalizada a partir da dor, promessa e mecanismo já persistidos. */
    private String buildSampleDescription(Hypothesis hypothesis) {
        return "Amostra visual personalizada para mostrar ao lead, antes da compra, uma prévia concreta da promessa: "
                + compact(hypothesis.getPromise()) + ".";
    }

    /** Registra o conteúdo funcional mínimo do entregável para manter rastreabilidade da oferta. */
    private String buildSampleContent(Hypothesis hypothesis) {
        return String.join("\n",
                "Dor: " + compact(hypothesis.getProblem()),
                "Persona: " + compact(hypothesis.getPersona()),
                "Promessa: " + compact(hypothesis.getPromise()),
                "Mecanismo: " + compact(resolveMechanism(hypothesis)),
                "Entrega: imagem/amostra personalizada gerada por IA para tangibilizar a transformação prometida.");
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

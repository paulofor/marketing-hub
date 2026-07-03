package com.marketinghub.productai.service;

import com.marketinghub.experiment.ExperimentCampaignObjective;
import com.marketinghub.experiment.ExperimentStage;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.productai.ProductAiSubtype;
import com.marketinghub.productai.dto.ProductAiExperimentPreparationDto;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import java.util.ArrayList;
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

    private final HypothesisRepository hypothesisRepository;

    /** Inicializa o serviço com o repositório de hipóteses. */
    public ProductAiExperimentPreparationService(HypothesisRepository hypothesisRepository) {
        this.hypothesisRepository = hypothesisRepository;
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

package com.marketinghub.experiment.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCreationSource;
import com.marketinghub.experiment.service.construction.ExperimentConstructionDto;
import com.marketinghub.experiment.service.construction.ExperimentConstructionItemDto;
import com.marketinghub.experiment.service.construction.ExperimentConstructionSectionDto;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Consolida a leitura de como um experimento foi construído a partir das entidades oficiais.
 */
@Service
public class ExperimentConstructionService {
    private final ExperimentRepository experimentRepository;

    /** Inicializa o serviço com o repositório oficial de experimentos. */
    public ExperimentConstructionService(ExperimentRepository experimentRepository) {
        this.experimentRepository = experimentRepository;
    }

    /** Retorna a construção comercial e operacional do experimento informado. */
    @Transactional(readOnly = true)
    public ExperimentConstructionDto getConstruction(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "experiment not found"));
        MarketNiche niche = experiment.getNiche();
        Hypothesis hypothesis = experiment.getHypothesisRef();
        boolean manualFlow = experiment.getCreationSource() == ExperimentCreationSource.MANUAL_FLOW;
        List<ExperimentConstructionSectionDto> sections = new ArrayList<>();
        sections.add(buildOriginSection(experiment, niche, hypothesis, manualFlow));
        sections.add(buildCommercialThesisSection(experiment, hypothesis));
        sections.add(buildOfferSection(experiment, hypothesis));
        sections.add(buildCampaignSection(experiment));
        sections.add(buildArtifactsSection(experiment));
        return new ExperimentConstructionDto(
                experiment.getId(),
                experiment.getName(),
                experiment.getCreationSource(),
                manualFlow,
                experiment.getCreatedAt(),
                experiment.getUpdatedAt(),
                sections);
    }

    /** Monta a seção que mostra a cadeia criada pelo fluxo. */
    private ExperimentConstructionSectionDto buildOriginSection(
            Experiment experiment,
            MarketNiche niche,
            Hypothesis hypothesis,
            boolean manualFlow) {
        List<ExperimentConstructionItemDto> items = new ArrayList<>();
        add(items, "Fluxo de criação", manualFlow ? "Manual, sem execução de IA" : "Fluxo sistêmico");
        add(items, "Nicho criado/usado", niche != null ? niche.getName() : null);
        add(items, "Descrição do nicho", niche != null ? niche.getDescription() : null);
        add(items, "Base de público", niche != null ? niche.getBaseSegmentation() : null);
        add(items, "Hipótese vinculada", hypothesis != null ? hypothesis.getTitle() : null);
        add(items, "Experimento", experiment.getName());
        return new ExperimentConstructionSectionDto(
                "Origem e cadeia criada",
                "Mostra a trilha oficial usada para transformar a entrada manual em experimento publicável.",
                items);
    }

    /** Monta a seção com a tese comercial do experimento. */
    private ExperimentConstructionSectionDto buildCommercialThesisSection(Experiment experiment, Hypothesis hypothesis) {
        List<ExperimentConstructionItemDto> items = new ArrayList<>();
        add(items, "Público/persona", hypothesis != null ? hypothesis.getPersona() : null);
        add(items, "Dor principal", firstText(experiment.getSinglePain(), hypothesis != null ? hypothesis.getProblem() : null));
        add(items, "Promessa", firstText(experiment.getFunnelPromise(), hypothesis != null ? hypothesis.getPromise() : null));
        add(items, "Mecanismo", hypothesis != null ? firstText(hypothesis.getUniqueMechanism(), hypothesis.getMechanism()) : null);
        add(items, "Narrativa do experimento", experiment.getHypothesis());
        add(items, "Prova ou regra de sucesso", hypothesis != null ? hypothesis.getSuccessRule() : null);
        return new ExperimentConstructionSectionDto(
                "Tese comercial",
                "Resume a aposta de venda que este experimento tenta validar.",
                items);
    }

    /** Monta a seção de oferta e produto testado. */
    private ExperimentConstructionSectionDto buildOfferSection(Experiment experiment, Hypothesis hypothesis) {
        List<ExperimentConstructionItemDto> items = new ArrayList<>();
        add(items, "Recompensa/isca", firstText(experiment.getFreeReward(), hypothesis != null ? hypothesis.getEntrega() : null));
        add(items, "CTA principal", experiment.getPrimaryCta());
        add(items, "Tipo de experimento", enumText(experiment.getExperimentType()));
        add(items, "Subtipo Produto IA", enumText(experiment.getProductAiSubtype()));
        add(items, "Preço de teste", experiment.getUnitPrice() != null ? "R$ " + experiment.getUnitPrice() : null);
        add(items, "Oferta da hipótese", hypothesis != null ? enumText(hypothesis.getOfferType()) : null);
        return new ExperimentConstructionSectionDto(
                "Oferta e produto testado",
                "Mostra o que foi prometido ao lead e qual ação comercial o teste quer provocar.",
                items);
    }

    /** Monta a seção de campanha, métrica e critérios de validação. */
    private ExperimentConstructionSectionDto buildCampaignSection(Experiment experiment) {
        List<ExperimentConstructionItemDto> items = new ArrayList<>();
        add(items, "Canal/plataforma", enumText(experiment.getPlatform()));
        add(items, "Objetivo de campanha", enumText(experiment.getCampaignObjective()));
        add(items, "Orçamento diário", experiment.getDailyBudget() != null ? "R$ " + experiment.getDailyBudget() : null);
        add(items, "CPL alvo", experiment.getKpiTargetCpl() != null ? "R$ " + experiment.getKpiTargetCpl() : null);
        add(items, "Tamanho de amostra", experiment.getSampleSize() != null ? String.valueOf(experiment.getSampleSize()) : null);
        add(items, "Variável principal", experiment.getPrimaryVariable());
        add(items, "Métrica principal", experiment.getPrimaryMetric());
        add(items, "Ângulos criativos", experiment.getCreativeTextPrompt());
        return new ExperimentConstructionSectionDto(
                "Plano de validação",
                "Explica como a aposta deve ser testada em mídia e quais sinais importam.",
                items);
    }

    /** Monta a seção de artefatos que já existem para execução/publicação. */
    private ExperimentConstructionSectionDto buildArtifactsSection(Experiment experiment) {
        List<ExperimentConstructionItemDto> items = new ArrayList<>();
        add(items, "Texto de anúncio", statusText(experiment.getAdCopy()));
        add(items, "Briefing de imagem", statusText(experiment.getAdImageBriefing()));
        add(items, "Copy da landing", statusText(experiment.getLandingPageCopy()));
        add(items, "Wireframe da landing", statusText(experiment.getLandingPageWireframe()));
        add(items, "Planejamento de imagens", statusText(experiment.getLandingPageImagePlanning()));
        add(items, "Preset visual", statusText(experiment.getLandingPageDesignPreset()));
        add(items, "HTML GeraLanding", statusText(experiment.getHtmlGeraLanding()));
        add(items, "Landing publicável", statusText(experiment.getLandingPageHtml()));
        add(items, "Entregáveis", statusText(experiment.getLandingPageDeliverables()));
        return new ExperimentConstructionSectionDto(
                "Ativos construídos",
                "Indica quais peças comerciais já existem para transformar o experimento em campanha.",
                items);
    }

    /** Adiciona um item apenas quando houver valor útil para exibição. */
    private void add(List<ExperimentConstructionItemDto> items, String label, String value) {
        if (StringUtils.hasText(value)) {
            items.add(new ExperimentConstructionItemDto(label, value.trim()));
        }
    }

    /** Retorna o primeiro texto útil entre duas fontes oficiais. */
    private String firstText(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    /** Converte enum opcional em texto simples. */
    private String enumText(Enum<?> value) {
        return value != null ? value.name() : null;
    }

    /** Resume se um artefato foi criado sem despejar o payload técnico na aba. */
    private String statusText(String value) {
        return StringUtils.hasText(value) ? "Construído" : "Ainda não construído";
    }
}

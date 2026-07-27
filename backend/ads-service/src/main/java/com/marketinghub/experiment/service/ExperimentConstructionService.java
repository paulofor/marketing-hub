package com.marketinghub.experiment.service;

import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCreationSource;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.service.construction.ExperimentConstructionDto;
import com.marketinghub.experiment.service.construction.ExperimentConstructionItemDto;
import com.marketinghub.experiment.service.construction.ExperimentConstructionSectionDto;
import com.marketinghub.experiment.service.construction.ExperimentConstructionStepDto;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Consolida a leitura de como um experimento foi construído a partir das entidades oficiais. */
@Service
public class ExperimentConstructionService {
  private final ExperimentRepository experimentRepository;
  private final CreativeRepository creativeRepository;

  /** Inicializa o serviço com as fontes oficiais de construção do experimento. */
  public ExperimentConstructionService(
      ExperimentRepository experimentRepository, CreativeRepository creativeRepository) {
    this.experimentRepository = experimentRepository;
    this.creativeRepository = creativeRepository;
  }

  /** Retorna a construção comercial e operacional do experimento informado. */
  @Transactional(readOnly = true)
  public ExperimentConstructionDto getConstruction(Long experimentId) {
    Experiment experiment =
        experimentRepository
            .findById(experimentId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "experiment not found"));
    MarketNiche niche = experiment.getNiche();
    Hypothesis hypothesis = experiment.getHypothesisRef();
    boolean manualFlow = experiment.getCreationSource() == ExperimentCreationSource.MANUAL_FLOW;
    List<ExperimentConstructionSectionDto> sections = new ArrayList<>();
    sections.add(buildNichePainSection(experiment, niche, hypothesis, manualFlow));
    sections.add(buildHypothesisSection(experiment, hypothesis));
    sections.add(buildMdsMechanismSection(hypothesis));
    sections.add(buildOfferProofSection(experiment, hypothesis));
    sections.add(buildExperimentFeoSection(experiment));
    sections.add(buildFunnelSection(experiment));
    sections.add(buildArtifactsSection(experiment));
    return new ExperimentConstructionDto(
        experiment.getId(),
        experiment.getName(),
        experiment.getCreationSource(),
        manualFlow,
        experiment.getCreatedAt(),
        experiment.getUpdatedAt(),
        buildFlowSteps(experiment, niche, hypothesis),
        sections);
  }

  /** Monta as etapas do cockpit com validação calculada no backend. */
  private List<ExperimentConstructionStepDto> buildFlowSteps(
      Experiment experiment, MarketNiche niche, Hypothesis hypothesis) {
    boolean hasNichePain =
        niche != null
            && StringUtils.hasText(niche.getName())
            && StringUtils.hasText(
                firstText(
                    experiment.getSinglePain(),
                    hypothesis != null ? hypothesis.getProblem() : null));
    boolean hasHypothesis =
        StringUtils.hasText(experiment.getHypothesis())
            || (hypothesis != null && StringUtils.hasText(hypothesis.getPersona()));
    boolean hasMdsMechanism =
        hypothesis != null
            && StringUtils.hasText(
                firstText(hypothesis.getUniqueMechanism(), hypothesis.getMechanism()));
    boolean hasOfferProof =
        StringUtils.hasText(
                firstText(
                    experiment.getFunnelPromise(),
                    hypothesis != null ? hypothesis.getPromise() : null))
            && StringUtils.hasText(experiment.getPrimaryCta())
            && (experiment.getUnitPrice() != null
                || (experiment.getExperimentType() != ExperimentType.LOW_TICKET_PRODUCT
                    && experiment.getExperimentType()
                        != ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL));
    boolean hasExperimentAssets =
        creativeRepository.existsByExperimentIdAndStatusAndUsableImage(
                experiment.getId(), CreativeStatus.READY)
            || StringUtils.hasText(experiment.getAdCopy())
            || StringUtils.hasText(experiment.getAdImageBriefing());
    boolean hasFeoDeliverables = StringUtils.hasText(experiment.getLandingPageDeliverables());
    boolean hasFunnelReady =
        StringUtils.hasText(experiment.getLandingPageHtml())
            && StringUtils.hasText(experiment.getFollowUpActionUrl());

    return List.of(
        step(
            "niche-pain",
            "Nicho/dor",
            "Confirmar público, dor raiz e desejo que reduzem esforço ou afastam dor.",
            "overview",
            "Ver base",
            hasNichePain),
        step(
            "hypothesis",
            "Hipótese",
            "Organizar a aposta comercial sem fechar promessa antes da evidência.",
            "content-structure",
            "Ver estrutura",
            hasHypothesis),
        step(
            "mds",
            "MDS",
            "Descobrir mecanismo plausível, limites e evidências para sustentar a promessa.",
            "content-structure",
            "Preparar mecanismo",
            hasMdsMechanism),
        step(
            "offer-proof",
            "Oferta/prova",
            "Transformar mecanismo em promessa, prova, isca, produto e CTA coerentes.",
            "landing",
            "Construir oferta",
            hasOfferProof),
        step(
            "experiment",
            "Experimento",
            "Materializar criativos, landing e publicação para medir resposta real.",
            "creatives",
            "Criar ativos",
            hasExperimentAssets),
        step(
            "feo",
            "FEO",
            "Fabricar entregáveis após a oferta estar validada ou pronta para teste controlado.",
            "deliverables",
            "Ver entregáveis",
            hasFeoDeliverables),
        step(
            "funnel",
            "Funil",
            "Medir leads, compra, custo, taxa e aprendizado comercial para decidir escala.",
            "funnel",
            "Medir venda",
            hasFunnelReady));
  }

  /** Cria uma etapa do fluxo com rótulo padronizado de validação. */
  private ExperimentConstructionStepDto step(
      String code, String title, String description, String tab, String action, boolean validated) {
    return new ExperimentConstructionStepDto(
        code, title, description, tab, action, validated, validated ? "Validado" : null);
  }

  /** Monta a seção que mostra o ponto de partida do fluxo manual. */
  private ExperimentConstructionSectionDto buildNichePainSection(
      Experiment experiment, MarketNiche niche, Hypothesis hypothesis, boolean manualFlow) {
    List<ExperimentConstructionItemDto> items = new ArrayList<>();
    add(items, "Fluxo de criação", manualFlow ? "Manual, sem execução de IA" : "Fluxo sistêmico");
    add(items, "Nicho", niche != null ? niche.getName() : null);
    add(items, "Descrição do nicho", niche != null ? niche.getDescription() : null);
    add(items, "Público inicial", niche != null ? niche.getBaseSegmentation() : null);
    add(
        items,
        "Dor principal",
        firstText(experiment.getSinglePain(), hypothesis != null ? hypothesis.getProblem() : null));
    add(items, "Experimento", experiment.getName());
    return new ExperimentConstructionSectionDto(
        "1. Nicho/dor",
        "Base inicial para entender público, dor raiz, desejo e contexto antes de definir solução.",
        items);
  }

  /** Monta a seção com a hipótese inicial que orienta a construção. */
  private ExperimentConstructionSectionDto buildHypothesisSection(
      Experiment experiment, Hypothesis hypothesis) {
    List<ExperimentConstructionItemDto> items = new ArrayList<>();
    add(items, "Público/persona", hypothesis != null ? hypothesis.getPersona() : null);
    add(items, "Narrativa do experimento", experiment.getHypothesis());
    add(items, "Sinal esperado", hypothesis != null ? hypothesis.getSuccessRule() : null);
    return new ExperimentConstructionSectionDto(
        "2. Hipótese",
        "Aposta comercial inicial que será refinada por MDS, oferta, prova e métricas reais.",
        items);
  }

  /** Monta a seção que explicita a descoberta do mecanismo pelo MDS. */
  private ExperimentConstructionSectionDto buildMdsMechanismSection(Hypothesis hypothesis) {
    List<ExperimentConstructionItemDto> items = new ArrayList<>();
    add(
        items,
        "Mecanismo atual",
        hypothesis != null
            ? firstText(hypothesis.getUniqueMechanism(), hypothesis.getMechanism())
            : null);
    add(
        items,
        "Uso esperado do MDS",
        "Descobrir mecanismo plausível, evidências, limites e linguagem segura antes da promessa final.");
    return new ExperimentConstructionSectionDto(
        "3. MDS descobre mecanismo",
        "O mecanismo não deve ser inventado na entrada; deve nascer de evidência e virar justificativa racional da oferta.",
        items);
  }

  /** Monta a seção de oferta, prova e produto testado. */
  private ExperimentConstructionSectionDto buildOfferProofSection(
      Experiment experiment, Hypothesis hypothesis) {
    List<ExperimentConstructionItemDto> items = new ArrayList<>();
    add(
        items,
        "Promessa",
        firstText(
            experiment.getFunnelPromise(), hypothesis != null ? hypothesis.getPromise() : null));
    add(
        items,
        "Prova ou regra de sucesso",
        hypothesis != null ? hypothesis.getSuccessRule() : null);
    add(
        items,
        "Recompensa/isca",
        firstText(experiment.getFreeReward(), hypothesis != null ? hypothesis.getEntrega() : null));
    add(items, "CTA principal", experiment.getPrimaryCta());
    add(items, "Tipo de experimento", enumText(experiment.getExperimentType()));
    add(items, "Subtipo Produto IA", enumText(experiment.getProductAiSubtype()));
    add(
        items,
        "Preço de teste",
        experiment.getUnitPrice() != null ? "R$ " + experiment.getUnitPrice() : null);
    add(
        items,
        "Oferta da hipótese",
        hypothesis != null ? enumText(hypothesis.getOfferType()) : null);
    return new ExperimentConstructionSectionDto(
        "4. Oferta e prova",
        "Transforma dor e mecanismo em promessa, prova, isca, produto, preço e CTA coerentes.",
        items);
  }

  /** Monta a seção de materialização do experimento e dos entregáveis FEO. */
  private ExperimentConstructionSectionDto buildExperimentFeoSection(Experiment experiment) {
    List<ExperimentConstructionItemDto> items = new ArrayList<>();
    add(items, "Canal/plataforma", enumText(experiment.getPlatform()));
    add(items, "Objetivo de campanha", enumText(experiment.getCampaignObjective()));
    add(items, "Ângulos criativos", experiment.getCreativeTextPrompt());
    add(items, "Entregáveis FEO", statusText(experiment.getLandingPageDeliverables()));
    return new ExperimentConstructionSectionDto(
        "5. Experimento e FEO",
        "Materializa criativos, landing e entregáveis depois que a tese comercial estiver clara.",
        items);
  }

  /** Monta a seção de campanha, métrica e critérios de validação do funil. */
  private ExperimentConstructionSectionDto buildFunnelSection(Experiment experiment) {
    List<ExperimentConstructionItemDto> items = new ArrayList<>();
    add(
        items,
        "Orçamento diário",
        experiment.getDailyBudget() != null ? "R$ " + experiment.getDailyBudget() : null);
    add(
        items,
        "CPL alvo",
        experiment.getKpiTargetCpl() != null ? "R$ " + experiment.getKpiTargetCpl() : null);
    add(
        items,
        "Tamanho de amostra",
        experiment.getSampleSize() != null ? String.valueOf(experiment.getSampleSize()) : null);
    add(items, "Variável principal", experiment.getPrimaryVariable());
    add(items, "Métrica principal", experiment.getPrimaryMetric());
    return new ExperimentConstructionSectionDto(
        "6. Funil mede venda",
        "Mostra como o experimento deve produzir evidência de lead, intenção de compra, custo e aprendizado.",
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

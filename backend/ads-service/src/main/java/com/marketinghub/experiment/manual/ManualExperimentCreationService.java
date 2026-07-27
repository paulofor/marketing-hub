package com.marketinghub.experiment.manual;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignObjective;
import com.marketinghub.experiment.ExperimentCreationSource;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStage;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.HypothesisStatus;
import com.marketinghub.hypothesis.OfferType;
import com.marketinghub.journey.model.JourneyPhase;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.repository.jpa.journey.JourneyTemplateRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Cria a cadeia oficial Nicho -> Hipótese -> Experimento a partir do fluxo manual do operador. */
@Service
public class ManualExperimentCreationService {
  private static final BigDecimal DEFAULT_KPI_TARGET_CPL = new BigDecimal("10.00");
  private static final BigDecimal DEFAULT_DAILY_BUDGET = new BigDecimal("50.00");
  private static final int DEFAULT_SAMPLE_SIZE = 100;
  private static final String PENDING_PROMISE = "Pendente na Construção: definir promessa após MDS";
  private static final String PENDING_MECHANISM =
      "Pendente na Construção: descobrir mecanismo com MDS";
  private static final String PENDING_LEAD_MAGNET =
      "Pendente na Construção: definir isca ou entrega inicial";
  private static final String PENDING_CTA = "Pendente na Construção: definir CTA principal";

  private final MarketNicheRepository nicheRepository;
  private final HypothesisRepository hypothesisRepository;
  private final ExperimentRepository experimentRepository;
  private final JourneyTemplateRepository journeyTemplateRepository;

  /** Inicializa o serviço com os repositórios oficiais do fluxo comercial. */
  public ManualExperimentCreationService(
      MarketNicheRepository nicheRepository,
      HypothesisRepository hypothesisRepository,
      ExperimentRepository experimentRepository,
      JourneyTemplateRepository journeyTemplateRepository) {
    this.nicheRepository = nicheRepository;
    this.hypothesisRepository = hypothesisRepository;
    this.experimentRepository = experimentRepository;
    this.journeyTemplateRepository = journeyTemplateRepository;
  }

  /** Cria nicho, hipótese e experimento em uma única transação identificada como fluxo manual. */
  @Transactional
  public Experiment create(ManualExperimentCreationRequest request) {
    validate(request);
    MarketNiche niche = nicheRepository.save(buildNiche(request));
    Hypothesis hypothesis = hypothesisRepository.save(buildHypothesis(request, niche));
    Experiment experiment = buildExperiment(request, niche, hypothesis);
    return experimentRepository.save(experiment);
  }

  /** Valida os campos mínimos para o experimento manual ser comercialmente acionável. */
  private void validate(ManualExperimentCreationRequest request) {
    if (request == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request required");
    }
    requireText(request.getNicheName(), "nicheName required");
    requireText(request.getPersona(), "persona required");
    requireText(request.getProblem(), "problem required");
    if (request.getDailyBudget() != null
        && request.getDailyBudget().compareTo(BigDecimal.ZERO) <= 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "dailyBudget must be greater than zero");
    }
    if (request.getKpiTargetCpl() != null
        && request.getKpiTargetCpl().compareTo(BigDecimal.ZERO) <= 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "kpiTargetCpl must be greater than zero");
    }
    if (request.getSampleSize() != null && request.getSampleSize() < 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sampleSize must be at least 1");
    }
  }

  /** Exige texto útil para campos críticos do wizard. */
  private void requireText(String value, String message) {
    if (!StringUtils.hasText(value)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
  }

  /** Monta o nicho oficial com os sinais de mercado informados manualmente. */
  private MarketNiche buildNiche(ManualExperimentCreationRequest request) {
    return MarketNiche.builder()
        .name(request.getNicheName().trim())
        .description(
            joinLines(
                request.getNicheDescription(),
                labeled("Referencia de mercado", request.getMarketReference()),
                labeled("Publico", request.getNicheAudience())))
        .baseSegmentation(request.getNicheAudience())
        .demandVolume(labeled("Dores observadas", request.getPains()))
        .promises(labeled("Desejos observados", request.getDesires()))
        .offers(labeled("Oferta inicial", request.getOfferName()))
        .extraTips(
            joinLines(
                labeled("Canais provaveis", request.getLikelyChannels()),
                labeled("Origem", "Fluxo manual de experimento")))
        .totalCost(BigDecimal.ZERO)
        .build();
  }

  /** Monta a hipótese oficial a partir da aposta comercial inserida pelo operador. */
  private Hypothesis buildHypothesis(ManualExperimentCreationRequest request, MarketNiche niche) {
    return Hypothesis.builder()
        .marketNiche(niche)
        .title(buildHypothesisTitle(niche))
        .promise(resolvePromise(request))
        .problem(request.getProblem())
        .persona(request.getPersona())
        .mechanism(resolveMechanism(request))
        .uniqueMechanism(resolveMechanism(request))
        .entrega(resolveLeadMagnet(request))
        .successRule(
            joinLines(
                request.getSuccessSignal(),
                labeled("Criterio de sucesso", request.getSuccessCriteria())))
        .offerType(resolveOfferType(request.getValidationType()))
        .price(request.getTestPrice())
        .kpiTargetCpl(resolveKpiTargetCpl(request))
        .status(HypothesisStatus.TESTING)
        .build();
  }

  /** Monta o experimento oficial planejado e marcado como criado pelo fluxo manual. */
  private Experiment buildExperiment(
      ManualExperimentCreationRequest request, MarketNiche niche, Hypothesis hypothesis) {
    String experimentName = buildExperimentName(niche, hypothesis);
    return Experiment.builder()
        .niche(niche)
        .name(experimentName)
        .creationSource(ExperimentCreationSource.MANUAL_FLOW)
        .hypothesisRef(hypothesis)
        .hypothesis(resolveNarrative(request))
        .singlePain(request.getProblem())
        .freeReward(resolveLeadMagnet(request))
        .funnelPromise(resolvePromise(request))
        .primaryCta(resolvePrimaryCta(request))
        .experimentType(ExperimentType.NICHE_TEST)
        .campaignObjective(ExperimentCampaignObjective.LEADS)
        .kpiTargetCpl(resolveKpiTargetCpl(request))
        .sampleSize(request.getSampleSize() != null ? request.getSampleSize() : DEFAULT_SAMPLE_SIZE)
        .dailyBudget(
            request.getDailyBudget() != null ? request.getDailyBudget() : DEFAULT_DAILY_BUDGET)
        .unitPrice(request.getTestPrice())
        .status(ExperimentStatus.PLANNED)
        .platform(ExperimentPlatform.FACEBOOK)
        .stage(ExperimentStage.AD)
        .primaryVariable("Promessa manual")
        .primaryMetric("Lead qualificado")
        .creativeTextPrompt(request.getCreativeAngles())
        .journeyTemplate(resolveJourneyTemplate())
        .build();
  }

  /** Resolve o KPI de CPL informado ou aplica um padrão conservador para teste manual. */
  private BigDecimal resolveKpiTargetCpl(ManualExperimentCreationRequest request) {
    return request.getKpiTargetCpl() != null ? request.getKpiTargetCpl() : DEFAULT_KPI_TARGET_CPL;
  }

  /** Resolve a promessa ou marca a pendência para a aba Construção. */
  private String resolvePromise(ManualExperimentCreationRequest request) {
    return StringUtils.hasText(request.getPromise())
        ? request.getPromise().trim()
        : PENDING_PROMISE;
  }

  /** Resolve o mecanismo ou marca a pendência para pesquisa MDS. */
  private String resolveMechanism(ManualExperimentCreationRequest request) {
    return StringUtils.hasText(request.getMechanism())
        ? request.getMechanism().trim()
        : PENDING_MECHANISM;
  }

  /** Resolve a isca inicial ou marca a pendência de oferta. */
  private String resolveLeadMagnet(ManualExperimentCreationRequest request) {
    return StringUtils.hasText(request.getLeadMagnet())
        ? request.getLeadMagnet().trim()
        : PENDING_LEAD_MAGNET;
  }

  /** Resolve o CTA principal ou marca a pendência de oferta. */
  private String resolvePrimaryCta(ManualExperimentCreationRequest request) {
    return StringUtils.hasText(request.getPrimaryCta())
        ? request.getPrimaryCta().trim()
        : PENDING_CTA;
  }

  /** Define o tipo de oferta da hipótese de acordo com o objetivo declarado no teste. */
  private OfferType resolveOfferType(String validationType) {
    if (validationType != null && validationType.toUpperCase(Locale.ROOT).contains("VENDA")) {
      return OfferType.TRIPWIRE;
    }
    return OfferType.LEAD;
  }

  /** Busca um template de jornada existente ou cria um template mínimo para testes manuais. */
  private JourneyTemplate resolveJourneyTemplate() {
    return journeyTemplateRepository.findAll(PageRequest.of(0, 1)).stream()
        .findFirst()
        .orElseGet(
            () ->
                journeyTemplateRepository.save(
                    JourneyTemplate.builder()
                        .name("Jornada manual de experimento")
                        .description("Template padrao para experimentos criados pelo fluxo manual.")
                        .objective("Capturar interesse e validar promessa comercial.")
                        .preferredChannel("EMAIL")
                        .phases(
                            List.of(
                                JourneyPhase.ATTENTION,
                                JourneyPhase.INTEREST,
                                JourneyPhase.DESIRE,
                                JourneyPhase.ACTION))
                        .build()));
  }

  /** Monta o título técnico da hipótese preservando o padrão de sigla e sequência do nicho. */
  private String buildHypothesisTitle(MarketNiche niche) {
    long nextNumber = hypothesisRepository.countByMarketNicheId(niche.getId()) + 1;
    return "%s-H%03d".formatted(nicheAcronym(niche), nextNumber);
  }

  /** Monta o nome técnico do experimento a partir da hipótese oficial. */
  private String buildExperimentName(MarketNiche niche, Hypothesis hypothesis) {
    String baseName =
        "%s-E%03d"
            .formatted(
                hypothesis.getTitle(), experimentRepository.countByHypothesisRef(hypothesis) + 1);
    if (!experimentRepository.existsByNicheAndName(niche, baseName)) {
      return baseName;
    }
    return "%s-%d".formatted(baseName, System.currentTimeMillis());
  }

  /** Gera uma sigla estável a partir do nome do nicho. */
  private String nicheAcronym(MarketNiche niche) {
    String normalized =
        Normalizer.normalize(niche.getName(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toUpperCase(Locale.ROOT);
    StringBuilder acronym = new StringBuilder();
    for (String word : normalized.split("[^A-Z0-9]+")) {
      if (!word.isBlank()) {
        acronym.append(word.charAt(0));
      }
      if (acronym.length() == 4) {
        break;
      }
    }
    while (acronym.length() < 3) {
      acronym.append('X');
    }
    return acronym.toString();
  }

  /** Resolve a narrativa principal exibida no experimento. */
  private String resolveNarrative(ManualExperimentCreationRequest request) {
    if (StringUtils.hasText(request.getHypothesisStatement())) {
      return request.getHypothesisStatement();
    }
    return "%s sofre com %s; mecanismo, promessa, oferta e prova devem ser definidos na Construção com apoio do MDS e validação do funil."
        .formatted(request.getPersona().trim(), request.getProblem().trim());
  }

  /** Prefixa um valor opcional com rótulo legível. */
  private String labeled(String label, String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return label + ": " + value.trim();
  }

  /** Junta blocos textuais opcionais em linhas sem valores vazios. */
  private String joinLines(String... values) {
    return java.util.Arrays.stream(values)
        .filter(StringUtils::hasText)
        .map(String::trim)
        .reduce((left, right) -> left + "\n" + right)
        .orElse(null);
  }
}

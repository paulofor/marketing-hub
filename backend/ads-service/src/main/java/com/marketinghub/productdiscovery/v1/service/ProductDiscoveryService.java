package com.marketinghub.productdiscovery.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.opportunitydossier.service.OpportunityDossierResearchSyncService;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunity;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunityDecision;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryOpportunityRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Coordena leitura, escrita e contratos do backend para descoberta de produtos PDE. */
@Service
public class ProductDiscoveryService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProductDiscoveryService.class);
  private static final String PIPELINE_CODE = "productdiscovery.v1";
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final int MINIMUM_COMPARABLE_MARKETPLACE_OFFERS = 10;
  private static final String STAGE_CODE = "research";
  private static final Duration EXECUTION_LEASE_DURATION = Duration.ofMinutes(20);
  private static final List<String> LEGACY_ARTIFICIAL_EVIDENCE_MARKERS =
      List.of(
          "não retornou resultados estruturados suficientes",
          "não retornou tópicos estruturados suficientes");
  private static final String LEGACY_ARCHIVE_REASON =
      "Evidências artificiais legadas invalidadas: a página de busca sem resultados não comprova"
          + " dor, ciência ou intenção de compra.";
  private final ProductDiscoveryCycleRepository cycleRepository;
  private final ProductDiscoveryOpportunityRepository opportunityRepository;
  private final OpportunityDossierResearchSyncService dossierResearchSyncService;

  /** Inicializa o serviço com repositórios canônicos do módulo. */
  public ProductDiscoveryService(
      ProductDiscoveryCycleRepository cycleRepository,
      ProductDiscoveryOpportunityRepository opportunityRepository,
      OpportunityDossierResearchSyncService dossierResearchSyncService) {
    this.cycleRepository = cycleRepository;
    this.opportunityRepository = opportunityRepository;
    this.dossierResearchSyncService = dossierResearchSyncService;
  }

  /** Cria um ciclo pronto para o worker pesquisar dores e lacunas na internet. */
  @Transactional
  public ProductDiscoveryCycleResponse createCycle(CreateProductDiscoveryCycleRequest request) {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setTheme(requiredText(request.theme(), "theme"));
    cycle.setTargetAudience(optionalText(request.targetAudience()));
    cycle.setCountry(defaultText(request.country(), "BR"));
    cycle.setLanguage(defaultText(request.language(), "pt-BR"));
    cycle.setAcquisitionChannel(optionalText(request.acquisitionChannel()));
    cycle.setCommercialConstraints(optionalText(request.commercialConstraints()));
    cycle.setForbiddenCategories(optionalText(request.forbiddenCategories()));
    cycle.setObjective(optionalText(request.objective()));
    cycle.setStatus(ProductDiscoveryCycleStatus.READY_FOR_RESEARCH);
    cycle.setStageCode(STAGE_CODE);
    return toCycleResponse(cycleRepository.save(cycle));
  }

  /** Lista ciclos recentes para acompanhamento administrativo. */
  @Transactional(readOnly = true)
  public List<ProductDiscoveryCycleResponse> listCycles() {
    return cycleRepository.findTop50ByOrderByUpdatedAtDesc().stream()
        .map(this::toCycleResponse)
        .toList();
  }

  /** Busca um ciclo com oportunidades e evidências para a tela de decisão. */
  @Transactional(readOnly = true)
  public ProductDiscoveryCycleDetailResponse getCycle(Long cycleId) {
    ProductDiscoveryCycle cycle = findCycle(cycleId);
    List<ProductDiscoveryOpportunityResponse> opportunities =
        opportunityRepository.findAllByCycleIdOrderByScoreDesc(cycleId).stream()
            .map(this::toOpportunityResponse)
            .toList();
    return new ProductDiscoveryCycleDetailResponse(toCycleResponse(cycle), opportunities);
  }

  /** Persiste o plano dirigido do Argos antes de qualquer coleta autenticada ou busca pública. */
  @Transactional
  public ProductDiscoveryResearchPlanResponse registerResearchPlan(
      Long cycleId, ProductDiscoveryResearchPlanRequest request) {
    ProductDiscoveryCycle cycle = findCycle(cycleId);
    validateExecutionLease(cycle, request.executionLeaseId());
    if (cycle.getStatus() != ProductDiscoveryCycleStatus.RESEARCHING) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O plano só pode ser registrado durante a pesquisa");
    }
    cycle.setResearchPlanJson(requiredText(request.planJson(), "planJson"));
    cycle.setResearchPlanRawResponse(requiredText(request.rawResponse(), "rawResponse"));
    cycle.setResearchPlanModel(requiredText(request.model(), "model"));
    cycle.setLeaseExpiresAt(Instant.now().plus(EXECUTION_LEASE_DURATION));
    ProductDiscoveryCycle saved = cycleRepository.save(cycle);
    return new ProductDiscoveryResearchPlanResponse(
        saved.getId(),
        saved.getResearchPlanJson(),
        saved.getResearchPlanModel(),
        saved.getUpdatedAt());
  }

  /** Retorna o plano dirigido persistido para auditoria e acompanhamento administrativo. */
  @Transactional(readOnly = true)
  public ProductDiscoveryResearchPlanResponse getResearchPlan(Long cycleId) {
    ProductDiscoveryCycle cycle = findCycle(cycleId);
    if (!StringUtils.hasText(cycle.getResearchPlanJson())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano de pesquisa ainda não criado");
    }
    return new ProductDiscoveryResearchPlanResponse(
        cycle.getId(),
        cycle.getResearchPlanJson(),
        cycle.getResearchPlanModel(),
        cycle.getUpdatedAt());
  }

  /** Confirma que uma integração auxiliar pertence à tentativa vigente do ciclo. */
  @Transactional(readOnly = true)
  public void validateActiveExecution(Long cycleId, String executionLeaseId) {
    validateExecutionLease(findCycle(cycleId), executionLeaseId);
  }

  /** Retorna o ranking gerencial atual por maturidade comercial para orientar novos ciclos PDE. */
  @Transactional(readOnly = true)
  public ProductDiscoveryMaturityRankingResponse getMaturityRanking() {
    ProductDiscoveryMaturityRankingResponse baseline =
        new ProductDiscoveryMaturityRankingResponse(
            "Ranking por maturidade comercial",
            "Muitas pessoas vivem uma dor concreta, as soluções atuais deixam uma lacuna clara e"
                + " conseguimos entregar uma microexperiência digital de valor rápido com baixo"
                + " esforço.",
            "Começar por renda extra para autônomos/MEIs, mantendo manicure como candidato pronto e"
                + " relacionamento sob trava de segurança.",
            List.of(
                new ProductDiscoveryMaturityItemResponse(
                    1,
                    "Manicure/nail designer",
                    "Produto pronto",
                    "Melhor candidato atual para venda direta de low-ticket.",
                    "Dor operacional clara: cliente some da manutenção, agenda fica imprevisível e"
                        + " o profissional perde recorrência.",
                    "Usar como benchmark comercial e preparar novos criativos orgânicos quando"
                        + " houver janela de execução.",
                    List.of(
                        "Oferta Mapa de Recorrência 7D",
                        "Campanha anterior publicada",
                        "Dor fácil de demonstrar em vídeo curto"),
                    List.of(
                        "Não ampliar para beleza genérica",
                        "Manter promessa em rotina, agenda e recorrência")),
                new ProductDiscoveryMaturityItemResponse(
                    2,
                    "Renda extra",
                    "Oportunidade promissora",
                    "Mercado grande, urgente e próximo do DNA de produto digital prático.",
                    "O caminho seguro é resolver etapas concretas de venda autônoma, sem prometer"
                        + " ganho garantido.",
                    "Abrir primeiro ciclo de descoberta para autônomos/MEIs com foco em WhatsApp,"
                        + " primeira venda, cobrança e oferta simples.",
                    List.of(
                        "Dor urgente",
                        "Compra por alívio rápido",
                        "Encaixe natural para microexperiência guiada"),
                    List.of(
                        "Evitar promessa de ganho",
                        "Evitar renda garantida",
                        "Exigir prova prática de ação executável")),
                new ProductDiscoveryMaturityItemResponse(
                    3,
                    "Melhoria pessoal",
                    "Oportunidade promissora",
                    "Bom encaixe para diagnóstico, plano de 7 dias e rotina guiada.",
                    "Pode vender bem quando recortada em decisão, clareza, rotina ou mudança"
                        + " prática visível.",
                    "Abrir ciclo separado buscando dores específicas de baixo esforço e"
                        + " transformação percebida em poucos dias.",
                    List.of(
                        "Alta demanda emocional",
                        "Formato PDE combina com diagnóstico e plano",
                        "Potencial de conteúdo orgânico"),
                    List.of(
                        "Evitar promessa ampla",
                        "Não vender mudança total de vida",
                        "Recortar em comportamento específico")),
                new ProductDiscoveryMaturityItemResponse(
                    4,
                    "Relacionamento",
                    "Pesquisar com cuidado",
                    "Mercado emocional forte, mas sensível.",
                    "Só deve avançar em clareza, comunicação, limites, decisão ou preparação"
                        + " pessoal.",
                    "Abrir ciclo com restrições explícitas e reprovar qualquer hipótese de"
                        + " manipulação, controle ou reconquista garantida.",
                    List.of(
                        "Dor intensa",
                        "Alto engajamento orgânico",
                        "Possibilidade de microexperiência reflexiva"),
                    List.of(
                        "Sem promessa de reconquista",
                        "Sem manipulação",
                        "Sem controle de comportamento de outra pessoa")),
                new ProductDiscoveryMaturityItemResponse(
                    5,
                    "MUSA/moda/estilo",
                    "Em validação",
                    "Produto PDE existente, mas a prioridade é validar vídeo, experiência e"
                        + " conversão.",
                    "Ainda não deve ser tratado como descoberta nova enquanto a versão com vídeo e"
                        + " o funil estão em leitura.",
                    "Medir a janela do experimento 69 antes de criar nova tese de moda/estilo.",
                    List.of(
                        "PDE MUSA ativo",
                        "Vídeo explicativo preparado",
                        "Experimento 69 como leitura principal"),
                    List.of("Não duplicar experimento", "Não trocar oferta antes de medir funil")),
                new ProductDiscoveryMaturityItemResponse(
                    6,
                    "Varejo, marmitas, promoção de vendas e personal trainer",
                    "Oportunidades secundárias",
                    "Territórios úteis para radar, mas sem prioridade sobre renda extra e manicure"
                        + " neste momento.",
                    "Podem gerar produtos bons, porém exigem recorte ou evidência adicional antes"
                        + " de tomar foco operacional.",
                    "Manter no backlog e comparar depois que as três trilhas prioritárias"
                        + " produzirem evidência.",
                    List.of(
                        "Dores operacionais conhecidas",
                        "Potencial B2B/MEI",
                        "Algumas teses já apareceram no repositório"),
                    List.of("Não dispersar execução", "Priorizar evidência de compra rápida"))),
            List.of(
                new ProductDiscoveryResearchTrackResponse(
                    "Renda extra para autônomos/MEIs",
                    "Aquisição de clientes, WhatsApp, cobrança e primeira venda.",
                    "Maior conexão com venda rápida, dor urgente e microexperiência prática.",
                    "renda extra para autonomos e MEIs",
                    "Autônomos, MEIs e pessoas tentando vender serviços simples pelo WhatsApp",
                    "TikTok, Reels e WhatsApp",
                    "Encontrar uma dor concreta de venda autônoma que possa virar PDE de ação"
                        + " prática em até 7 dias, sem promessa de ganho garantido.",
                    "Foco em conseguir primeiros clientes, montar oferta simples, responder"
                        + " interessados, cobrar com segurança e organizar rotina comercial.",
                    "Promessa de renda garantida, investimento financeiro, pirâmide, apostas,"
                        + " crédito, emprego garantido ou manipulação de plataformas."),
                new ProductDiscoveryResearchTrackResponse(
                    "Melhoria pessoal de baixo esforço",
                    "Clareza, rotina, decisão e plano de 7 dias.",
                    "Boa aderência a diagnóstico guiado e transformação percebida sem exigir"
                        + " mudança radical.",
                    "melhoria pessoal de baixo esforco",
                    "Adultos buscando clareza prática, rotina simples ou decisão pessoal sem"
                        + " terapia ou promessa médica",
                    "TikTok, Reels e Shorts",
                    "Encontrar uma dor específica de melhoria pessoal que aceite microexperiência"
                        + " digital simples, com resultado prático percebido em poucos dias.",
                    "Diagnóstico, plano de 7 dias, organização de decisão, clareza de prioridade e"
                        + " rotina mínima.",
                    "Promessa terapêutica, cura, tratamento médico, mudança total de vida ou"
                        + " resultado psicológico garantido."),
                new ProductDiscoveryResearchTrackResponse(
                    "Relacionamento responsável",
                    "Comunicação, autoconhecimento, limites e decisão.",
                    "Mercado forte, mas só avança se a promessa for ética, pessoal e controlável"
                        + " pelo usuário.",
                    "relacionamento responsavel",
                    "Pessoas buscando clareza e comunicação em relações, sem promessa de controlar"
                        + " ou reconquistar alguém",
                    "TikTok, Reels e Shorts",
                    "Encontrar dores de relacionamento que possam virar PDE responsável de clareza,"
                        + " conversa, limites ou tomada de decisão.",
                    "Foco em comunicação, preparação para conversa, limites pessoais, leitura de"
                        + " padrões e decisão responsável.",
                    "Reconquista garantida, manipulação emocional, controle do outro,"
                        + " aconselhamento jurídico ou situação de violência.")));
    List<ProductDiscoveryMaturityItemResponse> discoveredItems = buildDiscoveredRankingItems();
    if (discoveredItems.isEmpty()) {
      return baseline;
    }
    return new ProductDiscoveryMaturityRankingResponse(
        baseline.strategyName(),
        "Ranking derivado dos ciclos persistidos: score, decisão, evidência de escala, lacuna e"
            + " risco comercial.",
        "Priorizar validação comercial de " + discoveredItems.getFirst().niche() + ".",
        discoveredItems,
        baseline.recommendedTracks());
  }

  /**
   * Constrói o ranking atual usando oportunidades persistidas, sem depender de uma lista estática.
   */
  private List<ProductDiscoveryMaturityItemResponse> buildDiscoveredRankingItems() {
    List<ProductDiscoveryOpportunity> opportunities =
        opportunityRepository
            .findTop50ByCycleStatusOrderByScoreDesc(ProductDiscoveryCycleStatus.COMPLETED)
            .stream()
            .limit(10)
            .toList();
    return IntStream.range(0, opportunities.size())
        .mapToObj(index -> toMaturityItem(index + 1, opportunities.get(index)))
        .toList();
  }

  /** Converte uma oportunidade auditável em item gerencial do ranking. */
  private ProductDiscoveryMaturityItemResponse toMaturityItem(
      int position, ProductDiscoveryOpportunity opportunity) {
    return new ProductDiscoveryMaturityItemResponse(
        position,
        opportunity.getName(),
        opportunity.getDecision().name(),
        opportunity.getRootPain(),
        defaultText(opportunity.getUnmetnessEvidence(), "Lacuna comercial ainda não descrita."),
        opportunity.getDecision().name().equals("APPROVE")
            ? "Executar validação comercial controlada antes de ampliar investimento."
            : "Completar as evidências ausentes antes de criar campanha.",
        List.of(
            defaultText(opportunity.getScaleEvidence(), "Escala ainda não comprovada."),
            "Score comercial: " + opportunity.getScore()),
        List.of(defaultText(opportunity.getCommercialRisk(), "Revisão humana obrigatória.")));
  }

  /** Entrega pendências ao worker e marca ciclos como em pesquisa para evitar consumo duplicado. */
  @Transactional
  public List<ProductDiscoveryPendingResponse> pending() {
    Instant now = Instant.now();
    return cycleRepository
        .findClaimableForUpdate(
            ProductDiscoveryCycleStatus.READY_FOR_RESEARCH,
            ProductDiscoveryCycleStatus.RESEARCHING,
            now,
            now.minus(EXECUTION_LEASE_DURATION),
            PageRequest.of(0, 1))
        .stream()
        .map(
            cycle -> {
              cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
              cycle.setStageCode(STAGE_CODE);
              cycle.setErrorMessage(null);
              cycle.setExecutionLeaseId(UUID.randomUUID().toString());
              cycle.setLeaseExpiresAt(now.plus(EXECUTION_LEASE_DURATION));
              cycle.setExecutionAttempt(cycle.getExecutionAttempt() + 1);
              dossierResearchSyncService.start(cycle.getId());
              return toPendingResponse(cycleRepository.save(cycle));
            })
        .toList();
  }

  /** Registra o resultado somente depois dos gates comerciais controlados pelo backend. */
  @Transactional
  public ProductDiscoveryCycleDetailResponse complete(
      Long cycleId, ProductDiscoveryResultRequest request) {
    ProductDiscoveryCycle cycle = findCycle(cycleId);
    validateExecutionLease(cycle, request.executionLeaseId());
    validateMarketplaceEvidenceGate(cycle, request);
    validatePurchaseMomentGate(cycle, request);
    opportunityRepository.deleteAllByCycleId(cycleId);
    for (ProductDiscoveryOpportunityResultRequest item : request.opportunities()) {
      ProductDiscoveryOpportunity opportunity = new ProductDiscoveryOpportunity();
      opportunity.setCycle(cycle);
      opportunity.setName(requiredText(item.name(), "name"));
      opportunity.setPrimaryAudience(requiredText(item.primaryAudience(), "primaryAudience"));
      opportunity.setRootPain(requiredText(item.rootPain(), "rootPain"));
      opportunity.setPracticalPain(optionalText(item.practicalPain()));
      opportunity.setEmotionalPain(optionalText(item.emotionalPain()));
      opportunity.setScaleEvidence(optionalText(item.scaleEvidence()));
      opportunity.setUnmetnessEvidence(optionalText(item.unmetnessEvidence()));
      opportunity.setPdeExperience(optionalText(item.pdeExperience()));
      opportunity.setFirstCampaignAngle(optionalText(item.firstCampaignAngle()));
      opportunity.setCommercialRisk(optionalText(item.commercialRisk()));
      opportunity.setEvidenceJson(optionalText(item.evidenceJson()));
      opportunity.setScore(item.score());
      opportunity.setDecision(item.decision());
      opportunityRepository.save(opportunity);
    }
    cycle.setDecisionSummary(requiredText(request.decisionSummary(), "decisionSummary"));
    cycle.setStatus(ProductDiscoveryCycleStatus.COMPLETED);
    cycle.setStageCode("opportunity-gate");
    cycle.setErrorMessage(null);
    clearExecutionLease(cycle);
    cycleRepository.save(cycle);
    dossierResearchSyncService.synchronize(
        cycleId, opportunityRepository.findAllByCycleIdOrderByScoreDesc(cycleId));
    return getCycle(cycleId);
  }

  /** Bloqueia conclusao dirigida quando Argos nao recebeu ofertas reais comparaveis suficientes. */
  private void validateMarketplaceEvidenceGate(
      ProductDiscoveryCycle cycle, ProductDiscoveryResultRequest request) {
    if (!StringUtils.hasText(cycle.getResearchPlanJson())
        || !cycle.getResearchPlanJson().contains("marketplaceRequests")) {
      return;
    }
    long comparableOffers =
        request.opportunities().stream()
            .map(ProductDiscoveryOpportunityResultRequest::evidenceJson)
            .filter(StringUtils::hasText)
            .flatMap(
                evidenceJson -> {
                  try {
                    JsonNode offers = JSON.readTree(evidenceJson).path("marketplaceOffers");
                    return offers.isArray()
                        ? java.util.stream.StreamSupport.stream(offers.spliterator(), false)
                        : java.util.stream.Stream.empty();
                  } catch (Exception ex) {
                    LOGGER.error(
                        "[product-discovery] Falha ao ler ofertas comparáveis cycleId={}"
                            + " operação=complete",
                        cycle.getId(),
                        ex);
                    return java.util.stream.Stream.empty();
                  }
                })
            .map(
                node -> node.path("marketplace").asText() + ":" + node.path("referenceId").asText())
            .filter(key -> !key.endsWith(":"))
            .distinct()
            .count();
    if (comparableOffers < MINIMUM_COMPARABLE_MARKETPLACE_OFFERS) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "Dossie bloqueado: sao necessarias ao menos 10 ofertas reais comparaveis; recebidas "
              + comparableOffers);
    }
  }

  /** Impede aprovação B2C no Instagram sem evidência comportamental aceita pelo backend. */
  private void validatePurchaseMomentGate(
      ProductDiscoveryCycle cycle, ProductDiscoveryResultRequest request) {
    if (!requiresPurchaseMomentGate(cycle)) {
      return;
    }
    for (ProductDiscoveryOpportunityResultRequest opportunity : request.opportunities()) {
      JsonNode gate = readPurchaseMomentGate(cycle, opportunity);
      if (!gate.path("required").asBoolean(false)) {
        throw new ResponseStatusException(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Dossie bloqueado: Validação do Momento de Compra obrigatória para B2C no Instagram");
      }
      if (opportunity.decision() == ProductDiscoveryOpportunityDecision.APPROVE) {
        validateApprovedPurchaseMomentGate(cycle, opportunity, gate);
      }
    }
  }

  /** Recalcula no backend os fatos mínimos que autorizam uma priorização B2C no Instagram. */
  private void validateApprovedPurchaseMomentGate(
      ProductDiscoveryCycle cycle,
      ProductDiscoveryOpportunityResultRequest opportunity,
      JsonNode gate) {
    JsonNode sourceQuality = gate.path("sourceQuality");
    JsonNode criteria = gate.path("successCriteria");
    JsonNode candidate = findPurchaseMomentCandidate(gate, opportunity.name());
    if (!"PASS".equals(gate.path("status").asText())
        || !gate.path("sourceQualityPassed").asBoolean(false)
        || !gate.path("finalPrioritizationEligible").asBoolean(false)
        || gate.path("minimumIndependentReadings").asInt(0) < 2
        || !sourceQuality.path("passed").asBoolean(false)
        || !sourceQuality.path("reasons").isArray()
        || !sourceQuality.path("reasons").isEmpty()
        || sourceQuality.path("maxAgeDays").asInt(0) < 1
        || sourceQuality.path("maxAgeDays").asInt(0) > 90
        || candidate.isMissingNode()
        || !"PASS".equals(candidate.path("status").asText())
        || !candidate.path("eligibleForFinalPrioritization").asBoolean(false)
        || !containsText(gate.path("eligibleCandidateNames"), opportunity.name())) {
      throw invalidPurchaseMomentGate();
    }

    parsePurchaseMomentInstant(
        cycle, opportunity.name(), sourceQuality.path("evaluatedAt"), "sourceQuality.evaluatedAt");
    Instant declaredAt =
        parsePurchaseMomentInstant(
            cycle, opportunity.name(), criteria.path("declaredAt"), "successCriteria.declaredAt");
    int minimumParticipants = criteria.path("minimumEligibleParticipantsPerReading").asInt(0);
    double minimumExperienceStartRate = criteria.path("minimumExperienceStartRate").asDouble(-1);
    double minimumValueMomentRate = criteria.path("minimumValueMomentRate").asDouble(-1);
    double minimumReadyResultUseRate = criteria.path("minimumReadyResultUseRate").asDouble(-1);
    double minimumPrototypePreferenceRate =
        criteria.path("minimumPrototypePreferenceRate").asDouble(-1);
    double minimumCheckoutStartRate = criteria.path("minimumCheckoutStartRate").asDouble(-1);
    if (minimumParticipants < 1
        || !isValidRateThreshold(minimumExperienceStartRate)
        || !isValidRateThreshold(minimumValueMomentRate)
        || !isValidRateThreshold(minimumReadyResultUseRate)
        || minimumReadyResultUseRate <= 0d
        || !isValidRateThreshold(minimumPrototypePreferenceRate)
        || !isValidRateThreshold(minimumCheckoutStartRate)
        || !hasCompletePurchaseScene(candidate.path("scene"))
        || !hasText(candidate.path("freeAlternative"), "name")
        || !hasText(candidate.path("freeAlternative"), "prototypeAdvantage")
        || !hasCanonicalHumanValueDelivery(candidate.path("humanValueDelivery"))
        || !isPrivatePurchasePrototype(candidate.path("prototype"))) {
      throw invalidPurchaseMomentGate();
    }

    JsonNode readings = candidate.path("readings");
    if (!readings.isArray() || readings.size() < 2) {
      throw invalidPurchaseMomentGate();
    }
    Set<String> readingIds = new HashSet<>();
    for (JsonNode reading : readings) {
      validatePurchaseMomentReading(
          cycle,
          opportunity.name(),
          reading,
          readingIds,
          declaredAt,
          minimumParticipants,
          minimumExperienceStartRate,
          minimumValueMomentRate,
          minimumReadyResultUseRate,
          minimumPrototypePreferenceRate,
          minimumCheckoutStartRate);
    }
  }

  /** Localiza a candidata homologada dentro do gate sem aceitar vínculo por posição. */
  private JsonNode findPurchaseMomentCandidate(JsonNode gate, String opportunityName) {
    JsonNode candidates = gate.path("candidates");
    if (!candidates.isArray()) {
      return JSON.missingNode();
    }
    for (JsonNode candidate : candidates) {
      if (opportunityName.equals(candidate.path("candidateName").asText())) {
        return candidate;
      }
    }
    return JSON.missingNode();
  }

  /** Valida uma leitura independente e recalcula suas taxas a partir das contagens observadas. */
  private void validatePurchaseMomentReading(
      ProductDiscoveryCycle cycle,
      String opportunityName,
      JsonNode reading,
      Set<String> readingIds,
      Instant declaredAt,
      int minimumParticipants,
      double minimumExperienceStartRate,
      double minimumValueMomentRate,
      double minimumReadyResultUseRate,
      double minimumPrototypePreferenceRate,
      double minimumCheckoutStartRate) {
    String readingId = reading.path("readingId").asText("");
    Instant observedAt =
        parsePurchaseMomentInstant(
            cycle, opportunityName, reading.path("observedAt"), "readings.observedAt");
    int eligibleParticipants = nonNegativeInteger(reading.path("eligibleParticipants"));
    int experienceStarted = nonNegativeInteger(reading.path("experienceStarted"));
    int valueMoments = nonNegativeInteger(reading.path("valueMoments"));
    int readyResultsUsedWithoutAssembly =
        nonNegativeInteger(reading.path("readyResultsUsedWithoutAssembly"));
    int prototypePreferredOverFree = nonNegativeInteger(reading.path("prototypePreferredOverFree"));
    int checkoutStarted = nonNegativeInteger(reading.path("checkoutStarted"));
    boolean invalid =
        !StringUtils.hasText(readingId)
            || !readingIds.add(readingId)
            || observedAt.isBefore(declaredAt)
            || !"FIRST_PARTY_EVENTS".equals(reading.path("eventSource").asText())
            || !("PRIVATE_PROTOTYPE".equals(reading.path("testMarker").asText())
                || "LOCAL_QA".equals(reading.path("testMarker").asText()))
            || !"APPROVE".equals(reading.path("psiqueDecision").asText())
            || !"APPROVE".equals(reading.path("temisDecision").asText())
            || !reading.path("passed").asBoolean(false)
            || eligibleParticipants < minimumParticipants
            || experienceStarted < 0
            || valueMoments < 0
            || readyResultsUsedWithoutAssembly < 0
            || prototypePreferredOverFree < 0
            || checkoutStarted < 0
            || experienceStarted > eligibleParticipants
            || valueMoments > experienceStarted
            || readyResultsUsedWithoutAssembly > valueMoments
            || prototypePreferredOverFree > eligibleParticipants
            || checkoutStarted > experienceStarted
            || rate(experienceStarted, eligibleParticipants) < minimumExperienceStartRate
            || rate(valueMoments, experienceStarted) < minimumValueMomentRate
            || rate(readyResultsUsedWithoutAssembly, experienceStarted) < minimumReadyResultUseRate
            || rate(prototypePreferredOverFree, eligibleParticipants)
                < minimumPrototypePreferenceRate
            || rate(checkoutStarted, experienceStarted) < minimumCheckoutStartRate;
    if (invalid) {
      throw invalidPurchaseMomentGate();
    }
  }

  /** Confirma que a cena contém prazo, consequência, orçamento, tentativa e gasto observável. */
  private boolean hasCompletePurchaseScene(JsonNode scene) {
    return hasText(scene, "trigger")
        && hasText(scene, "deadline")
        && hasText(scene, "costOfError")
        && hasText(scene, "budgetEvidence")
        && hasText(scene, "failedAttempt")
        && hasText(scene, "currentPaidBehavior");
  }

  /**
   * Confirma território humano evidenciado e saída pronta sem trabalho de IA transferido ao
   * cliente.
   */
  private boolean hasCanonicalHumanValueDelivery(JsonNode delivery) {
    Set<String> allowedTerritories =
        Set.of("AFFECTION_AND_BELONGING", "RECOGNITION", "EFFORT_RELIEF");
    JsonNode territories = delivery.path("territories");
    if (!territories.isArray() || territories.isEmpty()) {
      return false;
    }
    for (JsonNode territory : territories) {
      if (!territory.isTextual() || !allowedTerritories.contains(territory.asText())) {
        return false;
      }
    }
    int customerStepsToValue = nonNegativeInteger(delivery.path("customerStepsToValue"));
    int timeToUsableResultMinutes = nonNegativeInteger(delivery.path("timeToUsableResultMinutes"));
    return hasAtLeastUniqueTexts(delivery.path("evidenceSourceIds"), 2)
        && hasAtLeastUniqueTexts(delivery.path("evidencePathways"), 2)
        && hasText(delivery, "desiredTransformation")
        && hasText(delivery, "readyMadeOutcome")
        && hasText(delivery, "minimumCustomerInput")
        && hasText(delivery, "automationBoundary")
        && delivery.has("requiresPromptEngineering")
        && !delivery.path("requiresPromptEngineering").asBoolean(true)
        && delivery.has("requiresManualAssembly")
        && !delivery.path("requiresManualAssembly").asBoolean(true)
        && delivery.path("usableWithoutAiKnowledge").asBoolean(false)
        && customerStepsToValue >= 1
        && customerStepsToValue <= 5
        && timeToUsableResultMinutes >= 1
        && timeToUsableResultMinutes <= 10;
  }

  /** Conta textos únicos de uma lista sem aceitar coerção de objetos ou valores vazios. */
  private boolean hasAtLeastUniqueTexts(JsonNode values, int minimum) {
    if (!values.isArray()) {
      return false;
    }
    Set<String> unique = new HashSet<>();
    for (JsonNode value : values) {
      if (!value.isTextual() || !StringUtils.hasText(value.asText())) {
        return false;
      }
      unique.add(value.asText());
    }
    return unique.size() >= minimum;
  }

  /** Confirma que o protótipo continua privado, segregado e sem mídia ou pagamento. */
  private boolean isPrivatePurchasePrototype(JsonNode prototype) {
    return hasText(prototype, "prototypeId")
        && prototype.path("private").asBoolean(false)
        && prototype.has("published")
        && !prototype.path("published").asBoolean(true)
        && prototype.has("paymentEnabled")
        && !prototype.path("paymentEnabled").asBoolean(true)
        && prototype.path("mediaSpend").isNumber()
        && Double.compare(prototype.path("mediaSpend").asDouble(), 0d) == 0
        && ("PRIVATE_PROTOTYPE".equals(prototype.path("testMarker").asText())
            || "LOCAL_QA".equals(prototype.path("testMarker").asText()));
  }

  /** Converte um instante do contrato e registra o contexto integral quando o valor é inválido. */
  private Instant parsePurchaseMomentInstant(
      ProductDiscoveryCycle cycle, String opportunityName, JsonNode value, String fieldName) {
    try {
      return Instant.parse(value.asText(""));
    } catch (Exception ex) {
      LOGGER.error(
          "[product-discovery] Instante inválido no gate cycleId={} oportunidade={} campo={}",
          cycle.getId(),
          opportunityName,
          fieldName,
          ex);
      throw invalidPurchaseMomentGate();
    }
  }

  /** Lê uma contagem não negativa sem converter números fracionários ou textos. */
  private int nonNegativeInteger(JsonNode value) {
    return value.isIntegralNumber() && value.canConvertToInt() && value.asInt() >= 0
        ? value.asInt()
        : -1;
  }

  /** Calcula uma taxa observada e reprova denominadores vazios. */
  private double rate(int numerator, int denominator) {
    return numerator >= 0 && denominator > 0 ? (double) numerator / denominator : -1d;
  }

  /** Confirma que um limiar declarado representa uma taxa possível. */
  private boolean isValidRateThreshold(double value) {
    return Double.isFinite(value) && value >= 0d && value <= 1d;
  }

  /** Verifica texto obrigatório sem aceitar coerção de objetos ou listas. */
  private boolean hasText(JsonNode node, String fieldName) {
    return node.isObject()
        && node.path(fieldName).isTextual()
        && StringUtils.hasText(node.path(fieldName).asText());
  }

  /** Verifica se uma lista JSON contém o vínculo nominal esperado. */
  private boolean containsText(JsonNode values, String expected) {
    if (!values.isArray()) {
      return false;
    }
    for (JsonNode value : values) {
      if (value.isTextual() && expected.equals(value.asText())) {
        return true;
      }
    }
    return false;
  }

  /** Cria a resposta canônica quando qualquer prova do momento de compra é inconsistente. */
  private ResponseStatusException invalidPurchaseMomentGate() {
    return new ResponseStatusException(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "Dossie bloqueado: a oportunidade não possui entrega pronta e duas leituras válidas do Momento de Compra");
  }

  /** Lê o gate persistível e preserva no log a causa de um callback inválido. */
  private JsonNode readPurchaseMomentGate(
      ProductDiscoveryCycle cycle, ProductDiscoveryOpportunityResultRequest opportunity) {
    try {
      return JSON.readTree(requiredText(opportunity.evidenceJson(), "evidenceJson"))
          .path("purchaseMomentGate");
    } catch (ResponseStatusException ex) {
      LOGGER.error(
          "[product-discovery] Gate do momento de compra ausente cycleId={} oportunidade={}",
          cycle.getId(),
          opportunity.name(),
          ex);
      throw ex;
    } catch (Exception ex) {
      LOGGER.error(
          "[product-discovery] Falha ao ler gate do momento de compra cycleId={} oportunidade={}",
          cycle.getId(),
          opportunity.name(),
          ex);
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "Dossie bloqueado: gate do Momento de Compra inválido",
          ex);
    }
  }

  /** Identifica o recorte B2C/Instagram declarado no ciclo sem inferir pelo tema. */
  private boolean requiresPurchaseMomentGate(ProductDiscoveryCycle cycle) {
    String acquisitionChannel =
        defaultText(cycle.getAcquisitionChannel(), "").toLowerCase(Locale.ROOT);
    String consumerContext =
        (defaultText(cycle.getCommercialConstraints(), "")
                + " "
                + defaultText(cycle.getTargetAudience(), ""))
            .toLowerCase(Locale.ROOT);
    return acquisitionChannel.contains("instagram")
        && (consumerContext.contains("b2c")
            || consumerContext.contains("consumidor")
            || consumerContext.contains("pessoa física")
            || consumerContext.contains("pessoa fisica"));
  }

  /** Arquiva ciclos legados compostos exclusivamente por páginas de busca sem resultados reais. */
  @Transactional
  public ProductDiscoveryLegacyCleanupResponse archiveArtificialLegacyEvidence() {
    List<ProductDiscoveryCycle> candidates =
        cycleRepository.findTop50ByOrderByUpdatedAtDesc().stream()
            .filter(cycle -> cycle.getStatus() == ProductDiscoveryCycleStatus.COMPLETED)
            .filter(this::containsOnlyArtificialLegacyEvidence)
            .toList();
    int opportunityCount = 0;
    for (ProductDiscoveryCycle cycle : candidates) {
      List<ProductDiscoveryOpportunity> opportunities =
          opportunityRepository.findAllByCycleIdOrderByScoreDesc(cycle.getId());
      opportunityCount += opportunities.size();
      cycle.setStatus(ProductDiscoveryCycleStatus.ARCHIVED);
      cycle.setStageCode("legacy-evidence-archived");
      cycle.setDecisionSummary(LEGACY_ARCHIVE_REASON);
      cycleRepository.save(cycle);
    }
    return new ProductDiscoveryLegacyCleanupResponse(
        candidates.size(),
        opportunityCount,
        candidates.stream().map(ProductDiscoveryCycle::getId).toList(),
        LEGACY_ARCHIVE_REASON);
  }

  /** Confirma que todas as oportunidades do ciclo usam o fallback artificial conhecido. */
  private boolean containsOnlyArtificialLegacyEvidence(ProductDiscoveryCycle cycle) {
    List<ProductDiscoveryOpportunity> opportunities =
        opportunityRepository.findAllByCycleIdOrderByScoreDesc(cycle.getId());
    return !opportunities.isEmpty()
        && opportunities.stream()
            .allMatch(
                opportunity ->
                    optionalText(opportunity.getEvidenceJson()) != null
                        && LEGACY_ARTIFICIAL_EVIDENCE_MARKERS.stream()
                            .anyMatch(
                                marker ->
                                    opportunity
                                        .getEvidenceJson()
                                        .toLowerCase(java.util.Locale.ROOT)
                                        .contains(marker)));
  }

  /** Registra falha operacional do worker preservando a causa para o usuário. */
  @Transactional
  public ProductDiscoveryCycleResponse fail(Long cycleId, ProductDiscoveryFailureRequest request) {
    ProductDiscoveryCycle cycle = findCycle(cycleId);
    validateExecutionLease(cycle, request.executionLeaseId());
    cycle.setStatus(ProductDiscoveryCycleStatus.FAILED);
    cycle.setErrorMessage(requiredText(request.errorMessage(), "errorMessage"));
    clearExecutionLease(cycle);
    dossierResearchSyncService.fail(cycleId);
    return toCycleResponse(cycleRepository.save(cycle));
  }

  /** Converte entidade de ciclo para resposta. */
  private ProductDiscoveryCycleResponse toCycleResponse(ProductDiscoveryCycle cycle) {
    return new ProductDiscoveryCycleResponse(
        cycle.getId(),
        cycle.getTheme(),
        cycle.getTargetAudience(),
        cycle.getCountry(),
        cycle.getLanguage(),
        cycle.getAcquisitionChannel(),
        cycle.getStatus(),
        cycle.getStageCode(),
        cycle.getDecisionSummary(),
        cycle.getErrorMessage(),
        cycle.getCreatedAt(),
        cycle.getUpdatedAt());
  }

  /** Converte entidade de oportunidade para resposta. */
  private ProductDiscoveryOpportunityResponse toOpportunityResponse(
      ProductDiscoveryOpportunity opportunity) {
    return new ProductDiscoveryOpportunityResponse(
        opportunity.getId(),
        opportunity.getCycle().getId(),
        opportunity.getName(),
        opportunity.getPrimaryAudience(),
        opportunity.getRootPain(),
        opportunity.getPracticalPain(),
        opportunity.getEmotionalPain(),
        opportunity.getScaleEvidence(),
        opportunity.getUnmetnessEvidence(),
        opportunity.getPdeExperience(),
        opportunity.getFirstCampaignAngle(),
        opportunity.getCommercialRisk(),
        opportunity.getEvidenceJson(),
        opportunity.getScore(),
        opportunity.getDecision(),
        opportunity.getCreatedAt(),
        opportunity.getUpdatedAt());
  }

  /** Converte ciclo para contrato de pendência do worker. */
  private ProductDiscoveryPendingResponse toPendingResponse(ProductDiscoveryCycle cycle) {
    return new ProductDiscoveryPendingResponse(
        cycle.getId(),
        PIPELINE_CODE,
        STAGE_CODE,
        cycle.getTheme(),
        cycle.getTargetAudience(),
        cycle.getCountry(),
        cycle.getLanguage(),
        cycle.getAcquisitionChannel(),
        cycle.getCommercialConstraints(),
        cycle.getForbiddenCategories(),
        cycle.getObjective(),
        cycle.getExecutionLeaseId(),
        cycle.getExecutionAttempt());
  }

  /** Impede que uma execução expirada sobrescreva o resultado de uma retomada mais recente. */
  private void validateExecutionLease(ProductDiscoveryCycle cycle, String executionLeaseId) {
    if (cycle.getStatus() != ProductDiscoveryCycleStatus.RESEARCHING
        || !StringUtils.hasText(executionLeaseId)
        || !executionLeaseId.equals(cycle.getExecutionLeaseId())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Lease da execução de descoberta expirou ou foi substituído");
    }
  }

  /** Encerra a reserva operacional depois de um callback terminal aceito. */
  private void clearExecutionLease(ProductDiscoveryCycle cycle) {
    cycle.setExecutionLeaseId(null);
    cycle.setLeaseExpiresAt(null);
  }

  /** Busca ciclo por id ou responde 404. */
  private ProductDiscoveryCycle findCycle(Long cycleId) {
    return cycleRepository
        .findById(cycleId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Ciclo de descoberta não encontrado"));
  }

  /** Normaliza texto obrigatório. */
  private String requiredText(String value, String fieldName) {
    if (!StringUtils.hasText(value)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " é obrigatório");
    }
    return value.trim();
  }

  /** Normaliza texto opcional. */
  private String optionalText(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  /** Aplica valor padrão para texto opcional ausente. */
  private String defaultText(String value, String fallback) {
    return StringUtils.hasText(value) ? value.trim() : fallback;
  }
}

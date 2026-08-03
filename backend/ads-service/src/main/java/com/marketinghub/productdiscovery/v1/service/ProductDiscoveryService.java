package com.marketinghub.productdiscovery.v1.service;

import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunity;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryOpportunityRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Coordena leitura, escrita e contratos do backend para descoberta de produtos PDE. */
@Service
public class ProductDiscoveryService {

  private static final String PIPELINE_CODE = "productdiscovery.v1";
  private static final String STAGE_CODE = "research";
  private final ProductDiscoveryCycleRepository cycleRepository;
  private final ProductDiscoveryOpportunityRepository opportunityRepository;

  /** Inicializa o serviço com repositórios canônicos do módulo. */
  public ProductDiscoveryService(
      ProductDiscoveryCycleRepository cycleRepository,
      ProductDiscoveryOpportunityRepository opportunityRepository) {
    this.cycleRepository = cycleRepository;
    this.opportunityRepository = opportunityRepository;
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

  /** Retorna o ranking gerencial atual por maturidade comercial para orientar novos ciclos PDE. */
  @Transactional(readOnly = true)
  public ProductDiscoveryMaturityRankingResponse getMaturityRanking() {
    List<ProductDiscoveryOpportunity> researchedOpportunities =
        opportunityRepository.findTop10ByOrderByScoreDescUpdatedAtDesc();
    if (!researchedOpportunities.isEmpty()) {
      return buildEvidenceBasedRanking(researchedOpportunities);
    }
    return new ProductDiscoveryMaturityRankingResponse(
        "Ranking por maturidade comercial",
        "Muitas pessoas vivem uma dor concreta, as soluções atuais deixam uma lacuna clara e conseguimos entregar uma microexperiência digital de valor rápido com baixo esforço.",
        "Começar por renda extra para autônomos/MEIs, mantendo manicure como candidato pronto e relacionamento sob trava de segurança.",
        List.of(
            new ProductDiscoveryMaturityItemResponse(
                1,
                "Manicure/nail designer",
                "Produto pronto",
                "Melhor candidato atual para venda direta de low-ticket.",
                "Dor operacional clara: cliente some da manutenção, agenda fica imprevisível e o profissional perde recorrência.",
                "Usar como benchmark comercial e preparar novos criativos orgânicos quando houver janela de execução.",
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
                "O caminho seguro é resolver etapas concretas de venda autônoma, sem prometer ganho garantido.",
                "Abrir primeiro ciclo de descoberta para autônomos/MEIs com foco em WhatsApp, primeira venda, cobrança e oferta simples.",
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
                "Pode vender bem quando recortada em decisão, clareza, rotina ou mudança prática visível.",
                "Abrir ciclo separado buscando dores específicas de baixo esforço e transformação percebida em poucos dias.",
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
                "Só deve avançar em clareza, comunicação, limites, decisão ou preparação pessoal.",
                "Abrir ciclo com restrições explícitas e reprovar qualquer hipótese de manipulação, controle ou reconquista garantida.",
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
                "Produto PDE existente, mas a prioridade é validar vídeo, experiência e conversão.",
                "Ainda não deve ser tratado como descoberta nova enquanto a versão com vídeo e o funil estão em leitura.",
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
                "Territórios úteis para radar, mas sem prioridade sobre renda extra e manicure neste momento.",
                "Podem gerar produtos bons, porém exigem recorte ou evidência adicional antes de tomar foco operacional.",
                "Manter no backlog e comparar depois que as três trilhas prioritárias produzirem evidência.",
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
                "Encontrar uma dor concreta de venda autônoma que possa virar PDE de ação prática em até 7 dias, sem promessa de ganho garantido.",
                "Foco em conseguir primeiros clientes, montar oferta simples, responder interessados, cobrar com segurança e organizar rotina comercial.",
                "Promessa de renda garantida, investimento financeiro, pirâmide, apostas, crédito, emprego garantido ou manipulação de plataformas."),
            new ProductDiscoveryResearchTrackResponse(
                "Melhoria pessoal de baixo esforço",
                "Clareza, rotina, decisão e plano de 7 dias.",
                "Boa aderência a diagnóstico guiado e transformação percebida sem exigir mudança radical.",
                "melhoria pessoal de baixo esforco",
                "Adultos buscando clareza prática, rotina simples ou decisão pessoal sem terapia ou promessa médica",
                "TikTok, Reels e Shorts",
                "Encontrar uma dor específica de melhoria pessoal que aceite microexperiência digital simples, com resultado prático percebido em poucos dias.",
                "Diagnóstico, plano de 7 dias, organização de decisão, clareza de prioridade e rotina mínima.",
                "Promessa terapêutica, cura, tratamento médico, mudança total de vida ou resultado psicológico garantido."),
            new ProductDiscoveryResearchTrackResponse(
                "Relacionamento responsável",
                "Comunicação, autoconhecimento, limites e decisão.",
                "Mercado forte, mas só avança se a promessa for ética, pessoal e controlável pelo usuário.",
                "relacionamento responsavel",
                "Pessoas buscando clareza e comunicação em relações, sem promessa de controlar ou reconquistar alguém",
                "TikTok, Reels e Shorts",
                "Encontrar dores de relacionamento que possam virar PDE responsável de clareza, conversa, limites ou tomada de decisão.",
                "Foco em comunicação, preparação para conversa, limites pessoais, leitura de padrões e decisão responsável.",
                "Reconquista garantida, manipulação emocional, controle do outro, aconselhamento jurídico ou situação de violência.")));
  }

  /** Monta o ranking a partir das oportunidades realmente persistidas, sem lista fixa de produtos. */
  private ProductDiscoveryMaturityRankingResponse buildEvidenceBasedRanking(
      List<ProductDiscoveryOpportunity> opportunities) {
    List<ProductDiscoveryMaturityItemResponse> items =
        java.util.stream.IntStream.range(0, opportunities.size())
            .mapToObj(
                index -> {
                  ProductDiscoveryOpportunity opportunity = opportunities.get(index);
                  return new ProductDiscoveryMaturityItemResponse(
                      index + 1,
                      opportunity.getName(),
                      opportunity.getDecision().name(),
                      opportunity.getRootPain(),
                      opportunity.getScaleEvidence(),
                      opportunity.getDecision()
                              == com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunityDecision.APPROVE
                          ? "Validar preço e compra com gate financeiro antes de escalar."
                          : "Coletar evidência comercial ausente antes de criar campanha.",
                      List.of(
                          defaultText(opportunity.getScaleEvidence(), "Escala não comprovada"),
                          defaultText(
                              opportunity.getUnmetnessEvidence(), "Lacuna não comprovada")),
                      List.of(
                          defaultText(
                              opportunity.getCommercialRisk(),
                              "Feedback de vendas ainda não disponível")));
                })
            .toList();
    return new ProductDiscoveryMaturityRankingResponse(
        "Ranking por evidência comercial persistida",
        "Demanda, concorrência, intenção de compra, lacuna, entrega e feedback real de vendas.",
        "Priorizar a primeira hipótese com evidência suficiente e menor risco; score textual não substitui compra.",
        items,
        List.of());
  }

  /** Entrega pendências ao worker e marca ciclos como em pesquisa para evitar consumo duplicado. */
  @Transactional
  public List<ProductDiscoveryPendingResponse> pending() {
    return cycleRepository
        .findTop5ByStatusInOrderByUpdatedAtAsc(
            List.of(ProductDiscoveryCycleStatus.READY_FOR_RESEARCH))
        .stream()
        .map(
            cycle -> {
              cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
              cycle.setStageCode(STAGE_CODE);
              cycle.setErrorMessage(null);
              return toPendingResponse(cycleRepository.save(cycle));
            })
        .toList();
  }

  /** Registra resultado funcional do worker e conclui o ciclo com ranking auditável. */
  @Transactional
  public ProductDiscoveryCycleDetailResponse complete(
      Long cycleId, ProductDiscoveryResultRequest request) {
    ProductDiscoveryCycle cycle = findCycle(cycleId);
    validateCommercialIntelligence(request);
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
    cycleRepository.save(cycle);
    return getCycle(cycleId);
  }

  /** Bloqueia resultados genéricos, fallback e ciclos sem hipóteses concorrentes auditáveis. */
  private void validateCommercialIntelligence(ProductDiscoveryResultRequest request) {
    if (request.opportunities() == null || request.opportunities().size() < 3) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "A descoberta comercial exige ao menos três hipóteses concorrentes");
    }
    for (ProductDiscoveryOpportunityResultRequest opportunity : request.opportunities()) {
      String evidenceJson = optionalText(opportunity.evidenceJson());
      if (evidenceJson == null
          || !evidenceJson.contains("\"scoreDimensions\"")
          || !evidenceJson.contains("\"buyingIntent\"")
          || !evidenceJson.contains("\"competingOffers\"")
          || evidenceJson.contains("INSUFFICIENT_FALLBACK")) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Oportunidade sem evidência comercial auditável não pode concluir o ciclo");
      }
    }
  }

  /** Registra falha operacional do worker preservando a causa para o usuário. */
  @Transactional
  public ProductDiscoveryCycleResponse fail(Long cycleId, ProductDiscoveryFailureRequest request) {
    ProductDiscoveryCycle cycle = findCycle(cycleId);
    cycle.setStatus(ProductDiscoveryCycleStatus.FAILED);
    cycle.setErrorMessage(requiredText(request.errorMessage(), "errorMessage"));
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
        cycle.getObjective());
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

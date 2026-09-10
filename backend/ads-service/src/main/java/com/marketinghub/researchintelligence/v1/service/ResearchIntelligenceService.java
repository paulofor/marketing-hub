package com.marketinghub.researchintelligence.v1.service;

import com.marketinghub.agent.Agent;
import com.marketinghub.agent.AgentInput;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.researchintelligence.ResearchIntelligenceCardVersionRepository;
import com.marketinghub.researchintelligence.v1.ResearchIntelligenceCardStatus;
import com.marketinghub.researchintelligence.v1.ResearchIntelligenceCardVersion;
import com.marketinghub.researchintelligence.v1.service.catalog.ResearchIntelligenceAgentPolicyResponse;
import com.marketinghub.researchintelligence.v1.service.catalog.ResearchIntelligenceAssignmentResponse;
import com.marketinghub.researchintelligence.v1.service.catalog.ResearchIntelligenceCatalogResponse;
import com.marketinghub.researchintelligence.v1.service.select.ResearchIntelligenceCardResponse;
import com.marketinghub.researchintelligence.v1.service.select.ResearchIntelligenceRouteResponse;
import com.marketinghub.researchintelligence.v1.service.select.ResearchIntelligenceSelectionResponse;
import com.marketinghub.salesvideo.VideoProject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Compila o catálogo global e seleciona cartões para o harness dos agentes. */
@Service
@Transactional(readOnly = true)
public class ResearchIntelligenceService {
  public static final String CONTRACT_VERSION = "HARNESS_RESEARCH_INTELLIGENCE_V1";
  public static final int MAX_CARDS_PER_ROUTE = 4;
  public static final String CURATED_INPUT_TYPE = "HARNESS_RESEARCH_CARD";
  private static final Logger log = LoggerFactory.getLogger(ResearchIntelligenceService.class);
  private static final String RESEARCH_PATTERN = "classpath*:research-library/pesquisas/**/*.md";
  private static final Pattern HEADING = Pattern.compile("(?m)^(#{1,4})\\s+(.+?)\\s*$");
  private static final Pattern DATE_PREFIX = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})");
  private static final Set<String> STOP_WORDS =
      Set.of(
          "ainda",
          "como",
          "comercial",
          "entre",
          "esta",
          "mais",
          "para",
          "pela",
          "pelo",
          "produto",
          "projeto",
          "sobre",
          "video");
  private static final Map<String, List<String>> COLLECTIONS_BY_AGENT =
      Map.of(
          "videomaker", List.of("video", "prazer-audio-visual"),
          "communication-director", List.of("neuromarketing", "momentos-de-compra-b2c"),
          "customer-agent", List.of("neuromarketing", "prazer-audio-visual"),
          "meta-ad-approver",
              List.of("video", "prazer-audio-visual", "neuromarketing", "momentos-de-compra-b2c"));
  private static final List<String> VIDEO_AGENT_ORDER =
      List.of("communication-director", "videomaker", "customer-agent", "meta-ad-approver");
  private static final List<String> OTHER_AGENT_ORDER =
      List.of(
          "market-radar",
          "experiment-strategist",
          "financial-agent",
          "landing-generator",
          "growth-operator");
  private static final List<String> LIMITATIONS =
      List.of(
          "Cartões são evidência externa ou inspiração; não comprovam demanda, venda ou satisfação.",
          "O artefato real e os eventos humanos reconciliados prevalecem sobre qualquer artigo.",
          "Cartões com validade expirada não são entregues aos agentes.",
          "Seleção por coleção, recência e relevância lexical não autoriza gasto nem publicação.");

  private final Clock clock;
  private final List<CatalogCard> bundledCatalog;
  private final ResearchIntelligenceCardVersionRepository versionRepository;
  private final AgentRepository agentRepository;

  /** Carrega uma vez todos os artigos versionados empacotados com o backend. */
  public ResearchIntelligenceService() {
    this(Clock.systemUTC(), null);
  }

  /** Permite controlar a data de validade em testes sem alterar o relógio de produção. */
  ResearchIntelligenceService(Clock clock) {
    this(clock, null);
  }

  /** Injeta as versões ativas mantendo o catálogo Markdown como base compatível. */
  @Autowired
  public ResearchIntelligenceService(
      ResearchIntelligenceCardVersionRepository versionRepository,
      AgentRepository agentRepository) {
    this(Clock.systemUTC(), versionRepository, agentRepository);
  }

  /** Centraliza a construção usada pela aplicação e pelos testes com relógio controlado. */
  ResearchIntelligenceService(
      Clock clock, ResearchIntelligenceCardVersionRepository versionRepository) {
    this(clock, versionRepository, null);
  }

  /** Carrega fontes e permite consultar os vínculos versionados sem criar outro cadastro. */
  ResearchIntelligenceService(
      Clock clock,
      ResearchIntelligenceCardVersionRepository versionRepository,
      AgentRepository agentRepository) {
    this.clock = Objects.requireNonNull(clock);
    this.bundledCatalog = loadCatalog(new PathMatchingResourcePatternResolver());
    this.versionRepository = versionRepository;
    this.agentRepository = agentRepository;
  }

  /**
   * Expõe a fonte global, as rotas audiovisuais e os vínculos consultivos cadastrados nos agentes.
   */
  public ResearchIntelligenceCatalogResponse getCatalog() {
    LocalDate evaluatedOn = LocalDate.now(clock);
    List<CatalogCard> catalog = currentCatalog();
    List<ResearchIntelligenceCardResponse> cards = catalog.stream().map(this::response).toList();
    List<ResearchIntelligenceAgentPolicyResponse> policies = new ArrayList<>();
    for (String agentKey : VIDEO_AGENT_ORDER) {
      policies.add(agentPolicy(agentKey, curation(agentKey), catalog));
    }
    for (String agentKey : OTHER_AGENT_ORDER) {
      AgentCuration curation = curation(agentKey);
      if (!curation.inputs().isEmpty()) policies.add(agentPolicy(agentKey, curation, catalog));
    }
    int activeCards = (int) catalog.stream().filter(this::isCurrent).count();
    return new ResearchIntelligenceCatalogResponse(
        CONTRACT_VERSION, evaluatedOn, catalog.size(), activeCards, policies, cards, LIMITATIONS);
  }

  /** Seleciona as quatro rotas consultivas que governam um projeto de vídeo. */
  public ResearchIntelligenceSelectionResponse selectForVideoProject(VideoProject project) {
    String context = videoContext(project);
    List<CatalogCard> catalog = currentCatalog();
    List<ResearchIntelligenceRouteResponse> routes =
        VIDEO_AGENT_ORDER.stream().map(agent -> route(agent, context, catalog)).toList();
    return selection(context, routes, catalog.size());
  }

  /** Seleciona somente a rota que será incorporada ao job de um agente de vídeo. */
  public ResearchIntelligenceSelectionResponse selectForVideoAgent(
      VideoProject project, String agentKey) {
    String context = videoContext(project);
    if (!COLLECTIONS_BY_AGENT.containsKey(agentKey)) {
      return null;
    }
    List<CatalogCard> catalog = currentCatalog();
    return selection(context, List.of(route(agentKey, context, catalog)), catalog.size());
  }

  /**
   * Entrega a curadoria explícita à tarefa ou preserva o roteamento audiovisual quando não há
   * vínculo.
   */
  public ResearchIntelligenceSelectionResponse selectForAgentTask(
      String agentKey, String... contextParts) {
    String context = joinContext(contextParts);
    AgentCuration curation = curation(agentKey);
    if (!curation.inputs().isEmpty()) {
      List<CatalogCard> catalog = currentCatalog();
      return selection(
          context, List.of(route(agentKey, context, catalog, curation, true)), catalog.size());
    }
    if (!COLLECTIONS_BY_AGENT.containsKey(agentKey) || !isAudiovisualContext(context)) return null;
    List<CatalogCard> catalog = currentCatalog();
    return selection(context, List.of(route(agentKey, context, catalog)), catalog.size());
  }

  /** Impede que pesquisa audiovisual seja injetada em tarefas alheias ao vídeo ou criativo. */
  private boolean isAudiovisualContext(String context) {
    String normalized = normalize(context);
    return List.of(
            "video",
            "audiovisual",
            "criativ",
            "reels",
            "instagram",
            "storyboard",
            "roteiro",
            "cena",
            "audio")
        .stream()
        .anyMatch(normalized::contains);
  }

  /** Localiza a rota de um agente dentro da seleção corrente do vídeo. */
  public ResearchIntelligenceRouteResponse selectVideoRoute(VideoProject project, String agentKey) {
    return selectForVideoProject(project).routes().stream()
        .filter(route -> route.agentKey().equals(agentKey))
        .findFirst()
        .orElse(null);
  }

  /** Reúne apenas campos persistidos que descrevem estratégia, narrativa, áudio e medição. */
  private String videoContext(VideoProject project) {
    return joinContext(
        project.getTitle(),
        project.getObjective(),
        project.getStoryText(),
        project.getTargetChannel(),
        project.getFunnelStage(),
        project.getCommercialHypothesis(),
        project.getScientificBasis(),
        project.getHookText(),
        project.getScriptText(),
        project.getScenePlan(),
        project.getVoiceoverPlan(),
        project.getSoundtrackPlan());
  }

  /** Monta uma seleção estável e inclui os hashes das fontes em sua impressão digital. */
  private ResearchIntelligenceSelectionResponse selection(
      String context, List<ResearchIntelligenceRouteResponse> routes, int availableCards) {
    String selectedHashes =
        routes.stream()
            .flatMap(route -> route.cards().stream())
            .map(card -> card.cardId() + ":" + card.sourceSha256())
            .distinct()
            .sorted()
            .reduce("", (left, right) -> left + right);
    return new ResearchIntelligenceSelectionResponse(
        CONTRACT_VERSION,
        sha256(
            normalize(context)
                + "|"
                + selectedHashes
                + "|"
                + routes.stream()
                    .map(route -> route.agentKey() + ":" + route.selectionReason())
                    .toList()),
        availableCards,
        routes,
        LIMITATIONS);
  }

  /** Cria uma rota curta, cobrindo primeiro cada coleção obrigatória do agente. */
  private ResearchIntelligenceRouteResponse route(
      String agentKey, String context, List<CatalogCard> catalog) {
    return route(agentKey, context, catalog, curation(agentKey), false);
  }

  /**
   * Prioriza referências explícitas e mantém cobertura audiovisual, validade e limite de contexto.
   */
  private ResearchIntelligenceRouteResponse route(
      String agentKey,
      String context,
      List<CatalogCard> catalog,
      AgentCuration curation,
      boolean curatedOnly) {
    List<String> collections = COLLECTIONS_BY_AGENT.getOrDefault(agentKey, List.of());
    Set<String> terms = meaningfulTerms(context);
    List<ScoredCard> scored =
        catalog.stream()
            .filter(
                card ->
                    curatedOnly
                        ? curation.inputs().containsKey(card.cardId())
                        : collections.contains(card.collection()))
            .filter(this::isCurrent)
            .map(card -> new ScoredCard(card, relevance(card, terms)))
            .sorted(
                Comparator.<ScoredCard, Boolean>comparing(
                        scoredCard -> curation.inputs().containsKey(scoredCard.card().cardId()))
                    .reversed()
                    .thenComparing(Comparator.comparingInt(ScoredCard::score).reversed())
                    .thenComparing(
                        scoredCard -> scoredCard.card().publishedOn(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(scoredCard -> scoredCard.card().sourcePath()))
            .toList();

    LinkedHashSet<CatalogCard> selected = new LinkedHashSet<>();
    for (String collection : curatedOnly ? List.<String>of() : collections) {
      scored.stream()
          .filter(candidate -> candidate.card().collection().equals(collection))
          .findFirst()
          .ifPresent(candidate -> selected.add(candidate.card()));
    }
    scored.stream()
        .map(ScoredCard::card)
        .filter(card -> !selected.contains(card))
        .limit(Math.max(0, MAX_CARDS_PER_ROUTE - selected.size()))
        .forEach(selected::add);

    return new ResearchIntelligenceRouteResponse(
        agentKey,
        agentName(agentKey),
        purpose(agentKey),
        authority(agentKey),
        selectionReason(curation, catalog, selected, curatedOnly, collections),
        selected.stream().limit(MAX_CARDS_PER_ROUTE).map(this::response).toList());
  }

  /** Descreve o roteamento global de um agente sem associá-lo a projeto específico. */
  private ResearchIntelligenceAgentPolicyResponse agentPolicy(
      String agentKey, AgentCuration curation, List<CatalogCard> catalog) {
    return new ResearchIntelligenceAgentPolicyResponse(
        agentKey,
        agentName(agentKey),
        purpose(agentKey),
        authority(agentKey),
        COLLECTIONS_BY_AGENT.getOrDefault(agentKey, List.of()),
        MAX_CARDS_PER_ROUTE,
        curation.agentId(),
        curation.inputs().entrySet().stream()
            .map(
                entry ->
                    new ResearchIntelligenceAssignmentResponse(
                        entry.getKey(),
                        entry.getValue(),
                        catalog.stream()
                            .anyMatch(
                                card -> card.cardId().equals(entry.getKey()) && isCurrent(card))))
            .toList());
  }

  /** Lê apenas entradas tipadas como referências e elimina duplicatas sem copiar o artigo. */
  private AgentCuration curation(String agentKey) {
    if (agentRepository == null || agentKey == null || agentKey.isBlank())
      return new AgentCuration(null, null, Map.of());
    Agent agent = agentRepository.findByAgentKey(agentKey).orElse(null);
    if (agent == null) return new AgentCuration(null, null, Map.of());
    Map<String, String> inputs = new LinkedHashMap<>();
    for (AgentInput input : agent.getInputs()) {
      if (CURATED_INPUT_TYPE.equals(input.getType())
          && input.getName() != null
          && !input.getName().isBlank()) {
        inputs.putIfAbsent(
            input.getName().trim(),
            compact(input.getDescription() == null ? "" : input.getDescription(), 255));
      }
    }
    return new AgentCuration(agent.getId(), agent.getCurrentVersion(), inputs);
  }

  /**
   * Separa orientação editorial da evidência científica e torna vínculos indisponíveis auditáveis.
   */
  private String selectionReason(
      AgentCuration curation,
      List<CatalogCard> catalog,
      Set<CatalogCard> selected,
      boolean curatedOnly,
      List<String> collections) {
    StringBuilder reason =
        new StringBuilder(
            curatedOnly
                ? "Curadoria explícita para tarefas do agente; até quatro cartões."
                : "Coleções "
                    + String.join(", ", collections)
                    + "; até quatro cartões por aderência e data.");
    if (curation.inputs().isEmpty()) return reason.toString();
    reason
        .append(" Orientação editorial do agente v")
        .append(curation.version())
        .append(" (não é achado científico):");
    selected.stream()
        .filter(card -> curation.inputs().containsKey(card.cardId()))
        .forEach(
            card ->
                reason
                    .append(" ")
                    .append(card.cardId())
                    .append(": ")
                    .append(curation.inputs().get(card.cardId())));
    List<String> unavailable =
        curation.inputs().keySet().stream()
            .filter(
                id ->
                    catalog.stream().noneMatch(card -> card.cardId().equals(id) && isCurrent(card)))
            .toList();
    if (!unavailable.isEmpty())
      reason
          .append(" Referências indisponíveis, vencidas ou futuras; não usar como evidência: ")
          .append(String.join(", ", unavailable))
          .append('.');
    return reason.toString();
  }

  /** Une fontes empacotadas e versões cadastradas ativas sem expor rascunhos aos agentes. */
  private List<CatalogCard> currentCatalog() {
    if (versionRepository == null) {
      return bundledCatalog;
    }
    List<CatalogCard> cards = new ArrayList<>(bundledCatalog);
    versionRepository
        .findByStatusOrderByCardKeyAscVersionNumberAsc(ResearchIntelligenceCardStatus.ACTIVE)
        .stream()
        .map(this::catalogCard)
        .forEach(cards::add);
    cards.sort(Comparator.comparing(CatalogCard::sourcePath).thenComparing(CatalogCard::cardId));
    return List.copyOf(cards);
  }

  /** Adapta uma versão persistida ao contrato já consumido pelos agentes e pelo Estúdio. */
  private CatalogCard catalogCard(ResearchIntelligenceCardVersion version) {
    return new CatalogCard(
        version.getCardId(),
        version.getCollection(),
        version.getTitle(),
        version.getFinding(),
        version.getMechanism(),
        version.getCommercialApplication(),
        version.getEvidenceStrength(),
        version.getPublishedOn(),
        version.getValidUntil(),
        version.getExperimentHypothesis(),
        version.getRisks(),
        version.getLimits(),
        version.getSourceUri(),
        version.getSourceSha256(),
        "EXTERNAL_RESEARCH");
  }

  /** Exclui evidência vencida ou futura antes de formar qualquer contexto de agente. */
  private boolean isCurrent(CatalogCard card) {
    LocalDate today = LocalDate.now(clock);
    return (card.publishedOn() == null || !card.publishedOn().isAfter(today))
        && (card.validUntil() == null || !card.validUntil().isBefore(today));
  }

  /** Pontua um cartão sem transformar repetição lexical em força de evidência. */
  private int relevance(CatalogCard card, Set<String> terms) {
    String title = normalize(card.collection() + " " + card.title());
    String content =
        normalize(
            card.finding()
                + " "
                + card.mechanism()
                + " "
                + card.commercialApplication()
                + " "
                + card.experimentHypothesis());
    int score = 0;
    for (String term : terms) {
      if (title.contains(term)) {
        score += 8;
      }
      if (content.contains(term)) {
        score += 2;
      }
    }
    return score;
  }

  /** Converte o cartão interno em contrato REST sem expor o Markdown completo. */
  private ResearchIntelligenceCardResponse response(CatalogCard card) {
    return new ResearchIntelligenceCardResponse(
        card.cardId(),
        card.collection(),
        card.title(),
        card.finding(),
        card.mechanism(),
        card.commercialApplication(),
        card.evidenceStrength(),
        card.publishedOn(),
        card.validUntil(),
        card.experimentHypothesis(),
        card.risks(),
        card.limits(),
        card.sourcePath(),
        card.sourceSha256(),
        card.evidenceKind());
  }

  /** Informa se uma coleção já possui política de consumo por algum agente. */
  boolean supportsCollection(String collection) {
    return COLLECTIONS_BY_AGENT.values().stream()
        .anyMatch(collections -> collections.contains(collection));
  }

  /** Lista os agentes já autorizados a receber a coleção informada. */
  List<String> routableAgentsForCollection(String collection) {
    return VIDEO_AGENT_ORDER.stream()
        .filter(agent -> COLLECTIONS_BY_AGENT.getOrDefault(agent, List.of()).contains(collection))
        .toList();
  }

  /** Lê recursos em ordem estável e bloqueia inicialização sem artigos elegíveis. */
  private List<CatalogCard> loadCatalog(PathMatchingResourcePatternResolver resolver) {
    try {
      List<CatalogCard> cards = new ArrayList<>();
      for (Resource resource : resolver.getResources(RESEARCH_PATTERN)) {
        if ("ini.md".equalsIgnoreCase(resource.getFilename())) {
          continue;
        }
        cards.add(compile(resource));
      }
      cards.sort(Comparator.comparing(CatalogCard::sourcePath));
      if (cards.isEmpty()) {
        throw new IllegalStateException(
            "A Biblioteca de Inteligência não possui artigos elegíveis.");
      }
      return List.copyOf(cards);
    } catch (IOException ex) {
      log.error(
          "Falha ao carregar artigos da Biblioteca de Inteligência pattern={} errorLine={} errorClass={} errorMessage={}",
          RESEARCH_PATTERN,
          errorLine(ex),
          ex.getClass().getName(),
          ex.getMessage(),
          ex);
      throw new IllegalStateException(
          "Não foi possível carregar a Biblioteca de Inteligência.", ex);
    }
  }

  /** Compila um Markdown em cartão limitado e vinculado ao hash integral da fonte. */
  private CatalogCard compile(Resource resource) throws IOException {
    String content;
    try (InputStream input = resource.getInputStream()) {
      content =
          new String(input.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n").trim();
    }
    String sourcePath = sourcePath(resource);
    String filename = resource.getFilename() == null ? "research.md" : resource.getFilename();
    LocalDate publishedOn = publishedOn(filename);
    String collection = collection(sourcePath);
    List<MarkdownSection> sections = sections(content);
    String title =
        sections.stream()
            .filter(section -> section.level() == 1)
            .findFirst()
            .map(MarkdownSection::heading)
            .orElse(filename.replaceFirst("\\.md$", ""));
    String finding =
        firstSection(
            sections,
            List.of(
                "resumo executivo",
                "resumo",
                "achado principal",
                "evidência encontrada",
                "evidência revisada",
                "evidência observada",
                "o que mudou",
                "o que aconteceu",
                "o que foi descoberto"),
            firstParagraph(content));
    String mechanism =
        firstSection(
            sections,
            List.of(
                "mecanismo proposto",
                "hipótese interpretativa",
                "mecanismo",
                "desejo/comportamento revelado",
                "por que importa"),
            "A fonte não declara um mecanismo causal isolado; usar o achado somente como hipótese de trabalho.");
    String application =
        firstSection(
            sections,
            List.of(
                "aplicação no marketing hub",
                "aplicação sugerida",
                "aplicação prática",
                "aplicação possível",
                "uso como hipótese",
                "por que isso importa"),
            "Aplicação ainda não explicitada na fonte; exigir adaptação ao produto e validação humana.");
    String hypothesis =
        firstSection(
            sections,
            List.of(
                "experimento sugerido",
                "experimento concreto",
                "hipótese comercial",
                "hipótese interpretativa",
                "uso como hipótese",
                "recomendação para arquitetura"),
            "Testar uma única variação controlada e comparar eventos humanos e custo com a versão anterior.");
    String evidenceStrength =
        firstSection(
            sections,
            List.of("força da evidência", "qualidade e evidência", "evidência observada"),
            "Força não declarada de forma uniforme; consultar a fonte antes de fazer alegação pública.");
    String risks =
        firstSection(
            sections,
            List.of("limitações", "limites", "atenção", "riscos", "licença e uso comercial"),
            "Risco de generalização para público, canal ou contexto diferente do estudo ou observação original.");
    return new CatalogCard(
        "RI1-" + sha256(sourcePath).substring(0, 12).toUpperCase(Locale.ROOT),
        collection,
        compact(title, 240),
        compact(finding, 700),
        compact(mechanism, 700),
        compact(application, 700),
        compact(evidenceStrength, 500),
        publishedOn,
        validUntil(collection, publishedOn),
        compact(hypothesis, 700),
        compact(risks, 700),
        "Não substituir artefato real, parecer independente, evento humano, custo ou venda reconciliada.",
        sourcePath,
        sha256(content),
        "EXTERNAL_RESEARCH");
  }

  /** Separa o documento em seções preservando apenas o texto abaixo de cada cabeçalho. */
  private List<MarkdownSection> sections(String content) {
    Matcher matcher = HEADING.matcher(content);
    List<HeadingMatch> headings = new ArrayList<>();
    while (matcher.find()) {
      headings.add(
          new HeadingMatch(
              matcher.group(1).length(), matcher.group(2).trim(), matcher.start(), matcher.end()));
    }
    List<MarkdownSection> sections = new ArrayList<>();
    for (int index = 0; index < headings.size(); index++) {
      HeadingMatch heading = headings.get(index);
      int end =
          index + 1 < headings.size() ? headings.get(index + 1).headingStart() : content.length();
      sections.add(
          new MarkdownSection(
              heading.level(),
              heading.heading(),
              content.substring(heading.contentStart(), end).trim()));
    }
    return sections;
  }

  /** Escolhe a primeira seção cujo título corresponda a uma finalidade semântica conhecida. */
  private String firstSection(
      List<MarkdownSection> sections, List<String> aliases, String fallback) {
    for (String alias : aliases) {
      for (MarkdownSection section : sections) {
        if (normalize(section.heading()).contains(normalize(alias))
            && !section.content().isBlank()) {
          return section.content();
        }
      }
    }
    return fallback;
  }

  /** Obtém o primeiro parágrafo útil quando o artigo não possui resumo explícito. */
  private String firstParagraph(String content) {
    for (String paragraph : content.split("\\n\\s*\\n")) {
      String cleaned = cleanMarkdown(paragraph);
      if (!cleaned.isBlank() && !paragraph.stripLeading().startsWith("#")) {
        return cleaned;
      }
    }
    return "A fonte não contém resumo textual elegível.";
  }

  /** Compacta Markdown em texto simples e limita o contexto enviado aos modelos. */
  private String compact(String value, int maxLength) {
    String cleaned = cleanMarkdown(value).replaceAll("\\s+", " ").trim();
    if (cleaned.length() <= maxLength) {
      return cleaned;
    }
    return cleaned.substring(0, maxLength - 1).stripTrailing() + "…";
  }

  /** Remove marcações estruturais sem apagar os fatos escritos no artigo. */
  private String cleanMarkdown(String value) {
    return value
        .replaceAll("(?m)^#{1,6}\\s+", "")
        .replaceAll("(?m)^[-*+]\\s+", "")
        .replaceAll("`{1,3}", "")
        .replaceAll("\\*{1,2}", "")
        .replaceAll("\\[([^]]+)]\\([^)]+\\)", "$1")
        .trim();
  }

  /** Deriva o caminho versionado a partir do recurso empacotado no classpath. */
  private String sourcePath(Resource resource) throws IOException {
    String location = resource.getURL().toString().replace('\\', '/');
    int marker = location.indexOf("pesquisas/");
    if (marker < 0) {
      throw new IOException("Artigo fora de /pesquisas: " + resource.getDescription());
    }
    return location.substring(marker);
  }

  /** Extrai o nome da coleção imediatamente abaixo de /pesquisas. */
  private String collection(String sourcePath) {
    String[] parts = sourcePath.split("/");
    return parts.length >= 2 ? parts[1] : "sem-colecao";
  }

  /** Lê a data do nome canônico do artigo quando ela estiver presente. */
  private LocalDate publishedOn(String filename) {
    Matcher matcher = DATE_PREFIX.matcher(filename);
    return matcher.find() ? LocalDate.parse(matcher.group(1)) : null;
  }

  /** Define validade conservadora conforme a volatilidade da coleção. */
  private LocalDate validUntil(String collection, LocalDate publishedOn) {
    if (publishedOn == null) {
      return null;
    }
    return switch (collection) {
      case "momentos-de-compra-b2c" -> publishedOn.plusDays(14);
      case "video" -> publishedOn.plusDays(45);
      default -> publishedOn.plusYears(1);
    };
  }

  /** Reduz o briefing a termos informativos para o ranking lexical local. */
  private Set<String> meaningfulTerms(String context) {
    LinkedHashSet<String> terms = new LinkedHashSet<>();
    for (String term : normalize(context).split("[^a-z0-9]+")) {
      if (term.length() >= 4 && !STOP_WORDS.contains(term)) {
        terms.add(term);
      }
      if (terms.size() >= 32) {
        break;
      }
    }
    return terms;
  }

  /** Une campos opcionais sem introduzir valores literais nulos no contexto. */
  private String joinContext(String... values) {
    StringBuilder context = new StringBuilder();
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        if (!context.isEmpty()) {
          context.append(' ');
        }
        context.append(value.trim());
      }
    }
    return context.toString();
  }

  /** Normaliza acentos e caixa somente para comparação determinística. */
  private String normalize(String value) {
    String normalized =
        java.text.Normalizer.normalize(value == null ? "" : value, java.text.Normalizer.Form.NFD);
    return normalized.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
  }

  /** Calcula SHA-256 para identidade do cartão, contexto e fonte. */
  private String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      log.error(
          "Algoritmo SHA-256 indisponível ao compilar Biblioteca de Inteligência errorLine={} errorClass={} errorMessage={}",
          errorLine(ex),
          ex.getClass().getName(),
          ex.getMessage(),
          ex);
      throw new IllegalStateException("SHA-256 indisponível.", ex);
    }
  }

  /**
   * Localiza a primeira linha da stack para tornar a falha pesquisável sem perder o stack trace.
   */
  private static int errorLine(Throwable error) {
    return error.getStackTrace().length == 0 ? -1 : error.getStackTrace()[0].getLineNumber();
  }

  /** Expõe o nome humano do agente sem alterar sua identidade técnica. */
  private String agentName(String agentKey) {
    return switch (agentKey) {
      case "communication-director" -> "Íris";
      case "videomaker" -> "Apolo";
      case "customer-agent" -> "Psique";
      case "meta-ad-approver" -> "Têmis";
      case "market-radar" -> "Argos";
      case "experiment-strategist" -> "Atena";
      case "financial-agent" -> "Plutus";
      case "landing-generator" -> "Dédalo";
      case "growth-operator" -> "Hermes";
      default -> agentKey;
    };
  }

  /** Explica como cada agente pode usar a pesquisa sem invadir outra responsabilidade. */
  private String purpose(String agentKey) {
    return switch (agentKey) {
      case "communication-director" -> "Orientar mensagem, ângulo e briefing de canal.";
      case "videomaker" -> "Orientar roteiro, ritmo, áudio, continuidade e escolha técnica.";
      case "customer-agent" -> "Revisar percepção, fluidez, prazer, esforço e desejo.";
      case "meta-ad-approver" -> "Verificar alegações, limites, coerência e integridade.";
      case "market-radar" -> "Orientar investigação factual e verificação das fontes.";
      case "experiment-strategist" -> "Orientar hipótese, valor e desenho do próximo teste.";
      case "financial-agent" -> "Revisar custo por resultado, margem e limites econômicos.";
      case "landing-generator" -> "Orientar primeiro resultado útil e jornada de valor do produto.";
      case "growth-operator" -> "Interpretar funil e testar o gargalo com eventos reconciliados.";
      default -> "Contexto consultivo limitado.";
    };
  }

  /** Declara se a rota orienta produção ou apenas revisão independente. */
  private String authority(String agentKey) {
    return switch (agentKey) {
      case "videomaker" -> "PRODUCTION_ADVISORY";
      case "communication-director" -> "COMMUNICATION_ADVISORY";
      case "market-radar" -> "FACTUAL_ADVISORY";
      case "experiment-strategist" -> "STRATEGY_ADVISORY";
      case "landing-generator" -> "PRODUCT_ADVISORY";
      case "growth-operator" -> "MEASUREMENT_ADVISORY";
      default -> "REVIEW_CRITERIA_ONLY";
    };
  }

  /** Mantém somente identidade, versão e referências editoriais do cadastro consultado. */
  private record AgentCuration(Long agentId, Integer version, Map<String, String> inputs) {}

  /** Mantém os campos do cartão compilado imutáveis dentro do catálogo em memória. */
  private record CatalogCard(
      String cardId,
      String collection,
      String title,
      String finding,
      String mechanism,
      String commercialApplication,
      String evidenceStrength,
      LocalDate publishedOn,
      LocalDate validUntil,
      String experimentHypothesis,
      String risks,
      String limits,
      String sourcePath,
      String sourceSha256,
      String evidenceKind) {}

  /** Representa a pontuação lexical sem misturá-la à força factual da fonte. */
  private record ScoredCard(CatalogCard card, int score) {}

  /** Marca a posição de um cabeçalho para recortar sua seção. */
  private record HeadingMatch(int level, String heading, int headingStart, int contentStart) {}

  /** Representa uma seção Markdown já separada do documento integral. */
  private record MarkdownSection(int level, String heading, String content) {}
}

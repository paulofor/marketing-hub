package com.marketinghub.agentmemory.service;

import com.marketinghub.agentmemory.PremiumAgentMemory;
import com.marketinghub.agentmemory.PremiumAgentMemoryFeedback;
import com.marketinghub.agentmemory.service.registerFeedback.RegisterMemoryFeedbackRequest;
import com.marketinghub.agentmemory.service.registerMemory.RegisterMemoryRequest;
import com.marketinghub.agentmemory.service.retrieveMemory.MemoryResponse;
import com.marketinghub.repository.jpa.agentmemory.PremiumAgentMemoryFeedbackRepository;
import com.marketinghub.repository.jpa.agentmemory.PremiumAgentMemoryRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: governar registro e recuperação segura da memória premium dos agentes. */
@Service
public class AgentMemoryService {
  private static final Set<String> AGENTS =
      Set.of(
          "customer-agent",
          "financial-agent",
          "growth-operator",
          "experiment-strategist",
          "communication-director",
          "landing-generator",
          "meta-ad-approver",
          "apollo");
  private static final String GLOBAL_TENANT = "__GLOBAL__";
  private final PremiumAgentMemoryRepository repository;
  private final PremiumAgentMemoryFeedbackRepository feedbackRepository;
  private final Clock clock;

  /** Inicializa o serviço com persistência e relógio do sistema. */
  @Autowired
  public AgentMemoryService(
      PremiumAgentMemoryRepository repository,
      PremiumAgentMemoryFeedbackRepository feedbackRepository) {
    this(repository, feedbackRepository, Clock.systemUTC());
  }

  /** Inicializa o serviço com relógio controlável para testes. */
  AgentMemoryService(
      PremiumAgentMemoryRepository repository,
      PremiumAgentMemoryFeedbackRepository feedbackRepository,
      Clock clock) {
    this.repository = repository;
    this.feedbackRepository = feedbackRepository;
    this.clock = clock;
  }

  /** Aplica feedback independente e preserva a trilha append-only da decisão. */
  @Transactional
  public MemoryResponse feedback(
      String agentKey, Long memoryId, RegisterMemoryFeedbackRequest request) {
    validateAgent(agentKey);
    PremiumAgentMemory memory =
        repository
            .findById(memoryId)
            .filter(value -> value.getAgentKey().equals(agentKey))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Memória não encontrada para o agente"));
    PremiumAgentMemoryFeedback feedback = new PremiumAgentMemoryFeedback();
    feedback.setMemory(memory);
    feedback.setOutcome(request.outcome());
    feedback.setEvidence(request.evidence().trim());
    feedback.setSourceReference(blankToNull(request.sourceReference()));
    feedback.setCreatedAt(clock.instant());
    feedbackRepository.save(feedback);
    if (!request.outcome().equals("INCONCLUSIVE")) memory.setStatus(request.outcome());
    memory.setUpdatedAt(clock.instant());
    return response(memory);
  }

  /** Registra ou devolve uma memória candidata idêntica sem permitir autopromoção. */
  @Transactional
  public MemoryResponse register(String agentKey, RegisterMemoryRequest request) {
    validateAgent(agentKey);
    String hash = sha256(normalize(request.content()));
    String tenantKey = tenant(request.tenantKey());
    return repository
        .findByAgentKeyAndTenantKeyAndScopeTypeAndScopeIdAndContentSha256(
            agentKey, tenantKey, request.scopeType(), request.scopeId(), hash)
        .map(this::response)
        .orElseGet(() -> response(repository.save(newCandidate(agentKey, request, hash))));
  }

  /** Recupera um contexto curto, segregado e sem memórias invalidadas. */
  @Transactional
  public List<MemoryResponse> retrieve(
      String agentKey, String tenantKey, String scopeType, String scopeId, int requestedLimit) {
    validateAgent(agentKey);
    int limit = Math.max(1, Math.min(12, requestedLimit));
    Instant now = clock.instant();
    List<PremiumAgentMemory> values =
        repository.retrieve(
            agentKey, tenant(tenantKey), scopeType, scopeId, now, PageRequest.of(0, limit));
    values.forEach(value -> value.markRetrieved(now));
    return values.stream().map(this::response).toList();
  }

  /** Materializa uma hipótese candidata com procedência completa. */
  private PremiumAgentMemory newCandidate(
      String agentKey, RegisterMemoryRequest request, String hash) {
    Instant now = clock.instant();
    PremiumAgentMemory value = new PremiumAgentMemory();
    value.setAgentKey(agentKey);
    value.setTenantKey(tenant(request.tenantKey()));
    value.setScopeType(request.scopeType());
    value.setScopeId(request.scopeId());
    value.setSpecialty(request.specialty());
    value.setContent(request.content().trim());
    value.setEvidence(request.evidence().trim());
    value.setSourceReference(blankToNull(request.sourceReference()));
    value.setSourceExecutionId(request.sourceExecutionId());
    value.setStatus("CANDIDATE");
    value.setConfidence(request.confidence());
    value.setContentSha256(hash);
    value.setContractVersion("v1");
    value.setValidUntil(request.validUntil());
    value.setRetrievalCount(0);
    value.setCreatedAt(now);
    value.setUpdatedAt(now);
    return value;
  }

  /** Converte a entidade no contrato público sem expor chaves internas. */
  private MemoryResponse response(PremiumAgentMemory value) {
    return new MemoryResponse(
        value.getId(),
        value.getStatus(),
        value.getSpecialty(),
        value.getContent(),
        value.getEvidence(),
        value.getSourceReference(),
        value.getSourceExecutionId(),
        value.getConfidence(),
        value.getValidUntil(),
        value.getRetrievalCount(),
        value.getCreatedAt());
  }

  /** Bloqueia agentes desconhecidos para impedir leitura cruzada por parâmetro. */
  private void validateAgent(String agentKey) {
    if (!AGENTS.contains(agentKey))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agente premium desconhecido");
  }

  /** Normaliza texto somente para deduplicação determinística. */
  private String normalize(String value) {
    return value.trim().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT);
  }

  /** Calcula o checksum sem armazenar uma segunda cópia do conteúdo. */
  private String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 indisponível", ex);
    }
  }

  /** Converte texto vazio em ausência canônica. */
  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  /** Resolve explicitamente o escopo global sem depender de NULL em chaves de segregacao. */
  private String tenant(String value) {
    String normalized = blankToNull(value);
    return normalized == null ? GLOBAL_TENANT : normalized;
  }
}

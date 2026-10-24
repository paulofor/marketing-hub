package com.marketinghub.product.bigidea.v1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.product.Product;
import com.marketinghub.product.bigidea.v1.*;
import com.marketinghub.product.bigidea.v1.service.get.*;
import com.marketinghub.product.bigidea.v1.service.save.*;
import com.marketinghub.repository.jpa.product.*;
import jakarta.validation.Validator;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Coordena criação, evolução e fixação da Big Idea preservando histórico e autoridade. */
@Service
@RequiredArgsConstructor
@Slf4j
public class BigIdeaService {
  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private BigIdeaResearch research;

  private final ProductRepository products;
  private final ProductBigIdeaRepository revisions;
  private final ProductBigIdeaBindingRepository bindings;
  private final ObjectMapper json;
  private final Validator validator;
  private final BigIdeaHistoricalAuthority history;

  /** Consulta pesquisa isolada de Atena, mantendo experimentos e revisões existentes. */
  @Transactional(readOnly = true)
  public com.marketinghub.product.bigidea.v1.service.research.BigIdeaResearchView research(
      Long productId) {
    return research.current(products.findById(productId).orElseThrow(() -> missing()));
  }

  /** Serializa o pedido para impedir pesquisas duplicadas por duplo clique. */
  @Transactional
  public com.marketinghub.product.bigidea.v1.service.research.BigIdeaResearchView requestResearch(
      Long productId,
      com.marketinghub.product.bigidea.v1.service.research.StartBigIdeaResearchRequest request) {
    return research.start(products.findLockedById(productId).orElseThrow(() -> missing()), request);
  }

  /** Adota os mesmos bytes da proposta concluída, sem sobrescrever uma revisão concorrente. */
  @Transactional
  public BigIdeaView adoptResearch(Long productId, Long executionId) {
    Product product = products.findLockedById(productId).orElseThrow(() -> missing());
    BigIdeaContent content = research.proposal(product, executionId);
    String reason = "Primeira Big Idea — pesquisa de Atena #" + executionId;
    if (product.getBigIdea() != null) {
      var existing = view(product.getBigIdea());
      if (reason.equals(existing.changeReason()) && content.equals(existing.content()))
        return existing;
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "A Big Idea foi definida durante a pesquisa. Preserve a revisão e confira a evolução manualmente.");
    }
    return create(product, content, reason, "Atena — pesquisa #" + executionId, null);
  }

  /** Consulta o registro persistido; ausência é explícita e nunca inventa uma ideia. */
  @Transactional(readOnly = true)
  public BigIdeaWorkspace get(Long productId) {
    Product product = products.findById(productId).orElseThrow(() -> missing());
    return new BigIdeaWorkspace(
        productId,
        product.getBigIdea() == null ? "MISSING" : "DEFINED",
        view(product.getBigIdea()),
        revisions.findByProductIdOrderByRevisionDesc(productId).stream().map(this::view).toList(),
        "Você pode pedir a primeira Big Idea a Atena nesta tela ou durante o planejamento. Defina ou evolua a ideia com motivo e histórico. Experimentos iniciados preservam sua versão; mudanças comerciais seguem um novo ciclo.");
  }

  /** Grava uma nova versão somente quando o operador editou a revisão ainda vigente. */
  @Transactional
  public BigIdeaView save(Long productId, SaveBigIdeaRequest request, String actor) {
    validate(request);
    Product product = products.findLockedById(productId).orElseThrow(() -> missing());
    int current = product.getBigIdea() == null ? 0 : product.getBigIdea().getRevision();
    // Uma repetição idêntica após perda da resposta reutiliza a versão já gravada.
    if (current == request.expectedRevision() + 1
        && product.getBigIdea().getSourceTaskId() == null
        && view(product.getBigIdea()).content().equals(request.content())
        && product.getBigIdea().getChangeReason().equals(request.changeReason().trim()))
      return view(product.getBigIdea());
    if (current != request.expectedRevision())
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "A Big Idea mudou. Atualize a tela e confira a nova revisão antes de salvar.");
    return create(product, request.content(), request.changeReason(), actor, null);
  }

  /** Cria a primeira ideia de Atena sem sobrescrever decisão anterior nem duplicar callback. */
  @Transactional
  public BigIdeaView initializeFromStrategy(Long productId, BigIdeaContent content, Long taskId) {
    validate(new SaveBigIdeaRequest(0, content, "Criação na estratégia aprovada de Atena"));
    Product product = products.findLockedById(productId).orElseThrow(() -> missing());
    if (product.getBigIdea() != null) {
      var current = view(product.getBigIdea());
      if (!current.content().equals(content))
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Uma Big Idea foi definida durante o planejamento. Reavalie a estratégia com a referência vigente.");
      return current;
    }
    return create(product, content, "Criação na estratégia aprovada de Atena", "Atena", taskId);
  }

  /**
   * Fixa a referência comercial preservando ausência histórica; tarefas isoladas usam a vigente.
   */
  @Transactional
  public BigIdeaView pin(Long productId, String reference) {
    Product product = products.findLockedById(productId).orElseThrow(() -> missing());
    String scope = reference == null ? "" : reference.replaceFirst("(@|:).*$", "");
    boolean commercial = "experiment".equals(scope) || "commercial-plan".equals(scope);
    String canonical =
        "experiment".equals(scope)
            ? reference.replaceFirst("^(experiment:[1-9][0-9]*).*$", "$1")
            : reference;
    if (!commercial) return view(product.getBigIdea());
    var existing = bindings.findByProductIdAndSourceReference(productId, canonical);
    var selected = existing.map(ProductBigIdeaBinding::getBigIdea).orElse(product.getBigIdea());
    if (history.preservesAbsence(selected, canonical)) return null;
    if (existing.isPresent()) return view(selected);
    if (product.getBigIdea() == null) return null;
    var binding = new ProductBigIdeaBinding();
    binding.setProductId(productId);
    binding.setSourceReference(canonical);
    binding.setBigIdea(product.getBigIdea());
    bindings.save(binding);
    return view(product.getBigIdea());
  }

  /** Consulta a referência comercial fixada sem produzir mutações em leituras de contexto. */
  @Transactional(readOnly = true)
  public BigIdeaView reference(Product product, String sourceReference) {
    String canonical = BigIdeaHistoricalAuthority.canonical(sourceReference);
    var idea =
        bindings
            .findByProductIdAndSourceReference(product.getId(), canonical)
            .map(ProductBigIdeaBinding::getBigIdea)
            .orElse(product.getBigIdea());
    return history.preservesAbsence(idea, canonical) ? null : view(idea);
  }

  /** Converte a entidade preservada em conteúdo estruturado e rastreável. */
  public BigIdeaView view(ProductBigIdea idea) {
    if (idea == null) return null;
    try {
      return new BigIdeaView(
          idea.getId(),
          idea.getProductId(),
          idea.getRevision(),
          json.readValue(idea.getContentJson(), BigIdeaContent.class),
          idea.getChangeReason(),
          idea.getCreatedBy(),
          idea.getSourceTaskId(),
          idea.getCreatedAt());
    } catch (JsonProcessingException ex) {
      log.error(
          "Big Idea inválida. productId={} revisionId={}", idea.getProductId(), idea.getId(), ex);
      throw new IllegalStateException("A Big Idea persistida não pôde ser lida.", ex);
    }
  }

  /** Persiste revisão e ponteiro vigente atomicamente sob lock do produto. */
  private BigIdeaView create(
      Product product, BigIdeaContent content, String reason, String actor, Long taskId) {
    var idea = new ProductBigIdea();
    idea.setProductId(product.getId());
    idea.setCentralIdea(content.centralIdea());
    idea.setRevision(product.getBigIdea() == null ? 1 : product.getBigIdea().getRevision() + 1);
    idea.setChangeReason(reason.trim());
    idea.setCreatedBy(actor);
    idea.setSourceTaskId(taskId);
    idea.setCreatedAt(Instant.now());
    try {
      idea.setContentJson(json.writeValueAsString(content));
    } catch (JsonProcessingException ex) {
      log.error("Falha ao gravar Big Idea. productId={} taskId={}", product.getId(), taskId, ex);
      throw new IllegalStateException("Não foi possível registrar a Big Idea.", ex);
    }
    revisions.saveAndFlush(idea);
    product.setBigIdea(idea);
    products.save(product);
    log.info(
        "Big Idea registrada. productId={} revision={} sourceTaskId={}",
        product.getId(),
        idea.getRevision(),
        taskId);
    return view(idea);
  }

  /** Exige conteúdo completo também em callbacks internos que não passam pelo controller. */
  private void validate(SaveBigIdeaRequest request) {
    if (request == null || !validator.validate(request).isEmpty())
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Informe ideia central, público, desejo, promessa, mecanismo, evidências, limites e motivo válidos.");
  }

  /** Produz ausência uniforme sem criar produto implicitamente. */
  private static ResponseStatusException missing() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado.");
  }
}

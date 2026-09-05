package com.marketinghub.product.privatereading.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.product.Product;
import com.marketinghub.product.privatereading.infrastructure.PdePrivateReadingClient;
import com.marketinghub.product.privatereading.service.evidence.PrivateReadingEvidence;
import com.marketinghub.product.privatereading.service.workspace.PrivateReadingWorkspace;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: vincular a atividade humana à evidência real da versão privada aceita. */
@Service
@Slf4j
public class PdePrivateReadingService {
  private static final Set<String> SIGNALS =
      Set.of(
          "EXPERIENCE_STARTED",
          "VALUE_MOMENT",
          "READY_RESULT_USED",
          "PREFERRED_OVER_FREE",
          "CHECKOUT_STARTED");
  private final ProductRepository products;
  private final PdePrivateReadingClient client;
  private final ObjectMapper json;
  private final long miraProductId;

  /** Configura catálogo, prova interna e identidade canônica de Mira sem consultar outro banco. */
  public PdePrivateReadingService(
      ProductRepository products,
      PdePrivateReadingClient client,
      ObjectMapper json,
      @Value("${integrations.pde-platform.mira-product-id:10}") long miraProductId) {
    this.products = products;
    this.client = client;
    this.json = json;
    this.miraProductId = miraProductId;
  }

  /** Reconhece Mira por identidade, impedindo fallback manual quando sua versão mudar. */
  public boolean supports(Product product) {
    return product != null && Long.valueOf(miraProductId).equals(product.getId());
  }

  /** Retorna acesso e prontidão calculados no backend, sem credenciais dos convites. */
  @Transactional(readOnly = true)
  public PrivateReadingWorkspace workspace(long productId, String activityId) {
    Product product =
        products
            .findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));
    requireSupported(product);
    int readingNumber = readingNumber(activityId);
    PrivateReadingEvidence evidence = checkedEvidence(product, readingNumber);
    boolean started = "PRIVATE_READING".equals(evidence.trafficClass());
    boolean finished = started && evidence.finishedAt() != null;
    String guidance =
        !started
            ? "Abra o protótipo com o convite individual desta leitura. A própria pessoa aceita participar e usa seus produtos."
            : !finished
                ? "A leitura começou. Após a pessoa terminar, clique em Atualizar resultado."
                : evidence.signals().values().stream().allMatch(Boolean.TRUE::equals)
                    ? "A leitura terminou com os cinco sinais. Confirme a observação humana para registrar."
                    : "A leitura terminou com sinais não observados. Registre o resultado para preservar o aprendizado; o avanço ficará bloqueado para ajuste.";
    return new PrivateReadingWorkspace(
        prototypeUrl(product),
        evidence.prototypeVersion(),
        activityId,
        readingNumber,
        evidence.participantReference(),
        evidence.evidenceId(),
        evidence.signals(),
        finished,
        !started ? "NOT_STARTED" : finished ? "FINISHED" : "IN_PROGRESS",
        guidance,
        evidence.finishedAt());
  }

  /**
   * Reconsulta a prova no momento da decisão e ignora sinais ou identidade informados pela tela.
   */
  public Map<String, Object> verifiedEvidence(
      Product product, String activityId, Map<String, Object> input) {
    requireSupported(product);
    if (!Boolean.TRUE.equals(input.get("humanReadingConfirmed"))) {
      throw new IllegalArgumentException(
          "Confirme a leitura de uma pessoa real, elegível e consentida.");
    }
    PrivateReadingEvidence evidence = checkedEvidence(product, readingNumber(activityId));
    if (!"PRIVATE_READING".equals(evidence.trafficClass()) || evidence.finishedAt() == null) {
      throw new IllegalStateException(
          "A pessoa ainda não encerrou esta leitura privada. QA não conta como leitura humana.");
    }
    if (!evidence.evidenceId().equals(input.get("evidenceId"))) {
      throw new IllegalArgumentException(
          "A evidência mudou. Atualize o resultado antes de confirmar.");
    }
    String observation = String.valueOf(input.getOrDefault("observation", "")).trim();
    if (observation.length() > 2000)
      throw new IllegalArgumentException("Limite a observação a 2000 caracteres.");
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("participantReference", evidence.participantReference());
    result.put("consentConfirmed", true);
    result.put("humanReadingConfirmed", true);
    result.put("firstPartyEvidenceConfirmed", true);
    result.put("signals", evidence.signals());
    result.put("evidenceId", evidence.evidenceId());
    result.put("evidenceReference", "pde-private-reading:" + evidence.evidenceId());
    result.put("consentedAt", evidence.consentedAt());
    result.put("finishedAt", evidence.finishedAt());
    result.put("trafficClass", evidence.trafficClass());
    result.put("observation", observation);
    return Map.copyOf(result);
  }

  /**
   * Valida origem, participante, versão, consentimento e fronteiras sem aceitar dados incompletos.
   */
  private PrivateReadingEvidence checkedEvidence(Product product, int number) {
    PrivateReadingEvidence evidence = client.fetch(number);
    if (evidence == null
        || !"mira-private-validation".equals(evidence.productSlug())
        || !acceptance(product)
            .path("prototypeVersion")
            .asText()
            .equals(evidence.prototypeVersion())
        || !("PV-00000000000" + number).equals(evidence.participantReference())
        || !Set.of("NOT_STARTED", "PRIVATE_READING")
            .contains(String.valueOf(evidence.trafficClass()))
        || evidence.signals() == null
        || !SIGNALS.equals(evidence.signals().keySet())
        || evidence.signals().values().stream().anyMatch(value -> value == null)
        || !"SIMULATED_NO_CHARGE".equals(evidence.checkoutMode())
        || !Boolean.FALSE.equals(evidence.paymentEnabled())
        || !Boolean.FALSE.equals(evidence.published())
        || !Integer.valueOf(0).equals(evidence.mediaSpendBrl())) {
      throw new IllegalStateException(
          "A prova recebida não corresponde à leitura privada deste produto e versão.");
    }
    if ("PRIVATE_READING".equals(evidence.trafficClass())) {
      if (evidence.evidenceId() == null
          || !evidence.evidenceId().matches("[a-f0-9-]{36}")
          || !Boolean.TRUE.equals(evidence.signals().get("EXPERIENCE_STARTED"))) {
        throw new IllegalStateException("A leitura não possui referência auditável íntegra.");
      }
      try {
        Instant consented = Instant.parse(evidence.consentedAt());
        Instant finished =
            evidence.finishedAt() == null ? null : Instant.parse(evidence.finishedAt());
        if (consented.isBefore(Instant.parse(acceptance(product).path("acceptedAt").asText()))
            || (finished != null && finished.isBefore(consented))) {
          throw new IllegalArgumentException(
              "A leitura deve ocorrer após a aceitação desta versão.");
        }
      } catch (RuntimeException ex) {
        log.error(
            "Prova temporal inválida da leitura privada; productId={} readingNumber={}",
            product.getId(),
            number,
            ex);
        throw new IllegalStateException("A prova não possui consentimento e horários válidos.", ex);
      }
    } else if (evidence.signals().values().stream().anyMatch(Boolean.TRUE::equals)
        || evidence.finishedAt() != null
        || evidence.consentedAt() != null
        || evidence.evidenceId() != null) {
      throw new IllegalStateException(
          "A prova de leitura não iniciada contém sinais inconsistentes.");
    }
    return evidence;
  }

  /** Exige a aceitação privada já persistida antes de consultar qualquer prova. */
  private void requireSupported(Product product) {
    if (!supports(product)
        || !"PLANNED".equals(product.getCommercialStatus())
        || !"mira-private-v1".equals(acceptance(product).path("prototypeVersion").asText())
        || !"READY".equals(acceptance(product).path("status").asText())) {
      throw new IllegalArgumentException(
          "Este produto ainda não possui leitura privada assistida disponível.");
    }
  }

  /** Resolve primeira e segunda leitura sem permitir consulta arbitrária de participantes. */
  private int readingNumber(String activityId) {
    return switch (activityId) {
      case "privateReading1" -> 1;
      case "privateReading2" -> 2;
      default -> throw new IllegalArgumentException("Atividade de leitura privada inválida.");
    };
  }

  /** Extrai a URL aceita, recusando segredos, credenciais ou esquemas executáveis. */
  private String prototypeUrl(Product product) {
    String value = acceptance(product).path("privateAccessUrl").asText();
    try {
      URI uri = URI.create(value);
      if (!"https".equals(uri.getScheme())
          || uri.getHost() == null
          || uri.getUserInfo() != null
          || uri.getQuery() != null
          || uri.getFragment() != null
          || !"/mira-private".equals(uri.getPath())) {
        throw new IllegalArgumentException("URL privada aceita inválida.");
      }
      return value;
    } catch (RuntimeException ex) {
      log.error("Falha ao validar URL do protótipo privado; productId={}", product.getId(), ex);
      throw new IllegalStateException("O acesso aceito do protótipo precisa ser corrigido.", ex);
    }
  }

  /** Lê a aceitação do contrato canônico com diagnóstico de corrupção sem imprimir seu conteúdo. */
  private JsonNode acceptance(Product product) {
    try {
      return json.readTree(product.getValidationDefinitionJson())
          .path("privatePrototypeAcceptance");
    } catch (Exception ex) {
      log.error("Falha ao ler aceitação privada; productId={}", product.getId(), ex);
      throw new IllegalStateException("O contrato privado do produto está inválido.", ex);
    }
  }
}

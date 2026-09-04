package com.marketinghub.researchintelligence.v1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.repository.jpa.researchintelligence.ResearchIntelligenceCardRepository;
import com.marketinghub.repository.jpa.researchintelligence.ResearchIntelligenceCardVersionRepository;
import com.marketinghub.researchintelligence.v1.ResearchIntelligenceCard;
import com.marketinghub.researchintelligence.v1.ResearchIntelligenceCardStatus;
import com.marketinghub.researchintelligence.v1.ResearchIntelligenceCardVersion;
import com.marketinghub.researchintelligence.v1.ResearchIntelligenceInternalRequestVerifier;
import com.marketinghub.researchintelligence.v1.service.managecard.RegisterResearchIntelligenceCardRequest;
import com.marketinghub.researchintelligence.v1.service.managecard.ResearchIntelligenceCardListResponse;
import com.marketinghub.researchintelligence.v1.service.managecard.ResearchIntelligenceCardVersionResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Governa autenticação, versionamento e estados editoriais dos cartões persistidos. */
@Service
public class ResearchIntelligenceCardManagementService {
  private static final Logger log =
      LoggerFactory.getLogger(ResearchIntelligenceCardManagementService.class);

  private final Clock clock;
  private final ResearchIntelligenceCardRepository cardRepository;
  private final ResearchIntelligenceCardVersionRepository versionRepository;
  private final ResearchIntelligenceInternalRequestVerifier requestVerifier;
  private final ObjectMapper objectMapper;
  private final ResearchIntelligenceService catalogService;

  /** Injeta persistência, autenticação e a política canônica de roteamento. */
  @Autowired
  public ResearchIntelligenceCardManagementService(
      ResearchIntelligenceCardRepository cardRepository,
      ResearchIntelligenceCardVersionRepository versionRepository,
      ResearchIntelligenceInternalRequestVerifier requestVerifier,
      ObjectMapper objectMapper,
      ResearchIntelligenceService catalogService) {
    this(
        Clock.systemUTC(),
        cardRepository,
        versionRepository,
        requestVerifier,
        objectMapper,
        catalogService);
  }

  /** Permite controlar relógio e dependências nos testes editoriais. */
  ResearchIntelligenceCardManagementService(
      Clock clock,
      ResearchIntelligenceCardRepository cardRepository,
      ResearchIntelligenceCardVersionRepository versionRepository,
      ResearchIntelligenceInternalRequestVerifier requestVerifier,
      ObjectMapper objectMapper,
      ResearchIntelligenceService catalogService) {
    this.clock = Objects.requireNonNull(clock);
    this.cardRepository = Objects.requireNonNull(cardRepository);
    this.versionRepository = Objects.requireNonNull(versionRepository);
    this.requestVerifier = Objects.requireNonNull(requestVerifier);
    this.objectMapper = Objects.requireNonNull(objectMapper);
    this.catalogService = Objects.requireNonNull(catalogService);
  }

  /** Autentica o contrato interno antes de delegar qualquer operação persistente. */
  public void verifyInternalRequest(
      HttpServletRequest request, String actor, String idempotencyKey, Object body) {
    requestVerifier.verify(request, actor, idempotencyKey, body);
  }

  /** Cadastra uma versão nova ou devolve a resposta anterior da mesma chave idempotente. */
  @Transactional
  public ResearchIntelligenceCardVersionResponse registerCard(
      RegisterResearchIntelligenceCardRequest request, String actor, String idempotencyKey) {
    validateRegistration(request, idempotencyKey);
    String payloadHash = payloadHash(request);
    var prior = versionRepository.findByIdempotencyKey(idempotencyKey);
    if (prior.isPresent()) {
      return idempotentResponse(prior.get(), payloadHash);
    }

    LocalDateTime now = now();
    cardRepository.insertIfMissing(request.cardKey(), now);
    ResearchIntelligenceCard card =
        cardRepository
            .findByCardKeyForUpdate(request.cardKey())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Identidade do cartão não foi materializada."));
    prior = versionRepository.findByIdempotencyKey(idempotencyKey);
    if (prior.isPresent()) {
      return idempotentResponse(prior.get(), payloadHash);
    }

    int version = versionRepository.findMaximumVersionNumber(request.cardKey()).orElse(0) + 1;
    String cardId =
        "RI1-"
            + sha256(request.cardKey() + ":" + version).substring(0, 12).toUpperCase(Locale.ROOT);
    ResearchIntelligenceCardVersion created =
        new ResearchIntelligenceCardVersion(
            request.cardKey(),
            version,
            cardId,
            request.collection(),
            cleanInput(request.title()),
            cleanInput(request.finding()),
            cleanInput(request.mechanism()),
            cleanInput(request.commercialApplication()),
            cleanInput(request.evidenceStrength()),
            request.publishedOn(),
            request.validUntil(),
            cleanInput(request.experimentHypothesis()),
            cleanInput(request.risks()),
            cleanInput(request.limits()),
            request.sourceKind(),
            request.sourceUri().trim(),
            cleanInput(request.sourceTitle()),
            request.sourceSha256(),
            idempotencyKey,
            payloadHash,
            actor,
            now);
    card.touch(now);
    cardRepository.save(card);
    return managementResponse(versionRepository.save(created));
  }

  /** Lista versões no banco aplicando estado, coleção e limite antes de montar a resposta. */
  @Transactional(readOnly = true)
  public ResearchIntelligenceCardListResponse listCards(
      String statusValue, String collection, int limit) {
    if (limit < 1 || limit > 200) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit deve estar entre 1 e 200.");
    }
    ResearchIntelligenceCardStatus status = parseStatus(statusValue);
    String normalizedCollection = StringUtils.hasText(collection) ? collection.trim() : null;
    List<ResearchIntelligenceCardVersionResponse> items =
        versionRepository
            .findForManagement(status, normalizedCollection, PageRequest.of(0, limit))
            .stream()
            .map(this::managementResponse)
            .toList();
    return new ResearchIntelligenceCardListResponse(items.size(), items);
  }

  /** Obtém uma versão específica com sua trilha editorial completa. */
  @Transactional(readOnly = true)
  public ResearchIntelligenceCardVersionResponse getCard(String cardKey, int version) {
    return managementResponse(
        versionRepository
            .findByCardKeyAndVersionNumber(cardKey, version)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cartão não encontrado.")));
  }

  /** Move um rascunho para revisão sem alterar o conteúdo cadastrado. */
  @Transactional
  public ResearchIntelligenceCardVersionResponse submitCardForReview(
      String cardKey, int version, String actor, String reason) {
    ResearchIntelligenceCardVersion card = lockVersion(cardKey, version);
    if (card.getStatus() == ResearchIntelligenceCardStatus.IN_REVIEW) {
      return managementResponse(card);
    }
    requireStatus(card, ResearchIntelligenceCardStatus.DRAFT);
    card.submitForReview(actor, cleanReason(reason), now());
    return managementResponse(versionRepository.save(card));
  }

  /** Ativa uma versão revisada e arquiva a ativa anterior na mesma transação. */
  @Transactional
  public ResearchIntelligenceCardVersionResponse activateCard(
      String cardKey, int version, String actor, String reason) {
    ResearchIntelligenceCardVersion card = lockVersion(cardKey, version);
    if (card.getStatus() == ResearchIntelligenceCardStatus.ACTIVE) {
      return managementResponse(card);
    }
    requireStatus(card, ResearchIntelligenceCardStatus.IN_REVIEW);
    if (card.getValidUntil().isBefore(LocalDate.now(clock))) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Cartão vencido não pode ser ativado.");
    }
    LocalDateTime now = now();
    String cleanReason = cleanReason(reason);
    versionRepository
        .findByCardKeyAndStatusForUpdate(cardKey, ResearchIntelligenceCardStatus.ACTIVE)
        .forEach(
            prior ->
                prior.archive(
                    actor, "Substituída pela versão " + version + ": " + cleanReason, now));
    card.activate(actor, cleanReason, now);
    return managementResponse(versionRepository.save(card));
  }

  /** Arquiva uma versão ativa sem apagar sua fonte ou histórico de uso. */
  @Transactional
  public ResearchIntelligenceCardVersionResponse archiveCard(
      String cardKey, int version, String actor, String reason) {
    ResearchIntelligenceCardVersion card = lockVersion(cardKey, version);
    if (card.getStatus() == ResearchIntelligenceCardStatus.ARCHIVED) {
      return managementResponse(card);
    }
    requireStatus(card, ResearchIntelligenceCardStatus.ACTIVE);
    card.archive(actor, cleanReason(reason), now());
    return managementResponse(versionRepository.save(card));
  }

  /** Confere se uma repetição idempotente conserva exatamente o payload original. */
  private ResearchIntelligenceCardVersionResponse idempotentResponse(
      ResearchIntelligenceCardVersion prior, String payloadHash) {
    if (!prior.getPayloadSha256().equals(payloadHash)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Idempotency-Key já utilizada com outro payload.");
    }
    return managementResponse(prior);
  }

  /** Valida fonte, cronologia, coleção e idempotência antes de criar qualquer linha. */
  private void validateRegistration(
      RegisterResearchIntelligenceCardRequest request, String idempotencyKey) {
    if (!catalogService.supportsCollection(request.collection())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Coleção não possui roteamento canônico.");
    }
    if (request.validUntil().isBefore(request.publishedOn())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "validUntil não pode ser anterior a publishedOn.");
    }
    validateSourceReference(request);
    if (!StringUtils.hasText(idempotencyKey)
        || !idempotencyKey.matches("^[A-Za-z0-9._:-]{8,128}$")) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Idempotency-Key inválida ou ausente.");
    }
  }

  /** Exige coerência entre o formato declarado e o esquema do endereço sem buscar a fonte. */
  private void validateSourceReference(RegisterResearchIntelligenceCardRequest request) {
    String sourceUri = request.sourceUri();
    boolean compatible =
        switch (request.sourceKind()) {
          case URL -> sourceUri.startsWith("https://");
          case PDF -> sourceUri.startsWith("https://") || sourceUri.startsWith("s3://");
          case MARKDOWN -> sourceUri.startsWith("https://") || sourceUri.startsWith("repo:");
          case TEXT -> sourceUri.startsWith("urn:");
        };
    if (!compatible) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "sourceUri incompatível com sourceKind.");
    }
  }

  /** Calcula a identidade exata do JSON usado pelo contrato de idempotência. */
  private String payloadHash(RegisterResearchIntelligenceCardRequest request) {
    try {
      return sha256(objectMapper.writeValueAsString(request));
    } catch (JsonProcessingException ex) {
      log.error(
          "Falha ao calcular payload do cartão cardKey={} collection={} errorLine={} errorClass={} errorMessage={}",
          request.cardKey(),
          request.collection(),
          errorLine(ex),
          ex.getClass().getName(),
          ex.getMessage(),
          ex);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível registrar o payload.", ex);
    }
  }

  /** Bloqueia a raiz e a versão antes de mudar seu estado editorial. */
  private ResearchIntelligenceCardVersion lockVersion(String cardKey, int version) {
    cardRepository
        .findByCardKeyForUpdate(cardKey)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cartão não encontrado."));
    return versionRepository
        .findVersionForUpdate(cardKey, version)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versão não encontrada."));
  }

  /** Exige a origem esperada de uma transição para impedir saltos editoriais. */
  private void requireStatus(
      ResearchIntelligenceCardVersion card, ResearchIntelligenceCardStatus expected) {
    if (card.getStatus() != expected) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Transição exige status " + expected + ", atual " + card.getStatus() + ".");
    }
  }

  /** Normaliza justificativas sem aceitar uma aprovação vazia. */
  private String cleanReason(String reason) {
    String clean = cleanInput(reason);
    if (clean.isBlank() || clean.length() > 500) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Justificativa deve conter de 1 a 500 caracteres.");
    }
    return clean;
  }

  /** Remove espaçamento acidental sem reescrever o conteúdo editorial recebido. */
  private String cleanInput(String value) {
    return value == null ? "" : value.trim().replaceAll("\\s+", " ");
  }

  /** Interpreta o filtro de estado sem aceitar enum inventado pelo cliente. */
  private ResearchIntelligenceCardStatus parseStatus(String statusValue) {
    if (!StringUtils.hasText(statusValue)) {
      return null;
    }
    try {
      return ResearchIntelligenceCardStatus.valueOf(statusValue.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      log.warn(
          "Filtro inválido na Biblioteca status={} errorLine={} errorClass={} errorMessage={}",
          statusValue,
          errorLine(ex),
          ex.getClass().getName(),
          ex.getMessage(),
          ex);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status de cartão inválido.", ex);
    }
  }

  /** Converte a entidade completa no contrato sanitizado da API de gestão. */
  private ResearchIntelligenceCardVersionResponse managementResponse(
      ResearchIntelligenceCardVersion version) {
    String effectiveStatus =
        version.getStatus() == ResearchIntelligenceCardStatus.ACTIVE
                && version.getValidUntil().isBefore(LocalDate.now(clock))
            ? "EXPIRED"
            : version.getStatus().name();
    return new ResearchIntelligenceCardVersionResponse(
        version.getCardKey(),
        version.getVersionNumber(),
        version.getCardId(),
        version.getStatus(),
        effectiveStatus,
        version.getCollection(),
        catalogService.routableAgentsForCollection(version.getCollection()),
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
        version.getSourceKind(),
        version.getSourceUri(),
        version.getSourceTitle(),
        version.getSourceSha256(),
        version.getCreatedBy(),
        version.getCreatedAt(),
        version.getReviewSubmittedBy(),
        version.getReviewSubmittedAt(),
        version.getReviewNote(),
        version.getActivatedBy(),
        version.getActivatedAt(),
        version.getActivationNote(),
        version.getArchivedBy(),
        version.getArchivedAt(),
        version.getArchiveNote(),
        version.getUpdatedAt());
  }

  /** Obtém horário UTC sem depender do timezone do host ou do banco. */
  private LocalDateTime now() {
    return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
  }

  /** Calcula SHA-256 para a identidade do payload e da versão. */
  private String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      log.error(
          "SHA-256 indisponível na gestão da Biblioteca errorLine={} errorClass={} errorMessage={}",
          errorLine(ex),
          ex.getClass().getName(),
          ex.getMessage(),
          ex);
      throw new IllegalStateException("SHA-256 indisponível.", ex);
    }
  }

  /** Localiza a primeira linha da stack sem perder o stack trace da falha. */
  private static int errorLine(Throwable error) {
    return error.getStackTrace().length == 0 ? -1 : error.getStackTrace()[0].getLineNumber();
  }
}

package com.marketinghub.experiment.directcontact.v1;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experimentdirectcontact.ExperimentDirectContactRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar, registrar e resumir a amostra de contatos diretos consentidos. */
@Service
public class ExperimentDirectContactService {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ExperimentDirectContactService.class);
  private static final long FUTURE_TOLERANCE_SECONDS = 300;
  private final ExperimentRepository experiments;
  private final ExperimentDirectContactRepository contacts;
  private final Clock clock;

  /** Configura a persistência da amostra usando o relógio UTC da aplicação. */
  @Autowired
  public ExperimentDirectContactService(
      ExperimentRepository experiments, ExperimentDirectContactRepository contacts) {
    this(experiments, contacts, Clock.systemUTC());
  }

  /** Permite testes determinísticos das regras temporais da amostra. */
  ExperimentDirectContactService(
      ExperimentRepository experiments, ExperimentDirectContactRepository contacts, Clock clock) {
    this.experiments = experiments;
    this.contacts = contacts;
    this.clock = clock;
  }

  /** Retorna o placar completo sem misturar visitas, QA ou eventos de mídia. */
  @Transactional(readOnly = true)
  public ExperimentDirectContactSampleResponse getSample(Long experimentId) {
    Experiment experiment = directExperiment(experimentId);
    long recorded = contacts.countByExperimentId(experimentId);
    int target = targetContacts(experiment);
    long remaining = Math.max(0L, target - recorded);
    return new ExperimentDirectContactSampleResponse(
        experiment.getId(),
        experiment.getPlatform().name(),
        experiment.getStatus() == null ? null : experiment.getStatus().name(),
        target,
        recorded,
        remaining,
        target > 0 && recorded >= target,
        recorded >= target ? "READY_FOR_HERMES_REVIEW" : "ACCUMULATING_CONSENTED_SAMPLE",
        contacts.findByExperimentIdOrderByContactedAtAscIdAsc(experimentId).stream()
            .map(this::response)
            .toList());
  }

  /** Registra um contato realizado somente após validar canal, consentimento e aderência. */
  @Transactional
  public ExperimentDirectContactSampleResponse register(
      Long experimentId, RegisterExperimentDirectContactRequest request) {
    LOGGER.info(
        "experiment_direct_contact_received experimentId={} payload={}", experimentId, request);
    Experiment experiment = directExperimentForUpdate(experimentId);
    requireRunning(experiment);
    long recorded = contacts.countByExperimentId(experimentId);
    int target = targetContacts(experiment);
    if (target <= 0) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Defina uma amostra maior que zero antes de registrar contatos.");
    }
    if (recorded >= target) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "A amostra já atingiu a meta definida para o experimento.");
    }
    String fingerprint = request.contactFingerprint().trim().toLowerCase(Locale.ROOT);
    requireEvidenceReference(request.consentEvidenceReference());
    requireTimeline(request.consentRecordedAt(), request.contactedAt());
    if (!Boolean.TRUE.equals(request.audienceFitConfirmed())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Confirme a aderência ao público antes de registrar o contato.");
    }
    if (contacts.existsByExperimentIdAndContactFingerprint(experimentId, fingerprint)) {
      throw duplicateContact();
    }
    ExperimentDirectContact contact = new ExperimentDirectContact();
    contact.setExperiment(experiment);
    contact.setContactFingerprint(fingerprint);
    contact.setConsentEvidenceReference(request.consentEvidenceReference().trim());
    contact.setConsentRecordedAt(request.consentRecordedAt());
    contact.setContactedAt(request.contactedAt());
    contact.setAudienceFitConfirmed(true);
    contact.setRecordedBy(request.recordedBy().trim());
    contact.setCreatedAt(Instant.now(clock));
    try {
      contacts.saveAndFlush(contact);
    } catch (DataIntegrityViolationException ex) {
      LOGGER.warn(
          "Falha ao persistir contato direto. modulo=backend operacao=registerDirectContact experimentId={}",
          experimentId,
          ex);
      throw duplicateContact();
    }
    LOGGER.info(
        "experiment_direct_contact_recorded experimentId={} contactId={} contactedAt={}",
        experimentId,
        contact.getId(),
        contact.getContactedAt());
    return getSample(experimentId);
  }

  /** Conta somente contatos consentidos e aderentes já persistidos na amostra oficial. */
  @Transactional(readOnly = true)
  public long countRecordedContacts(Long experimentId) {
    return contacts.countByExperimentId(experimentId);
  }

  /** Informa se o experimento usa o canal individual sem impor esse gate a canais pagos. */
  @Transactional(readOnly = true)
  public boolean isDirectOneToOne(Long experimentId) {
    return experiments
        .findById(experimentId)
        .map(experiment -> experiment.getPlatform() == ExperimentPlatform.DIRECT_ONE_TO_ONE)
        .orElse(false);
  }

  /** Resolve o alvo vigente sem inventar piso genérico de tráfego. */
  public int targetContacts(Experiment experiment) {
    return experiment.getSampleSize() == null ? 0 : Math.max(0, experiment.getSampleSize());
  }

  /** Exige que a referência pertença a um experimento realmente individual. */
  private Experiment directExperiment(Long experimentId) {
    Experiment experiment =
        experiments
            .findById(experimentId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Experimento não encontrado."));
    if (experiment.getPlatform() != ExperimentPlatform.DIRECT_ONE_TO_ONE) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "A amostra de contatos consentidos existe somente para DIRECT_ONE_TO_ONE.");
    }
    return experiment;
  }

  /** Carrega e bloqueia o experimento para serializar o limite da amostra. */
  private Experiment directExperimentForUpdate(Long experimentId) {
    Experiment experiment =
        contacts
            .findExperimentByIdForUpdate(experimentId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Experimento não encontrado."));
    if (experiment.getPlatform() != ExperimentPlatform.DIRECT_ONE_TO_ONE) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "A amostra de contatos consentidos existe somente para DIRECT_ONE_TO_ONE.");
    }
    return experiment;
  }

  /** Impede registrar contatos antes da ativação ou depois do encerramento. */
  private void requireRunning(Experiment experiment) {
    if (experiment.getStatus() != ExperimentStatus.RUNNING) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "O experimento precisa estar RUNNING para registrar um contato da amostra.");
    }
  }

  /** Aceita somente referência auditável interna ou HTTPS, sem texto solto. */
  private void requireEvidenceReference(String evidenceReference) {
    String value = evidenceReference == null ? "" : evidenceReference.trim();
    if ((!value.startsWith("internal://") && !value.startsWith("https://"))
        || value.contains(" ")) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "A evidência de consentimento deve usar uma referência internal:// ou HTTPS.");
    }
  }

  /** Garante consentimento anterior ao contato e rejeita horários futuros. */
  private void requireTimeline(Instant consentRecordedAt, Instant contactedAt) {
    Instant latestAccepted = Instant.now(clock).plusSeconds(FUTURE_TOLERANCE_SECONDS);
    if (consentRecordedAt.isAfter(contactedAt)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "O consentimento precisa existir antes do contato.");
    }
    if (consentRecordedAt.isAfter(latestAccepted) || contactedAt.isAfter(latestAccepted)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Consentimento e contato não podem estar no futuro.");
    }
  }

  /** Padroniza o conflito de duplicidade sem revelar o identificador pseudonimizado. */
  private ResponseStatusException duplicateContact() {
    return new ResponseStatusException(
        HttpStatus.CONFLICT, "Este contato já foi contabilizado na amostra do experimento.");
  }

  /** Converte a entidade em resposta pseudonimizada para a tela administrativa. */
  private ExperimentDirectContactResponse response(ExperimentDirectContact contact) {
    String fingerprint = contact.getContactFingerprint();
    return new ExperimentDirectContactResponse(
        contact.getId(),
        fingerprint.substring(Math.max(0, fingerprint.length() - 12)),
        contact.getConsentEvidenceReference(),
        contact.getConsentRecordedAt(),
        contact.getContactedAt(),
        contact.isAudienceFitConfirmed(),
        contact.getRecordedBy(),
        contact.getCreatedAt());
  }
}

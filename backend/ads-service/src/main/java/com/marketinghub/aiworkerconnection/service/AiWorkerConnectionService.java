package com.marketinghub.aiworkerconnection.service;

import com.marketinghub.aiworkerconnection.AiWorkerConnection;
import com.marketinghub.aiworkerconnection.service.deviceCode.DeviceCodeRequest;
import com.marketinghub.aiworkerconnection.service.session.SessionReport;
import com.marketinghub.aiworkerconnection.service.status.ConnectionResponse;
import com.marketinghub.repository.jpa.aiworkerconnection.AiWorkerConnectionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Mantém comandos e provas de conexão; o login e os agendamentos pertencem ao Worker IA. */
@Service
@Transactional
public class AiWorkerConnectionService {
  private static final Set<String> ACTIVE =
      Set.of("REQUESTED", "STARTING", "AWAITING_CONFIRMATION");
  private final AiWorkerConnectionRepository repository;
  private final Clock clock;

  /** Usa o relógio de produção sem depender do fuso do servidor. */
  @Autowired
  public AiWorkerConnectionService(AiWorkerConnectionRepository repository) {
    this(repository, Clock.systemUTC());
  }

  /** Permite verificar prazos e provas vencidas com relógio determinístico. */
  AiWorkerConnectionService(AiWorkerConnectionRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  /** Consulta o estado e encerra um comando cujo prazo absoluto expirou. */
  public ConnectionResponse current() {
    AiWorkerConnection control = repository.lockControl();
    return response(latest(), control);
  }

  /** Reutiliza a tentativa ativa e cria outra somente após seu encerramento. */
  public ConnectionResponse request() {
    AiWorkerConnection control = repository.lockControl();
    AiWorkerConnection item = latest();
    if (!ACTIVE.contains(item.getStatus())) {
      Instant now = clock.instant();
      item = repository.save(new AiWorkerConnection(now, now.plus(Duration.ofMinutes(15))));
    }
    return response(item, control);
  }

  /** Reserva uma única tentativa pendente para o Worker sem realizar chamadas externas. */
  public ConnectionResponse pending() {
    AiWorkerConnection control = repository.lockControl();
    AiWorkerConnection item = latest();
    if (!"REQUESTED".equals(item.getStatus())) return null;
    item.start();
    return response(item, control);
  }

  /** Aceita publicação idempotente do mesmo código dentro da tentativa vigente. */
  public ConnectionResponse deviceCode(Long id, DeviceCodeRequest request) {
    AiWorkerConnection control = repository.lockControl();
    AiWorkerConnection item = active(id);
    if (!"STARTING".equals(item.getStatus())
        && !("AWAITING_CONFIRMATION".equals(item.getStatus())
            && request.userCode().equals(item.getUserCode())
            && request.verificationUrl().equals(item.getVerificationUrl()))) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Código não pertence à tentativa ativa.");
    }
    item.awaitConfirmation(request.verificationUrl(), request.userCode());
    return response(item, control);
  }

  /** Registra confirmação ou falha sem reabrir tentativa expirada nem repetir o login. */
  public ConnectionResponse complete(Long id, SessionReport request) {
    AiWorkerConnection control = repository.lockControl();
    AiWorkerConnection item = latest();
    String terminal = request.authenticated() ? "AUTHENTICATED" : "FAILED";
    if (item.getId().equals(id) && terminal.equals(item.getStatus()))
      return response(item, control);
    item = active(id);
    if ("REQUESTED".equals(item.getStatus())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Tentativa ainda não foi reservada.");
    }
    item.finish(terminal, clock.instant());
    control.reportSession(request.authenticated(), clock.instant());
    return response(item, control);
  }

  /** Atualiza a disponibilidade a partir de uma prova independente enviada pelo executor. */
  public ConnectionResponse session(SessionReport report) {
    AiWorkerConnection control = repository.lockControl();
    control.reportSession(report.authenticated(), clock.instant());
    return response(latest(), control);
  }

  /** Recupera a tentativa atual e elimina seus dados temporários após o vencimento. */
  private AiWorkerConnection latest() {
    AiWorkerConnection item = repository.findFirstByOrderByIdDesc().orElseThrow();
    if (ACTIVE.contains(item.getStatus()) && !clock.instant().isBefore(item.getExpiresAt())) {
      item.finish("EXPIRED", clock.instant());
    }
    return item;
  }

  /** Impede callbacks atrasados ou pertencentes a outra tentativa de conexão. */
  private AiWorkerConnection active(Long id) {
    AiWorkerConnection item = latest();
    if (!item.getId().equals(id) || !ACTIVE.contains(item.getStatus())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Tentativa encerrada; gere um novo código.");
    }
    return item;
  }

  /** Distingue uma prova recente de um histórico de login sem contato atual do Worker. */
  private ConnectionResponse response(AiWorkerConnection item, AiWorkerConnection control) {
    boolean available =
        control.getCheckedAt() != null
            && control.getCheckedAt().isAfter(clock.instant().minus(Duration.ofMinutes(3)));
    return new ConnectionResponse(
        item.getId(),
        item.getStatus(),
        available && control.isSessionAuthenticated(),
        available,
        control.getCheckedAt(),
        item.getVerificationUrl(),
        item.getUserCode(),
        item.getRequestedAt(),
        item.getExpiresAt());
  }
}

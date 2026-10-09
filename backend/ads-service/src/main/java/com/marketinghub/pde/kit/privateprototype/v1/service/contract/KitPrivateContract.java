package com.marketinghub.pde.kit.privateprototype.v1.service.contract;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Responsabilidade: declarar os contratos de acesso, composição e prova privada sem venda. */
public final class KitPrivateContract {
  /** Impede instanciação do catálogo de contratos. */
  private KitPrivateContract() {}

  /** Correlaciona uma sessão sintética com o contexto exato recebido pelo executor. */
  public record Create(
      @NotNull UUID requestKey,
      @NotNull @Min(1) Long productId,
      @NotNull @Min(1) Long cycleId,
      @NotBlank @Size(max = 160) String prototypeVersion,
      @NotBlank String scenarioCode,
      @NotBlank String deviceProfile,
      SafetyCase safetyCase) {
    /** Preserva a emissão anterior sem atribuir uma causa inexistente ao cenário de segurança. */
    public Create(
        UUID requestKey,
        Long productId,
        Long cycleId,
        String prototypeVersion,
        String scenarioCode,
        String deviceProfile) {
      this(requestKey, productId, cycleId, prototypeVersion, scenarioCode, deviceProfile, null);
    }
  }

  /** Identifica a condição sintética bloqueada, sem deduzi-la de dados pessoais ou texto livre. */
  public enum SafetyCase {
    UNVERIFIED_VISUAL_ORIGIN,
    EXTERNAL_ACTION
  }

  /** Entrega o acesso opaco uma única vez, sem colocá-lo em URLs ou evidências. */
  public record Created(String sessionId, String sessionToken, SessionView session) {}

  /** Recebe somente briefing sintético, com limites anteriores à reserva de recursos. */
  public record Input(
      @NotBlank @Email @Size(max = 160) String email,
      @NotBlank @Size(max = 100) String professionalName,
      @NotBlank @Size(max = 100) String cityRegion,
      @NotBlank @Size(max = 24) String whatsapp,
      @NotBlank @Size(max = 160) String services,
      @NotBlank @Size(max = 100) String visualStyle,
      @NotBlank @Size(max = 200) String weeklyGoal,
      @Size(max = 100) String preferredColors,
      @Size(max = 500) String notes,
      boolean consentAccepted) {}

  /** Registra uma ação técnica idempotente, nunca uma evidência humana. */
  public record Event(@NotNull UUID eventId, @NotBlank String code) {}

  /** Identifica a tentativa previamente reservada pelo backend. */
  public record Claim(@NotNull UUID claimKey) {}

  /** Transporta o arquivo composto e os limites auditados pelo executor. */
  public record Result(
      @NotNull UUID claimKey,
      @NotBlank @Size(max = 45000000) String zipBase64,
      @NotBlank @Pattern(regexp = "[a-f0-9]{64}") String zipSha256,
      @Min(0) @Max(0) int providerCalls) {}

  /** Preserva a falha terminal sem retentativa ou inferência automática. */
  public record Failure(@NotNull UUID claimKey, @NotBlank @Size(max = 2000) String error) {}

  /** Expõe o estado que a interface apresenta sem deduzir conclusão local. */
  public record SessionView(
      String id,
      Long productId,
      Long cycleId,
      Long experimentId,
      String prototypeVersion,
      String scenarioCode,
      String deviceProfile,
      String profileCode,
      String productName,
      String status,
      String reason,
      Presentation presentation,
      JsonNode input,
      JsonNode firstApplication,
      JsonNode manifest,
      List<JsonNode> events,
      int transfers,
      Instant expiresAt) {}

  /** Entrega explicação e ação permitida do estado persistido para apresentação pela interface. */
  public record Presentation(
      String title,
      String introduction,
      String reasonCode,
      String nextStep,
      String nextActionLabel,
      String nextActionPath) {}

  /** Entrega entrada estruturada ao único compositor registrado para o perfil do kit. */
  public record Pending(
      String id,
      Long productId,
      Long cycleId,
      Long experimentId,
      String prototypeVersion,
      String profileCode,
      String fixtureOwner,
      JsonNode input) {}

  /** Apresenta prova sanitizada para os pareceres independentes, sem credenciais ou arquivo. */
  public record Report(
      String contractVersion,
      Long cycleId,
      List<SessionView> sessions,
      int providerCalls,
      boolean commercialEvidenceEligible) {}

  /** Declara disponibilidade real da capacidade de materialização para a mesma versão. */
  public record Capability(
      boolean available, String profileCode, String reason, String prototypeUrl) {}
}

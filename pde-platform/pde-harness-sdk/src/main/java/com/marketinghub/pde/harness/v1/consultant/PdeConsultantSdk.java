package com.marketinghub.pde.harness.v1.consultant;

import com.marketinghub.pde.harness.v1.PdeAgentRunRequest;
import com.marketinghub.pde.harness.v1.PdeAgentRunResult;
import com.marketinghub.pde.harness.v1.PdeExecutionObserver;
import com.marketinghub.pde.harness.v1.PdeHarnessConfiguration;
import com.marketinghub.pde.harness.v1.PdeHarnessHealth;
import com.marketinghub.pde.harness.v1.PdeHarnessSdk;
import com.marketinghub.pde.harness.v1.PdeThreadBinding;
import java.util.Objects;

/** Oferece o perfil Java comum aos consultores PWA e WhatsApp sobre o PDE Harness. */
public final class PdeConsultantSdk implements AutoCloseable {
  private final PdeHarnessSdk harness;
  private final PdeConsultantPromptComposer promptComposer;

  /** Cria o perfil de consultoria usando a configuração segura do harness central. */
  public PdeConsultantSdk(PdeHarnessConfiguration configuration) {
    this(new PdeHarnessSdk(configuration));
  }

  /** Permite injetar o harness em homologações locais sem mudar o contrato do consultor. */
  PdeConsultantSdk(PdeHarnessSdk harness) {
    this.harness = Objects.requireNonNull(harness, "harness");
    this.promptComposer = new PdeConsultantPromptComposer();
  }

  /** Executa um turno e devolve as duas partes do prompt junto ao resultado auditável. */
  public PdeConsultantRunResult execute(PdeConsultantTurnRequest request) {
    return execute(request, PdeExecutionObserver.noop());
  }

  /** Executa um turno emitindo eventos incrementais para o worker responsável. */
  public PdeConsultantRunResult execute(
      PdeConsultantTurnRequest request, PdeExecutionObserver observer) {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(observer, "observer");
    String completePrompt = promptComposer.compose(request.channel(), request.promptParts());
    String completePromptVersion = promptComposer.composedVersion(request.promptParts());
    PdeAgentRunRequest agentRequest =
        toAgentRequest(request, completePrompt, completePromptVersion);
    PdeAgentRunResult result = harness.execute(agentRequest, observer);
    return new PdeConsultantRunResult(
        request.channel(),
        request.channel().productTypeCode(),
        request.promptParts().agentPart(),
        request.promptParts().activityPart(),
        completePrompt,
        completePromptVersion,
        result);
  }

  /** Expõe a prova de prontidão do App Server sem abrir um turno de modelo. */
  public PdeHarnessHealth health() {
    return harness.health();
  }

  /** Exclui a thread somente depois da autorização de esquecimento do backend. */
  public void forgetThread(
      com.marketinghub.pde.harness.v1.PdeConversationScope scope, PdeThreadBinding binding) {
    harness.forgetThread(scope, binding);
  }

  /** Encerra o processo local do App Server sem alterar estado funcional no backend. */
  @Override
  public void close() {
    harness.close();
  }

  /** Converte o contrato de canal na entrada multimodal já protegida pelo harness. */
  private PdeAgentRunRequest toAgentRequest(
      PdeConsultantTurnRequest request, String prompt, String promptVersion) {
    if (request.existingThreadBinding() == null) {
      return PdeAgentRunRequest.newThreadWithImages(
          request.context(),
          request.memory(),
          request.model(),
          prompt,
          promptVersion,
          request.outputSchema(),
          request.outputSchemaVersion(),
          request.imageInputs(),
          request.ephemeralThread());
    }
    return PdeAgentRunRequest.resumeThreadWithImages(
        request.context(),
        request.memory(),
        request.model(),
        prompt,
        promptVersion,
        request.outputSchema(),
        request.outputSchemaVersion(),
        request.imageInputs(),
        request.existingThreadBinding());
  }
}

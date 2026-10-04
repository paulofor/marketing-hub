package com.marketinghub.experiment.run.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: impedir nova homologação comercial de experimentos encerrados. */
public final class ExperimentHomologationLifecycle {
  private static final Set<ExperimentStatus> TERMINAL =
      Set.of(
          ExperimentStatus.VALIDATED,
          ExperimentStatus.INVALIDATED,
          ExperimentStatus.INCONCLUSIVE,
          ExperimentStatus.FINISHED,
          ExperimentStatus.FAILED);

  /** Impede instâncias de uma política determinística sem estado. */
  private ExperimentHomologationLifecycle() {}

  /** Consulta o impedimento comercial na data UTC usada pelos contratos do backend. */
  public static String blockReason(Experiment experiment) {
    return blockReason(experiment, LocalDate.now(ZoneOffset.UTC));
  }

  /** Confere estado terminal e fim da janela sem alterar provas, orçamento ou resultado. */
  public static String blockReason(Experiment experiment, LocalDate today) {
    if (experiment == null) return null;
    boolean terminal = experiment.getStatus() != null && TERMINAL.contains(experiment.getStatus());
    boolean expired = experiment.getEndDate() != null && experiment.getEndDate().isBefore(today);
    if (!terminal && !expired) return null;
    return "O experimento #"
        + experiment.getId()
        + (terminal
            ? " está encerrado (" + experiment.getStatus() + ")."
            : " tem janela encerrada.")
        + (expired ? " A janela terminou em " + experiment.getEndDate() + "." : "")
        + " Preserve resultados, custos e provas; não renove Plutus nem a homologação nesta referência."
        + " A continuidade exige decisão no aprendizado e novo ciclo/experimento com limites próprios."
        + " Esta execução não autoriza gasto.";
  }

  /** Recusa a mutação antes de criar tentativa ou substituir gates históricos. */
  public static void requireOpen(Experiment experiment) {
    String reason = blockReason(experiment);
    if (reason != null) throw new ResponseStatusException(HttpStatus.CONFLICT, reason);
  }
}

import type { VideoProductionCycle } from "../../api/salesVideo/useVideoProductionCycles";

/** Apresenta somente valores do controle financeiro persistido pelo servidor. */
export default function SingleVideoSpendingSummary({
  cycle,
}: {
  cycle: VideoProductionCycle;
}) {
  if (!cycle.financialSnapshot) return null;
  let snapshot;
  try {
    snapshot = JSON.parse(cycle.financialSnapshot);
  } catch {
    return (
      <p role="alert">
        O relatório financeiro não pôde ser lido. Confira o ciclo antes de
        produzir.
      </p>
    );
  }
  const control = snapshot.singleVideoSpendingControl;
  if (!control?.receiptId) return null;
  const money = (value: unknown) =>
    typeof value === "number" ? `US$ ${value.toFixed(4)}` : "Não informado";
  return (
    <section aria-label="Controle integral do vídeo">
      <h3>Um teto para todo o vídeo</h3>
      <p>
        Recibo #{control.receiptId}: {money(control.totalLimitUsd)} no total,
        incluindo agentes, produção, revisões e tentativas.
      </p>
      <dl>
        <dt>Máximo já reservado ou retido</dt>
        <dd>{money(control.retainedMaximumUsd)}</dd>
        <dt>Parcela da autorização ainda não reservada</dt>
        <dd>{money(control.unallocatedAuthorizationUsd)}</dd>
        <dt>Operações com resposta incerta</dt>
        <dd>{control.uncertainOperations}</dd>
        <dt>Operações com conciliação pendente</dt>
        <dd>{control.unsettledOperations}</dd>
      </dl>
      <p>{control.guidance}</p>
      {control.voiceRoute ? (
        <p>
          <strong>Voz desta produção:</strong> {control.voiceRoute.model} ·{" "}
          {control.voiceRoute.voice}. {control.voiceRoute.notice}
        </p>
      ) : null}
      {snapshot.irisProductionBrief ? (
        <p>
          Íris · tarefa #{snapshot.irisProductionBrief.taskId}:{" "}
          {snapshot.irisProductionBrief.status}. A fala é entrada da produção; o
          vídeo final ainda precisa das revisões independentes.
        </p>
      ) : null}
    </section>
  );
}

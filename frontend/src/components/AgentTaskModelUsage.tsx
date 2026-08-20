import type { AgentTask } from "../api/agentTask/types";

type ModelUsage = Pick<
  AgentTask,
  | "inputTokens"
  | "cachedInputTokens"
  | "outputTokens"
  | "estimatedCostUsd"
  | "costEstimationStatus"
>;

const integer = new Intl.NumberFormat("pt-BR");
const decimal = new Intl.NumberFormat("pt-BR", {
  minimumFractionDigits: 4,
  maximumFractionDigits: 8,
});

/** Apresenta o consumo econômico persistido da tarefa sem estimar valores no navegador. */
export default function AgentTaskModelUsage({ usage }: { usage: ModelUsage }) {
  if (usage.costEstimationStatus === "NOT_REPORTED") {
    return (
      <div className="small text-body-secondary" data-testid="task-model-usage">
        Consumo de IA não informado
      </div>
    );
  }

  const cost =
    usage.estimatedCostUsd == null
      ? "Preço do modelo indisponível"
      : `Custo estimado: US$ ${decimal.format(usage.estimatedCostUsd)}`;

  return (
    <div className="small" data-testid="task-model-usage">
      <div>
        Tokens: entrada {integer.format(usage.inputTokens ?? 0)} · saída{" "}
        {integer.format(usage.outputTokens ?? 0)} · cache{" "}
        {integer.format(usage.cachedInputTokens ?? 0)}
      </div>
      <div className="text-body-secondary">
        {usage.costEstimationStatus === "PARTIALLY_ESTIMATED"
          ? `${cost} (parcial)`
          : cost}
      </div>
    </div>
  );
}

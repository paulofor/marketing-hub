import { useState, type FormEvent } from "react";
import {
  cycleError,
  useCycleMutation,
  type LearningCycle,
} from "../../api/learningCycle/useLearningCycles";
import { createCycleRequestKey } from "../../api/learningCycle/createCycleRequestKey";
const money = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

/** Explica projeções disponíveis e registra a meta escolhida sem autorizar aquisição. */
export default function CycleValueFlowDecisions({
  cycle,
  onUpdated,
}: {
  cycle: LearningCycle;
  onUpdated?: (cycle: LearningCycle) => void;
}) {
  const mutation = useCycleMutation(cycle.productId, cycle.id);
  const [requestKey, setRequestKey] = useState(createCycleRequestKey);
  const [feedback, setFeedback] = useState("");
  const flow = cycle.valueFlow;
  if (!flow?.costScenarios.length) return null;
  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setFeedback("");
    const data = new FormData(e.currentTarget);
    try {
      const updated = await mutation.mutateAsync({
        contributionTarget: true,
        requestKey,
        expectedRevision: cycle.revision,
        minimumContributionPercent: Number(
          String(data.get("percent")).replace(",", "."),
        ),
        rationale: String(data.get("rationale")),
      });
      onUpdated?.(updated);
      setFeedback(
        "Sua decisão foi registrada neste ciclo e está disponível para Plutus. Não liberou mídia ou outros gastos.",
      );
      setRequestKey(createCycleRequestKey());
    } catch {
      /* O erro permanece no formulário, sem registrar rascunho como decisão. */
    }
  }
  return (
    <details className="mt-3">
      <summary>Custos por venda e sua meta de contribuição</summary>
      <p>
        Margem mínima de contribuição é a menor sobra aceitável por venda após
        entrega, processamento, suporte, taxas/impostos e aquisição. Essa sobra
        ajuda a pagar custos fixos e gerar lucro; não é lucro líquido. O
        percentual usa o valor cobrado por venda após descontos, considerando
        reembolsos sem dupla contagem.
      </p>
      <div className="table-responsive">
        <table className="table">
          <thead>
            <tr>
              <th>Cenário de Plutus</th>
              <th>Preço</th>
              <th>Custos variáveis projetados</th>
              <th>Sobra antes de aquisição</th>
            </tr>
          </thead>
          <tbody>
            {flow.costScenarios.map((s) => (
              <tr key={s.name}>
                <td>{s.name}</td>
                <td>{money.format(s.priceBrl)}</td>
                <td>{money.format(s.variableCostBrl)}</td>
                <td>
                  {money.format(s.remainderBeforeAcquisitionBrl)} (
                  {s.percentBeforeAcquisition ?? "indisponível"}%)
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p>
        São hipóteses do parecer, sem custo real conciliado ou CAC medido. A
        sobra após aquisição depende desse custo; os valores acima não autorizam
        gasto.
      </p>
      <details>
        <summary>Premissas, custos ausentes e fontes</summary>
        {flow.costScenarios.map((s) => (
          <p key={s.name}>
            <strong>
              {s.name} · tarefa #{s.sourceTaskId}:
            </strong>{" "}
            {s.assumptions}
          </p>
        ))}
      </details>
      <p>
        Exemplo fictício: uma venda de R$ 100 menos R$ 20 de entrega, R$ 10 de
        taxas/impostos e R$ 30 de aquisição deixa R$ 40, ou 40%. Exemplo não é
        recomendação nem decisão.
      </p>
      <p>
        Você pode definir uma meta agora ou manter a escolha para a preparação
        comercial. Uma meta maior reduz o valor disponível para aquisição e pode
        exigir rever preço ou custos; não interrompe esta implementação privada.
      </p>
      {flow.contributionTargetPercent != null ? (
        <p>
          <strong>Meta registrada: {flow.contributionTargetPercent}%.</strong>{" "}
          Plutus recebe a decisão com a identidade deste ciclo e a confere com
          os custos.
        </p>
      ) : (
        <p>Nenhuma meta foi escolhida pelo usuário neste ciclo.</p>
      )}
      {cycle.status === "OPEN" &&
        ["PLANNING", "ADJUSTMENT", "VALIDATION", "AUTHORIZATION"].includes(
          cycle.stage,
        ) && (
          <form aria-label="Definir meta de contribuição" onSubmit={submit}>
            <label className="form-label">
              Sua resposta: meta mínima (%) *
              <input
                className="form-control"
                name="percent"
                type="number"
                min="0.01"
                max="100"
                step="0.01"
                required
              />
            </label>
            <label className="form-label">
              Motivo ou condições da escolha *
              <textarea
                className="form-control"
                name="rationale"
                required
                maxLength={1000}
                placeholder="Para este produto, quero margem mínima de X% do valor cobrado por venda, após entrega, taxas/impostos e aquisição."
              />
            </label>
            {mutation.isError && (
              <p role="alert">{cycleError(mutation.error)}</p>
            )}
            <button className="btn btn-primary" disabled={mutation.isPending}>
              {mutation.isPending
                ? "Registrando…"
                : "Registrar minha meta neste ciclo"}
            </button>
            {feedback && <p role="status">{feedback}</p>}
          </form>
        )}
    </details>
  );
}

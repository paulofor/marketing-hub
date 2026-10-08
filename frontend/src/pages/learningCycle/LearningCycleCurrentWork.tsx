import { Link } from "react-router-dom";
import type { LearningCycle } from "../../api/learningCycle/useLearningCycles";
import CycleValueFlowDecisions from "./CycleValueFlowDecisions";
import CyclePrivateKitLaunch from "./CyclePrivateKitLaunch";

/** Destaca a dependência real do ciclo sem transformar acompanhamento em nova decisão. */
export default function LearningCycleCurrentWork({
  cycle,
  onUpdated,
}: {
  cycle: LearningCycle;
  onUpdated?: (cycle: LearningCycle) => void;
}) {
  const work = cycle.delegatedWork;
  const flow = cycle.valueFlow;
  if (flow)
    return (
      <section
        className="alert alert-info"
        aria-label="Trabalho atual do ciclo"
      >
        <h3 className="h5">O que está acontecendo</h3>
        <p className="fw-bold">{flow.situation}</p>
        <p>
          <strong>O que impede a venda:</strong> {flow.saleBlocker}
        </p>
        <p>
          <strong>Quem resolve:</strong> {flow.resolvingResponsible}.{" "}
          {flow.activeExecution
            ? "Há uma execução ativa neste contexto."
            : "Nenhuma execução está em andamento nesta passagem."}
        </p>
        {flow.awaitingResponsible && (
          <p>Quem aguarda essa entrega: {flow.awaitingResponsible}.</p>
        )}
        <p>
          <strong>Você precisa decidir algo?</strong>{" "}
          {flow.decisionNeeded === true
            ? "Sim. "
            : flow.decisionNeeded === false
              ? "Não nesta passagem. "
              : "Confira a pendência da atividade. "}
          {flow.decisionReason}
        </p>
        <p>
          <strong>Para avançar:</strong> {flow.acceptance}
        </p>
        {flow.stalledSeconds != null && (
          <p>
            Sem execução desde{" "}
            {new Date(flow.stalledSince!).toLocaleString("pt-BR")}:{" "}
            {Math.floor(flow.stalledSeconds / 3600)} horas. Fonte: última
            entrega registrada deste ciclo.
          </p>
        )}
        <CyclePrivateKitLaunch cycle={cycle} />
        {work && (
          <Link to={work.url} className="btn btn-outline-primary mt-2">
            {flow.activeExecution
              ? "Acompanhar atividade"
              : "Ver atividade e pendência"}
          </Link>
        )}
        <details className="mt-3">
          <summary>Chegada ao mercado e entregas utilizáveis</summary>
          <p>
            Versão privada:{" "}
            {flow.prototypeRegistered
              ? "prova registrada para revisão independente"
              : "prova ainda não registrada"}
            . Pacotes íntegros preparados: {flow.readyPackages}. Teste
            comercial:{" "}
            {cycle.budgetLimitBrl === 0
              ? "sem mídia autorizada neste ciclo"
              : "confira a autorização e a publicação efetiva"}
            .
          </p>
          <p>
            Compras líquidas:{" "}
            {flow.marketMeasurement?.netSales ??
              "desconhecidas · sem conciliação deste ciclo"}
            . Receita:{" "}
            {flow.marketMeasurement?.revenueBrl != null
              ? `R$ ${flow.marketMeasurement.revenueBrl}`
              : "desconhecida"}
            . Contribuição:{" "}
            {flow.marketMeasurement?.contributionBrl != null
              ? `R$ ${flow.marketMeasurement.contributionBrl}`
              : "desconhecida"}
            .
          </p>
          {flow.measurementSource && (
            <p>
              Fonte: {flow.measurementSource}.{" "}
              {flow.marketMeasurement?.testDataExcluded
                ? "Dados internos de teste excluídos."
                : "Conferir segregação antes de interpretar métricas."}
            </p>
          )}
          <ul>
            {flow.deliverables.map((d) => (
              <li key={d.taskId}>
                Tarefa #{d.taskId} · {d.activity} · {d.status}: {d.usableOutput}
              </li>
            ))}
          </ul>
          <p className="small">
            Estado consultado em{" "}
            {new Date(flow.observedAt).toLocaleString("pt-BR")}. Conclusão de
            tarefa, clique ou protótipo não representa venda ou lucro.
          </p>
        </details>
        <CycleValueFlowDecisions cycle={cycle} onUpdated={onUpdated} />
      </section>
    );
  if (!work || cycle.status !== "OPEN") return null;
  return (
    <section className="alert alert-info" aria-label="Trabalho atual do ciclo">
      <h3 className="h5">
        {work.state === "IN_PROGRESS"
          ? "Atividade em andamento"
          : "Preparação pendente"}
      </h3>
      <p className="fw-bold">
        {work.processNumber}.{work.activityNumber} — {work.activityName}
      </p>
      <p>{work.reason}</p>
      <p>Responsável pela atividade: {work.responsible}</p>
      <p>
        A etapa será concluída quando esta atividade comprovar sua entrega.
        Acompanhe o processo para ver o bloqueio e as decisões necessárias.
      </p>
      <Link to={work.url} className="btn btn-primary">
        {work.state === "IN_PROGRESS"
          ? "Acompanhar atividade"
          : "Ver atividade e pendência"}
      </Link>
    </section>
  );
}

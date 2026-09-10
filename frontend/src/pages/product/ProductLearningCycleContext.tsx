import { Link } from "react-router-dom";
import type { CycleProcessContext } from "../../api/learningCycle/useCycleProcessContext";

/** Mantém ciclo, aprendizado e próximo trabalho visíveis dentro da atividade do produto. */
export default function ProductLearningCycleContext({
  context,
}: {
  context: CycleProcessContext;
}) {
  const work = context.nextWork;
  return (
    <section
      className="card border-primary mb-4"
      aria-label="Ciclo de vendas desta atividade"
    >
      <div className="card-body" style={{ overflowWrap: "anywhere" }}>
        <h2 className="h5">
          {context.cycleNumber}º ciclo de vendas · Experimento #
          {context.experimentId}
        </h2>
        <p className="text-body-secondary">
          Ciclo #{context.cycleId} · {context.stageLabel} · Versão alvo:{" "}
          {context.productVersion}
        </p>
        <p>
          <strong>Hipótese desta passagem:</strong> {context.hypothesis}
        </p>
        <p>
          <strong>Melhoria a testar:</strong> {context.mainChange}
        </p>
        {context.previousLearning.length ? (
          <details className="mb-3" open>
            <summary>
              <strong>Aprendizado dos ciclos anteriores</strong>
            </summary>
            {context.previousLearning.map((learning, index) => (
              <div
                className="border-start ps-3 mt-3"
                key={`${learning.cycleId}-${learning.action}-${index}`}
              >
                <p className="mb-1">
                  <strong>
                    Ciclo #{learning.cycleId} · Experimento #
                    {learning.experimentId}
                  </strong>
                </p>
                <p className="mb-1">{learning.learning || learning.summary}</p>
                {learning.limitation ? (
                  <p className="mb-1">
                    <strong>Limites da conclusão:</strong> {learning.limitation}
                  </p>
                ) : null}
                <details>
                  <summary>Fonte registrada</summary>
                  <p className="small">{learning.evidenceReference}</p>
                </details>
              </div>
            ))}
          </details>
        ) : (
          <p>Primeira passagem registrada; ainda não há aprendizado herdado.</p>
        )}
        {work ? (
          <div
            className={`alert ${work.state === "BLOCKED" ? "alert-warning" : "alert-primary"}`}
          >
            <strong>
              Próxima atividade: {work.processNumber}.{work.activityNumber} —{" "}
              {work.activityName}
            </strong>
            <p className="mb-1">
              Processo {work.processNumber} — {work.processName} ·{" "}
              {work.responsible}
            </p>
            <p>{work.reason}</p>
            <Link className="btn btn-primary" to={work.url}>
              Abrir próxima atividade
            </Link>
          </div>
        ) : (
          <p>Consulte a etapa e os critérios de continuidade no ciclo.</p>
        )}
        <Link className="btn btn-outline-primary" to={context.cycleUrl}>
          Ver ciclo #{context.cycleId} e decisões
        </Link>
        <p className="small text-body-secondary mt-3 mb-0">
          As tarefas e os custos abaixo pertencem a esta passagem. As execuções
          anteriores permanecem no histórico do produto.
        </p>
      </div>
    </section>
  );
}

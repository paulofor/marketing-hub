import { Link } from "react-router-dom";
import type { ExecutionFinance } from "../../api/financial/useExecutionFinance";
import FinancialPlanPlutusDetails from "./FinancialPlanPlutusDetails";

const money = (value: number | null | undefined, currency = "BRL") =>
  value == null
    ? "Não informado"
    : new Intl.NumberFormat("pt-BR", { style: "currency", currency }).format(
        value,
      );

export default function ExecutionFinancialAdvice({
  data,
}: {
  data: ExecutionFinance;
}) {
  const { advice, financialPlan: plan } = data;
  return (
    <section
      className="card p-3 mb-3 finance-advice"
      aria-label="Conselho de Plutus"
    >
      <h2 className="h4">Plutus: números para decidir</h2>
      {advice ? (
        <>
          <p className="finance-advice-title">
            <strong>{advice.title}</strong>
          </p>
          <p>{advice.recommendation}</p>
          {!!advice.figures.length && (
            <dl className="finance-metrics">
              {advice.figures.map((figure) => (
                <div key={figure.label}>
                  <dt>{figure.label}</dt>
                  <dd className="fs-4 fw-semibold">
                    {figure.unit === "BRL"
                      ? money(figure.value)
                      : (figure.value?.toLocaleString("pt-BR") ??
                        "Não informado")}
                  </dd>
                </div>
              ))}
            </dl>
          )}
          <p className="small mb-2">{advice.caution}</p>
          {advice.sourceId != null && (
            <p className="small">
              <Link
                to={advice.sourceUrl}
                target="_blank"
                rel="noopener noreferrer"
              >
                Ver parecer de Plutus #{advice.sourceId}
              </Link>
              {advice.producedAt && (
                <> · {new Date(advice.producedAt).toLocaleString("pt-BR")}</>
              )}
            </p>
          )}
        </>
      ) : (
        <p>
          O resumo do parecer ainda não está disponível. Consulte os detalhes
          financeiros abaixo.
        </p>
      )}
      <div className="finance-advice-budget">
        <strong>
          Preparação acumulada: {money(data.knownPreparationUsd, "USD")}{" "}
          estimados.
        </strong>{" "}
        {data.availableUsd == null ? (
          "Saldo ainda não calculado para esta versão."
        ) : (
          <>Saldo após reserva: {money(data.availableUsd, "USD")}.</>
        )}{" "}
        {data.unreportedTasks > 0 && (
          <>{data.unreportedTasks} tarefas ainda sem custo informado.</>
        )}
      </div>
      {(advice?.fullRationale || plan) && (
        <details className="mt-3">
          <summary>Ver parecer completo, cenários e fontes</summary>
          {advice?.fullRationale && (
            <p className="mt-3">{advice.fullRationale}</p>
          )}
          {plan && (
            <>
              <p>
                Plano financeiro #{plan.id}, revisão {plan.revision}.
                {plan.stale &&
                  " Referências desatualizadas: reavalie antes de decidir."}
              </p>
              <div
                className="finance-scenarios"
                aria-label="Cenários de contribuição projetada"
              >
                {plan.evaluation.scenarios.map((scenario) => (
                  <div key={scenario.code}>
                    <strong>{scenario.label}</strong>
                    <p>
                      {scenario.marginPercent == null
                        ? "Margem não calculada"
                        : `${scenario.marginPercent.toLocaleString("pt-BR")}% de contribuição`}
                    </p>
                    <p>
                      Aquisição máxima por cliente:{" "}
                      {money(scenario.maximumAffordableCacBrl)}
                    </p>
                  </div>
                ))}
              </div>
              {plan.analysis?.result && (
                <FinancialPlanPlutusDetails
                  result={plan.analysis.result}
                  money={money}
                  percent={(n) =>
                    n == null
                      ? "Não informado"
                      : `${n.toLocaleString("pt-BR")}%`
                  }
                />
              )}
              {!!plan.pendingActions.length && (
                <ul>
                  {plan.pendingActions.map((item, i) => (
                    <li key={i}>{item}</li>
                  ))}
                </ul>
              )}
            </>
          )}
        </details>
      )}
      <Link
        className="btn btn-outline-primary align-self-start mt-3"
        to={data.planUrl}
      >
        Analisar cenários e conselho de Plutus
      </Link>
    </section>
  );
}

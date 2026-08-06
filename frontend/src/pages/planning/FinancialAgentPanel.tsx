import {
  FinancialAgentExecution,
  useFinancialAgentExecutions,
  useStartFinancialAgent,
} from "../../api/planning/useFinancialAgent";
import CodexExecutionTelemetry from "../../components/CodexExecutionTelemetry";

function formatDate(value?: string | null) {
  if (!value) return "Horário não informado";
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "medium",
    timeZone: "America/Sao_Paulo",
  }).format(new Date(value));
}

function snapshot(execution: FinancialAgentExecution) {
  try {
    return JSON.parse(execution.financialSnapshot) as Record<string, unknown>;
  } catch {
    return null;
  }
}

interface StudioProviderEfficiency {
  provider: string;
  totalAttempts: number;
  knownCostAttempts: number;
  unknownCostAttempts: number;
  knownCostUsd: number;
  reviewedAssets: number;
  approvedAssets: number;
  pendingReviewAssets: number;
  commercialApprovalRatePercent?: number | null;
  knownCostPerApprovedAssetUsd?: number | null;
  decisionCoverage:
    | "INCOMPLETE_COSTS"
    | "NO_COMMERCIAL_REVIEWS"
    | "READY_FOR_COMPARISON";
}

function formatMetric(value?: number | null, suffix = "") {
  return value == null ? "Dados insuficientes" : `${value}${suffix}`;
}

export function StudioProviderEfficiencyTable({
  rows,
}: {
  rows: StudioProviderEfficiency[];
}) {
  if (rows.length === 0) return null;
  return (
    <div className="table-responsive mt-3">
      <h3 className="h6">Custo-benefício por provedor</h3>
      <table className="table table-sm align-middle mb-1">
        <thead>
          <tr>
            <th>Provedor</th>
            <th>Tentativas</th>
            <th>Aprovados</th>
            <th>Taxa de aprovação</th>
            <th>Custo conhecido/aprovado</th>
            <th>Cobertura</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.provider}>
              <td>{row.provider}</td>
              <td>{row.totalAttempts}</td>
              <td>{row.approvedAssets}</td>
              <td>
                {formatMetric(row.commercialApprovalRatePercent, "%")}
              </td>
              <td>
                {row.knownCostPerApprovedAssetUsd == null
                  ? "Dados insuficientes"
                  : `US$ ${row.knownCostPerApprovedAssetUsd}`}
              </td>
              <td>
                {row.decisionCoverage === "READY_FOR_COMPARISON"
                  ? "Pronto para comparar"
                  : row.decisionCoverage === "INCOMPLETE_COSTS"
                    ? `${row.unknownCostAttempts} custo(s) desconhecido(s)`
                    : "Sem revisões comerciais"}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <p className="text-muted small mb-0">
        A recomendação de recarga exige custos completos e avaliações comerciais;
        não autoriza compra de créditos.
      </p>
    </div>
  );
}

export default function FinancialAgentPanel({ planId }: { planId: number }) {
  const executionsQuery = useFinancialAgentExecutions(planId);
  const start = useStartFinancialAgent(planId);

  return (
    <section
      className="card border-0 shadow-sm mt-4"
      data-testid="financial-agent-panel"
    >
      <div className="card-body d-grid gap-3">
        <div>
          <div className="d-flex align-items-center gap-2 flex-wrap">
            <h2 className="h5 mb-0">Agente Financeiro</h2>
            <span className="badge text-bg-success">Somente leitura</span>
          </div>
          <p className="text-muted mb-0 mt-1">
            Concilia campanha, provedores de IA, vendas e cobertura das fontes.
            Não movimenta dinheiro nem altera preço, orçamento ou campanha.
          </p>
        </div>

        <div>
          <button
            className="btn btn-outline-primary"
            type="button"
            disabled={start.isPending}
            onClick={() => start.mutate()}
          >
            {start.isPending
              ? "Solicitando..."
              : "Executar conciliação financeira"}
          </button>
          {start.isError ? (
            <span className="text-danger ms-2">
              Não foi possível solicitar a conciliação.
            </span>
          ) : null}
        </div>

        {(executionsQuery.data ?? []).map((execution) => {
          const values = snapshot(execution);
          return (
            <article className="border rounded p-3" key={execution.id}>
              <div className="d-flex justify-content-between gap-2 flex-wrap">
                <strong>Conciliação #{execution.id}</strong>
                <span className="badge text-bg-secondary">
                  {execution.status}
                </span>
              </div>
              <CodexExecutionTelemetry
                agentType="FINANCIAL_AGENT"
                executionId={execution.id}
              />
              {values ? (
                <>
                  <div className="row g-2 mt-1 small">
                    <div className="col-md-3">
                      Custo total: R$ {String(values.totalCostBrl ?? 0)}
                    </div>
                    <div className="col-md-3">
                      Mídia: R$ {String(values.campaignCostBrl ?? 0)}
                    </div>
                    <div className="col-md-3">
                      IA/provedores: R${" "}
                      {String(values.aiProviderCostBrl ?? 0)}
                    </div>
                    <div className="col-md-3">
                      Receita: R$ {String(values.approvedRevenueBrl ?? 0)}
                    </div>
                  </div>
                  <StudioProviderEfficiencyTable
                    rows={
                      Array.isArray(values.studioProviderEfficiency)
                        ? (values.studioProviderEfficiency as StudioProviderEfficiency[])
                        : []
                    }
                  />
                </>
              ) : null}
              {execution.dailyReport ? (
                <div className="alert alert-light border mt-2 mb-0">
                  <div className="d-flex justify-content-between gap-2 flex-wrap">
                    <strong>Relatório diário financeiro</strong>
                    <span className="text-muted small">
                      {formatDate(execution.finishedAt ?? execution.createdAt)}
                    </span>
                  </div>
                  <p className="mb-0 mt-1" style={{ whiteSpace: "pre-wrap" }}>
                    {execution.dailyReport}
                  </p>
                </div>
              ) : null}
              {execution.errorMessage ? (
                <p className="text-danger mb-0 mt-2">
                  {execution.errorMessage}
                </p>
              ) : null}
            </article>
          );
        })}
      </div>
    </section>
  );
}

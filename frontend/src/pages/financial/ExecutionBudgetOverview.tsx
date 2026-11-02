import type { ExecutionFinance } from "../../api/financial/useExecutionFinance";
const money = (value: number | null, currency: string) =>
  value == null
    ? "Não registrado"
    : new Intl.NumberFormat("pt-BR", { style: "currency", currency }).format(
        value,
      );

/** Apresenta escopos oficiais e abre os contratos existentes sem sair da demonstração. */
export default function ExecutionBudgetOverview({
  data,
  disabled,
}: {
  data: ExecutionFinance;
  disabled: boolean;
}) {
  return (
    <section
      id="operating-approvals"
      className="finance-budget-overview mb-4"
      aria-label="Entenda os três orçamentos"
    >
      <h2 className="h4">Aprovações de agentes, vídeos e anúncios</h2>
      <p>
        <strong>
          Produzir o vídeo é uma despesa. Pagar para exibi-lo como anúncio é
          outra.
        </strong>{" "}
        A preparação do produto também tem seu próprio limite. Aprovar um deles
        não aumenta os outros.
      </p>
      <div className="finance-budget-grid">
        {data.budgets?.map((budget, index) => (
          <article
            className="card p-3"
            key={budget.code}
            aria-label={budget.title}
          >
            <span className="finance-budget-number" aria-hidden="true">
              {index + 1}
            </span>
            <h3 className="h5 mt-2">{budget.title}</h3>
            <p>{budget.purpose}</p>
            <div className="finance-budget-limit">
              <span>
                {budget.limitLabel} · {budget.currency === "USD" ? "US$" : "R$"}
              </span>
              <strong className="d-block fs-4">
                {money(budget.limit, budget.currency)}
              </strong>
              <span className="small fw-semibold">{budget.status}</span>
            </div>
            <p className="small mt-3">{budget.scope}</p>
            <p className="small">{budget.explanation}</p>
            <details className="small mb-3">
              <summary>O que sabemos sobre o consumo</summary>
              <p className="mt-2">{budget.consumptionNote}</p>
            </details>
            <a
              className={`btn btn-outline-primary mt-auto ${disabled ? "disabled" : ""}`}
              href={`#ceiling-${budget.code === "PREPARATION" ? "AGENTS" : budget.code === "VIDEO" ? "PRODUCTION" : "MEDIA"}`}
            >
              Ver teto na planilha
            </a>
          </article>
        ))}
      </div>
      <p className="small mt-3 mb-0">
        US$ são dólares; R$ são reais. Não somamos essas moedas sem câmbio.
        Limites são autorizações máximas, não cobranças. Uma revisão já
        contabilizada não deve ser somada novamente como outro gasto.
      </p>
    </section>
  );
}

import type { FinancialProjection } from "../../api/financial/useFinancialPlans";

export default function FinancialPlanPlutusDetails({
  result,
  money,
  percent,
}: {
  result: FinancialProjection;
  money: (value: number | null | undefined) => string;
  percent: (value: number | null) => string;
}) {
  const decisions = {
    continue: "Continuar",
    adjust: "Ajustar",
    stop: "Parar novos compromissos",
  } as const;
  const names = {
    CONSERVATIVE: "Conservador",
    BASE: "Base",
    OPTIMISTIC: "Otimista",
  };
  return (
    <section aria-label="Recomendações de Plutus" className="mt-3">
      {("recommendedInitialInvestmentBrl" in result ||
        "recommendedCycleLimitBrl" in result) && (
        <dl className="row">
          <div className="col-md-6">
            <dt>Investimento inicial sugerido</dt>
            <dd>{money(result.recommendedInitialInvestmentBrl)}</dd>
          </div>
          <div className="col-md-6">
            <dt>Teto sugerido por ciclo</dt>
            <dd>{money(result.recommendedCycleLimitBrl)}</dd>
          </div>
        </dl>
      )}
      {result.breakEven && (
        <p>
          <strong>Ponto de equilíbrio:</strong> {result.breakEven}
        </p>
      )}
      {result.decisionCriteria && (
        <div className="row g-3">
          {(Object.keys(decisions) as Array<keyof typeof decisions>).map(
            (key) => (
              <section className="col-lg-4" key={key}>
                <h3 className="h6">{decisions[key]}</h3>
                <ul>
                  {result.decisionCriteria?.[key]?.map((item, index) => (
                    <li key={index}>{item}</li>
                  ))}
                </ul>
              </section>
            ),
          )}
        </div>
      )}
      {!!result.scenarios?.length && (
        <details>
          <summary>Premissas e projeções de Plutus por cenário</summary>
          <p className="small text-muted mt-2">
            Parecer para revisão humana. Os cálculos do plano e as premissas
            salvas permanecem disponíveis na comparação acima.
          </p>
          {result.scenarios.map((s) => (
            <section key={s.name} className="border rounded p-3 mt-2">
              <h3 className="h6">{names[s.name]}</h3>
              <ul>
                {s.assumptions.map((item, i) => (
                  <li key={i}>{item}</li>
                ))}
              </ul>
              <dl className="row mb-0">
                <div className="col-md-4">
                  <dt>Preço médio</dt>
                  <dd>{money(s.averagePriceBrl)}</dd>
                </div>
                <div className="col-md-4">
                  <dt>Receita projetada</dt>
                  <dd>{money(s.revenueBrl)}</dd>
                </div>
                <div className="col-md-4">
                  <dt>Resultado após investimento inicial</dt>
                  <dd>{money(s.profitBrl)}</dd>
                </div>
                <div className="col-md-4">
                  <dt>Margem informada por Plutus</dt>
                  <dd>{percent(s.contributionMarginPercent)}</dd>
                </div>
                <div className="col-md-4">
                  <dt>CAC projetado</dt>
                  <dd>{money(s.cacBrl)}</dd>
                </div>
                <div className="col-md-4">
                  <dt>Tráfego projetado</dt>
                  <dd>{s.traffic ?? "Não informado"}</dd>
                </div>
                <div className="col-md-4">
                  <dt>Conversão projetada</dt>
                  <dd>{percent(s.conversionRatePercent)}</dd>
                </div>
                <div className="col-md-4">
                  <dt>ROAS projetado</dt>
                  <dd>
                    {s.roas == null
                      ? "Indisponível"
                      : s.roas.toLocaleString("pt-BR")}
                  </dd>
                </div>
              </dl>
            </section>
          ))}
        </details>
      )}
      {!!result.limitations?.length && (
        <div className="mt-3">
          <h3 className="h6">Limitações do parecer</h3>
          <ul>
            {result.limitations.map((v, i) => (
              <li key={i}>{v}</li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}

import type { MonthlySubscription } from "../../api/financial/useFinancialPlans";

export function subscriptionInputs(data: FormData): MonthlySubscription {
  const read = (key: string) => {
    const value = data.get(key);
    return value == null || value === "" ? null : Number(value);
  };
  return {
    renewals: read("renewals"),
    freeUsers: read("freeUsers"),
    costPerFreeUserBrl: read("costPerFreeUserBrl"),
  };
}

export default function FinancialSubscriptionFields({
  terms,
}: {
  terms?: MonthlySubscription | null;
}) {
  return (
    <div className="row g-3">
      {(
        [
          ["renewals", "Renovações previstas no mês"],
          ["freeUsers", "Pessoas ativas na base gratuita"],
          [
            "costPerFreeUserBrl",
            "Custo mensal por pessoa na base gratuita (R$)",
          ],
        ] as const
      ).map(([key, label]) => (
        <label key={key} className="col-md-4">
          {label}
          <input
            className="form-control"
            name={key}
            type="number"
            min="0"
            step={key === "costPerFreeUserBrl" ? "0.000001" : "1"}
            defaultValue={terms?.[key] ?? ""}
          />
        </label>
      ))}
    </div>
  );
}

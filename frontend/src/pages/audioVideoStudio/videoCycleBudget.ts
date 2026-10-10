/** Preserva a moeda autorizada e prepara o contrato financeiro existente do Estúdio. */
export function videoCycleBudget(input: {
  amount: string;
  currency: "BRL" | "USD";
  exchangeRate: string;
  exchangeRateSource: string;
  exchangeRateDate: string;
}) {
  const amount = Number(input.amount);
  if (!Number.isFinite(amount) || amount < 0.01) return null;
  if (input.currency === "USD") {
    if (!/^\d+(?:\.\d{1,2})?$/.test(input.amount.trim())) return null;
    return {
      budgetLimitUsd: amount,
      authorizedBudgetAmount: amount,
      authorizedBudgetCurrency: "USD" as const,
    };
  }
  const rate = Number(input.exchangeRate);
  if (
    !Number.isFinite(rate) ||
    rate <= 0 ||
    !input.exchangeRateSource.trim() ||
    !input.exchangeRateDate
  )
    return null;
  const budgetLimitUsd = Math.floor((amount / rate) * 100) / 100;
  if (budgetLimitUsd < 0.01) return null;
  return {
    budgetLimitUsd,
    authorizedBudgetAmount: amount,
    authorizedBudgetCurrency: "BRL" as const,
    usdBrlExchangeRate: rate,
    exchangeRateSource: input.exchangeRateSource.trim(),
    exchangeRateDate: input.exchangeRateDate,
  };
}

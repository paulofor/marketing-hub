import { describe, expect, it } from "vitest";
import { videoCycleBudget } from "./videoCycleBudget";

const input = {
  amount: "10.00",
  currency: "USD" as const,
  exchangeRate: "",
  exchangeRateSource: "",
  exchangeRateDate: "",
};

describe("autorização financeira na moeda original", () => {
  it("preserva dólares sem criar cotação e ignora campos antigos em reais", () => {
    expect(videoCycleBudget(input)).toEqual({
      budgetLimitUsd: 10,
      authorizedBudgetAmount: 10,
      authorizedBudgetCurrency: "USD",
    });
    expect(
      videoCycleBudget({
        ...input,
        amount: "9.47",
        exchangeRate: "6",
        exchangeRateSource: "Fonte anterior em BRL",
        exchangeRateDate: "2026-10-09",
      }),
    ).toEqual({
      budgetLimitUsd: 9.47,
      authorizedBudgetAmount: 9.47,
      authorizedBudgetCurrency: "USD",
    });
  });

  it.each(["", "0", "-1", "Infinity", "NaN", "0.001", "10.001"])(
    "impede consumo com valor inválido ou precisão monetária ambígua: %s",
    (amount) => expect(videoCycleBudget({ ...input, amount })).toBeNull(),
  );

  it("preserva a conversão conservadora e a fonte de autorizações em reais", () => {
    const result = videoCycleBudget({
      ...input,
      currency: "BRL",
      amount: "10",
      exchangeRate: "6",
      exchangeRateSource: " Banco Central ",
      exchangeRateDate: "2026-10-09",
    });
    expect(result).toEqual({
      budgetLimitUsd: 1.66,
      authorizedBudgetAmount: 10,
      authorizedBudgetCurrency: "BRL",
      usdBrlExchangeRate: 6,
      exchangeRateSource: "Banco Central",
      exchangeRateDate: "2026-10-09",
    });
    expect(result!.budgetLimitUsd * 6).toBeLessThanOrEqual(10);
  });

  it.each([
    { exchangeRate: "" },
    { exchangeRate: "0" },
    { exchangeRate: "-1" },
    { exchangeRate: "Infinity" },
    { exchangeRateSource: " " },
    { exchangeRateDate: "" },
  ])("bloqueia cotação incompleta em reais: %j", (override) => {
    expect(
      videoCycleBudget({
        ...input,
        currency: "BRL",
        exchangeRate: "5",
        exchangeRateSource: "Banco Central",
        exchangeRateDate: "2026-10-09",
        ...override,
      }),
    ).toBeNull();
  });
});

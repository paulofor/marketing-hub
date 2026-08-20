import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import AgentTaskModelUsage from "./AgentTaskModelUsage";

describe("AgentTaskModelUsage", () => {
  it("mostra entrada, saída, cache e custo persistidos", () => {
    render(
      <AgentTaskModelUsage
        usage={{
          inputTokens: 1234,
          cachedInputTokens: 900,
          outputTokens: 321,
          estimatedCostUsd: 0.01234567,
          costEstimationStatus: "ESTIMATED",
        }}
      />,
    );

    expect(
      screen.getByText("Tokens: entrada 1.234 · saída 321 · cache 900"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Custo estimado: US$ 0,01234567"),
    ).toBeInTheDocument();
  });

  it("distingue tarefa legada sem consumo informado", () => {
    render(
      <AgentTaskModelUsage usage={{ costEstimationStatus: "NOT_REPORTED" }} />,
    );

    expect(screen.getByText("Consumo de IA não informado")).toBeInTheDocument();
  });
});

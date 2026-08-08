import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import ExperimentStrategistPanel from "./ExperimentStrategistPanel";

vi.mock("../../api/planning/useExperimentStrategist", () => ({
  useExperimentStrategistExecutions: () => ({ data: [] }),
  useStartExperimentStrategist: () => ({
    isPending: false,
    isError: false,
    mutate: vi.fn(),
  }),
}));

vi.mock("../../components/CodexExecutionTelemetry", () => ({
  default: () => null,
}));

describe("ExperimentStrategistPanel", () => {
  it("explica a comparação de portfólio sem assumir a execução do Operador", () => {
    render(<ExperimentStrategistPanel planId={2} />);

    expect(
      screen.getByText(/Compara formatos e resultados do portfólio/),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/responsabilidade do Operador de Crescimento/),
    ).toBeInTheDocument();
    expect(
      screen.getByDisplayValue(/O que o portfólio aprendeu/),
    ).toBeInTheDocument();
  });
});

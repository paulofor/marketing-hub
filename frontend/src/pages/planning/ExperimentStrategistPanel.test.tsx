import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import ExperimentStrategistPanel from "./ExperimentStrategistPanel";

const mutate = vi.fn();
const requestedPlans: number[] = [];
afterEach(() => {
  cleanup();
  mutate.mockClear();
  requestedPlans.length = 0;
});

vi.mock("../../api/planning/useExperimentStrategist", () => ({
  useExperimentStrategistExecutions: () => ({ data: [] }),
  useStartExperimentStrategist: (planId: number) => {
    requestedPlans.push(planId);
    return {
      isPending: false,
      isError: false,
      mutate,
    };
  },
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
  it("usa o bloqueio carregado e preserva a edição no mesmo plano", () => {
    const view = render(<ExperimentStrategistPanel planId={701} />);
    view.rerender(
      <ExperimentStrategistPanel
        planId={701}
        defaultQuestion="Aguardando economia"
      />,
    );
    const question = screen.getByLabelText(/Pergunta comercial/);
    expect(question).toHaveValue("Aguardando economia");
    fireEvent.change(question, {
      target: { value: "Investigar custo da entrega" },
    });
    view.rerender(
      <ExperimentStrategistPanel
        planId={701}
        defaultQuestion="Outra atualização"
      />,
    );
    expect(question).toHaveValue("Investigar custo da entrega");
    fireEvent.click(
      screen.getByRole("button", { name: "Solicitar parecer estratégico" }),
    );
    expect(mutate).toHaveBeenCalledWith("Investigar custo da entrega");
  });

  it("não transporta diagnóstico ou edição para outro plano", () => {
    const view = render(
      <ExperimentStrategistPanel
        planId={701}
        defaultQuestion="Contexto antigo"
      />,
    );
    fireEvent.change(screen.getByLabelText(/Pergunta comercial/), {
      target: { value: "Edição antiga" },
    });
    view.rerender(
      <ExperimentStrategistPanel
        planId={702}
        defaultQuestion="Sem experimento"
      />,
    );
    expect(screen.getByLabelText(/Pergunta comercial/)).toHaveValue(
      "Sem experimento",
    );
    expect(requestedPlans[requestedPlans.length - 1]).toBe(702);
    fireEvent.click(
      screen.getByRole("button", { name: "Solicitar parecer estratégico" }),
    );
    expect(mutate).toHaveBeenCalledWith("Sem experimento");
  });

  it("campo apagado não volta ao padrão nem permite pesquisa vazia", () => {
    const view = render(
      <ExperimentStrategistPanel planId={701} defaultQuestion="Economia" />,
    );
    fireEvent.change(screen.getByLabelText(/Pergunta comercial/), {
      target: { value: "" },
    });
    view.rerender(
      <ExperimentStrategistPanel planId={701} defaultQuestion="Atualizado" />,
    );
    expect(screen.getByLabelText(/Pergunta comercial/)).toHaveValue("");
    expect(
      screen.getByRole("button", { name: "Solicitar parecer estratégico" }),
    ).toBeDisabled();
  });
  it("não permite pesquisa sem identidade persistida do plano", () => {
    render(
      <ExperimentStrategistPanel
        planId={0}
        defaultQuestion="Contexto provisório"
      />,
    );
    const button = screen.getByRole("button", {
      name: "Solicitar parecer estratégico",
    });
    expect(button).toBeDisabled();
    fireEvent.click(button);
    expect(mutate).not.toHaveBeenCalled();
  });
});

import { cleanup, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import ProductHistoricalContinuation from "./ProductHistoricalContinuation";
import ProductProcessActivityExecutionPanel from "./ProductProcessActivityExecutionPanel";

vi.mock("../experiment/ExperimentRunPanel", () => ({ default: () => null }));
afterEach(cleanup);
const navigation = {
  productId: 902,
  chainDefinitionId: 903,
  cycleId: 904,
  experimentId: 905,
  status: "ADJUSTED",
  stage: "DECISION",
  reason: "O ajuste já foi aprovado; sucessor ainda não preparado.",
  url: "/business-process-chains/learning-cycles?productId=902&chainId=903&cycleId=904",
};

describe("continuidade de referência encerrada", () => {
  it("abre o ciclo fornecido pelo backend sem inferir uma nova aprovação", () => {
    render(
      <MemoryRouter>
        <ProductHistoricalContinuation navigation={navigation} />
      </MemoryRouter>,
    );
    expect(
      screen.getByText(/Não há ação sua nesta atividade/),
    ).toBeInTheDocument();
    expect(screen.getByRole("link")).toHaveAttribute("href", navigation.url);
    expect(screen.getByText(navigation.reason)).toBeInTheDocument();
  });
  it("não oferece destino antigo se a atualização falhou", () => {
    render(
      <MemoryRouter>
        <ProductHistoricalContinuation navigation={navigation} unavailable />
      </MemoryRouter>,
    );
    expect(screen.queryByRole("link")).not.toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent(
      "Não foi possível confirmar",
    );
  });
  it("declara a ausência de continuidade sem inventar um ciclo", () => {
    render(
      <MemoryRouter>
        <ProductHistoricalContinuation />
      </MemoryRouter>,
    );
    expect(screen.queryByRole("link")).not.toBeInTheDocument();
    expect(
      screen.getByText(/ainda não informou uma continuidade/),
    ).toBeInTheDocument();
  });
  it("remove promessa de execução e comando de atividade histórica mesmo com controle inconsistente", () => {
    const execute = vi.fn();
    const activity: any = {
      activityId: "financialGuardrails",
      objectiveAchieved: false,
      operationalState: "NOT_STARTED",
      tasks: [],
      requirements: [],
      executionControl: {
        executorType: "BACKEND",
        interactionType: "WORKSPACE",
        actionAvailable: true,
        actionLabel: "Comprovar atividade",
        requirements: [],
      },
    };
    render(
      <MemoryRouter>
        <ProductProcessActivityExecutionPanel
          activity={activity}
          productId={902}
          pending={false}
          onExecute={execute}
          processManaged
          historical
          continuation={navigation}
        />
      </MemoryRouter>,
    );
    expect(
      screen.queryByRole("button", { name: "Comprovar atividade" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText(/executada automaticamente pelo controle/),
    ).not.toBeInTheDocument();
    expect(screen.getByRole("link")).toHaveAttribute("href", navigation.url);
    expect(execute).not.toHaveBeenCalled();
  });
});

import { cleanup, render, screen, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it } from "vitest";
import ProductSalesFlow from "./ProductSalesFlow";
import type { SalesFlow } from "../api/learningCycle/salesFlow";

const flow: SalesFlow = {
  productId: 4,
  experimentId: 91,
  cycleId: 1,
  chainDefinitionId: 13,
  modelProcessDefinitionId: 74,
  currentActivityId: "learningCycle",
  currentActivityName: "Conduzir o ciclo de aprendizado e vendas",
  currentActivitySequenceNumber: 4,
  state: "IN_PROGRESS",
  reason: "Decisão comercial após conciliação automática.",
  navigationUrl:
    "/business-process-chains/learning-cycles?chainId=13&productId=4&cycleId=1",
  modelUrl: "/business-processes?processId=74&productId=4",
  activities: [
    {
      activityId: "optimization",
      sequenceNumber: 1,
      name: "Operar e otimizar",
      state: "HISTORICAL",
      objectiveAchieved: false,
      reason: "Hermes antigo permanece no histórico.",
    },
    {
      activityId: "learningCycle",
      sequenceNumber: 4,
      name: "Conduzir o ciclo",
      state: "IN_PROGRESS",
      objectiveAchieved: false,
      reason: "Decidir próximo movimento.",
    },
  ],
  transitions: [
    {
      eventId: 1,
      revision: 1,
      action: "FIX_MEASUREMENT",
      flowId: "fix-measurement",
      from: "Decisão",
      to: "Consolidação",
      returnFlow: true,
      reason: "Fonte indisponível",
      responsible: "Operador local",
      evidenceReference: "internal://fixture/1",
      occurredAt: "2026-09-09T06:00:00Z",
    },
  ],
};

describe("Fluxo comercial dentro do Processo 6", () => {
  afterEach(cleanup);
  it("apresenta 6.4 e conserva a referência histórica do ciclo, sem retomar operação antiga", () => {
    render(
      <MemoryRouter>
        <ProductSalesFlow flow={flow} />
      </MemoryRouter>,
    );
    const region = screen.getByRole("region", { name: "Fluxo do Processo 6" });
    expect(
      within(region).getByText(/Atividade atual: 6.4/),
    ).toBeInTheDocument();
    expect(
      within(region).getByText("Referência histórica"),
    ).toBeInTheDocument();
    expect(
      within(region).getByRole("link", { name: "Continuar fluxo registrado" }),
    ).toHaveAttribute(
      "href",
      "/products/4/value-chain-history/processes/74/activities?learningCycleId=1#activity-learningCycle",
    );
    expect(
      within(region).getByText("Retorno: Decisão → Consolidação"),
    ).toBeInTheDocument();
    expect(within(region).getByText("Fonte indisponível")).toBeInTheDocument();
  });
  it("não inventa uma próxima operação para uma passagem encerrada", () => {
    render(
      <MemoryRouter>
        <ProductSalesFlow
          flow={{
            ...flow,
            currentActivityId: null,
            currentActivityName: null,
            state: "COMPLETED",
          }}
        />
      </MemoryRouter>,
    );
    expect(screen.getByText("Passagem encerrada")).toBeInTheDocument();
    expect(
      screen.queryByRole("link", { name: "Continuar fluxo registrado" }),
    ).not.toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Consultar decisão" }),
    ).toHaveAttribute("href", flow.navigationUrl);
  });
});

import { cleanup, render, screen } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import { afterEach, describe, expect, it } from "vitest";
import ProductValueChainPosition from "./ProductValueChainPosition";

describe("ProductValueChainPosition", () => {
  afterEach(() => cleanup());

  it("shows the human process name, progress and canonical link", () => {
    render(
      <BrowserRouter>
        <ProductValueChainPosition
          productName="Kit WhatsApp Pronto"
          position={{
            productId: 9,
            commercialStatus: "COMUNICACAO_E_JORNADA",
            resolutionStatus: "IDENTIFIED",
            resolutionMessage: "Posição identificada.",
            chainDefinitionId: 5,
            chainName:
              "Criação e entrega de valor de Produtos Digitais Experienciais",
            chainVersion: 5,
            processDefinitionId: 43,
            processCode: "pde-communication-sales-journey",
            processName: "Comunicação e jornada de venda do PDE",
            processVersion: 4,
            sequenceNumber: 4,
            processCount: 6,
            processMeasurements: [
              {
                stageType: "PROCESS",
                trackingStatus: "CURRENT",
                processDefinitionId: 43,
                processCode: "pde-communication-sales-journey",
                processName: "Comunicação e jornada de venda do PDE",
                enteredAt: "2026-08-20T12:00:00Z",
                entryEvidence: "COMMERCIAL_STATUS_TRANSITION",
                objectiveAchieved: false,
                elapsedDays: 5,
                knownEstimatedCostUsd: 3.25,
                costCoverage: "PARTIAL",
                costedExecutionCount: 8,
                uncostedExecutionCount: 1,
              },
            ],
            subprocessPosition: {
              trackingStatus: "IN_PROGRESS",
              subprocessCount: 2,
              currentActivityName: "Criar e aprovar peças",
              currentSubprocessDefinitionId: 17,
              currentSubprocessCode: "creative-production-approval",
              currentSubprocessName: "Criação e aprovação de criativos",
              currentSubprocessObjective: "Criativos aprovados e prontos.",
              nextSubprocessDefinitionId: 18,
              nextSubprocessCode: "landing-page-generation",
              nextSubprocessName: "Geração de landing page",
              nextSubprocessObjective: "Landing aprovada para publicação.",
              measurements: [
                {
                  stageType: "SUBPROCESS",
                  trackingStatus: "CURRENT",
                  processDefinitionId: 17,
                  processCode: "creative-production-approval",
                  processName: "Criação e aprovação de criativos",
                  enteredAt: "2026-08-22T12:00:00Z",
                  entryEvidence: "FIRST_SUBPROCESS_TASK",
                  objectiveAchieved: false,
                  elapsedDays: 3,
                  knownEstimatedCostUsd: 1.125,
                  costCoverage: "COMPLETE",
                  costedExecutionCount: 4,
                  uncostedExecutionCount: 0,
                },
              ],
            },
          }}
        />
      </BrowserRouter>,
    );

    expect(screen.getByText("Etapa 4 de 6")).toBeTruthy();
    expect(
      screen.getByRole("link", { name: "Histórico da cadeia" }),
    ).toHaveAttribute("href", "/products/9/value-chain-history");
    expect(
      screen.getByRole("link", {
        name: /Comunicação e jornada de venda do PDE/i,
      }),
    ).toHaveAttribute("href", "/business-processes?processId=43");
    expect(screen.getByText(/Criação e entrega de valor/i)).toBeTruthy();
    expect(screen.getByText("Subprocesso atual")).toBeTruthy();
    expect(
      screen.getByRole("link", { name: "Criação e aprovação de criativos" }),
    ).toHaveAttribute("href", "/business-processes?processId=17");
    expect(screen.getByText("Próximo subprocesso")).toBeTruthy();
    expect(
      screen.getByRole("link", { name: /Geração de landing page/i }),
    ).toHaveAttribute("href", "/business-processes?processId=18");
    expect(screen.getByText("5 dias")).toBeTruthy();
    expect(screen.getByText("3 dias")).toBeTruthy();
    expect(screen.getByText(/US\$\s*3,25.*cobertura parcial/i)).toBeTruthy();
    expect(screen.getByText(/US\$\s*1,125/i)).toBeTruthy();
  });

  it("does not fabricate a process when the backend cannot identify it", () => {
    render(
      <BrowserRouter>
        <ProductValueChainPosition
          productName="Produto futuro"
          position={{
            productId: 12,
            commercialStatus: "STATUS_FUTURO",
            resolutionStatus: "NOT_IDENTIFIED",
            resolutionMessage: "Status comercial sem vínculo com um processo.",
            chainDefinitionId: 5,
            chainName: "Cadeia PDE",
            chainVersion: 5,
            processCount: 6,
            processMeasurements: [],
          }}
        />
      </BrowserRouter>,
    );

    expect(screen.getByText("Processo ainda não identificado")).toBeTruthy();
    expect(
      screen.getByRole("link", { name: "Histórico da cadeia" }),
    ).toHaveAttribute("href", "/products/12/value-chain-history");
    expect(screen.getAllByRole("link")).toHaveLength(1);
  });

  it("does not mark a subprocess as finished without evidence of the next entry", () => {
    render(
      <BrowserRouter>
        <ProductValueChainPosition
          productName="Produto"
          position={{
            productId: 9,
            commercialStatus: "COMUNICACAO_E_JORNADA",
            resolutionStatus: "IDENTIFIED",
            resolutionMessage: "Posição identificada.",
            processDefinitionId: 43,
            processCode: "pde-communication-sales-journey",
            processName: "Comunicação e jornada",
            processCount: 6,
            sequenceNumber: 4,
            processMeasurements: [],
            subprocessPosition: {
              trackingStatus: "RECORDED",
              subprocessCount: 2,
              nextSubprocessDefinitionId: 18,
              nextSubprocessCode: "landing-page-generation",
              nextSubprocessName: "Geração de landing page",
              measurements: [
                {
                  stageType: "SUBPROCESS",
                  trackingStatus: "RECORDED",
                  processDefinitionId: 17,
                  processCode: "creative-production-approval",
                  processName: "Criação e aprovação de criativos",
                  enteredAt: "2026-08-22T12:00:00Z",
                  entryEvidence: "FIRST_SUBPROCESS_TASK",
                  objectiveAchieved: false,
                  elapsedDays: 3,
                  knownEstimatedCostUsd: 1,
                  costCoverage: "COMPLETE",
                  costedExecutionCount: 1,
                  uncostedExecutionCount: 0,
                },
              ],
            },
          }}
        />
      </BrowserRouter>,
    );

    expect(screen.getByText("Último subprocesso registrado")).toBeTruthy();
    expect(
      screen.getByText("Objetivo ainda sem saída comprovada"),
    ).toBeTruthy();
    expect(screen.queryByText("Último subprocesso concluído")).toBeNull();
  });

  it("makes loading and integration failures explicit", () => {
    const { rerender } = render(
      <BrowserRouter>
        <ProductValueChainPosition productName="Produto" isLoading />
      </BrowserRouter>,
    );
    expect(screen.getByText("Carregando posição...")).toBeTruthy();

    rerender(
      <BrowserRouter>
        <ProductValueChainPosition productName="Produto" isError />
      </BrowserRouter>,
    );
    expect(
      screen.getByText("Posição temporariamente indisponível."),
    ).toBeTruthy();
  });
});

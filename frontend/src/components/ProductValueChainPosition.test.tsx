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
            },
          }}
        />
      </BrowserRouter>,
    );

    expect(screen.getByText("Etapa 4 de 6")).toBeTruthy();
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
          }}
        />
      </BrowserRouter>,
    );

    expect(screen.getByText("Processo ainda não identificado")).toBeTruthy();
    expect(screen.queryByRole("link")).toBeNull();
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

import { cleanup, render, screen, within } from "@testing-library/react";
import type { ReactElement } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
  type Mock,
} from "vitest";
import ExperimentFunnelTab from "./ExperimentFunnelTab";
import { useExperimentFunnel } from "../../api/experiment/useExperimentFunnel";
import { useRegisterExperimentFunnelEvent } from "../../api/experiment/useRegisterExperimentFunnelEvent";
import { useResetExperimentFunnel } from "../../api/experiment/useResetExperimentFunnel";
import { useExperimentFunnelDiagnostics } from "../../api/experiment/useExperimentFunnelDiagnostics";
import "@testing-library/jest-dom/vitest";

vi.mock("../../api/experiment/useExperimentFunnel");
vi.mock("../../api/experiment/useRegisterExperimentFunnelEvent");
vi.mock("../../api/experiment/useResetExperimentFunnel");
vi.mock("../../api/experiment/useExperimentFunnelDiagnostics");

const renderWithClient = (ui: ReactElement) => {
  const client = new QueryClient();
  return render(
    <QueryClientProvider client={client}>{ui}</QueryClientProvider>,
  );
};

describe("ExperimentFunnelTab", () => {
  afterEach(() => cleanup());
  beforeEach(() => {
    (useExperimentFunnel as unknown as Mock).mockReturnValue({
      data: [
        {
          stage: "VISUALIZACAO_ANUNCIO",
          label: "Visualização do anúncio",
          order: 1,
          autoCount: 90,
          manualCount: 10,
          totalCount: 100,
          uniqueCount: null,
          lastEventAt: "2024-03-10T12:00:00Z",
          source: "Meta",
        },
        {
          stage: "ENVIO_FORM",
          label: "Envio do formulário",
          order: 4,
          autoCount: 0,
          manualCount: 0,
          totalCount: 0,
          uniqueCount: null,
          lastEventAt: null,
          source: null,
        },
      ],
      isLoading: false,
      isError: false,
    });

    (useRegisterExperimentFunnelEvent as unknown as Mock).mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
      isSuccess: false,
      isError: false,
    });
    (useResetExperimentFunnel as unknown as Mock).mockReturnValue({
      mutate: vi.fn(),
      mutateAsync: vi.fn(),
      isPending: false,
    });

    (useExperimentFunnelDiagnostics as unknown as Mock).mockReturnValue({
      data: {
        diagnostics: [
          {
            stageKey: "ENVIO_FORM",
            stageLabel: "Envio do formulário",
            attempts: 39,
            successes: 0,
            observedRate: 0,
            minAcceptableRate: 0.1,
            upper95RateIfZero: 0.0769,
            thresholdChecks: [
              {
                minAcceptableRate: 0.1,
                attemptsFor95Confidence: 30,
                upper95RateIfZero: 0.0769,
                statisticallyFailed: true,
                attemptsTargetReached: true,
              },
              {
                minAcceptableRate: 0.05,
                attemptsFor95Confidence: 60,
                upper95RateIfZero: 0.0769,
                statisticallyFailed: false,
                attemptsTargetReached: false,
              },
            ],
            status: "STATISTICALLY_FAILED",
            reasonCode: "RULE_OF_THREE_FAILED",
            message: "Etapa reprovada estatisticamente no limite definido.",
            technicalIssueSuspected: false,
          },
        ],
        contextualAlert:
          "Alerta contextual: o evento principal de otimização ainda está com volume baixo para aprendizado da mídia.",
      },
      isLoading: false,
      isError: false,
    });
  });

  it("highlights the total spend", () => {
    renderWithClient(
      <ExperimentFunnelTab
        experimentId="42"
        totalSpend={123.45}
        spendLastSyncedAt="2024-03-10T12:00:00Z"
      />,
    );

    expect(screen.getByText("Total gasto na campanha")).toBeInTheDocument();
    expect(screen.getByText(/R\$\s*123,45/)).toBeInTheDocument();
    expect(screen.getByText(/Última sincronização/)).toBeInTheDocument();
  });

  it("renders the diagnostic block with backend messages", () => {
    renderWithClient(
      <ExperimentFunnelTab experimentId="42" totalSpend={100} />,
    );

    expect(
      screen.getByText("Diagnóstico estatístico do funil"),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Etapa reprovada estatisticamente/),
    ).toBeInTheDocument();
    expect(screen.getByText(/Alerta contextual/)).toBeInTheDocument();
    expect(
      screen.getByText(/Limite 10,0% · Tentativas mín\. .*: 30/),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Limite 5,0% · Tentativas mín\. .*: 60/),
    ).toBeInTheDocument();
  });

  it("keeps the reset button available while campaign spend is zero even when manual changes are locked", () => {
    renderWithClient(
      <ExperimentFunnelTab
        experimentId="42"
        totalSpend={0}
        alterationLocked={true}
      />,
    );

    expect(
      screen.getByRole("button", { name: "Zerar contagens" }),
    ).toBeEnabled();
  });

  it("hides the reset button after the campaign has spend", () => {
    renderWithClient(
      <ExperimentFunnelTab
        experimentId="42"
        totalSpend={100}
        alterationLocked={false}
      />,
    );

    expect(
      screen.queryByRole("button", { name: "Zerar contagens" }),
    ).not.toBeInTheDocument();
  });

  it("shows the cost per conversion for each stage", () => {
    renderWithClient(
      <ExperimentFunnelTab experimentId="42" totalSpend={100} />,
    );

    const rows = screen.getAllByRole("row");
    const firstStageRow = rows[1];
    const firstStageCells = within(firstStageRow).getAllByRole("cell");
    expect(firstStageCells[3]).toHaveTextContent(/R\$\s*1,00/);

    const secondStageRow = rows[2];
    const secondStageCells = within(secondStageRow).getAllByRole("cell");
    expect(secondStageCells[3]).toHaveTextContent("—");
  });
});

import { cleanup, render, screen, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from "vitest";
import ExperimentFunnelTab from "./ExperimentFunnelTab";
import { useExperimentFunnel } from "../../api/experiment/useExperimentFunnel";
import { useRegisterExperimentFunnelEvent } from "../../api/experiment/useRegisterExperimentFunnelEvent";
import "@testing-library/jest-dom/vitest";

vi.mock("../../api/experiment/useExperimentFunnel");
vi.mock("../../api/experiment/useRegisterExperimentFunnelEvent");

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
  });

  it("highlights the total spend", () => {
    render(
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

  it("shows the cost per conversion for each stage", () => {
    render(
      <ExperimentFunnelTab experimentId="42" totalSpend={100} />,
    );

    const rows = screen.getAllByRole("row");
    const firstStageRow = rows[1];
    const firstStageCells = within(firstStageRow).getAllByRole("cell");
    expect(firstStageCells[4]).toHaveTextContent(/R\$\s*1,00/);

    const secondStageRow = rows[2];
    const secondStageCells = within(secondStageRow).getAllByRole("cell");
    expect(secondStageCells[4]).toHaveTextContent("—");
  });
});

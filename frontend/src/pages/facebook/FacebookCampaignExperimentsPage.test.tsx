import "@testing-library/jest-dom/vitest";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  within,
} from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import FacebookCampaignExperimentsPage from "./FacebookCampaignExperimentsPage";
import type { ExperimentSummary } from "../../api/useFacebookCampaignExperiments";

const useFacebookCampaignExperimentsMock = vi.fn();
const useFacebookConfigurationStatusMock = vi.fn();

vi.mock("../../api/useFacebookCampaignExperiments", () => ({
  useFacebookCampaignExperiments: (status: string) =>
    useFacebookCampaignExperimentsMock(status),
}));

vi.mock("../../api/useFacebookConfigurationStatus", () => ({
  useFacebookConfigurationStatus: () => useFacebookConfigurationStatusMock(),
}));

function makeExperiment(id: number): ExperimentSummary {
  return {
    id,
    name: `Experimento ${id}`,
    hypothesis: `Hipótese ${id}`,
    kpiTargetCpl: null,
    startDate: null,
    endDate: null,
    nicheName: null,
    hypothesisTitle: null,
    missingConfiguration: [],
  };
}

function setup(experiments: ExperimentSummary[]) {
  useFacebookCampaignExperimentsMock.mockReturnValue({
    data: experiments,
    isLoading: false,
  });
  useFacebookConfigurationStatusMock.mockReturnValue({
    data: { hasConfiguredPages: true },
  });

  return render(
    <MemoryRouter>
      <FacebookCampaignExperimentsPage />
    </MemoryRouter>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
});

afterEach(() => {
  cleanup();
});

describe("FacebookCampaignExperimentsPage", () => {
  it("mostra os experimentos mais recentes primeiro e limita a página a 25 itens", () => {
    const experiments = Array.from({ length: 30 }, (_, index) =>
      makeExperiment(index + 1),
    );

    setup(experiments);

    const table = screen.getByRole("table");
    const rows = within(table).getAllByRole("row").slice(1);

    expect(rows).toHaveLength(25);
    expect(
      within(rows[0]).getByRole("link", { name: "Experimento 30" }),
    ).toBeInTheDocument();
    expect(
      within(rows[24]).getByRole("link", { name: "Experimento 6" }),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Mais recentes primeiro · exibindo 1-25 de 30"),
    ).toBeInTheDocument();
    expect(screen.getByText("25 por página")).toBeInTheDocument();
  });

  it("avança para a próxima página mantendo a ordenação decrescente", () => {
    const experiments = Array.from({ length: 30 }, (_, index) =>
      makeExperiment(index + 1),
    );

    setup(experiments);
    fireEvent.click(screen.getByRole("button", { name: /próxima/i }));

    const table = screen.getByRole("table");
    const rows = within(table).getAllByRole("row").slice(1);

    expect(rows).toHaveLength(5);
    expect(
      within(rows[0]).getByRole("link", { name: "Experimento 5" }),
    ).toBeInTheDocument();
    expect(
      within(rows[4]).getByRole("link", { name: "Experimento 1" }),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Mais recentes primeiro · exibindo 26-30 de 30"),
    ).toBeInTheDocument();
  });
});

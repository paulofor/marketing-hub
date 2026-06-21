import { cleanup, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router-dom";
import { afterEach, describe, it, expect, vi } from "vitest";
import ExperimentListPage from "./ExperimentListPage";
import axios from "axios";

vi.mock("axios");

afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});

function renderPage() {
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });
  return render(
    <QueryClientProvider client={client}>
      <BrowserRouter>
        <ExperimentListPage />
      </BrowserRouter>
    </QueryClientProvider>,
  );
}

describe("ExperimentListPage", () => {
  it("renders table", async () => {
    (axios.get as any).mockResolvedValueOnce({ data: [] });
    (axios.get as any).mockResolvedValueOnce({ data: [] });

    renderPage();

    expect(await screen.findByText(/Novo Teste/)).toBeTruthy();
  });

  it("shows the 25 most recent experiments with requested columns and pagination", async () => {
    const experiments = Array.from({ length: 26 }, (_, index) => {
      const id = index + 1;
      return {
        id: String(id),
        nicheId: 10,
        hypothesisId: `hypothesis-${id}`,
        name: `Experimento ${id}`,
        hypothesis: `Hipótese ${id}`,
        cost: id,
        startDate: `2026-06-${String(id).padStart(2, "0")}`,
        endDate: null,
        creativeApproved: false,
        status: "PLANNED",
        platform: "FACEBOOK",
        stage: "AD",
        createdAt: `2026-06-${String(id).padStart(2, "0")}T00:00:00Z`,
        updatedAt: `2026-06-${String(id).padStart(2, "0")}T00:00:00Z`,
      };
    });

    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/experiments")
        return Promise.resolve({ data: experiments });
      if (url === "/api/niches") {
        return Promise.resolve({
          data: [
            {
              id: 10,
              name: "Nicho Principal",
              description: "",
              demandVolume: "",
              promises: "",
              offers: "",
              baseSegmentation: "",
              interests: "",
              demographicFilters: "",
              extraTips: "",
            },
          ],
        });
      }
      return Promise.resolve({ data: [] });
    });

    renderPage();

    expect(
      await screen.findByRole("columnheader", { name: "ID do experimento" }),
    ).toBeTruthy();
    expect(
      screen.getByRole("columnheader", { name: "Nome do experimento" }),
    ).toBeTruthy();
    expect(
      screen.getByRole("columnheader", { name: "Data de criação" }),
    ).toBeTruthy();
    expect(screen.getByRole("columnheader", { name: "Nicho" })).toBeTruthy();
    expect(screen.getByRole("columnheader", { name: "Hipótese" })).toBeTruthy();
    expect(screen.getByRole("columnheader", { name: "Custo" })).toBeTruthy();
    expect(screen.getByRole("columnheader", { name: "Status" })).toBeTruthy();
    expect(
      screen.getByRole("columnheader", { name: "Botões/Ações" }),
    ).toBeTruthy();
    expect(await screen.findByText("Experimento 26")).toBeTruthy();
    expect(
      screen.getByText((content) =>
        content.includes(
          "Exibindo 1-25 de 26 experimentos, com 25 por página.",
        ),
      ),
    ).toBeTruthy();
    expect(screen.queryByText("Experimento 1")).toBeNull();

    await userEvent.click(screen.getByRole("button", { name: "Próxima" }));

    expect(screen.getByText("Página 2 de 2")).toBeTruthy();
    expect(screen.getByText("Experimento 1")).toBeTruthy();
    expect(screen.queryByText("Experimento 26")).toBeNull();
    const row = screen.getByText("Experimento 1").closest("tr");
    expect(row).not.toBeNull();
    expect(
      within(row as HTMLTableRowElement).getByText("01/06/2026"),
    ).toBeTruthy();
    expect(
      within(row as HTMLTableRowElement).getByText("Nicho Principal"),
    ).toBeTruthy();
    expect(
      within(row as HTMLTableRowElement).getByText("R$ 1,00"),
    ).toBeTruthy();
  });

  it("shows the individual backend cost for each experiment instead of the niche total", async () => {
    const experiments = [
      {
        id: "2",
        nicheId: 10,
        hypothesisId: "hypothesis-2",
        name: "Experimento 2",
        hypothesis: "Hipótese 2",
        cost: 33.18,
        startDate: "2026-06-02",
        endDate: null,
        creativeApproved: false,
        status: "PLANNED",
        platform: "FACEBOOK",
        stage: "AD",
        createdAt: "2026-06-02T00:00:00Z",
        updatedAt: "2026-06-02T00:00:00Z",
      },
      {
        id: "1",
        nicheId: 10,
        hypothesisId: "hypothesis-1",
        name: "Experimento 1",
        hypothesis: "Hipótese 1",
        cost: 66.63,
        startDate: "2026-06-01",
        endDate: null,
        creativeApproved: false,
        status: "PLANNED",
        platform: "FACEBOOK",
        stage: "AD",
        createdAt: "2026-06-01T00:00:00Z",
        updatedAt: "2026-06-01T00:00:00Z",
      },
    ];

    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/experiments")
        return Promise.resolve({ data: experiments });
      if (url === "/api/niches") {
        return Promise.resolve({
          data: [
            {
              id: 10,
              name: "Nicho Principal",
              description: "",
              demandVolume: "",
              promises: "",
              offers: "",
              baseSegmentation: "",
              interests: "",
              demographicFilters: "",
              extraTips: "",
            },
          ],
        });
      }
      return Promise.resolve({ data: [] });
    });

    renderPage();

    const row2 = (await screen.findByText("Experimento 2")).closest("tr");
    const row1 = screen.getByText("Experimento 1").closest("tr");

    expect(row2).not.toBeNull();
    expect(row1).not.toBeNull();
    expect(
      within(row2 as HTMLTableRowElement).getByText("R$ 33,18"),
    ).toBeTruthy();
    expect(
      within(row1 as HTMLTableRowElement).getByText("R$ 66,63"),
    ).toBeTruthy();
    expect(screen.queryByText("R$ 99,81")).toBeNull();
  });

});

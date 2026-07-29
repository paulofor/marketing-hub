import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import axios from "axios";
import { afterEach, describe, expect, it, vi } from "vitest";
import ExperimentSalesPageAbTab from "./ExperimentSalesPageAbTab";

vi.mock("axios");

afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});

function renderTab() {
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });
  return render(
    <QueryClientProvider client={client}>
      <ExperimentSalesPageAbTab experimentId="76" />
    </QueryClientProvider>,
  );
}

describe("ExperimentSalesPageAbTab", () => {
  it("does not infer A/B planning from default sales page types", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/experiments/76/sales-page-ab-tests/results") {
        return Promise.resolve({ data: [] });
      }
      if (url === "/api/experiments/76/sales-page-types") {
        return Promise.resolve({ data: [] });
      }
      if (url === "/api/sales-page-types") {
        return Promise.resolve({
          data: [
            {
              code: "HUMAN_VIDEO_SALES_PAGE",
              name: "Pagina com video humano",
              description: "Pagina de venda com video principal.",
              commercialMechanism: "Usa presenca humana.",
              leadCaptureStrategy: "Captura lead por CTA.",
              digitalBaitDelivery: "Entrega isca por bloco visual.",
              defaultForAbTest: true,
              active: true,
            },
            {
              code: "TRADITIONAL_LONG_FORM",
              name: "Pagina tradicional de venda",
              description: "Pagina linear com promessa.",
              commercialMechanism: "Conduz o lead por narrativa.",
              leadCaptureStrategy: "Captura lead por formulario.",
              digitalBaitDelivery: "Entrega isca por e-mail.",
              defaultForAbTest: true,
              active: true,
            },
          ],
        });
      }
      return Promise.resolve({ data: [] });
    });

    renderTab();

    expect(await screen.findByText("Tipos de página de venda")).toBeTruthy();
    await waitFor(() => {
      expect(screen.queryByText(/Planejamento A\/B/)).toBeNull();
    });
    expect(screen.getAllByRole("checkbox")).toHaveLength(2);
    screen.getAllByRole("checkbox").forEach((checkbox) => {
      expect(checkbox).not.toBeChecked();
    });
  });
});

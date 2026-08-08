import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import axios from "axios";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ExperimentHistoryTab from "./ExperimentHistoryTab";

vi.mock("axios");

function renderTab() {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <ExperimentHistoryTab experimentId="85" />
    </QueryClientProvider>,
  );
}

describe("ExperimentHistoryTab", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("shows persisted evidence and registers a new occurrence", async () => {
    (axios.get as any).mockResolvedValue({
      data: [
        {
          id: 1,
          category: "INCIDENTE",
          title: "Público incompatível",
          description: "Segmentação aprovada não foi aplicada.",
          evidenceJson: '{"impressions":182,"clicks":0}',
          source: "META_ADS",
          occurredAt: "2026-08-08T10:00:00Z",
          createdAt: "2026-08-08T10:01:00Z",
        },
      ],
    });
    (axios.post as any).mockResolvedValue({ data: { id: 2 } });
    renderTab();

    expect(await screen.findByText("Público incompatível")).toBeInTheDocument();
    expect(screen.getByText(/"impressions": 182/)).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Título"), {
      target: { value: "Campanha pausada" },
    });
    fireEvent.change(screen.getByLabelText("Descrição factual"), {
      target: { value: "Pausa autorizada para corrigir a causa-raiz." },
    });
    fireEvent.click(
      screen.getByRole("button", { name: "Registrar no experimento" }),
    );

    await waitFor(() =>
      expect(axios.post).toHaveBeenCalledWith(
        "/api/experiments/85/history-events",
        expect.objectContaining({ title: "Campanha pausada" }),
      ),
    );
  });
});

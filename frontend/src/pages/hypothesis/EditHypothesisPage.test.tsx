import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import EditHypothesisPage from "./EditHypothesisPage";
import axios from "axios";
import { describe, it, expect, vi } from "vitest";

vi.mock("axios");

function setup() {
  (axios.get as any).mockImplementation((url: string) => {
    if (url === "/api/niches/1/hypotheses") {
      return Promise.resolve({
        data: [
          {
            id: "10",
            marketNicheId: 1,
            title: "Hip 1",
            promise: "p1",
            problem: "pb1",
            persona: "pe1",
            successRule: "sr1",
            premiseAngleId: 1,
            offerType: "LEAD",
            kpiTargetCpl: 5,
            status: "BACKLOG",
            createdAt: "",
          },
        ],
      });
    }
    if (url === "/api/angles") {
      return Promise.resolve({ data: [{ id: 1, name: "Angle" }] });
    }
    return Promise.resolve({ data: [] });
  });
  (axios.put as any).mockResolvedValue({ data: {} });

  const client = new QueryClient();
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={["/niches/1/hypotheses/10/edit"]}>
        <Routes>
          <Route
            path="/niches/:nicheId/hypotheses/:hypothesisId/edit"
            element={<EditHypothesisPage />}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("EditHypothesisPage", () => {
  it("envia dados ao backend", async () => {
    setup();
    const titleInput = await screen.findByLabelText("Título");
    fireEvent.change(titleInput, { target: { value: "Nova Hipotese" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));
    await waitFor(() => expect((axios.put as any).mock.calls.length).toBe(1));
    expect((axios.put as any).mock.calls[0][0]).toBe("/api/hypotheses/10");
    expect((axios.put as any).mock.calls[0][1]).toMatchObject({
      title: "Nova Hipotese",
    });
  });
});

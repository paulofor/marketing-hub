import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import axios from "axios";
import NewHypothesisPage from "./NewHypothesisPage";

vi.mock("axios");

function renderPage() {
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={["/niches/18/hypotheses/new"]}>
        <Routes>
          <Route
            path="/niches/:nicheId/hypotheses/new"
            element={<NewHypothesisPage />}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("NewHypothesisPage", () => {
  it("shows the pain job id as links to the job detail page", async () => {
    (axios.get as any).mockResolvedValue({
      data: [
        {
          jobid: "9bb83a22-3894-43bd-9752-374f84eb6a2c",
          marketNicheId: 18,
          stageCode: "hypothesis-pain",
          status: "INICIADO",
          executionRequestedAt: "2026-06-11T00:16:01Z",
        },
      ],
    });

    renderPage();

    const links = await screen.findAllByRole("link", {
      name: "9bb83a22-3894-43bd-9752-374f84eb6a2c",
    });

    expect(links).toHaveLength(2);
    links.forEach((link) => {
      expect(link).toHaveAttribute(
        "href",
        "/niches/18/hypothesis-pipeline/pain/stage-executions/9bb83a22-3894-43bd-9752-374f84eb6a2c",
      );
    });
  });
});

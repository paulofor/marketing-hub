import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router-dom";
import { describe, it, expect, vi } from "vitest";
import ExperimentListPage from "./ExperimentListPage";
import axios from "axios";

vi.mock("axios");

describe("ExperimentListPage", () => {
  it("renders table", async () => {
    (axios.get as any).mockResolvedValueOnce({ data: [] });
    (axios.get as any).mockResolvedValueOnce({ data: [] });
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <BrowserRouter>
          <ExperimentListPage />
        </BrowserRouter>
      </QueryClientProvider>,
    );
    expect(await screen.findByText(/Novo Teste/)).toBeTruthy();
  });
});

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, expect, it, vi } from "vitest";
import axios from "axios";
import ProductProcessAutomationPanel from "./ProductProcessAutomationPanel";
vi.mock("axios");
let client: QueryClient;
afterEach(() => {
  cleanup();
  client?.clear();
  vi.resetAllMocks();
});
it.each(["WAITING_ACTIVITY", "PAUSED", "BLOCKED", "CLOSED"])(
  "mantém a análise acessível quando estado é %s",
  async (status) => {
    const url = "/financial/execution?productId=700&chainId=2700&cycleId=1900";
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        id: 900,
        productId: 700,
        processDefinitionId: 126,
        chainId: 2700,
        learningCycleId: 1900,
        status,
        sourceReference: "experiment:11200",
        financialReviewUrl: url,
        completedActivities: 0,
        remainingActivities: 3,
        totalActivities: 3,
        completionPercentage: 0,
        omittedActivities: 0,
      },
    });
    client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={client}>
        <MemoryRouter>
          <ProductProcessAutomationPanel
            productId={700}
            processId={126}
            chainId={2700}
            cycleId={1900}
            sourceReference="experiment:11200"
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );
    expect(
      await screen.findByRole("link", { name: "Ver demonstração financeira" }),
    ).toHaveAttribute("href", url);
    expect(axios.post).not.toHaveBeenCalled();
  },
);

import { render, screen, fireEvent, cleanup, waitFor } from "@testing-library/react";
import { describe, expect, it, vi, afterEach, beforeEach } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import MdsWorkspacePage from "./MdsWorkspacePage";

const invalidateQueries = vi.fn();
const mutateAsync = vi.fn();

vi.mock("@tanstack/react-query", async () => {
  const actual = await vi.importActual<typeof import("@tanstack/react-query")>(
    "@tanstack/react-query",
  );
  return {
    ...actual,
    useQueryClient: () => ({ invalidateQueries }),
  };
});

vi.mock("../../api/mds/useMdsAdmin", () => ({
  useMdsRequests: vi.fn(() => ({
    isLoading: false,
    isError: false,
    isFetching: false,
    data: {
      items: [
        {
          requestId: 12,
          market: "fitness",
          problem: "plateau",
          desiredOutcome: "perder gordura",
          status: "FAILED",
          currentStage: "pipeline",
          attempt: 2,
          lastHeartbeatAt: null,
          updatedAt: "2026-04-27T10:01:00Z",
          retryEligible: true,
          retryReason: "READY",
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    },
  })),
  useMdsHealth: vi.fn(() => ({
    isLoading: false,
    isError: false,
    data: { status: "ok", module: "mds-admin-api" },
  })),
  useRetryMdsRequest: vi.fn(() => ({
    isPending: false,
    mutateAsync,
  })),
}));

function renderPage() {
  const queryClient = new QueryClient();
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <MdsWorkspacePage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("MdsWorkspacePage", () => {
  beforeEach(() => {
    invalidateQueries.mockClear();
    mutateAsync.mockResolvedValue({ requestId: 12, previousStatus: "FAILED" });
  });

  afterEach(() => {
    cleanup();
  });

  it("dispara refresh com botão atualizar", () => {
    renderPage();
    const updateButtons = screen.getAllByRole("button", { name: /Atualizar/i });
    fireEvent.click(updateButtons[0]);
    expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: ["mds", "requests"] });
  });

  it("executa retry e exibe feedback operacional", async () => {
    renderPage();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));

    await waitFor(() => expect(mutateAsync).toHaveBeenCalledWith(12));
    expect(screen.getByText(/Retry aceito para request #12/)).toBeTruthy();
  });
});

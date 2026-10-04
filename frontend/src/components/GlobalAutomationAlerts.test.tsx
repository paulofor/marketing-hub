import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import GlobalAutomationAlerts from "./GlobalAutomationAlerts";

vi.mock("axios");
vi.mock("../api/useFacebookConfigurationStatus", () => ({
  useFacebookConfigurationStatus: () => ({ data: undefined }),
}));
const get = vi.mocked(axios.get);
afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});
function setup(path: string) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[path]}>
        <GlobalAutomationAlerts />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}
describe("GlobalAutomationAlerts", () => {
  it("mostra somente contagem humana oficial e mantém o escopo no link", async () => {
    get.mockResolvedValue({
      data: { awaitingReviewCount: 2, blockedCount: 8, historicalCount: 11 },
    });
    setup(
      "/products/81/value-chain-history/processes/25/activities?sourceReference=experiment%3A820",
    );
    expect(
      await screen.findByText(
        "2 novas peças de vídeo disponíveis para revisão",
      ),
    ).toBeInTheDocument();
    expect(get).toHaveBeenCalledWith("/api/creatives/video-review/summary", {
      params: { productId: 81, experimentId: 820 },
    });
    expect(
      screen.getByRole("link", { name: "Ver peças para revisão" }),
    ).toHaveAttribute(
      "href",
      "/creative-video-review?productId=81&experimentId=820",
    );
  });
  it("não atribui vídeos alheios ou bloqueados ao produto sem revisões humanas", async () => {
    get.mockResolvedValue({
      data: { awaitingReviewCount: 0, blockedCount: 2, historicalCount: 4 },
    });
    setup("/products/82/edit");
    await waitFor(() =>
      expect(get).toHaveBeenCalledWith("/api/creatives/video-review/summary", {
        params: { productId: 82, experimentId: undefined },
      }),
    );
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });
  it("identifica a fila geral e não usa o endpoint legado DRAFT", async () => {
    get.mockResolvedValue({ data: { awaitingReviewCount: 1 } });
    setup("/creative-video-review");
    expect(
      await screen.findByText("1 nova peça de vídeo disponível para revisão"),
    ).toBeInTheDocument();
    expect(screen.getByText(/Na fila geral do Hub/)).toBeInTheDocument();
    expect(
      get.mock.calls.every(
        ([url]) => url === "/api/creatives/video-review/summary",
      ),
    ).toBe(true);
  });
});

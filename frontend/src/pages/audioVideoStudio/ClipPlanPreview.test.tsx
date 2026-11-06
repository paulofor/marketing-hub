import { render, screen, fireEvent, cleanup } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, expect, it, vi } from "vitest";
import axios from "axios";
import { ClipPlanPreview } from "./ClipPlanPreview";
vi.mock("axios");
afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});
it("consulta somente ao pedir, mostra custo desconhecido e não autoriza render", async () => {
  vi.mocked(axios.post).mockResolvedValue({
    data: {
      capabilityRevision: "v1",
      finalSeconds: 32,
      generatedSeconds: 32,
      trimSeconds: 0,
      scenes: [
        {
          order: 1,
          modelId: "gen4.5",
          requestedSeconds: 10,
          usedSeconds: 10,
          billedSeconds: 10,
          maximumCostUsd: null,
          narrativeRole: "Gancho",
        },
      ],
      clipsCostUsd: null,
      productionAllowed: false,
      blockers: ["Preço vencido; renovar pesquisa"],
    },
  });
  const wrap = ({ children }: any) => (
    <QueryClientProvider
      client={
        new QueryClient({
          defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
          },
        })
      }
    >
      {children}
    </QueryClientProvider>
  );
  render(<ClipPlanPreview providerName="RUNWAY" finalSeconds={32} />, {
    wrapper: wrap,
  });
  expect(axios.post).not.toHaveBeenCalled();
  fireEvent.click(
    screen.getByRole("button", { name: "Prever tomadas sem gerar" }),
  );
  expect(
    await screen.findByText("Preço vencido; renovar pesquisa"),
  ).toBeTruthy();
  expect(screen.getByText("Desconhecido")).toBeTruthy();
  expect(axios.post).toHaveBeenCalledWith(
    "/api/sales-videos/studio/clip-plan",
    expect.objectContaining({ providerName: "RUNWAY", finalSeconds: 32 }),
  );
  expect(axios.post).toHaveBeenCalledTimes(1);
});
it("usa resoluções do catálogo e não reaproveita custos de outro plano", async () => {
  vi.mocked(axios.post).mockResolvedValue({
    data: {
      capabilityRevision: "2026-11-05.v1",
      finalSeconds: 32,
      generatedSeconds: 32,
      trimSeconds: 0,
      scenes: [],
      clipsCostUsd: 3.84,
      productionAllowed: false,
      blockers: [],
    },
  });
  const remainingCosts = Object.fromEntries(
    [
      "AGENTS",
      "REFERENCES",
      "VOICE",
      "MUSIC",
      "POST_PRODUCTION",
      "FEES",
      "RETRIES",
    ].map((key) => [key, { maximumUsd: 0, evidence: "fixture local" }]),
  );
  const client = new QueryClient({
    defaultOptions: { mutations: { retry: false } },
  });
  const { rerender } = render(
    <QueryClientProvider client={client}>
      <ClipPlanPreview
        providerName="RUNWAY"
        finalSeconds={32}
        providerPlan="(RUNWAY)"
        productionCostPlanJson={JSON.stringify({
          providerPlan: "(RUNWAY_VEO_3_1)",
          capabilityRevision: "2026-11-05.v1",
          finalSeconds: 32,
          expiresAt: "2099-01-01T00:00:00Z",
          unresolvedAttemptIds: [],
          remainingCosts,
        })}
      />
    </QueryClientProvider>,
  );
  fireEvent.click(
    screen.getByRole("button", { name: "Prever tomadas sem gerar" }),
  );
  expect(
    await screen.findByText(/Previsão integral restante: incompleta/),
  ).toBeTruthy();
  rerender(
    <QueryClientProvider client={client}>
      <ClipPlanPreview providerName="RUNWAY_HAILUO_3" finalSeconds={32} />
    </QueryClientProvider>,
  );
  expect(screen.getByRole("option", { name: "768P" })).toBeTruthy();
  expect(screen.queryByRole("option", { name: "1080p" })).toBeNull();
});

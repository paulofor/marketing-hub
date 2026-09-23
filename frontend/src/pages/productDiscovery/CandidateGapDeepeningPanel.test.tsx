import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import CandidateGapDeepeningPanel from "./CandidateGapDeepeningPanel";

const fetchMock = vi.fn();

const opportunities = [
  {
    id: 701,
    cycleId: 65,
    name: "Preparação para ocasião especial",
    primaryAudience: "Mulheres com ocasião marcada",
    rootPain: "Escolha difícil",
    score: 61,
    maturity: "RESEARCHABLE" as const,
    decision: "RESEARCH_MORE" as const,
    createdAt: "2026-09-23T00:00:00Z",
    updatedAt: "2026-09-23T00:00:00Z",
  },
  {
    id: 702,
    cycleId: 65,
    name: "Imagem para encontro importante",
    primaryAudience: "Mulheres com encontro marcado",
    rootPain: "Incerteza sobre imagem",
    score: 59,
    maturity: "RESEARCHABLE" as const,
    decision: "RESEARCH_MORE" as const,
    createdAt: "2026-09-23T00:00:00Z",
    updatedAt: "2026-09-23T00:00:00Z",
  },
];

const gapResponse = {
  cycleId: 65,
  applicable: true,
  cycleStatus: "AWAITING_CUSTOMER_EVIDENCE",
  stageCode: "customer-evidence",
  minimumInterviews: 5,
  maximumInterviews: 8,
  interviewCount: 0,
  purchasedCount: 0,
  abandonedCount: 0,
  coveredOpportunityIds: [],
  missingOpportunityIds: [701, 702],
  readyForResearch: false,
  maximumPublicQueriesPerAttempt: 12,
  maximumAttempts: 2,
  maximumModelInvocations: 4,
  maximumSearchCostUsd: 0.12,
  searchCostCoverage: "ESTIMATED_SEARCH_ONLY",
  modelCostCoverage: "AGENT_TASK_AUDIT_AFTER_CALLBACK",
  searchPricingSource: "https://brave.com/search/api/",
  searchPricingObservedOn: "2026-09-23",
  guidance: "Registre situações concretas de compra e desistência.",
  interviews: [],
};

function renderPanel() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <CandidateGapDeepeningPanel cycleId={65} opportunities={opportunities} />
    </QueryClientProvider>,
  );
}

async function fillRequiredInterview(
  user: ReturnType<typeof userEvent.setup>,
  participantCode = "P01",
) {
  await user.selectOptions(screen.getByLabelText(/Candidata/), "702");
  await user.type(screen.getByLabelText(/Código anônimo/), participantCode);
  await user.selectOptions(
    screen.getByLabelText(/Decisão passada/),
    "ABANDONED",
  );
  await user.type(screen.getByLabelText(/Data da situação/), "2026-09-01");
  await user.type(
    screen.getByLabelText(/Conte a última vez/),
    "Tinha um encontro marcado para a semana seguinte.",
  );
  await user.type(
    screen.getByLabelText(/Que resultado buscava/),
    "Queria sentir segurança com uma escolha coerente.",
  );
  await user.type(
    screen.getByLabelText(/O que estava difícil/),
    "Não conseguia comparar as referências.",
  );
  await user.type(
    screen.getByLabelText(/O que tentou ou considerou/),
    "Vídeos gratuitos e uma consultoria.",
  );
  await user.type(
    screen.getByLabelText(/O que continuou difícil/),
    "Ainda precisava montar a decisão sozinha.",
  );
  await user.click(screen.getByLabelText(/consentiu com o uso anônimo/i));
  await user.click(screen.getByLabelText(/não contém dados pessoais/i));
}

describe("CandidateGapDeepeningPanel", () => {
  beforeEach(() => {
    fetchMock.mockReset();
    fetchMock.mockImplementation(async (_input, init?: RequestInit) => ({
      ok: true,
      json: async () => gapResponse,
      status: init?.method === "POST" ? 200 : 200,
    }));
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it("registra comportamento passado consentido sem dados de contato", async () => {
    const user = userEvent.setup();
    renderPanel();

    expect(
      await screen.findByRole("heading", {
        name: "Aprofundar lacunas das candidatas",
      }),
    ).toBeTruthy();
    await fillRequiredInterview(user);
    await user.click(
      screen.getByRole("button", { name: "Registrar entrevista" }),
    );

    await waitFor(() =>
      expect(
        fetchMock.mock.calls.some(
          ([, init]) => (init as RequestInit | undefined)?.method === "POST",
        ),
      ).toBe(true),
    );
    const postCall = fetchMock.mock.calls.find(
      ([, init]) => (init as RequestInit | undefined)?.method === "POST",
    );
    const payload = JSON.parse(String((postCall?.[1] as RequestInit).body));
    expect(payload).toMatchObject({
      opportunityId: 702,
      anonymousParticipantCode: "P01",
      outcome: "ABANDONED",
      consentConfirmed: true,
      noPersonalDataConfirmed: true,
    });
    expect(payload).not.toHaveProperty("amountSpent");
    expect(
      screen.getByRole("link", { name: "Brave Search API" }),
    ).toHaveAttribute("target", "_blank");
    expect(
      screen.getByRole("link", { name: /orientação de entrevistas/i }),
    ).toHaveAttribute("target", "_blank");
  });

  it("mostra a causa segura devolvida pelo backend sem apagar o relato", async () => {
    fetchMock.mockImplementation(async (_input, init?: RequestInit) =>
      init?.method === "POST"
        ? {
            ok: false,
            json: async () => ({
              detail:
                "Remova e-mail, telefone ou contato pessoal do resumo anônimo",
            }),
            status: 422,
          }
        : {
            ok: true,
            json: async () => gapResponse,
            status: 200,
          },
    );
    const user = userEvent.setup();
    renderPanel();

    await screen.findByRole("heading", {
      name: "Aprofundar lacunas das candidatas",
    });
    await fillRequiredInterview(user, "P02");
    await user.click(
      screen.getByRole("button", { name: "Registrar entrevista" }),
    );

    expect(
      await screen.findByRole("alert", {
        name: "",
      }),
    ).toHaveTextContent(
      "Remova e-mail, telefone ou contato pessoal do resumo anônimo (status 422).",
    );
    expect(screen.getByLabelText(/Código anônimo/)).toHaveValue("P02");
  });
});

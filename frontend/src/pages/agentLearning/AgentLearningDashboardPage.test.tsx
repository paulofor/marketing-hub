import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import axios from "axios";
import { describe, expect, it, vi } from "vitest";
import AgentLearningDashboardPage from "./AgentLearningDashboardPage";

vi.mock("axios");

describe("AgentLearningDashboardPage", () => {
  /** Exibe reutilização real e não apresenta memória como venda comprovada. */
  it("shows governed learning evidence without claiming commercial attribution", async () => {
    vi.mocked(axios.get).mockImplementation(async (url) => ({
      data: String(url).includes("/agents/apollo/skills")
        ? [
            {
              id: 3,
              experimentId: 2,
              skillKey: "commercial-storyboard",
              baselineVersion: "v1",
              candidateVersion: "v2",
              diffSummary: "Demonstração antecipada",
              provenanceJson: "{}",
              safetyDecision: "APPROVED",
              safetyEvidence: "Sem expansão de autoridade",
              status: "CANDIDATE",
              monitoredCases: 5,
              approvedCases: 4,
            },
          ]
        : {
            totalMemories: 1,
            candidateMemories: 1,
            confirmedMemories: 0,
            contradictedMemories: 0,
            retiredMemories: 0,
            totalRetrievals: 4,
            agents: [
              {
                agentKey: "apollo",
                agentName: "Apolo",
                totalMemories: 1,
                candidateMemories: 1,
                confirmedMemories: 0,
                contradictedMemories: 0,
                retiredMemories: 0,
                totalRetrievals: 4,
              },
            ],
            memories: [
              {
                id: 7,
                agentKey: "apollo",
                agentName: "Apolo",
                tenantKey: "tenant-1",
                scopeType: "PROJECT",
                scopeId: "15",
                specialty: "Roteiro",
                content: "Demonstrar o produto antes da promessa.",
                evidence: "Replay aprovado.",
                sourceExecutionId: "execution-15",
                status: "CANDIDATE",
                confidence: 0.8,
                retrievalCount: 4,
                createdAt: "2026-08-15T10:00:00Z",
                updatedAt: "2026-08-15T10:00:00Z",
              },
            ],
          },
    }));
    render(
      <QueryClientProvider client={new QueryClient()}>
        <AgentLearningDashboardPage />
      </QueryClientProvider>,
    );

    expect(await screen.findByText("Aprendizado dos Agentes")).toBeTruthy();
    expect(
      screen.getByText("Demonstrar o produto antes da promessa."),
    ).toBeTruthy();
    expect(screen.getByText("Sem atribuição comprovada")).toBeTruthy();
    expect(await screen.findByText("commercial-storyboard")).toBeTruthy();
    expect(screen.getByText("4/5 aprovados")).toBeTruthy();
  });
});

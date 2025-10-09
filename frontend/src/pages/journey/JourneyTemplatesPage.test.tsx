import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import "@testing-library/jest-dom/vitest";
import JourneyTemplatesPage from "./JourneyTemplatesPage";
import { MemoryRouter } from "react-router-dom";

vi.mock("../../api/journey/useJourneyTemplates", () => ({
  useJourneyTemplates: () => ({
    data: {
      content: [
        {
          id: 1,
          name: "Lifecycle Pós-Clique Lead Ads 14d",
          objective: "Converter curiosidade em relacionamento contínuo",
          phases: ["ATTENTION", "INTEREST", "DESIRE", "ACTION"],
          preferredChannel: null,
          tags: ["facebook", "lifecycle"],
          metadata: {},
          steps: [
            {
              id: 101,
              templateId: 1,
              position: 1,
              name: "Primeiro contato",
              description: "Enviar email de boas-vindas",
              phase: "ATTENTION",
              stimulusType: "EMAIL",
              creativeId: null,
              angleId: null,
              visualProofId: null,
              emotionalTriggerId: null,
              entryCondition: null,
              exitCondition: null,
              delayMinutes: 0,
              metadata: {},
            },
          ],
          createdAt: null,
          updatedAt: undefined,
        },
      ],
      totalElements: 1,
      totalPages: 1,
      number: 0,
    },
    isLoading: false,
  }),
}));

describe("JourneyTemplatesPage", () => {
  it("exibe templates mesmo quando timestamps não estão disponíveis", () => {
    render(
      <MemoryRouter>
        <JourneyTemplatesPage />
      </MemoryRouter>,
    );

    expect(
      screen.getByRole("heading", {
        level: 2,
        name: "Lifecycle Pós-Clique Lead Ads 14d",
      }),
    ).toBeInTheDocument();
    expect(screen.getByText("Multicanal")).toBeInTheDocument();
    expect(screen.getByText("ATTENTION • INTEREST • DESIRE • ACTION")).toBeInTheDocument();
    expect(screen.getByText(/Criado em\s+—/)).toBeInTheDocument();
    expect(screen.getByText(/Atualizado em\s+—/)).toBeInTheDocument();
    expect(screen.getByText("Primeiro contato")).toBeInTheDocument();
    expect(screen.getByText("Atenção • Email")).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Criar template" }),
    ).toHaveAttribute("href", "/journey-templates/new");
  });
});

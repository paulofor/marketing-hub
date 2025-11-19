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
          createdAt: "2023-11-01T10:00:00Z",
          updatedAt: "2023-12-15T10:00:00Z",
        },
        {
          id: 2,
          name: "Funil de onboarding no WhatsApp",
          objective: "Transformar leads em clientes em até 7 dias",
          phases: ["ATTENTION", "ACTION"],
          preferredChannel: "WhatsApp",
          tags: ["whatsapp", "onboarding"],
          metadata: {},
          steps: [],
          createdAt: "2024-02-01T10:00:00Z",
          updatedAt: "2024-02-20T10:00:00Z",
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
  it("exibe templates com os metadados principais", () => {
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
    expect(screen.getAllByText("ATTENTION • INTEREST • DESIRE • ACTION")[0]).toBeInTheDocument();
    expect(screen.getByText(/Criado em\s+01\/11\/2023/)).toBeInTheDocument();
    expect(screen.getByText(/Atualizado em\s+15\/12\/2023/)).toBeInTheDocument();
    expect(screen.queryByText("Primeiro contato")).not.toBeInTheDocument();
    expect(screen.getAllByText("Multicanal")[0]).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Criar template" }),
    ).toHaveAttribute("href", "/journey-templates/new");
    const detailLinks = screen.getAllByRole("link", { name: "Ver detalhes" });
    expect(detailLinks).toHaveLength(2);
    expect(detailLinks[1]).toHaveAttribute("href", "/journey-templates/1");
  });

  it("ordena os templates do mais recente para o mais antigo", () => {
    render(
      <MemoryRouter>
        <JourneyTemplatesPage />
      </MemoryRouter>,
    );

    const templateTitles = screen.getAllByRole("heading", { level: 2 });
    expect(templateTitles[0]).toHaveTextContent("Funil de onboarding no WhatsApp");
    expect(templateTitles[1]).toHaveTextContent("Lifecycle Pós-Clique Lead Ads 14d");
  });
});

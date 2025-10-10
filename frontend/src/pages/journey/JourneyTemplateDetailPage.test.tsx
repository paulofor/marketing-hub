import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, beforeEach, vi } from "vitest";
import "@testing-library/jest-dom/vitest";
import JourneyTemplateDetailPage from "./JourneyTemplateDetailPage";
import { useJourneyTemplate } from "../../api/journey/useJourneyTemplate";
import type { JourneyTemplate } from "../../api/journey/types";

vi.mock("../../api/journey/useJourneyTemplate", () => ({
  useJourneyTemplate: vi.fn(),
}));

const mockedUseJourneyTemplate = vi.mocked(useJourneyTemplate);

function mockQuerySuccess(template: JourneyTemplate) {
  mockedUseJourneyTemplate.mockImplementation(() => ({
    data: template,
    isLoading: false,
    isError: false,
  }) as unknown as ReturnType<typeof useJourneyTemplate>);
}

function renderWithRoute(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/journey-templates/:id" element={<JourneyTemplateDetailPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("JourneyTemplateDetailPage", () => {
  beforeEach(() => {
    mockedUseJourneyTemplate.mockReset();
  });

  it("exibe detalhes completos do template e suas etapas", () => {
    mockQuerySuccess({
      id: 7,
      name: "Recuperação pós lead ads",
      description: "Sequência focada em nutrir novos leads com conteúdo progressivo.",
      objective: "Transformar novos contatos em oportunidades.",
      phases: ["ATTENTION", "INTEREST"],
      preferredChannel: "Email",
      tags: ["nutrição", "lead"],
      metadata: { playbook: "Lead Ads" },
      steps: [
        {
          id: 1,
          templateId: 7,
          position: 1,
          name: "Email de boas-vindas",
          description: "Compartilhe o benefício prometido e convide para o próximo passo.",
          phase: "ATTENTION",
          stimulusType: "EMAIL",
          creativeId: null,
          angleId: null,
          visualProofId: null,
          emotionalTriggerId: null,
          entryCondition: "Lead captado pelo formulário instantâneo",
          exitCondition: "Lead abriu o email",
          delayMinutes: 0,
          metadata: {},
        },
      ],
      createdAt: "2024-02-01T00:00:00Z",
      updatedAt: "2024-02-12T00:00:00Z",
    });

    renderWithRoute("/journey-templates/7");

    expect(screen.getByRole("heading", { level: 1, name: "Recuperação pós lead ads" })).toBeInTheDocument();
    expect(screen.getByText("Transformar novos contatos em oportunidades.")).toBeInTheDocument();
    expect(screen.getByText("Email")).toBeInTheDocument();
    expect(screen.getByText("Atenção • Interesse")).toBeInTheDocument();
    expect(screen.getByText("Email de boas-vindas")).toBeInTheDocument();
    expect(screen.getByText("Condição de entrada")).toBeInTheDocument();
    expect(screen.getByText("Lead captado pelo formulário instantâneo")).toBeInTheDocument();
    expect(screen.getByText("Lead Ads")).toBeInTheDocument();
  });

  it("mostra mensagem orientativa quando o template não possui etapas", () => {
    mockQuerySuccess({
      id: 8,
      name: "Template vazio",
      description: null,
      objective: null,
      phases: [],
      preferredChannel: null,
      tags: [],
      metadata: {},
      steps: [],
      createdAt: "",
      updatedAt: "",
    });

    renderWithRoute("/journey-templates/8");

    expect(screen.getByText("Sem etapas cadastradas")).toBeInTheDocument();
    expect(
      screen.getByText(
        "Nenhuma etapa cadastrada até o momento. Cadastre etapas para visualizar o fluxo completo.",
      ),
    ).toBeInTheDocument();
  });
});

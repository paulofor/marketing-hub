import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import DirectRecruitmentPanel from "./DirectRecruitmentPanel";

const createMutate = vi.fn();
const activateMutate = vi.fn();
const pauseMutate = vi.fn();
const useCampaign = vi.fn();

vi.mock("../../api/experiment/useExperimentDirectRecruitment", () => ({
  useExperimentDirectRecruitment: (...args: unknown[]) => useCampaign(...args),
  useCreateDirectRecruitmentDraft: () => ({
    mutateAsync: createMutate,
    isPending: false,
  }),
  useActivateDirectRecruitment: () => ({
    mutateAsync: activateMutate,
    isPending: false,
  }),
  usePauseDirectRecruitment: () => ({
    mutateAsync: pauseMutate,
    isPending: false,
  }),
}));

const baseCampaign = {
  id: null,
  experimentId: 89,
  productName: "Kit WhatsApp Pronto",
  status: "NOT_CREATED",
  contractVersion: "direct-recruitment-v1",
  headline: "Seu atendimento no WhatsApp poderia vender mais?",
  bodyText: "Convite de validação.",
  audienceSummary: "Prestadores de serviços.",
  consentText: "Aceito participar.",
  consentVersion: "consent-v1",
  offerUrl: "https://rigel.example",
  offerCta: "Conhecer o Rigel",
  privacyPolicyUrl: "https://rigel.example/privacidade",
  publicPath: null,
  targetContacts: 15,
  remainingContacts: 15,
  uniqueVisits: 0,
  submissions: 0,
  qualifiedSubmissions: 0,
  notQualifiedSubmissions: 0,
  recordedContacts: 0,
  connectedOrganicAccounts: 0,
  acquisitionStatus: "NOT_CREATED",
  distributionGuidance: "Crie o rascunho.",
  createdBy: null,
  statusChangedBy: null,
  statusReason: null,
  createdAt: null,
  updatedAt: null,
  activatedAt: null,
  pausedAt: null,
  completedAt: null,
};

describe("DirectRecruitmentPanel", () => {
  beforeEach(() => {
    cleanup();
    vi.clearAllMocks();
    useCampaign.mockReturnValue({
      data: baseCampaign,
      isLoading: false,
      isError: false,
    });
    createMutate.mockResolvedValue(baseCampaign);
    activateMutate.mockResolvedValue(baseCampaign);
    pauseMutate.mockResolvedValue(baseCampaign);
  });

  it("prepara o convite sem representar a preparação como distribuição", async () => {
    render(<DirectRecruitmentPanel experimentId={89} />);

    expect(screen.getByText("Atividade não preparada")).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Responsável pela atividade *"), {
      target: { value: "Operador QA" },
    });
    fireEvent.click(
      screen.getByRole("button", { name: "Preparar convite para aprovação" }),
    );

    await waitFor(() =>
      expect(createMutate).toHaveBeenCalledWith("Operador QA"),
    );
    expect(
      screen.getByText(/Ativação, distribuição e venda/),
    ).toBeInTheDocument();
  });

  it("exige aprovação explícita antes de ativar o rascunho", async () => {
    useCampaign.mockReturnValue({
      data: {
        ...baseCampaign,
        id: 10,
        status: "DRAFT",
        publicPath: "/participar/convite",
        acquisitionStatus: "DRAFT_REQUIRES_APPROVAL",
        distributionGuidance: "Revise e aprove.",
      },
      isLoading: false,
      isError: false,
    });
    render(<DirectRecruitmentPanel experimentId={89} />);

    const activateButton = screen.getByRole("button", {
      name: "Aprovar e ativar convite",
    });
    expect(activateButton).toBeDisabled();
    expect(
      screen.queryByText("Link público rastreável"),
    ).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Responsável pela atividade *"), {
      target: { value: "Operador QA" },
    });
    fireEvent.click(
      screen.getByLabelText(/Aprovo esta comunicação e confirmo/),
    );
    fireEvent.click(activateButton);

    await waitFor(() =>
      expect(activateMutate).toHaveBeenCalledWith("Operador QA"),
    );
  });

  it("mostra o link somente quando o mesmo rascunho passa a ativo", () => {
    useCampaign.mockReturnValue({
      data: {
        ...baseCampaign,
        id: 10,
        status: "DRAFT",
        publicPath: "/participar/convite",
        acquisitionStatus: "DRAFT_REQUIRES_APPROVAL",
      },
      isLoading: false,
      isError: false,
    });
    const view = render(<DirectRecruitmentPanel experimentId={89} />);

    expect(
      screen.queryByText("Link público rastreável"),
    ).not.toBeInTheDocument();

    useCampaign.mockReturnValue({
      data: {
        ...baseCampaign,
        id: 10,
        status: "ACTIVE",
        publicPath: "/participar/convite",
        acquisitionStatus: "ACTIVE_WITHOUT_DISTRIBUTION",
      },
      isLoading: false,
      isError: false,
    });
    view.rerender(<DirectRecruitmentPanel experimentId={89} />);

    expect(screen.getByText("Link público rastreável")).toBeInTheDocument();
  });

  it("expõe o bloqueio quando não há canal orgânico conectado", () => {
    useCampaign.mockReturnValue({
      data: {
        ...baseCampaign,
        id: 10,
        status: "ACTIVE",
        publicPath: "/participar/convite",
        acquisitionStatus: "ACTIVE_WITHOUT_DISTRIBUTION",
        distributionGuidance: "Conecte uma conta orgânica no Marketing Hub.",
        uniqueVisits: 4,
        submissions: 2,
        qualifiedSubmissions: 1,
        recordedContacts: 1,
      },
      isLoading: false,
      isError: false,
    });

    render(<DirectRecruitmentPanel experimentId={89} />);

    expect(
      screen.getByText("Ativo, sem canal de distribuição"),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: /Conectar canal orgânico/ }),
    ).toHaveAttribute("href", "/social-distribution");
    expect(screen.getByText("1/15")).toBeInTheDocument();
  });
});

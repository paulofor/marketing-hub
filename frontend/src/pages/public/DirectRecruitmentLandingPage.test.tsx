import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import DirectRecruitmentLandingPage from "./DirectRecruitmentLandingPage";

const registerVisit = vi.fn();
const submit = vi.fn();
const useCampaign = vi.fn();

vi.mock("../../api/experiment/useExperimentDirectRecruitment", () => ({
  recruitmentAttribution: () => ({ utmSource: "instagram" }),
  registerDirectRecruitmentVisit: (...args: unknown[]) =>
    registerVisit(...args),
  usePublicDirectRecruitment: (...args: unknown[]) => useCampaign(...args),
  useSubmitDirectRecruitment: () => ({
    mutateAsync: submit,
    isPending: false,
  }),
}));

const campaign = {
  token: "11111111-2222-4333-8444-555555555555",
  experimentId: 89,
  status: "ACTIVE",
  acceptingSubmissions: true,
  productName: "Kit WhatsApp Pronto",
  headline: "Seu atendimento no WhatsApp poderia vender mais?",
  bodyText: "Participe de uma validação rápida.",
  audienceSummary: "Pequenos prestadores de serviços.",
  consentText: "Aceito participar e conhecer a oferta.",
  consentVersion: "consent-v1",
  privacyPolicyUrl: "https://rigel.example/privacidade",
  targetContacts: 15,
  remainingContacts: 15,
  availabilityMessage: "A validação está aberta.",
};

/** Renderiza a página com o mesmo contrato de rota usado pela aplicação. */
function renderPage() {
  return render(
    <MemoryRouter initialEntries={[`/participar/${campaign.token}`]}>
      <Routes>
        <Route
          path="/participar/:token"
          element={<DirectRecruitmentLandingPage />}
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe("DirectRecruitmentLandingPage", () => {
  beforeEach(() => {
    cleanup();
    window.localStorage.clear();
    vi.clearAllMocks();
    useCampaign.mockReturnValue({
      data: campaign,
      isLoading: false,
      isError: false,
    });
    registerVisit.mockResolvedValue({ counted: true, uniqueVisits: 1 });
    submit.mockResolvedValue({
      submissionId: 20,
      status: "QUALIFIED",
      qualified: true,
      message: "Seu perfil é aderente.",
      offerUrl: "https://rigel.example",
      remainingContacts: 14,
      sampleComplete: false,
    });
  });

  it("qualifica o prestador e só então apresenta a oferta", async () => {
    renderPage();

    expect(
      screen.queryByRole("link", { name: /Conhecer/ }),
    ).not.toBeInTheDocument();
    await waitFor(() => expect(registerVisit).toHaveBeenCalledTimes(1));
    fireEvent.change(screen.getByLabelText(/Em qual tipo de serviço/), {
      target: { value: "CONSULTING" },
    });
    fireEvent.change(screen.getByLabelText(/Quantas conversas/), {
      target: { value: "ELEVEN_TO_THIRTY" },
    });
    fireEvent.change(screen.getByLabelText(/Usa WhatsApp/), {
      target: { value: "true" },
    });
    fireEvent.change(screen.getByLabelText(/Decide sobre esse atendimento/), {
      target: { value: "true" },
    });
    fireEvent.change(
      screen.getByLabelText(/Quer uma implantação personalizada/),
      {
        target: { value: "true" },
      },
    );
    fireEvent.change(screen.getByLabelText(/Seu WhatsApp ou e-mail/), {
      target: { value: "maria@example.com" },
    });
    fireEvent.click(screen.getByLabelText(/Aceito participar/));
    fireEvent.click(
      screen.getByRole("button", {
        name: "Quero participar e conhecer a solução",
      }),
    );

    await waitFor(() => expect(submit).toHaveBeenCalledTimes(1));
    expect(submit).toHaveBeenCalledWith(
      expect.objectContaining({
        contactReference: "maria@example.com",
        serviceSegment: "CONSULTING",
        consentVersion: "consent-v1",
      }),
    );
    expect(
      screen.getByRole("link", { name: /Conhecer Kit WhatsApp Pronto/ }),
    ).toHaveAttribute("href", "https://rigel.example");
    expect(
      screen.getByRole("link", { name: "política de privacidade" }),
    ).toHaveAttribute("href", "https://rigel.example/privacidade");
  });

  it("bloqueia o formulário quando a amostra já foi encerrada", () => {
    useCampaign.mockReturnValue({
      data: {
        ...campaign,
        status: "COMPLETED",
        acceptingSubmissions: false,
        remainingContacts: 0,
        availabilityMessage: "A validação atingiu a amostra planejada.",
      },
      isLoading: false,
      isError: false,
    });

    renderPage();

    expect(screen.getByText("Participação encerrada")).toBeInTheDocument();
    expect(
      screen.queryByLabelText(/Seu WhatsApp ou e-mail/),
    ).not.toBeInTheDocument();
    expect(registerVisit).not.toHaveBeenCalled();
  });

  it("mantém a oferta visível para a pessoa que completa a última vaga", async () => {
    submit.mockImplementation(async () => {
      useCampaign.mockReturnValue({
        data: {
          ...campaign,
          status: "COMPLETED",
          acceptingSubmissions: false,
          remainingContacts: 0,
          availabilityMessage: "A validação atingiu a amostra planejada.",
        },
        isLoading: false,
        isError: false,
      });
      return {
        submissionId: 34,
        status: "QUALIFIED",
        qualified: true,
        message: "Seu perfil é aderente.",
        offerUrl: "https://rigel.example",
        remainingContacts: 0,
        sampleComplete: true,
      };
    });
    renderPage();

    await waitFor(() => expect(registerVisit).toHaveBeenCalledTimes(1));
    fireEvent.change(screen.getByLabelText(/Em qual tipo de serviço/), {
      target: { value: "CONSULTING" },
    });
    fireEvent.change(screen.getByLabelText(/Quantas conversas/), {
      target: { value: "ELEVEN_TO_THIRTY" },
    });
    fireEvent.change(screen.getByLabelText(/Usa WhatsApp/), {
      target: { value: "true" },
    });
    fireEvent.change(screen.getByLabelText(/Decide sobre esse atendimento/), {
      target: { value: "true" },
    });
    fireEvent.change(
      screen.getByLabelText(/Quer uma implantação personalizada/),
      { target: { value: "true" } },
    );
    fireEvent.change(screen.getByLabelText(/Seu WhatsApp ou e-mail/), {
      target: { value: "ultima-vaga@example.com" },
    });
    fireEvent.click(screen.getByLabelText(/Aceito participar/));
    fireEvent.click(
      screen.getByRole("button", {
        name: "Quero participar e conhecer a solução",
      }),
    );

    expect(
      await screen.findByRole("link", { name: /Conhecer Kit WhatsApp Pronto/ }),
    ).toHaveAttribute("href", "https://rigel.example");
    expect(
      screen.queryByText("Participação encerrada"),
    ).not.toBeInTheDocument();
  });
});

import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import EditExperimentPage from "./EditExperimentPage";

const fixture = vi.hoisted(() => ({
  save: vi.fn(),
  navigate: vi.fn(),
  experiment: {
    id: 301,
    name: "Oferta de teste",
    hypothesis: "Entrega útil",
    platform: "FACEBOOK",
    status: "USER_STOPPED",
    experimentType: "LOW_TICKET_PRODUCT",
    campaignObjective: "SALES",
    stage: "AD",
    primaryVariable: "Promessa",
    primaryMetric: "Compra",
    singlePain: "Organizar a comunicação",
    funnelPromise: "Prazo antigo",
    primaryCta: "Comprar",
    journeyTemplateId: 401,
    unitPrice: 59,
    imagesPerPackage: 3,
    dailyBudget: 12,
    mediaSpendLimit: 0,
    baselineCvr: 0,
    targetCvr: 0,
    startDate: "2026-08-01",
    endDate: "2026-08-05",
  },
}));
vi.mock("react-router-dom", async () => ({
  ...(await vi.importActual("react-router-dom")),
  useNavigate: () => fixture.navigate,
  useParams: () => ({ id: String(fixture.experiment.id) }),
}));
vi.mock("../../api/experiment/useExperiment", () => ({
  useExperiment: () => ({ data: fixture.experiment }),
}));
vi.mock("../../api/experiment/useUpdateExperiment", () => ({
  useUpdateExperiment: () => ({ mutateAsync: fixture.save }),
}));
vi.mock("../../api/experiment/useCommercialCheckout", () => ({
  useCommercialCheckout: () => ({}),
}));
vi.mock("./ExperimentFacebookSuccessorAdoptionPanel", () => ({
  default: () => <div>Vínculo de sucessor isolado neste teste</div>,
}));
vi.mock("../../api/experiment/useMetricPresets", () => ({
  useMetricPresets: () => ({ data: [] }),
}));
vi.mock("../../api/experiment/useExperimentPlaybook", () => ({
  useExperimentPlaybook: () => ({ data: [] }),
}));
vi.mock("../../api/journey/useJourneyTemplates", () => ({
  useJourneyTemplates: () => ({
    data: { content: [{ id: 401, name: "Jornada de teste", steps: [] }] },
  }),
}));
vi.mock("../../api/ai/useImageGenerationModels", () => ({
  useImageGenerationModels: () => ({ data: [] }),
}));
vi.mock("../../api/useAllFacebookPages", () => ({
  useAllFacebookPages: () => ({ data: [] }),
}));
vi.mock("../../api/useInstagramAccounts", () => ({
  useInstagramAccounts: () => ({ data: [] }),
}));

describe("correção da promessa preservando o planejamento pendente", () => {
  beforeEach(() => {
    cleanup();
    vi.clearAllMocks();
    fixture.save.mockResolvedValue(fixture.experiment);
    vi.spyOn(window, "alert").mockImplementation(() => {});
    fixture.experiment.platform = "FACEBOOK";
    fixture.experiment.status = "USER_STOPPED";
    fixture.experiment.mediaSpendLimit = 0;
  });

  it("não oferece abordagem individual para uma nova divulgação", () => {
    render(<EditExperimentPage />);
    const select = screen.getByLabelText(/Canal de aquisição/);
    expect(Array.from((select as HTMLSelectElement).options)).toHaveLength(1);
    expect((select as HTMLSelectElement).options[0].text).toBe(
      "Instagram Ads (via Meta Ads)",
    );
  });

  it("preserva o canal direto histórico como opção desabilitada", () => {
    fixture.experiment.platform = "DIRECT_ONE_TO_ONE";
    render(<EditExperimentPage />);
    const select = screen.getByLabelText(
      /Canal de aquisição/,
    ) as HTMLSelectElement;
    const options = Array.from(select.options);
    expect(options).toHaveLength(2);
    expect(select).toHaveValue("DIRECT_ONE_TO_ONE");
    expect(options[0].disabled).toBe(true);
    expect(options[0].text).toContain("Histórico");
    expect(options[1].value).toBe("FACEBOOK");
  });

  it("salva o texto sem criar verba, meta de conversão ou reativação", async () => {
    const { container } = render(<EditExperimentPage />);
    fireEvent.change(container.querySelector('[name="funnelPromise"]')!, {
      target: { value: "Entrega no prazo aprovado" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));
    await waitFor(() => expect(fixture.save).toHaveBeenCalledOnce());
    expect(fixture.save.mock.calls[0][0]).toMatchObject({
      funnelPromise: "Entrega no prazo aprovado",
      unitPrice: 59,
    });
    // O DTO pode exibir zero para NULL persistido; campos intactos não devem voltar na request.
    const serialized = JSON.parse(
      JSON.stringify(fixture.save.mock.calls[0][0]),
    );
    for (const field of [
      "dailyBudget",
      "mediaSpendLimit",
      "baselineCvr",
      "targetCvr",
    ]) {
      expect(serialized).not.toHaveProperty(field);
    }
    expect(fixture.save.mock.calls[0][0]).not.toHaveProperty("status");
    expect(window.alert).not.toHaveBeenCalled();
  });

  it("rejeita nova verba sem teto", async () => {
    const { container } = render(<EditExperimentPage />);
    fireEvent.change(container.querySelector('[name="dailyBudget"]')!, {
      target: { value: "24" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));
    await waitFor(() =>
      expect(window.alert).toHaveBeenCalledWith(
        "Orçamento diário e teto total de mídia devem ser informados juntos",
      ),
    );
    expect(fixture.save).not.toHaveBeenCalled();
  });

  it("rejeita conversão-alvo alterada abaixo da base", async () => {
    const { container } = render(<EditExperimentPage />);
    fireEvent.change(container.querySelector('[name="baselineCvr"]')!, {
      target: { value: "6" },
    });
    fireEvent.change(container.querySelector('[name="targetCvr"]')!, {
      target: { value: "5" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));
    await waitFor(() =>
      expect(window.alert).toHaveBeenCalledWith(
        "A conversão-alvo deve ser maior que a conversão atual",
      ),
    );
    expect(fixture.save).not.toHaveBeenCalled();
  });

  it("salva as paradas financeiras que protegem margem", async () => {
    fixture.experiment.status = "PLANNED";
    fixture.experiment.mediaSpendLimit = 100;
    const { container } = render(<EditExperimentPage />);
    fireEvent.change(
      container.querySelector('[name="zeroPurchaseSpendLimit"]')!,
      { target: { value: "50" } },
    );
    fireEvent.change(container.querySelector('[name="purchaseStopCount"]')!, {
      target: { value: "2" },
    });

    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));

    await waitFor(() => expect(fixture.save).toHaveBeenCalledOnce());
    expect(fixture.save.mock.calls[0][0]).toMatchObject({
      zeroResultSpendLimit: 50,
      zeroPurchaseSpendLimit: 50,
      purchaseStopCount: 2,
    });
  });
});

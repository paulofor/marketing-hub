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
    journeyTemplateId: 401 as number | null,
    unitPrice: 59,
    imagesPerPackage: 3,
    dailyBudget: 12,
    mediaSpendLimit: 0,
    zeroPurchaseSpendLimit: 0,
    purchaseStopCount: null as number | null,
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
    fixture.experiment.id = 301;
    fixture.experiment.journeyTemplateId = 401;
    fixture.experiment.status = "USER_STOPPED";
    fixture.experiment.mediaSpendLimit = 0;
    fixture.experiment.zeroPurchaseSpendLimit = 0;
    fixture.experiment.purchaseStopCount = null;
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

  it.each([105, 302])(
    "salva a preparação do experimento %s sem escolher jornada ou liberar gasto",
    async (experimentId) => {
      fixture.experiment.id = experimentId;
      fixture.experiment.status = "PLANNED";
      fixture.experiment.journeyTemplateId = null;
      const { container } = render(<EditExperimentPage />);
      fireEvent.change(container.querySelector('[name="funnelPromise"]')!, {
        target: { value: "Kit no prazo aprovado, sem garantia de resultado" },
      });

      const save = screen.getByRole("button", { name: "Salvar" });
      expect(save).toBeEnabled();
      fireEvent.click(save);

      await waitFor(() => expect(fixture.save).toHaveBeenCalledOnce());
      const payload = JSON.parse(JSON.stringify(fixture.save.mock.calls[0][0]));
      expect(payload.funnelPromise).toBe(
        "Kit no prazo aprovado, sem garantia de resultado",
      );
      for (const field of [
        "journeyTemplateId",
        "status",
        "dailyBudget",
        "mediaSpendLimit",
        "targetCvr",
        "zeroPurchaseSpendLimit",
        "zeroResultSpendLimit",
        "purchaseStopCount",
      ]) {
        expect(payload).not.toHaveProperty(field);
      }
      expect(window.alert).not.toHaveBeenCalled();
    },
  );

  it("envia a jornada somente quando escolhida explicitamente", async () => {
    fixture.experiment.journeyTemplateId = null;
    render(<EditExperimentPage />);
    fireEvent.change(screen.getByLabelText("Template de Jornada"), {
      target: { value: "401" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));
    await waitFor(() => expect(fixture.save).toHaveBeenCalledOnce());
    expect(fixture.save.mock.calls[0][0]).toMatchObject({
      journeyTemplateId: 401,
    });
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

  it("recusa substituir uma parada financeira por zero", async () => {
    fixture.experiment.mediaSpendLimit = 100;
    fixture.experiment.zeroPurchaseSpendLimit = 50;
    fixture.experiment.purchaseStopCount = 2;
    const { container } = render(<EditExperimentPage />);
    fireEvent.change(
      container.querySelector('[name="zeroPurchaseSpendLimit"]')!,
      { target: { value: "0" } },
    );
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));
    await waitFor(() =>
      expect(window.alert).toHaveBeenCalledWith(
        "Informe uma parada sem compra válida ou deixe o campo vazio",
      ),
    );
    expect(fixture.save).not.toHaveBeenCalled();
  });
});

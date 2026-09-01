import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import DirectContactSamplePanel from "./DirectContactSamplePanel";

const mutateAsync = vi.fn();

vi.mock("../../api/experiment/useExperimentDirectContactSample", () => ({
  useExperimentDirectContactSample: vi.fn(),
  useRegisterExperimentDirectContact: () => ({
    mutateAsync,
    isPending: false,
  }),
}));

vi.mock("./DirectRecruitmentPanel", () => ({
  default: () => <div>Aquisição consentida simulada</div>,
}));

import { useExperimentDirectContactSample } from "../../api/experiment/useExperimentDirectContactSample";

const accumulatingSample = {
  experimentId: 89,
  platform: "DIRECT_ONE_TO_ONE" as const,
  experimentStatus: "RUNNING",
  targetContacts: 15,
  recordedContacts: 0,
  remainingContacts: 15,
  readyForHermesReview: false,
  operationalStatus: "ACCUMULATING_CONSENTED_SAMPLE" as const,
  contacts: [],
};

describe("DirectContactSamplePanel", () => {
  beforeEach(() => {
    cleanup();
    vi.clearAllMocks();
    vi.mocked(useExperimentDirectContactSample).mockReturnValue({
      data: accumulatingSample,
      isLoading: false,
      isError: false,
    } as unknown as ReturnType<typeof useExperimentDirectContactSample>);
    mutateAsync.mockResolvedValue(accumulatingSample);
  });

  it("expõe 0/15 e registra somente uma abordagem já consentida", async () => {
    render(
      <DirectContactSamplePanel
        experimentId={89}
        productId={9}
        processDefinitionId={66}
      />,
    );

    expect(screen.getByText("0/15 contatos")).toBeInTheDocument();
    expect(screen.getByText(/Faltam 15 contatos/)).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText(/Telefone ou e-mail do contato/), {
      target: { value: "+55 (11) 99999-9999" },
    });
    fireEvent.change(
      screen.getByLabelText(/Referência da evidência de consentimento/),
      { target: { value: "internal://consentimentos/rigel-001" } },
    );
    fireEvent.change(screen.getByLabelText(/Registrado por/), {
      target: { value: "Operador QA" },
    });
    fireEvent.click(
      screen.getByLabelText(
        "Confirmo que o contato consentiu e pertence ao público do experimento.",
      ),
    );
    fireEvent.click(
      screen.getByRole("button", { name: "Registrar contato realizado" }),
    );

    await waitFor(() => expect(mutateAsync).toHaveBeenCalledTimes(1));
    expect(mutateAsync).toHaveBeenCalledWith(
      expect.objectContaining({
        contactReference: "+55 (11) 99999-9999",
        consentEvidenceReference: "internal://consentimentos/rigel-001",
        audienceFitConfirmed: true,
        recordedBy: "Operador QA",
      }),
    );
  });

  it("orienta a revisão de Hermes quando os 15 contatos estão comprovados", () => {
    vi.mocked(useExperimentDirectContactSample).mockReturnValue({
      data: {
        ...accumulatingSample,
        recordedContacts: 15,
        remainingContacts: 0,
        readyForHermesReview: true,
        operationalStatus: "READY_FOR_HERMES_REVIEW",
      },
      isLoading: false,
      isError: false,
    } as unknown as ReturnType<typeof useExperimentDirectContactSample>);

    render(
      <DirectContactSamplePanel
        experimentId={89}
        productId={9}
        processDefinitionId={66}
      />,
    );

    expect(screen.getByText("15/15 contatos")).toBeInTheDocument();
    expect(screen.getByText(/A amostra atingiu a meta/)).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Registrar contato realizado" }),
    ).toBeDisabled();
  });
});

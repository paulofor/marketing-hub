import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  useCreateExperimentRun,
  useExperimentRunPreflight,
  useExperimentRuns,
  useRecordExperimentRunHomologation,
  useRunExperimentPreflight,
} from "../../api/experiment/useExperimentRuns";
import ExperimentRunPanel from "./ExperimentRunPanel";

vi.mock("../../api/experiment/useExperimentRuns", () => ({
  useCreateExperimentRun: vi.fn(),
  useExperimentRunPreflight: vi.fn(),
  useExperimentRuns: vi.fn(),
  useRecordExperimentRunHomologation: vi.fn(),
  useRunExperimentPreflight: vi.fn(),
}));

const gateCodes = [
  "LANDING_QUALITY_REVIEW_APPROVED",
  "CHECKOUT_AND_DELIVERY_CAN_BE_COMPLETED",
  "DIRECT_CHANNEL_READINESS_CONFIRMED",
  "DATA_FRESHNESS_VALID",
];

describe("ExperimentRunPanel", () => {
  const recordHomologation = vi.fn();

  beforeEach(() => {
    vi.resetAllMocks();
    vi.mocked(useExperimentRuns).mockReturnValue({
      data: [
        {
          id: 51,
          experimentId: 99,
          runNumber: 1,
          mode: "PRODUCTION",
          status: "PREFLIGHT_FAILED",
          evidenceValidity: "TECHNICALLY_INVALID",
          dataQualityStatus: "BLOCKED",
          stopPolicy: "MANUAL_ONLY",
          requestedAt: "2026-08-24T12:00:00Z",
        },
      ],
      isLoading: false,
    } as ReturnType<typeof useExperimentRuns>);
    vi.mocked(useExperimentRunPreflight).mockReturnValue({
      data: {
        runId: 51,
        runStatus: "PREFLIGHT_FAILED",
        hasBlockers: true,
        gates: gateCodes.map((gateCode, index) => ({
          gateCode,
          gateGroup:
            index === 0
              ? "ASSET_QUALITY"
              : index === 1
                ? "FUNCTIONAL_E2E"
                : index === 2
                  ? "DISTRIBUTION"
                  : "MEASUREMENT",
          status: "PENDING",
          severity: "WARNING",
          summary: "Evidência pendente.",
          evaluatedAt: "2026-08-24T12:01:00Z",
          evaluatorType: "DETERMINISTIC",
        })),
      },
      isLoading: false,
    } as ReturnType<typeof useExperimentRunPreflight>);
    vi.mocked(useCreateExperimentRun).mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
    } as unknown as ReturnType<typeof useCreateExperimentRun>);
    vi.mocked(useRunExperimentPreflight).mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
    } as unknown as ReturnType<typeof useRunExperimentPreflight>);
    vi.mocked(useRecordExperimentRunHomologation).mockReturnValue({
      mutate: recordHomologation,
      isPending: false,
      isError: false,
    } as unknown as ReturnType<typeof useRecordExperimentRunHomologation>);
  });

  afterEach(cleanup);

  it("registra os quatro gates funcionais somente com evidências completas", () => {
    render(<ExperimentRunPanel experimentId="99" />);

    const submit = screen.getByRole("button", {
      name: "Registrar homologação",
    });
    expect(submit).toBeDisabled();

    for (const gateCode of gateCodes) {
      fireEvent.change(screen.getByLabelText(`Resultado ${gateCode}`), {
        target: { value: "PASS" },
      });
      fireEvent.change(screen.getByLabelText(`Evidência ${gateCode}`), {
        target: { value: `evidence://${gateCode}` },
      });
      fireEvent.change(screen.getByLabelText(`Conclusão ${gateCode}`), {
        target: { value: `Gate ${gateCode} comprovado.` },
      });
    }

    expect(submit).toBeEnabled();
    fireEvent.click(submit);

    expect(recordHomologation).toHaveBeenCalledWith({
      runId: 51,
      gates: gateCodes.map((gateCode) => ({
        gateCode,
        status: "PASS",
        summary: `Gate ${gateCode} comprovado.`,
        evidenceReference: `evidence://${gateCode}`,
      })),
    });
  });
});

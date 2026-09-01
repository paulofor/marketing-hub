import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { Experiment } from "../../api/experiment/useExperiments";
import ExperimentFacebookSuccessorPanel from "./ExperimentFacebookSuccessorPanel";

const navigate = vi.fn();
const createSuccessor = vi.fn();
const readiness = vi.fn();

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual<typeof import("react-router-dom")>(
    "react-router-dom",
  );
  return { ...actual, useNavigate: () => navigate };
});

vi.mock("../../api/experiment/useFacebookSuccessor", () => ({
  useFacebookSuccessorReadiness: () => readiness(),
  useCreateFacebookSuccessor: () => ({
    mutateAsync: createSuccessor,
    isPending: false,
  }),
}));

vi.mock("../../api/useAllFacebookPages", () => ({
  useAllFacebookPages: () => ({
    data: [{ id: 1, name: "Produtividade 360" }],
    isLoading: false,
  }),
}));

vi.mock("../../api/useInstagramAccounts", () => ({
  useInstagramAccounts: () => ({
    data: [{ id: 1, handle: "@produtividade360_", name: "Produtividade 360" }],
    isLoading: false,
  }),
}));

vi.mock("react-toastify", () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

const vegaExperiment = {
  id: 90,
  name: "MUSA-H003-E001",
} as unknown as Experiment;

describe("ExperimentFacebookSuccessorPanel", () => {
  beforeEach(() => {
    cleanup();
    vi.clearAllMocks();
    readiness.mockReturnValue({
      data: { available: true, existingSuccessorId: null, blockers: [] },
      isLoading: false,
    });
    createSuccessor.mockResolvedValue({ id: 91 });
    vi.spyOn(window, "confirm").mockReturnValue(true);
  });

  it("cria o sucessor com orçamento diário e teto total separados", async () => {
    render(<ExperimentFacebookSuccessorPanel experiment={vegaExperiment} />);

    fireEvent.click(
      screen.getByRole("button", { name: "Criar sucessor Facebook" }),
    );

    expect(screen.getByLabelText("Orçamento diário (R$) *")).toHaveValue(20);
    expect(screen.getByLabelText("Teto total de mídia (R$) *")).toHaveValue(100);
    expect(screen.getByLabelText("Página do Facebook *")).toHaveValue("1");
    expect(screen.getByLabelText("Conta do Instagram *")).toHaveValue("1");

    fireEvent.click(
      screen.getByRole("button", { name: "Criar experimento com teto" }),
    );

    await waitFor(() =>
      expect(createSuccessor).toHaveBeenCalledWith(
        expect.objectContaining({
          dailyBudget: 20,
          mediaSpendLimit: 100,
          facebookPageId: 1,
          instagramAccountId: 1,
        }),
      ),
    );
    expect(window.confirm).toHaveBeenCalledWith(
      "Criar um experimento Facebook separado? Esta ação não publica campanha nem inicia gasto.",
    );
    expect(navigate).toHaveBeenCalledWith("/experiments/91");
  });

  it("direciona para o sucessor existente sem oferecer duplicação", () => {
    readiness.mockReturnValue({
      data: { available: false, existingSuccessorId: 91, blockers: [] },
      isLoading: false,
    });

    render(<ExperimentFacebookSuccessorPanel experiment={vegaExperiment} />);

    expect(
      screen.queryByRole("button", { name: "Criar sucessor Facebook" }),
    ).not.toBeInTheDocument();
    fireEvent.click(
      screen.getByRole("button", { name: "Abrir sucessor Facebook" }),
    );
    expect(navigate).toHaveBeenCalledWith("/experiments/91");
  });
});

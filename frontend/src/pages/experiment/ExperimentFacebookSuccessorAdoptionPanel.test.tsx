import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ExperimentFacebookSuccessorAdoptionPanel from "./ExperimentFacebookSuccessorAdoptionPanel";

const fixture = vi.hoisted(() => ({
  mutateAsync: vi.fn(),
}));

vi.mock("../../api/experiment/useFacebookSuccessor", () => ({
  useAdoptFacebookSuccessor: () => ({
    mutateAsync: fixture.mutateAsync,
    isPending: false,
  }),
}));

describe("adoção da superfície comercial de um sucessor Facebook", () => {
  beforeEach(() => {
    cleanup();
    vi.clearAllMocks();
    fixture.mutateAsync.mockResolvedValue({ id: 94 });
  });

  it("vincula uma origem válida sem sugerir cópia de execução", async () => {
    render(
      <ExperimentFacebookSuccessorAdoptionPanel targetExperimentId={94} />,
    );

    expect(
      screen.getByText(/campanha, métricas, público e/),
    ).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Experimento de origem"), {
      target: { value: "88" },
    });
    fireEvent.click(
      screen.getByRole("button", { name: "Reutilizar página e checkout" }),
    );

    await waitFor(() =>
      expect(fixture.mutateAsync).toHaveBeenCalledWith({
        sourceExperimentId: 88,
      }),
    );
    expect(
      await screen.findByText(/checkout foram reutilizados sem copiar/),
    ).toBeInTheDocument();
  });

  it("mantém o comando desabilitado sem uma origem positiva", () => {
    render(
      <ExperimentFacebookSuccessorAdoptionPanel targetExperimentId={94} />,
    );

    expect(
      screen.getByRole("button", { name: "Reutilizar página e checkout" }),
    ).toBeDisabled();
  });
});

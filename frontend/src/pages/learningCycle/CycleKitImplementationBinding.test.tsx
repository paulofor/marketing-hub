import {
  render,
  screen,
  fireEvent,
  waitFor,
  cleanup,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import axios from "axios";
import { describe, it, expect, vi, afterEach } from "vitest";
import type { LearningCycle } from "../../api/learningCycle/useLearningCycles";
import CycleKitImplementationBinding from "./CycleKitImplementationBinding";
import CyclePrivateKitLaunch from "./CyclePrivateKitLaunch";
vi.mock("axios", () => ({
  default: { get: vi.fn(), post: vi.fn(), isAxiosError: vi.fn() },
}));
function show(productId: number) {
  return render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <CycleKitImplementationBinding
        cycle={{ productId, id: 15, revision: 2 } as LearningCycle}
      />
    </QueryClientProvider>,
  );
}
describe("Vínculo do compositor", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });
  it("preserva outros produtos e versões sem consultar capacidade não oferecida pelo backend", () => {
    render(
      <CyclePrivateKitLaunch
        cycle={
          {
            productId: 10,
            id: 9,
            valueFlow: {
              implementation: { available: false, bindingAvailable: false },
            },
          } as LearningCycle
        }
      />,
    );
    expect(axios.get).not.toHaveBeenCalled();
    expect(screen.queryByRole("button")).toBeNull();
  });
  it.each([7, 707])(
    "preserva desenho e contexto do produto %s sem aprovar ou lançar",
    async (productId) => {
      vi.mocked(axios.get).mockResolvedValue({
        data: {
          revision: 2,
          architectureTaskId: 829,
          architectureSha256: "a".repeat(64),
          boundProfileCode: null,
          installedProfiles: ["beauty-v1"],
        },
      });
      let finish!: (value: unknown) => void;
      vi.mocked(axios.post).mockReturnValue(
        new Promise((resolve) => {
          finish = resolve;
        }),
      );
      show(productId);
      const button = await screen.findByRole("button", {
        name: "Preparar implementação por especialidade",
      });
      fireEvent.click(button);
      expect(button).toBeDisabled();
      await waitFor(() =>
        expect(axios.post).toHaveBeenCalledWith(
          expect.any(String),
          expect.objectContaining({
            productId,
            cycleId: 15,
            expectedRevision: 2,
            architectureTaskId: 829,
            architectureSha256: "a".repeat(64),
            profileCode: "beauty-v1",
          }),
          expect.any(Object),
        ),
      );
      expect(
        screen.getByText(/ilustrações de teste não aprovam/),
      ).toBeInTheDocument();
      finish({ data: {} });
      await waitFor(() => expect(button).not.toBeDisabled());
    },
  );
});

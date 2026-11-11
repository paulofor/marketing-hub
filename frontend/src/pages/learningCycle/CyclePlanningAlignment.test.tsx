import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import axios from "axios";
import { afterEach, expect, it, vi } from "vitest";
import CyclePlanningAlignment from "./CyclePlanningAlignment";
import {
  cycleApi,
  type LearningCycle,
} from "../../api/learningCycle/useLearningCycles";
vi.mock("axios", async (importOriginal) => {
  const actual = await importOriginal<typeof import("axios")>();
  return {
    default: {
      get: vi.fn(),
      post: vi.fn(),
      isAxiosError: actual.default.isAxiosError,
    },
  };
});
afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});
const cycle = {
  id: 22,
  productId: 4,
  stage: "PLANNING",
  status: "OPEN",
  revision: 1,
} as LearningCycle;
const preview = {
  available: true,
  reason: "Sem cobrança, anúncios ou vídeos.",
  cycleRevision: 1,
  bigIdeaRevision: 3,
  currentOffer: "R$67, pagamento único e 90 dias",
  proposedOffer:
    "Aplicativo contínuo, base gratuita e premium opcional. Preço a definir com Plutus.",
  proposedPriceBrl: null,
};
function mount(value = cycle) {
  const updated = vi.fn();
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <CyclePlanningAlignment cycle={value} onUpdated={updated} />
    </QueryClientProvider>,
  );
  return updated;
}
it("mostra a oferta oficial e envia só a reconciliação com a revisão exibida", async () => {
  vi.mocked(axios.get).mockResolvedValue({ data: preview });
  vi.mocked(axios.post).mockResolvedValue({ data: { ...cycle, revision: 2 } });
  const updated = mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Alinhar à definição registrada",
    }),
  );
  await waitFor(() =>
    expect(updated).toHaveBeenCalledWith({ ...cycle, revision: 2 }),
  );
  expect(axios.post).toHaveBeenCalledWith(
    `${cycleApi}/products/4/22/planning-alignment`,
    {
      requestKey: expect.any(String),
      expectedRevision: 1,
      expectedBigIdeaRevision: 3,
      operatorName: expect.any(String),
      reason: expect.any(String),
    },
  );
  expect(screen.getByText(/Preço a definir com Plutus/)).toBeTruthy();
});
it("não oferece ação quando o backend recusa ou o ciclo é histórico", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: { ...preview, available: false },
  });
  mount();
  await waitFor(() => expect(axios.get).toHaveBeenCalled());
  expect(screen.queryByRole("button")).toBeNull();
  cleanup();
  vi.clearAllMocks();
  mount({ ...cycle, status: "CLOSED" });
  expect(axios.get).not.toHaveBeenCalled();
  expect(axios.post).not.toHaveBeenCalled();
});
it("impede duplo envio e mostra erro recuperável sem retomar agentes", async () => {
  vi.mocked(axios.get).mockResolvedValue({ data: preview });
  let reject!: (value: unknown) => void;
  vi.mocked(axios.post).mockReturnValue(
    new Promise((_, no) => {
      reject = no;
    }),
  );
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Alinhar à definição registrada",
    }),
  );
  const pending = await screen.findByRole("button", { name: "Alinhando…" });
  expect((pending as HTMLButtonElement).disabled).toBe(true);
  reject({
    isAxiosError: true,
    response: { data: { message: "O ciclo mudou; atualize a tela." } },
  });
  expect(await screen.findByRole("alert")).toHaveProperty(
    "textContent",
    "O ciclo mudou; atualize a tela.",
  );
  expect(axios.post).toHaveBeenCalledTimes(1);
});

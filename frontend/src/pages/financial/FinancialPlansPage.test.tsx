import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import FinancialPlansPage from "./FinancialPlansPage";
import FinancialPlanPlutusDetails from "./FinancialPlanPlutusDetails";
import projectionText from "../../../../backend/ads-service/src/test/resources/financial-plan/plutus-response.json?raw";
import { costLabels } from "../../api/financial/useFinancialPlans";
const mocks = vi.hoisted(() => ({
  history: { data: [] as unknown[], isLoading: false, isError: false },
  mutation: {
    mutateAsync: vi.fn(),
    isPending: false,
    isError: false,
    error: null,
    reset: vi.fn(),
  },
}));
vi.mock("../../api/financial/useFinancialPlans", async (importOriginal) => ({
  ...(await importOriginal<
    typeof import("../../api/financial/useFinancialPlans")
  >()),
  useFinancialPlanCatalog: () => ({
    data: {
      products: [{ id: 51, name: "Produto local", productTypeId: 1 }],
      productTypes: [{ id: 1, name: "Tipo local" }],
      commercialPlans: [{ id: 7, name: "Plano sintético" }],
    },
  }),
  useFinancialPlans: (scope: string) =>
    scope === "products"
      ? mocks.history
      : { data: [], isLoading: false, isError: false },
  useSaveFinancialPlan: () => mocks.mutation,
  useAnalyzeFinancialPlan: () => ({
    isPending: false,
    isError: false,
    error: null,
    reset: vi.fn(),
    mutate: vi.fn(),
  }),
}));
function page(path = "/financial/plans?productId=51") {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <FinancialPlansPage />
    </MemoryRouter>,
  );
}
beforeEach(() => {
  vi.clearAllMocks();
  mocks.history.data = [];
  mocks.history.isError = false;
  mocks.history.isLoading = false;
  mocks.mutation.isPending = false;
  mocks.mutation.isError = false;
});
afterEach(cleanup);
describe("Plano financeiro", () => {
  it("apresenta limites, decisões e premissas completas do parecer de Plutus", () => {
    const result = JSON.parse(projectionText);
    render(
      <FinancialPlanPlutusDetails
        result={result}
        money={(v) => (v == null ? "Não informado" : `R$ ${v}`)}
        percent={(v) => (v == null ? "Indisponível" : `${v}%`)}
      />,
    );
    expect(
      screen.getByRole("region", { name: "Recomendações de Plutus" }),
    ).toHaveTextContent("R$ 200");
    for (const key of ["continue", "adjust", "stop"])
      expect(
        screen.getByText(result.decisionCriteria[key][0]),
      ).toBeInTheDocument();
    expect(
      screen.getByText(result.scenarios[0].assumptions[1]),
    ).toBeInTheDocument();
    expect(screen.getAllByText("Não informado")).toHaveLength(3);
    expect(screen.getAllByText("Indisponível")).toHaveLength(6);
  });
  it("preserva limites recomendados ausentes sem convertê-los em zero", () => {
    const result = {
      ...JSON.parse(projectionText),
      recommendedInitialInvestmentBrl: null,
      recommendedCycleLimitBrl: null,
    };
    render(
      <FinancialPlanPlutusDetails
        result={result}
        money={(v) => (v == null ? "Não informado" : `R$ ${v}`)}
        percent={(v) => (v == null ? "Indisponível" : `${v}%`)}
      />,
    );
    expect(screen.getAllByText("Não informado")).toHaveLength(5);
    expect(screen.queryByText("R$ 0")).not.toBeInTheDocument();
  });
  it("oferece produto ou modelo e preserva navegação aos checkpoints", () => {
    page();
    expect(
      screen.getByRole("link", { name: "Ficha e checkpoints da cadeia" }),
    ).toHaveAttribute("href", "/products/51/execution-profiles");
    expect(
      screen.getByRole("button", { name: "Criar plano do produto" }),
    ).toBeEnabled();
  });
  it("mantém números ausentes nulos no rascunho", async () => {
    mocks.mutation.mutateAsync.mockRejectedValue(new Error("fixture"));
    page();
    fireEvent.click(
      screen.getByRole("button", { name: "Criar plano do produto" }),
    );
    fireEvent.change(screen.getByLabelText("Nome do plano"), {
      target: { value: "Plano inicial" },
    });
    fireEvent.change(screen.getByLabelText("Responsável pelo registro"), {
      target: { value: "Operador local" },
    });
    fireEvent.change(screen.getByLabelText("Versão do produto"), {
      target: { value: "v1" },
    });
    fireEvent.change(screen.getByLabelText("Plano comercial"), {
      target: { value: "7" },
    });
    fireEvent.change(screen.getByLabelText("Validade das premissas"), {
      target: { value: "2026-12-31" },
    });
    fireEvent.change(
      screen.getByLabelText(
        "Premissas, fontes e justificativa dos valores zero",
      ),
      { target: { value: "Custos ainda sob pesquisa" } },
    );
    fireEvent.submit(
      screen
        .getByRole("button", { name: "Salvar revisão e calcular" })
        .closest("form")!,
    );
    await waitFor(() =>
      expect(mocks.mutation.mutateAsync).toHaveBeenCalledOnce(),
    );
    const body = mocks.mutation.mutateAsync.mock.calls[0][0];
    expect(body.assumptions.priceBrl).toBeNull();
    for (const field of Object.keys(costLabels))
      expect(body.assumptions.costs[field]).toBeNull();
    expect(body.assumptions.scenarios[0].customers).toBeNull();
    expect(screen.getByLabelText("Nome do plano")).toHaveValue("Plano inicial");
  });
  it("bloqueia interação durante gravação sem perder as premissas", () => {
    const r = page();
    fireEvent.click(
      screen.getByRole("button", { name: "Criar plano do produto" }),
    );
    mocks.mutation.isPending = true;
    r.rerender(
      <MemoryRouter initialEntries={["/financial/plans?productId=51"]}>
        <FinancialPlansPage />
      </MemoryRouter>,
    );
    expect(screen.getByRole("button", { name: "Salvando..." })).toBeDisabled();
    expect(screen.getByLabelText("Nome do plano")).toBeDisabled();
  });
  it("exibe falha de consulta sem apresentar zero como lucro", () => {
    mocks.history.isError = true;
    page();
    expect(screen.getByRole("alert")).toHaveTextContent(
      "Não foi possível carregar o plano financeiro",
    );
    expect(screen.queryByText("R$ 0,00")).not.toBeInTheDocument();
  });
  it("permite cadastrar modelo do tipo sem vincular outro produto", () => {
    page("/financial/plans?typeId=1");
    fireEvent.click(
      screen.getByRole("button", { name: "Criar modelo do tipo" }),
    );
    expect(screen.queryByLabelText("Plano comercial")).not.toBeInTheDocument();
    expect(
      screen.queryByLabelText("Versão do produto"),
    ).not.toBeInTheDocument();
  });
});

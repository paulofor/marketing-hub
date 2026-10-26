import { render, screen, fireEvent, cleanup } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import ProductModelingPage from "./ProductModelingPage";
const f = vi.hoisted(() => ({
  mutate: vi.fn(),
  reset: vi.fn(),
  items: [] as unknown[],
}));
vi.mock("../../api/product/useProducts", () => ({
  useProducts: () => ({ data: [{ id: 99101, internalName: "Estrela" }] }),
}));
vi.mock("../../api/product/useProductModeling", () => ({
  useProductModelings: () => ({ data: f.items }),
  useModelingAction: () => ({
    mutate: f.mutate,
    reset: f.reset,
    isPending: false,
    isSuccess: false,
  }),
}));
const item = {
  id: 91,
  productId: 99101,
  productName: "Estrela",
  desiredOutcome: "Resultado útil",
  referenceUrls: ["https://reference.example/"],
  status: "DRAFT",
  researchScope: "INTERNATIONAL_TO_BR_V1",
  targetMarket: "Brasil",
  guidance: "Preparada, sem execução",
  researchUrl: "/product-discovery/cycles/41",
  stages: [],
  availableCycles: [],
  bigIdeaSnapshot: { centralIdea: "Direção preservada" },
};
describe("modelagem por referências", () => {
  afterEach(cleanup);
  beforeEach(() => {
    f.items = [];
    f.mutate.mockClear();
    f.reset.mockClear();
  });
  it("salva somente a preparação e preserva produto selecionado", () => {
    render(
      <MemoryRouter initialEntries={["/product-modeling?productId=99101"]}>
        <ProductModelingPage />
      </MemoryRouter>,
    );
    fireEvent.change(screen.getByLabelText(/O que a cliente/), {
      target: { value: "Montar combinação própria" },
    });
    fireEvent.change(screen.getByLabelText(/Links das referências/), {
      target: { value: "https://reference.example/\nhttps://other.example/" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Salvar preparação" }));
    expect(f.mutate).toHaveBeenCalledOnce();
    expect(f.mutate.mock.calls[0][0]).toMatchObject({
      productId: 99101,
      newProductName: null,
      desiredOutcome: "Montar combinação própria",
      referenceUrls: ["https://reference.example/", "https://other.example/"],
    });
    expect(f.mutate.mock.calls[0][0]).not.toHaveProperty("start");
  });
  it("explica consumo antes do comando de pesquisa", () => {
    f.items = [item];
    render(
      <MemoryRouter>
        <ProductModelingPage />
      </MemoryRouter>,
    );
    fireEvent.click(
      screen.getByRole("button", { name: "Preparar início da pesquisa" }),
    );
    expect(f.mutate).not.toHaveBeenCalled();
    expect(
      screen.getByText("Referências internacionais → Brasil"),
    ).toBeInTheDocument();
    expect(screen.getByText(/pode consumir IA/)).toBeInTheDocument();
    fireEvent.click(
      screen.getByRole("button", { name: "Iniciar pesquisa com Argos" }),
    );
    expect(f.mutate.mock.calls[0][0]).toEqual({ start: 91 });
  });
  it("vincula a preparação elegível sem disparar o processo", () => {
    f.items = [
      {
        ...item,
        status: "COMPLETED",
        availableCycles: [{ cycleId: 201, productVersion: "v2" }],
      },
    ];
    render(
      <MemoryRouter>
        <ProductModelingPage />
      </MemoryRouter>,
    );
    fireEvent.click(screen.getByRole("button", { name: /Usar no ciclo #201/ }));
    expect(f.mutate.mock.calls[0][0]).toEqual({ adopt: 91, cycleId: 201 });
    expect(screen.getByText(/não inicia agentes/)).toBeInTheDocument();
  });
  it("retoma somente quando o backend comprova a reconexão, após confirmação de consumo", () => {
    f.items = [
      {
        ...item,
        status: "FAILED",
        researchRecovery: {
          available: true,
          reconnectionId: 25,
          reason: "Reconexão confirmada; histórico preservado.",
        },
      },
    ];
    render(
      <MemoryRouter>
        <ProductModelingPage />
      </MemoryRouter>,
    );
    fireEvent.click(
      screen.getByRole("button", { name: "Retomar pesquisa com Argos" }),
    );
    expect(f.mutate).not.toHaveBeenCalled();
    expect(screen.getByText(/pode consumir IA/)).toBeInTheDocument();
    fireEvent.click(
      screen.getByRole("button", { name: "Confirmar retomada da pesquisa" }),
    );
    expect(f.mutate.mock.calls[0][0]).toEqual({ resume: 91 });
    expect(
      screen.queryByRole("button", { name: "Iniciar pesquisa com Argos" }),
    ).toBeNull();
  });
  it("não deduz retomada apenas por falha e apresenta a causa fornecida pelo backend", () => {
    f.items = [
      {
        ...item,
        status: "FAILED",
        researchRecovery: {
          available: false,
          reconnectionId: null,
          reason: "Há resultado preservado; recupere o recibo.",
        },
      },
    ];
    render(
      <MemoryRouter>
        <ProductModelingPage />
      </MemoryRouter>,
    );
    expect(
      screen.queryByRole("button", { name: "Retomar pesquisa com Argos" }),
    ).toBeNull();
    expect(screen.getByText(/Há resultado preservado/)).toBeInTheDocument();
    expect(f.mutate).not.toHaveBeenCalled();
  });
  it("mostra adaptação e fonte sem declarar demanda local comprovada", () => {
    f.items = [
      {
        ...item,
        internationalAssessment: {
          references: [
            {
              referenceUrl: "https://reference.example/",
              sourceUrl: "https://reference.example/pricing",
              originMarket: "Reino Unido",
              observedEvidence: "O vendedor anuncia um plano pago",
              limitation: "Receita não auditada",
            },
          ],
          brazilAdaptation: {
            recognizedDesire: "Combinações com a sua cara",
            firstValue: "Uma orientação antes do cadastro",
            paidValue: "Jornada própria de sete dias",
          },
        },
      },
    ];
    render(
      <MemoryRouter>
        <ProductModelingPage />
      </MemoryRouter>,
    );
    expect(
      screen.getByRole("region", { name: "Adaptação ao Brasil" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Receita não auditada")).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Fonte · Reino Unido" }),
    ).toHaveAttribute("href", "https://reference.example/pricing");
    expect(f.mutate).not.toHaveBeenCalled();
  });
});

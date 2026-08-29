import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import ProductTypeCatalogPage from "./ProductTypeCatalogPage";

vi.mock("axios");

const pwaBlueprint = {
  version: "consultant-pwa-v1",
  primaryChannel: "PWA",
  customerJob: "Receber orientação pessoal no celular.",
  valueMechanism: "Transformar contexto em recomendação.",
  experienceFlow: "Entrar; conversar; refinar; avaliar.",
  requiredInputs: "Cliente, contexto, consentimento e foto opcional.",
  expectedOutputs: "Orientação, motivo e próximo passo.",
  memoryStrategy: "Memória segregada por cliente.",
  integrationRequirements: "Backend PDE, worker Java e App Server.",
  safetyGuardrails: "Bloquear mistura de clientes e mídia sem consentimento.",
  successMetrics: "Orientação, utilidade, retorno, venda e margem.",
  backendSdkModule: "pde-platform/pde-harness-sdk",
  frontendSdkModule: "pde-platform/frontend/src/consultant-sdk/v1",
};

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <BrowserRouter>
        <ProductTypeCatalogPage />
      </BrowserRouter>
    </QueryClientProvider>,
  );
}

describe("ProductTypeCatalogPage", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });
  afterEach(cleanup);

  it("shows canonical types, aliases and product usage", async () => {
    (axios.get as any).mockResolvedValue({
      data: [
        {
          id: 1,
          code: "PDE",
          name: "PDE - Produto Digital Experiencial",
          internalName: "Opala",
          description: "Jornada de valor observável.",
          aliases: ["PDE", "Experiência guiada"],
          status: "ACTIVE",
          blueprint: null,
          constructionReady: false,
          missingBlueprintFields: ["Versão da base"],
          productCount: 4,
        },
      ],
    });

    renderPage();

    expect(
      await screen.findByText("PDE - Produto Digital Experiencial"),
    ).toBeTruthy();
    expect(screen.getByText("Mineral: Opala")).toBeTruthy();
    expect(screen.getByText("4")).toBeTruthy();
    const aliases = screen.getByLabelText(
      "Apelidos de PDE - Produto Digital Experiencial",
    );
    expect(within(aliases).getByText("Experiência guiada")).toBeTruthy();
    expect(screen.getAllByText("Em uso").length).toBeGreaterThan(0);
  });

  it("creates a proposed type without limiting the new idea", async () => {
    (axios.get as any).mockResolvedValue({ data: [] });
    (axios.post as any).mockResolvedValue({
      data: {
        id: 12,
        code: "EXPERIENCIA_IMERSIVA",
        name: "Experiência imersiva",
        internalName: "Granada",
        aliases: ["Produto imersivo"],
        status: "PROPOSED",
        blueprint: null,
        constructionReady: false,
        missingBlueprintFields: ["Versão da base"],
        productCount: 0,
      },
    });
    renderPage();
    await screen.findByText("Nenhum tipo corresponde à busca.");

    fireEvent.change(screen.getByLabelText("Nome canônico *"), {
      target: { value: "Experiência imersiva" },
    });
    fireEvent.change(screen.getByLabelText("Nome interno (mineral) *"), {
      target: { value: "Granada" },
    });
    fireEvent.change(screen.getByLabelText("Apelidos internos"), {
      target: { value: "Produto imersivo" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Cadastrar tipo" }));

    await waitFor(() =>
      expect(axios.post).toHaveBeenCalledWith("/api/product-types", {
        code: undefined,
        name: "Experiência imersiva",
        internalName: "Granada",
        description: undefined,
        aliases: ["Produto imersivo"],
        status: "PROPOSED",
        blueprint: null,
      }),
    );
  });

  it("shows and edits the detailed construction blueprint", async () => {
    const type = {
      id: 7,
      code: "AI_PWA_CONSULTANT_PRODUCT",
      name: "Consultor PWA com IA",
      internalName: "Turmalina",
      description: "Consultoria visual mobile-first.",
      aliases: ["Consultor PWA"],
      status: "ACTIVE" as const,
      blueprint: pwaBlueprint,
      constructionReady: true,
      missingBlueprintFields: [],
      productCount: 0,
    };
    (axios.get as any).mockResolvedValue({ data: [type] });
    (axios.put as any).mockResolvedValue({ data: type });

    renderPage();

    expect(await screen.findByText("Consultor PWA com IA")).toBeTruthy();
    expect(screen.getByText("Base pronta")).toBeTruthy();
    expect(
      screen.getByText("Receber orientação pessoal no celular."),
    ).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Editar tipo" }));
    expect(screen.getByLabelText("Versão da base *")).toHaveValue(
      "consultant-pwa-v1",
    );
    fireEvent.change(screen.getByLabelText("Métricas de sucesso *"), {
      target: { value: "Orientação, utilidade, retorno, receita e margem." },
    });
    fireEvent.click(screen.getByRole("button", { name: "Salvar alterações" }));

    await waitFor(() =>
      expect(axios.put).toHaveBeenCalledWith(
        "/api/product-types/7",
        expect.objectContaining({
          code: "AI_PWA_CONSULTANT_PRODUCT",
          blueprint: expect.objectContaining({
            primaryChannel: "PWA",
            successMetrics: "Orientação, utilidade, retorno, receita e margem.",
          }),
        }),
      ),
    );
  });

  it("shows a useful failure when the catalog cannot be loaded", async () => {
    (axios.get as any).mockRejectedValue(new Error("offline"));
    renderPage();

    expect(
      await screen.findByText("Não foi possível carregar o catálogo de tipos."),
    ).toBeTruthy();
  });
});

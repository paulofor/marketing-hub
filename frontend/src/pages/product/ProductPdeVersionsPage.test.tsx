import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import axios from "axios";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import ProductPdeVersionsPage, {
  defaultPdeSlotForm,
} from "./ProductPdeVersionsPage";

vi.mock("axios");

describe("ProductPdeVersionsPage", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it("sugere slot corporativo neutro para o Kit WhatsApp Pronto", () => {
    expect(
      defaultPdeSlotForm({
        slug: "kit-whatsapp-pronto",
        pdeExperienceJson: JSON.stringify({ layoutKey: "assisted-service-v1" }),
      }),
    ).toMatchObject({
      slotCode: "v1",
      domain: "kit-whatsapp-pronto.digicomdigital.com.br",
      experienceVersion: "kit-whatsapp-pronto-pde-v1",
      layoutKey: "assisted-service-v1",
    });
  });

  it("preserva os padrões históricos do MUSA", () => {
    expect(defaultPdeSlotForm({ slug: "metodo-musa-7-dias" })).toMatchObject({
      slotCode: "v2",
      domain: "v2.clubemusa.com.br",
      experienceVersion: "musa-pde-entry-v5-estrada-desejo",
    });
  });

  it("sugere a candidata comercial exata de Mira sem autorizar mídia", () => {
    expect(defaultPdeSlotForm({ slug: "pde-planejado-36" })).toMatchObject({
      slotCode: "v1",
      domain: "mira.digicomdigital.com.br",
      backendUrl: "https://mira.digicomdigital.com.br/api",
      experienceVersion: "mira-commercial-v1",
      layoutKey: "mira-routine-v1",
      sourceExperimentId: "93",
      status: "CANDIDATE",
      notes: expect.stringContaining("mídia permanece desligada"),
    });
  });

  it("permite reconciliar a URL do backend de um slot já publicado", async () => {
    const slot = {
      id: 9,
      slotCode: "v1",
      productSlug: "pde-planejado-36",
      domain: "mira.digicomdigital.com.br",
      publicUrl: "https://mira.digicomdigital.com.br",
      backendUrl: null,
      experienceVersion: "mira-commercial-v1",
      layoutKey: "mira-routine-v1",
      targetEnvironment: "production-v1",
      status: "ACTIVE",
      sourceExperimentId: null,
      draftExperienceJson: '{"slug":"pde-planejado-36"}',
      publishedExperienceJson: '{"slug":"pde-planejado-36"}',
    };
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/products/10") {
        return Promise.resolve({
          data: { id: 10, slug: "pde-planejado-36", name: "Mira" },
        });
      }
      if (url === "/api/products/10/pde-production-slots") {
        return Promise.resolve({ data: [slot] });
      }
      if (url === "/api/products/10/pde-versions") {
        return Promise.resolve({ data: [] });
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`));
    });
    (axios.post as any).mockResolvedValue({
      data: {
        ...slot,
        backendUrl: "https://mira.digicomdigital.com.br/api",
      },
    });
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    render(
      <QueryClientProvider client={client}>
        <MemoryRouter initialEntries={["/products/10/pde-versions"]}>
          <Routes>
            <Route
              path="/products/:productId/pde-versions"
              element={<ProductPdeVersionsPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const editor = await screen.findByText("Editor/publicador de contrato PDE");
    const card = editor.closest(".card") as HTMLElement;
    await waitFor(() =>
      expect(screen.getByLabelText("Domínio *")).toHaveValue(
        "mira.digicomdigital.com.br",
      ),
    );
    expect(screen.getByLabelText("Versão PDE *")).toHaveValue(
      "mira-commercial-v1",
    );
    expect(
      screen.getByLabelText("URL do backend de acesso e eventos", {
        selector: "#pde-slot-backend-url",
      }),
    ).toHaveValue("https://mira.digicomdigital.com.br/api");
    await waitFor(() =>
      expect(within(card).getByLabelText("Slot *")).toHaveValue("v1"),
    );
    const backendUrl = within(card).getByLabelText(
      "URL do backend de acesso e eventos",
    );
    fireEvent.change(backendUrl, {
      target: { value: "https://mira.digicomdigital.com.br/api" },
    });
    await waitFor(() =>
      expect(backendUrl).toHaveValue("https://mira.digicomdigital.com.br/api"),
    );
    fireEvent.click(
      within(card).getByRole("button", { name: "Salvar rascunho" }),
    );

    await waitFor(() =>
      expect(axios.post).toHaveBeenCalledWith(
        "/api/products/10/pde-production-slots",
        expect.objectContaining({
          slotCode: "v1",
          backendUrl: "https://mira.digicomdigital.com.br/api",
          status: "ACTIVE",
        }),
      ),
    );
  });

  it("shows the Opala version lifecycle and its actionable pending items", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/products/4") {
        return Promise.resolve({
          data: {
            id: 4,
            slug: "metodo-musa-7-dias",
            name: "Vega",
            productTypeCode: "PDE",
            productTypeInternalName: "Opala",
          },
        });
      }
      if (url === "/api/products/4/pde-production-slots") {
        return Promise.resolve({ data: [] });
      }
      if (url === "/api/products/4/pde-versions") {
        return Promise.resolve({
          data: [
            {
              id: 12,
              slotCode: "v8",
              name: "Vega com vídeo de apresentação",
              experienceVersion: "musa-pde-entry-v12-primeiro-ajuste-aplicavel",
              lifecycleStage: "CANDIDATE",
              lifecycleLabel: "Candidata",
              operationalStatus: "CANDIDATE",
              hypothesis:
                "Vídeo de apresentação aumenta o avanço para o primeiro valor",
              primaryChange: "Inclui vídeo de apresentação na entrada",
              sourceExperimentId: 92,
              sourceExperimentName: "Vega · vídeo de apresentação",
              sourceExperimentStatus: "PLANNED",
              videoCount: 2,
              approvedVideoCount: 2,
              priceBrl: 67,
              primaryCta: "Começar agora",
              checkoutUrl: "https://checkout.example/v12",
              publicUrl: "https://v8.clubemusa.com.br",
              validationStatus: "OK",
              validationSummary: "URL produtiva validada",
              homologationSummary:
                "Testes técnicos aprovados; homologação comercial ainda pendente.",
              publishedContract: false,
              canPreparePublication: true,
              canPublishContract: false,
              pendingItems: ["Concluir a homologação comercial da v12."],
              lifecycle: [
                { code: "CONCEPT", label: "Conceito", status: "DONE" },
                {
                  code: "HOMOLOGATION",
                  label: "Homologação",
                  status: "CURRENT",
                },
                {
                  code: "PUBLICATION",
                  label: "Publicação",
                  status: "PENDING",
                },
              ],
              updatedAt: "2026-09-16T12:00:00Z",
            },
          ],
        });
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`));
    });
    (axios.post as any).mockResolvedValue({
      data: { slotCode: "v8", status: "READY" },
    });

    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    render(
      <QueryClientProvider client={client}>
        <MemoryRouter initialEntries={["/products/4/pde-versions"]}>
          <Routes>
            <Route
              path="/products/:productId/pde-versions"
              element={<ProductPdeVersionsPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const version = await screen.findByRole("article", {
      name: "Versão PDE v8",
    });
    expect(within(version).getByText("Candidata")).toBeTruthy();
    expect(
      within(version).getByText(/Vídeo de apresentação aumenta/),
    ).toBeTruthy();
    expect(within(version).getByText("R$ 67,00")).toBeTruthy();
    expect(within(version).getAllByText("Homologação")).toHaveLength(2);
    expect(
      within(version).getByText("Concluir a homologação comercial da v12."),
    ).toBeTruthy();
    fireEvent.click(
      within(version).getByRole("button", {
        name: "Preparar para publicação",
      }),
    );
    await waitFor(() =>
      expect(axios.post).toHaveBeenCalledWith(
        "/api/products/4/pde-production-slots/v8/prepare-publication",
      ),
    );
    expect(
      within(version).getByRole("link", { name: "Abrir pré-visualização" }),
    ).toHaveAttribute("target", "_blank");
  });
});

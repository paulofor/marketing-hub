import { cleanup, render, screen, within } from "@testing-library/react";
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
              slotCode: "v12",
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
              publicUrl: "https://v12.clubemusa.com.br",
              validationStatus: "OK",
              validationSummary: "URL produtiva validada",
              homologationSummary:
                "Testes técnicos aprovados; homologação comercial ainda pendente.",
              publishedContract: false,
              pendingItems: ["Concluir a homologação comercial."],
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
      name: "Versão PDE v12",
    });
    expect(within(version).getByText("Candidata")).toBeTruthy();
    expect(
      within(version).getByText(/Vídeo de apresentação aumenta/),
    ).toBeTruthy();
    expect(within(version).getByText("R$ 67,00")).toBeTruthy();
    expect(within(version).getAllByText("Homologação")).toHaveLength(2);
    expect(
      within(version).getByText("Concluir a homologação comercial."),
    ).toBeTruthy();
    expect(
      within(version).queryByRole("link", {
        name: "Preparar para publicação",
      }),
    ).toBeNull();
    expect(
      within(version).getByRole("link", { name: "Abrir pré-visualização" }),
    ).toHaveAttribute("target", "_blank");
  });
});

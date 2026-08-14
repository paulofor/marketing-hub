import "@testing-library/jest-dom/vitest";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import CriativosTab from "./CriativosTab";
import axios from "axios";

vi.mock("axios");

describe("CriativosTab", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it("separa conceitos, revisões e candidatos finais no resumo do portfólio", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url.endsWith("/products/experiments/1/ads-in-use")) {
        return Promise.resolve({
          data: {
            ads: [
              {
                creativeId: 10,
                experimentId: 1,
                sourceCreativeId: null,
                versionNumber: 1,
                finalCandidate: false,
                headline: "Conceito A",
                status: "DRAFT",
              },
              {
                creativeId: 11,
                experimentId: 1,
                sourceCreativeId: 10,
                versionNumber: 2,
                finalCandidate: true,
                headline: "Conceito A aprovado",
                status: "READY",
              },
              {
                creativeId: 20,
                experimentId: 1,
                sourceCreativeId: null,
                versionNumber: 1,
                finalCandidate: false,
                headline: "Conceito B",
                status: "DRAFT",
              },
              {
                creativeId: 21,
                experimentId: 1,
                sourceCreativeId: 20,
                versionNumber: 2,
                finalCandidate: false,
                headline: "Conceito B revisão",
                status: "DRAFT",
              },
              {
                creativeId: 22,
                experimentId: 1,
                sourceCreativeId: 21,
                versionNumber: 3,
                finalCandidate: false,
                headline: "Conceito B ajuste",
                status: "DRAFT",
              },
            ],
          },
        });
      }
      if (url.endsWith("/experiments/1")) {
        return Promise.resolve({ data: { creativesToGenerate: 0 } });
      }
      return Promise.resolve({ data: [] });
    });

    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <CriativosTab experimentId="1" />
      </QueryClientProvider>,
    );

    const summary = await screen.findByLabelText(
      "Resumo das linhagens criativas",
    );
    expect(summary).toHaveTextContent("2Conceitos originais");
    expect(summary).toHaveTextContent("3Revisões acumuladas");
    expect(summary).toHaveTextContent("1Candidatos finais");
    expect(summary).toHaveTextContent(
      "Total técnico: 5 registros = 2 conceitos + 3 revisões",
    );
  });

  it("shows preview", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url.endsWith("/products/experiments/1/ads-in-use")) {
        return Promise.resolve({
          data: [
            {
              id: 42,
              headline: "H1",
              primaryText: "P1",
              imageUrl: "img.jpg",
              status: "READY",
              agentReviewStatus: "APPROVED",
            },
            {
              id: 43,
              headline: "H2",
              primaryText: "P2",
              imageUrl: "img-2.jpg",
              status: "DRAFT",
              agentReviewStatus: "REJECTED",
            },
          ],
        });
      }
      if (url.endsWith("/experiments/1")) {
        return Promise.resolve({ data: { creativesToGenerate: 0 } });
      }
      return Promise.resolve({ data: [] });
    });
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <CriativosTab experimentId="1" />
      </QueryClientProvider>,
    );
    expect(await screen.findByText("Agente: aprovado")).toBeInTheDocument();
    expect(await screen.findByText("Agente: reprovado")).toBeInTheDocument();
    (await screen.findAllByLabelText("Preview"))[0].click();
    await screen.findByText("Patrocinado");
  });

  it("shows the auditable Codex improvement cycle and its blocking reason", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url.endsWith("/products/experiments/1/ads-in-use")) {
        return Promise.resolve({
          data: {
            ads: [
              {
                creativeId: 88,
                experimentId: 1,
                headline: "Agenda cheia",
                primaryText: "Texto",
                imageUrl: "creative.jpg",
                status: "DRAFT",
                agentReviewStatus: "REJECTED",
                agentImprovementStatus: "LIMIT_REACHED",
                agentImprovementAttempts: 3,
                agentImprovementError:
                  "Limite de três correções automáticas atingido",
              },
            ],
          },
        });
      }
      if (url.endsWith("/experiments/1")) {
        return Promise.resolve({ data: { creativesToGenerate: 0 } });
      }
      return Promise.resolve({ data: [] });
    });

    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <CriativosTab experimentId="1" />
      </QueryClientProvider>,
    );

    expect(
      await screen.findByText("Codex: limite de 8 correções atingido"),
    ).toBeInTheDocument();
    expect(
      await screen.findByText(/Limite de três correções automáticas atingido/),
    ).toBeInTheDocument();
  });

  it("replaces a blocked generated image with real product proof and resubmits it to Temis", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url.endsWith("/products/experiments/1/ads-in-use")) {
        return Promise.resolve({
          data: [
            {
              id: 311,
              experimentId: 1,
              headline: "Veja sua agenda organizada",
              primaryText: "Conheça o Agenda Cheia",
              imageUrl: "generated.jpg",
              destinationUrl: "https://agenda-cheia.test",
              status: "DRAFT",
              agentReviewStatus: "REJECTED",
              agentImprovementStatus: "LIMIT_REACHED",
              agentImprovementAttempts: 8,
            },
          ],
        });
      }
      if (url.endsWith("/experiments/1")) {
        return Promise.resolve({ data: { creativesToGenerate: 0 } });
      }
      return Promise.resolve({ data: [] });
    });
    (axios.post as any).mockImplementation((url: string) => {
      if (url === "/api/assets") {
        return Promise.resolve({ data: { url: "/uploads/product-proof.png" } });
      }
      if (url === "/api/creatives/311/versions") {
        return Promise.resolve({ data: { id: 312 } });
      }
      if (url === "/api/creatives/312/agent-review/request") {
        return Promise.resolve({
          data: { id: 312, agentReviewStatus: "PENDING" },
        });
      }
      return Promise.resolve({ data: {} });
    });

    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <CriativosTab experimentId="1" />
      </QueryClientProvider>,
    );

    await userEvent.click(
      await screen.findByRole("button", { name: "Usar prova real do produto" }),
    );
    const file = new File(["proof"], "agenda-cheia.png", { type: "image/png" });
    await userEvent.upload(
      screen.getByLabelText("Imagem real do produto"),
      file,
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Criar versão e enviar para Têmis" }),
    );

    await waitFor(() =>
      expect(axios.post).toHaveBeenCalledWith(
        "/api/creatives/311/versions",
        expect.objectContaining({ imageUrl: "/uploads/product-proof.png" }),
      ),
    );
    expect(axios.post).toHaveBeenCalledWith(
      "/api/creatives/312/agent-review/request",
    );
    expect(
      await screen.findByText("Prova visual enviada para Têmis"),
    ).toBeInTheDocument();
  });

  it("shows the real landing examples selected by Temis before image generation", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url.endsWith("/products/experiments/1/ads-in-use")) {
        return Promise.resolve({ data: [] });
      }
      if (url.endsWith("/experiments/1")) {
        return Promise.resolve({
          data: {
            creativesToGenerate: 0,
            adCopy: JSON.stringify({
              adCopy: {
                primaryTextVariants: [
                  { label: "prova", primaryText: "Veja o produto" },
                ],
              },
            }),
            adImageBriefing: JSON.stringify({
              adImageBriefing: {
                briefings: [
                  { mustMatchAdVariant: "prova", visualBriefing: "Prova real" },
                ],
              },
            }),
            landingPageImageAssets: JSON.stringify({
              images: [
                {
                  planningItemKey: "exemplo-post-real",
                  sectionName: "Exemplo de post",
                  status: "COMPLETED",
                  resolvedUrl: "/uploads/post-real.png",
                },
                {
                  planningItemKey: "decoracao-planejada",
                  status: "PLANNED",
                  resolvedUrl: "/uploads/inexistente.png",
                },
              ],
            }),
          },
        });
      }
      return Promise.resolve({ data: [] });
    });

    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <CriativosTab experimentId="1" />
      </QueryClientProvider>,
    );

    expect(
      await screen.findByLabelText("Referências reais selecionadas por Têmis"),
    ).toBeInTheDocument();
    expect(screen.getByText(/Exemplo de post/)).toHaveAttribute(
      "href",
      expect.stringContaining("/uploads/post-real.png"),
    );
    expect(
      screen.getByRole("button", { name: "Gerar anúncios do pipeline" }),
    ).toBeEnabled();
  });

  it("submits a legacy creative to the agent even when commercial editing is locked", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url.endsWith("/products/experiments/1/ads-in-use")) {
        return Promise.resolve({
          data: [
            {
              id: 261,
              headline: "Criativo legado",
              primaryText: "Texto preservado",
              imageUrl: "legacy.jpg",
              status: "DRAFT",
              agentReviewStatus: null,
            },
          ],
        });
      }
      if (url.endsWith("/experiments/1")) {
        return Promise.resolve({ data: { creativesToGenerate: 0 } });
      }
      return Promise.resolve({ data: [] });
    });
    (axios.post as any).mockResolvedValue({
      data: { id: 261, agentReviewStatus: "PENDING" },
    });

    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <CriativosTab experimentId="1" alterationLocked />
      </QueryClientProvider>,
    );

    await userEvent.click(
      await screen.findByRole("button", { name: "Enviar ao Aprovador" }),
    );
    expect(axios.post).toHaveBeenCalledWith(
      "/api/creatives/261/agent-review/request",
    );
    expect(
      await screen.findByText("Criativo enviado ao Especialista em Anúncios"),
    ).toBeInTheDocument();
  });

  it("creates an auditable draft version while commercial editing is locked", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url.endsWith("/products/experiments/1/ads-in-use"))
        return Promise.resolve({
          data: [
            {
              id: 259,
              headline: "Original",
              primaryText: "Texto",
              imageUrl: "original.jpg",
              destinationUrl: "",
              status: "DRAFT",
              agentReviewStatus: "ADJUST",
            },
          ],
        });
      if (url.endsWith("/experiments/1"))
        return Promise.resolve({ data: { creativesToGenerate: 0 } });
      return Promise.resolve({ data: [] });
    });
    (axios.post as any).mockResolvedValue({
      data: {
        id: 300,
        sourceCreativeId: 259,
        versionNumber: 2,
        status: "DRAFT",
      },
    });
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <CriativosTab experimentId="1" alterationLocked />
      </QueryClientProvider>,
    );

    await userEvent.click(
      await screen.findByRole("button", { name: "Criar nova versão" }),
    );
    await userEvent.type(
      screen.getByLabelText("URL de destino"),
      "https://agenda-cheia.test/previa",
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Salvar nova versão" }),
    );

    await waitFor(() =>
      expect(axios.post).toHaveBeenCalledWith(
        "/api/creatives/259/versions",
        expect.objectContaining({
          destinationUrl: "https://agenda-cheia.test/previa",
          status: "DRAFT",
        }),
      ),
    );
    expect(await screen.findByText("Nova versão criada")).toBeInTheDocument();
  });

  it("shows image prompt below ad card when toggled", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url.endsWith("/products/experiments/1/ads-in-use")) {
        return Promise.resolve({
          data: [
            {
              id: 77,
              headline: "H1",
              primaryText: "P1",
              imagePrompt:
                "Prompt detalhado da imagem. Briefing visual:\n\n Contraste alto.",
              status: "DRAFT",
            },
          ],
        });
      }
      if (url.endsWith("/experiments/1")) {
        return Promise.resolve({ data: { creativesToGenerate: 0 } });
      }
      return Promise.resolve({ data: [] });
    });

    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <CriativosTab experimentId="1" />
      </QueryClientProvider>,
    );

    const toggleButton = await screen.findByRole("button", {
      name: "Ver prompt da imagem",
    });
    toggleButton.click();
    expect(
      await screen.findByText(/Prompt detalhado da imagem/i),
    ).toBeInTheDocument();
    expect(
      await screen.findByText("Origem dos trechos do prompt"),
    ).toBeInTheDocument();
    expect(await screen.findByText("Briefing visual")).toBeInTheDocument();
    expect(await screen.findByText("Hierarquia sugerida")).toBeInTheDocument();
    expect(await screen.findByText("Margens de segurança")).toBeInTheDocument();
    expect(screen.getByText("Com conteúdo")).toBeInTheDocument();
    expect(screen.getByText("Contraste alto.")).toBeInTheDocument();
    expect(screen.getAllByText("Sem conteúdo").length).toBeGreaterThan(0);
  });

  it("shows previous image prompt below ad card when toggled", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url.endsWith("/products/experiments/1/ads-in-use")) {
        return Promise.resolve({
          data: [
            {
              id: 78,
              headline: "H1",
              primaryText: "P1",
              imagePrompt: "Prompt final da imagem",
              status: "DRAFT",
            },
          ],
        });
      }
      if (url.endsWith("/experiments/1")) {
        return Promise.resolve({
          data: {
            creativesToGenerate: 0,
            creativeImagePrompt: "Prompt base anterior",
          },
        });
      }
      return Promise.resolve({ data: [] });
    });

    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <CriativosTab experimentId="1" />
      </QueryClientProvider>,
    );

    const previousPromptButton = await screen.findByRole("button", {
      name: "Ver prompt anterior da imagem",
    });
    previousPromptButton.click();
    expect(await screen.findByText("Prompt base anterior")).toBeInTheDocument();
  });

  it("shows intermediate image prompt below ad card when toggled", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url.endsWith("/products/experiments/1/ads-in-use")) {
        return Promise.resolve({
          data: [
            {
              id: 79,
              headline: "H1",
              primaryText: "P1",
              imagePrompt: "Prompt final da imagem",
              imageIntermediatePrompt: "Prompt intermediário da evolução",
              status: "DRAFT",
            },
          ],
        });
      }
      if (url.endsWith("/experiments/1")) {
        return Promise.resolve({
          data: {
            creativesToGenerate: 0,
          },
        });
      }
      return Promise.resolve({ data: [] });
    });

    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <CriativosTab experimentId="1" />
      </QueryClientProvider>,
    );

    const intermediatePromptButton = await screen.findByRole("button", {
      name: "Ver prompt intermediário",
    });
    intermediatePromptButton.click();
    expect(
      await screen.findByText("Prompt intermediário da evolução"),
    ).toBeInTheDocument();
  });

  it("shows pipeline banner when data is available", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url.endsWith("/products/experiments/1/ads-in-use")) {
        return Promise.resolve({ data: [] });
      }
      if (url.endsWith("/experiments/1")) {
        return Promise.resolve({
          data: {
            creativesToGenerate: 0,
            adCopy:
              '{"adCopy":{"primaryTextVariants":[{"label":"dor","primaryText":"Texto","headline":"Headline","description":"Descrição","ctaText":"Saiba mais"}]}}',
            adImageBriefing:
              '{"adImageBriefing":{"briefings":[{"mustMatchAdVariant":"dor","visualBriefing":"Use contraste","assetType":"estatico"}]}}',
          },
        });
      }
      return Promise.resolve({ data: [] });
    });
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <CriativosTab experimentId="1" />
      </QueryClientProvider>,
    );
    expect(
      await screen.findByText(/Anúncios do pipeline prontos/i),
    ).toBeInTheDocument();
  });

  it("shows pipeline progress banner when worker is running", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url.endsWith("/products/experiments/1/ads-in-use")) {
        return Promise.resolve({ data: [] });
      }
      if (url.endsWith("/experiments/1")) {
        return Promise.resolve({
          data: {
            creativesToGenerate: 2,
            creativeGenerationMode: "PIPELINE_ADS",
            creativeGenerationStatus: "PROCESSING",
          },
        });
      }
      return Promise.resolve({ data: [] });
    });
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <CriativosTab experimentId="1" />
      </QueryClientProvider>,
    );
    expect(
      await screen.findByText(/Worker AI em produção/i),
    ).toBeInTheDocument();
  });

  it("shows recoverable pipeline failure from backend status", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url.endsWith("/products/experiments/1/ads-in-use")) {
        return Promise.resolve({ data: [] });
      }
      if (url.endsWith("/experiments/1")) {
        return Promise.resolve({
          data: {
            creativesToGenerate: 0,
            creativeGenerationMode: "DEFAULT",
            creativeGenerationStatus: "TIMEOUT",
            creativeGenerationError:
              "Geração excedeu o tempo operacional de 30 minutos; solicite nova tentativa.",
            adCopy: JSON.stringify({
              adCopy: { primaryTextVariants: [{ primaryText: "Texto" }] },
            }),
            adImageBriefing: JSON.stringify({
              adImageBriefing: { briefings: [{ visualBriefing: "Imagem" }] },
            }),
            landingPageImageAssets: JSON.stringify({
              images: [
                {
                  planningItemKey: "post-real",
                  status: "COMPLETED",
                  resolvedUrl: "/uploads/post.png",
                },
              ],
            }),
          },
        });
      }
      return Promise.resolve({ data: [] });
    });
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <CriativosTab experimentId="1" />
      </QueryClientProvider>,
    );
    expect(
      await screen.findByText(/solicite nova tentativa/i),
    ).toBeInTheDocument();
  });

  it("starts GeraAnuncio v2 from the first stage using the explicit experiment route", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url.endsWith("/products/experiments/1/ads-in-use")) {
        return Promise.resolve({ data: [] });
      }
      if (url.endsWith("/experiments/1")) {
        return Promise.resolve({
          data: {
            creativesToGenerate: 0,
            creativeGenerationMode: "DEFAULT",
            creativeGenerationStatus: "IDLE",
            adCopy: JSON.stringify({
              adCopy: { primaryTextVariants: [{ primaryText: "Texto" }] },
            }),
            adImageBriefing: JSON.stringify({
              adImageBriefing: { briefings: [{ visualBriefing: "Imagem" }] },
            }),
            landingPageImageAssets: JSON.stringify({
              images: [
                {
                  planningItemKey: "post-real",
                  status: "COMPLETED",
                  resolvedUrl: "/uploads/post.png",
                },
              ],
            }),
          },
        });
      }
      return Promise.resolve({ data: [] });
    });
    (axios.post as any).mockResolvedValue({ data: { idJob: "job-1" } });

    const client = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });
    render(
      <QueryClientProvider client={client}>
        <CriativosTab experimentId="1" />
      </QueryClientProvider>,
    );

    await userEvent.click(
      await screen.findByRole("button", {
        name: "Gerar anúncios do pipeline",
      }),
    );

    await waitFor(() => {
      expect(axios.post).toHaveBeenCalledWith(
        "/api/internal/aiworker/geracaoanuncios/v1/texto/stage-executions/experiments/1/start",
      );
    });
  });

  it("hides the previous generation failure while a retry request is pending", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url.endsWith("/products/experiments/1/ads-in-use")) {
        return Promise.resolve({ data: [] });
      }
      if (url.endsWith("/experiments/1")) {
        return Promise.resolve({
          data: {
            creativesToGenerate: 0,
            creativeGenerationStatus: "FAILED",
            creativeGenerationError: "Falha anterior",
            adCopy: JSON.stringify({
              adCopy: { primaryTextVariants: [{ primaryText: "Texto" }] },
            }),
            adImageBriefing: JSON.stringify({
              adImageBriefing: { briefings: [{ visualBriefing: "Imagem" }] },
            }),
            landingPageImageAssets: JSON.stringify({
              images: [
                {
                  planningItemKey: "post-real",
                  status: "COMPLETED",
                  resolvedUrl: "/uploads/post.png",
                },
              ],
            }),
          },
        });
      }
      return Promise.resolve({ data: [] });
    });
    let resolveRetry: (value: unknown) => void = () => {};
    (axios.post as any).mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveRetry = resolve;
        }),
    );

    const client = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });
    render(
      <QueryClientProvider client={client}>
        <CriativosTab experimentId="1" />
      </QueryClientProvider>,
    );

    expect(await screen.findByText("Falha anterior")).toBeInTheDocument();

    await userEvent.click(
      await screen.findByRole("button", {
        name: "Gerar anúncios do pipeline",
      }),
    );

    expect(screen.queryByText("Falha anterior")).not.toBeInTheDocument();
    expect(
      screen.queryByText("Geração falhou; revise a causa e tente novamente"),
    ).not.toBeInTheDocument();

    resolveRetry({ data: { idJob: "job-1" } });
  });

  it("clears approval loading state when the backend rejects the update", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url.endsWith("/products/experiments/1/ads-in-use")) {
        return Promise.resolve({
          data: [
            {
              id: 80,
              format: "LINK",
              headline: "Criativo em rascunho",
              primaryText: "Texto",
              imageUrl: "img.jpg",
              description: "Descrição",
              cta: "LEARN_MORE",
              destinationUrl: "",
              leadGenFormId: "",
              instagramUserId: "",
              status: "DRAFT",
            },
          ],
        });
      }
      if (url.endsWith("/experiments/1")) {
        return Promise.resolve({ data: { creativesToGenerate: 0 } });
      }
      return Promise.resolve({ data: [] });
    });
    let rejectUpdate: (error: Error) => void = () => {};
    (axios.put as any).mockImplementation(
      () =>
        new Promise((_, reject) => {
          rejectUpdate = reject;
        }),
    );

    const client = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });
    render(
      <QueryClientProvider client={client}>
        <CriativosTab experimentId="1" />
      </QueryClientProvider>,
    );

    await userEvent.click(
      await screen.findByRole("button", { name: "Aprovar" }),
    );

    expect(await screen.findByText("Aprovando...")).toBeInTheDocument();

    rejectUpdate(new Error("backend unavailable"));

    expect(
      await screen.findByText("Não foi possível aprovar o criativo"),
    ).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Aprovar" })).toBeEnabled();
    });
  });
});

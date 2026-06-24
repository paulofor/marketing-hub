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
  it("shows preview", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url.endsWith("/experiments/1/creatives")) {
        return Promise.resolve({
          data: [
            {
              id: 42,
              headline: "H1",
              primaryText: "P1",
              imageUrl: "img.jpg",
              status: "READY",
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
    (await screen.findByLabelText("Preview")).click();
    await screen.findByText("Patrocinado");
  });

  it("shows image prompt below ad card when toggled", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url.endsWith("/experiments/1/creatives")) {
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
      if (url.endsWith("/experiments/1/creatives")) {
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
      if (url.endsWith("/experiments/1/creatives")) {
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
      if (url.endsWith("/experiments/1/creatives")) {
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
      if (url.endsWith("/experiments/1/creatives")) {
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
      if (url.endsWith("/experiments/1/creatives")) {
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

  it("clears approval loading state when the backend rejects the update", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url.endsWith("/experiments/1/creatives")) {
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

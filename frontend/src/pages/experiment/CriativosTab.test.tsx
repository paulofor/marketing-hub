import "@testing-library/jest-dom/vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, it, expect, vi, beforeEach } from "vitest";
import CriativosTab from "./CriativosTab";
import axios from "axios";

vi.mock("axios");

describe("CriativosTab", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });
  it("opens request dialog", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url.endsWith("/experiments/1/creatives")) {
        return Promise.resolve({ data: [] });
      }
      if (url.endsWith("/experiments/1")) {
        return Promise.resolve({ data: { creativesToGenerate: 3 } });
      }
      return Promise.resolve({ data: [] });
    });
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <CriativosTab experimentId="1" />
      </QueryClientProvider>,
    );
    const requestedBadges = await screen.findAllByText("Solicitados: 3");
    expect(requestedBadges.length).toBeGreaterThan(0);
    screen.getByText("Gerar criativos").click();
    expect(
      await screen.findByLabelText("Quantidade de criativos"),
    ).toBeTruthy();
  });

  it("opens manual creation modal", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url.endsWith("/experiments/1/creatives")) {
        return Promise.resolve({ data: [] });
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
    await screen.findAllByText("Solicitados: 0");
    const manualButtons = screen.getAllByText("Adicionar anúncio manual");
    manualButtons[0].click();
    await screen.findByText(/Novo Criativo/i);
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
    await screen.findAllByText("Solicitados: 0");
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
              imagePrompt: "Prompt detalhado da imagem. Briefing visual:\n\n Contraste alto.",
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
    expect(await screen.findByText(/Prompt detalhado da imagem/i)).toBeInTheDocument();
    expect(await screen.findByText("Origem dos trechos do prompt")).toBeInTheDocument();
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
            adCopy: '{"adCopy":{"primaryTextVariants":[{"label":"dor","primaryText":"Texto","headline":"Headline","description":"Descrição","ctaText":"Saiba mais"}]}}',
            adImageBriefing: '{"adImageBriefing":{"briefings":[{"mustMatchAdVariant":"dor","visualBriefing":"Use contraste","assetType":"estatico"}]}}',
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
    expect(await screen.findByText(/Anúncios do pipeline prontos/i)).toBeInTheDocument();
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
    expect(await screen.findByText(/Worker AI em produção/i)).toBeInTheDocument();
  });

});

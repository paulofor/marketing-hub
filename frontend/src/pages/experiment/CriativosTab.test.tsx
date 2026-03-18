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
});

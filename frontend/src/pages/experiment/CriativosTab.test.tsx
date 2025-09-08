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
  it("opens modal", async () => {
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
    expect(await screen.findByText("Solicitados: 3")).toBeTruthy();
    screen.getByText("Novo Criativo").click();
    expect(await screen.findByText("Novo Criativo")).toBeTruthy();
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
    await screen.findByText("Solicitados: 0");
    (await screen.findByLabelText("Preview")).click();
    const iframe = await screen.findByTitle("preview");
    expect(iframe.getAttribute("src")).toBe("/api/creatives/42/preview");
  });
});

import { render, screen, cleanup } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, describe, expect, it } from "vitest";
import React from "react";
import App from "../App";

function setup(ui: React.ReactNode, initialEntries: string[]) {
  const client = new QueryClient();
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={initialEntries}>{ui}</MemoryRouter>
    </QueryClientProvider>,
  );
}

afterEach(() => {
  cleanup();
});

describe("OPRM navigation", () => {
  it("renders CNAE score page on /oprm route", () => {
    setup(<App />, ["/oprm"]);
    expect(screen.getByText(/^CNAEs por Score OPRM$/)).toBeTruthy();
    expect(
      screen.getByRole("button", { name: /ver nichos enriquecidos/i }),
    ).toBeTruthy();
    expect(screen.queryByRole("link", { name: /^pipeline$/i })).toBeNull();
  });

  it("has menu link to /oprm", () => {
    setup(<App />, ["/"]);
    const link = screen.getByRole("link", { name: /oprm/i });
    expect(link).toBeTruthy();
    expect(link.getAttribute("href")).toBe("/oprm");
  });

  it("has menu link to /oprm for CNAEs", () => {
    setup(<App />, ["/"]);
    const link = screen.getByRole("link", { name: /^cnaes$/i });
    expect(link).toBeTruthy();
    expect(link.getAttribute("href")).toBe("/oprm");
  });

  it("renders placeholder niche detail page on /oprm/cnaes/:cnaeCode route", () => {
    setup(<App />, ["/oprm/cnaes/9602501"]);
    expect(screen.getByText(/^Subnichos do CNAE$/)).toBeTruthy();
    expect(screen.getByText(/^CNAE 9602501$/)).toBeTruthy();
    expect(
      screen.getByRole("link", { name: /voltar para cnaes/i }),
    ).toBeTruthy();
  });

  it("renders v2 pipeline design page for a CNAE", () => {
    setup(<App />, ["/oprm/cnaes/9602501/pipeline-v2"]);
    expect(screen.getByText(/^Pipeline NichoCNAE v2$/)).toBeTruthy();
    expect(screen.getByText(/^CNAE 9602501$/)).toBeTruthy();
    expect(screen.getByText(/^Candidate Generator$/)).toBeTruthy();
    expect(screen.getByText(/^Knowledge Accumulator$/)).toBeTruthy();
  });

  it("redirects obsolete /oprm/pipeline route to CNAE niche creation flow", () => {
    setup(<App />, ["/oprm/pipeline"]);
    expect(screen.getByText(/^CNAEs por Score OPRM$/)).toBeTruthy();
    expect(screen.queryByText(/^Pipeline NichoCNAE$/)).toBeNull();
    expect(screen.queryByRole("link", { name: /^pipeline$/i })).toBeNull();
  });

  it("renders loading state on /oprm/operations route", async () => {
    setup(<App />, ["/oprm/operations"]);
    expect(await screen.findByText(/carregando jobs do oprm/i)).toBeTruthy();
  });

  it("renders loading state on /oprm/occupations route", async () => {
    setup(<App />, ["/oprm/occupations"]);
    expect(
      await screen.findByText(/carregando catálogo de ocupações/i),
    ).toBeTruthy();
  });
});

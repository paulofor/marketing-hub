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

describe("image generator navigation", () => {
  it("has menu link to /ai/image-generator", () => {
    setup(<App />, ["/"]);

    const link = screen.getByRole("link", { name: /gerador de imagens/i });

    expect(link).toBeTruthy();
    expect(link.getAttribute("href")).toBe("/ai/image-generator");
  });

  it("renders image generator page on /ai/image-generator route", () => {
    setup(<App />, ["/ai/image-generator"]);

    expect(
      screen.getByRole("heading", { name: /gerador de imagens/i }),
    ).toBeTruthy();
    expect(screen.getByLabelText(/prompt da imagem/i)).toBeTruthy();
  });
});

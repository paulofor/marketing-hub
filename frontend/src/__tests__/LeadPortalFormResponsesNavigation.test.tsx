import { render, screen, cleanup } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, describe, expect, it } from "vitest";
import React from "react";
import App from "../App";

function setup(initialEntries: string[]) {
  const client = new QueryClient();
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={initialEntries}>
        <App />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

afterEach(() => {
  cleanup();
});

describe("lead portal form responses navigation", () => {
  it("shows loading state when opening the page", async () => {
    setup(["/lead-portal/form-responses"]);
    expect(
      await screen.findByText(/carregando respostas recentes/i),
    ).toBeTruthy();
  });

  it("contains menu item for form responses", () => {
    setup(["/"]);
    const link = screen.getByRole("link", { name: /respostas de formulários/i });
    expect(link).toBeTruthy();
    expect(link.getAttribute("href")).toBe("/lead-portal/form-responses");
  });
});

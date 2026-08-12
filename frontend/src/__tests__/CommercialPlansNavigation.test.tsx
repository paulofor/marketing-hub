import { cleanup, render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, describe, expect, it } from "vitest";
import { MemoryRouter } from "react-router-dom";
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

describe("navegação dos planos comerciais", () => {
  it("oferece acesso direto aos planos comerciais pelo menu", () => {
    setup(["/"]);

    const link = screen.getByRole("link", { name: /planos comerciais/i });

    expect(link.getAttribute("href")).toBe("/planning");
  });

  it("separa o planejamento mensal em um item de menu próprio", () => {
    setup(["/"]);

    const link = screen.getByRole("link", {
      name: /planejamentos mensais/i,
    });

    expect(link.getAttribute("href")).toBe("/monthly-planning");
  });
});

import { render, screen, cleanup } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, it, expect, afterEach } from "vitest";
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

describe("funnels navigation", () => {
  it("renders FunnelBuilder on /funnels route", async () => {
    setup(<App />, ["/funnels"]);
    expect(
      await screen.findByRole("button", { name: /add step/i }),
    ).toBeTruthy();
  });

  it("has menu link to /funnels", () => {
    setup(<App />, ["/"]);
    const link = screen.getByRole("link", { name: /funil de vendas/i });
    expect(link).toBeTruthy();
    expect(link.getAttribute("href")).toBe("/funnels");
  });
});

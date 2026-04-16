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
  it("renders loading state on /oprm route", async () => {
    setup(<App />, ["/oprm"]);
    expect(
      await screen.findByText(/carregando ocupações do oprm/i),
    ).toBeTruthy();
  });

  it("has menu link to /oprm", () => {
    setup(<App />, ["/"]);
    const link = screen.getByRole("link", { name: /oprm/i });
    expect(link).toBeTruthy();
    expect(link.getAttribute("href")).toBe("/oprm");
  });
});

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

describe("app ideas navigation", () => {
  it("renders loading state on /app-ideas route", async () => {
    setup(<App />, ["/app-ideas"]);
    expect(
      await screen.findByText(/carregando ideias de aplicativo/i),
    ).toBeTruthy();
  });

  it("has menu link to /app-ideas", () => {
    setup(<App />, ["/"]);
    const link = screen.getByRole("link", { name: /ideias de aplicativo/i });
    expect(link).toBeTruthy();
    expect(link.getAttribute("href")).toBe("/app-ideas");
  });
});

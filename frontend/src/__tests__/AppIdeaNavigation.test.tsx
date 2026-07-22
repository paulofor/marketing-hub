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

  it("keeps obsolete app ideas route out of the main menu", () => {
    setup(<App />, ["/"]);
    expect(
      screen.queryByRole("link", { name: /ideias de aplicativo/i }),
    ).toBeNull();
  });
});

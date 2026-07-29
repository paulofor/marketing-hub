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

describe("operational inventory navigation", () => {
  it("has menu link to the VPS inventory", () => {
    setup(<App />, ["/"]);
    const links = screen.getAllByRole("link", { name: /inventário vps/i });
    expect(
      links.some(
        (link) => link.getAttribute("href") === "/microservices/vps-inventory",
      ),
    ).toBe(true);
  });

  it("renders loading state on VPS inventory route", () => {
    setup(<App />, ["/microservices/vps-inventory"]);
    expect(screen.getByText(/carregando inventário operacional/i)).toBeTruthy();
  });
});

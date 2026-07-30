import { render, screen, cleanup } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import React from "react";
import App from "../App";

vi.mock("axios", () => ({
  default: {
    get: vi.fn(() => new Promise(() => {})),
  },
}));

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

describe("pde copy editor navigation", () => {
  it("has menu link to the PDE copy editor", () => {
    setup(<App />, ["/"]);
    const link = screen.getByRole("link", { name: /copy pde/i });
    expect(link).toBeTruthy();
    expect(link.getAttribute("href")).toBe("/pde-copy");
  });

  it("renders loading state on the PDE copy editor route", () => {
    setup(<App />, ["/pde-copy"]);
    expect(screen.getByText(/carregando produtos pde/i)).toBeTruthy();
  });
});

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

describe("facebook campaigns navigation", () => {
  it("has menu link to /facebook-campaigns", () => {
    setup(<App />, ["/"]); 
    const link = screen.getByRole("link", { name: /experimentos para campanha/i });
    expect(link).toBeTruthy();
    expect(link.getAttribute("href")).toBe("/facebook-campaigns");
  });
});

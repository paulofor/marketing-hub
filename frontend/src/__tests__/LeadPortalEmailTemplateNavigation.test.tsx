import { cleanup, render, screen } from "@testing-library/react";
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

describe("lead portal email template navigation", () => {
  it("shows loading state when accessing the editor", async () => {
    setup(["/lead-portal/email-template"]);
    expect(
      await screen.findByText(/carregando template do e-mail/i),
    ).toBeTruthy();
  });

  it("contains menu item for the email template", () => {
    setup(["/"]);
    const link = screen.getByRole("link", { name: /template do e-mail do lead/i });
    expect(link).toBeTruthy();
    expect(link.getAttribute("href")).toBe("/lead-portal/email-template");
  });
});

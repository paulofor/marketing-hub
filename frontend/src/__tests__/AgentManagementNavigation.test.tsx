import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import App from "../App";

describe("gestão de agentes no menu", () => {
  it("oferece acesso ao cadastro de agentes nas configurações", () => {
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <MemoryRouter>
          <App />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const links = screen.getAllByRole("link", { name: /gestão de agentes/i });
    expect(links.some((link) => link.getAttribute("href") === "/agents")).toBe(
      true,
    );
  });
});

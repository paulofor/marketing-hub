import { cleanup, render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import SocialDistributionPage from "./SocialDistributionPage";

vi.mock("axios");

describe("SocialDistributionPage", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/products") {
        return Promise.resolve({
          data: [{ id: 76, name: "Método MUSA", slug: "metodo-musa-7-dias" }],
        });
      }
      if (url === "/api/social-distribution/accounts") {
        return Promise.resolve({ data: [] });
      }
      if (url === "/api/social-distribution/publications") {
        return Promise.resolve({ data: [] });
      }
      return Promise.resolve({ data: [] });
    });
  });

  afterEach(() => {
    cleanup();
  });

  it("shows the social distribution workspace", async () => {
    const client = new QueryClient();

    render(
      <QueryClientProvider client={client}>
        <SocialDistributionPage />
      </QueryClientProvider>,
    );

    expect(
      await screen.findByRole("heading", { name: /Distribuição orgânica/i }),
    ).toBeTruthy();
    expect(screen.getByText(/YouTube primeiro/i)).toBeTruthy();
    expect(
      screen.getByRole("button", { name: /Cadastrar conta/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole("button", { name: /Criar rascunho/i }),
    ).toBeTruthy();
  });
});

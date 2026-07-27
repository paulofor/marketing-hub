import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import ProductScientificArticlesPage from "./ProductScientificArticlesPage";

vi.mock("axios");

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  window.history.pushState({}, "", "/products/1/scientific-articles");
  return render(
    <QueryClientProvider client={client}>
      <BrowserRouter>
        <Routes>
          <Route
            path="/products/:productId/scientific-articles"
            element={<ProductScientificArticlesPage />}
          />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>,
  );
}

describe("ProductScientificArticlesPage", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it("shows scientific articles with source link and mechanism application", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/products/1") {
        return Promise.resolve({
          data: {
            id: 1,
            slug: "metodo-musa-7-dias",
            name: "Método MUSA - Presença Elegante em 7 Dias",
          },
        });
      }
      if (url === "/api/products/1/scientific-articles") {
        return Promise.resolve({
          data: [
            {
              id: 10,
              productId: 1,
              link: "https://doi.org/10.1016/j.jesp.2012.02.008",
              originalTitle: "Enclothed cognition",
              portugueseTitle: "Cognição vestida",
              summary: "Roupa pode influenciar autopercepção.",
              mechanismApplication: "Sustenta a peça-sinal do mecanismo MUSA.",
            },
          ],
        });
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`));
    });

    renderPage();

    expect(await screen.findByText("Cognição vestida")).toBeTruthy();
    expect(screen.getByText("Enclothed cognition")).toBeTruthy();
    expect(
      screen.getByText("Sustenta a peça-sinal do mecanismo MUSA."),
    ).toBeTruthy();
    expect(screen.getByRole("link", { name: /Abrir artigo/i })).toHaveAttribute(
      "href",
      "https://doi.org/10.1016/j.jesp.2012.02.008",
    );
  });

  it("creates scientific article using product endpoint", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/products/1") {
        return Promise.resolve({
          data: { id: 1, name: "Método MUSA" },
        });
      }
      if (url === "/api/products/1/scientific-articles") {
        return Promise.resolve({ data: [] });
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`));
    });
    (axios.post as any).mockResolvedValue({
      data: {
        id: 11,
        productId: 1,
        link: "https://doi.org/exemplo",
        originalTitle: "Original",
        portugueseTitle: "Português",
        summary: "Resumo",
        mechanismApplication: "Aplicação",
      },
    });

    renderPage();

    fireEvent.change(await screen.findByLabelText("Link"), {
      target: { value: "https://doi.org/exemplo" },
    });
    fireEvent.change(screen.getByLabelText("Título original"), {
      target: { value: "Original" },
    });
    fireEvent.change(screen.getByLabelText("Título em português"), {
      target: { value: "Português" },
    });
    fireEvent.change(screen.getByLabelText("Resumo"), {
      target: { value: "Resumo" },
    });
    fireEvent.change(screen.getByLabelText("Aplicação no mecanismo"), {
      target: { value: "Aplicação" },
    });
    fireEvent.click(screen.getByRole("button", { name: /Cadastrar artigo/i }));

    await waitFor(() => {
      expect(axios.post).toHaveBeenCalledWith(
        "/api/products/1/scientific-articles",
        {
          link: "https://doi.org/exemplo",
          originalTitle: "Original",
          portugueseTitle: "Português",
          summary: "Resumo",
          mechanismApplication: "Aplicação",
        },
      );
    });
  });
});

import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { expect, it, vi } from "vitest";
import axios from "axios";
import ProductFichasPage from "./ProductFichasPage";
import blockedJson from "../../../../backend/ads-service/src/main/resources/fichas/v1/exemplo-bloqueado.json?raw";
import recoveredJson from "../../../../backend/ads-service/src/main/resources/fichas/v1/exemplo-recuperado.json?raw";
const blocked = JSON.parse(blockedJson);
const recovered = JSON.parse(recoveredJson);
vi.mock("axios", () => ({ default: { get: vi.fn() } }));
it("exibe revisões, lacunas, fontes e consulta o ciclo do produto sem despachar", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: {
      ambiente: "HOMOLOGACAO_HISTORICA",
      despachoPermitido: false,
      fichas: [...blocked, ...recovered],
    },
  });
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <MemoryRouter initialEntries={["/products/7/fichas?learningCycleId=20"]}>
        <Routes>
          <Route
            path="/products/:productId/fichas"
            element={<ProductFichasPage />}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
  expect(await screen.findByText(/BLOCKED:/)).toBeInTheDocument();
  expect(screen.getByText(/COMPLETED:/)).toBeInTheDocument();
  expect(screen.getByText(/Destino #127 v12: PUBLISHED/)).toBeInTheDocument();
  expect(screen.getAllByText(/Sem fonte registrada/)).toHaveLength(2);
  expect(
    screen.getByRole("link", { name: "capella-c20-e97-passagem · revisão 1" }),
  ).toHaveAttribute("href", "#capella-c20-e97-passagem-r1");
  expect(axios.get).toHaveBeenCalledWith("/api/products/7/fichas/v1", {
    params: { ciclo: "20" },
  });
});

it("informa indisponibilidade sem fabricar fichas ou liberar despacho", async () => {
  vi.mocked(axios.get).mockRejectedValue(new Error("piloto desabilitado"));
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <MemoryRouter initialEntries={["/products/8/fichas?learningCycleId=21"]}>
        <Routes>
          <Route
            path="/products/:productId/fichas"
            element={<ProductFichasPage />}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Piloto indisponível",
  );
});

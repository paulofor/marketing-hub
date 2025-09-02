import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, it, expect, vi } from "vitest";
import CriativosTab from "./CriativosTab";
import axios from "axios";

vi.mock("axios");

describe("CriativosTab", () => {
  it("opens modal", async () => {
    (axios.get as any)
      .mockResolvedValueOnce({ data: [] })
      .mockResolvedValueOnce({ data: { creativesToGenerate: 3 } });
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <CriativosTab experimentId="1" />
      </QueryClientProvider>,
    );
    expect(
      await screen.findByText("Solicitados: 3"),
    ).toBeTruthy();
    screen.getByText("Novo Criativo").click();
    expect(await screen.findByText("Novo Criativo")).toBeTruthy();
  });
});

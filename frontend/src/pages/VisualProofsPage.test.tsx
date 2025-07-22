import { render, screen, fireEvent } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, it, expect, vi } from "vitest";
import VisualProofsPage from "./VisualProofsPage";
import axios from "axios";

vi.mock("axios");

describe("VisualProofsPage", () => {
  it("não cria item quando input vazio", async () => {
    (axios.get as any).mockResolvedValue({ data: [] });
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <VisualProofsPage />
      </QueryClientProvider>,
    );
    const button = await screen.findByText(/Nova Prova Visual/);
    fireEvent.click(button);
    expect((axios.post as any).mock.calls.length).toBe(0);
  });

  it("edita item e espera texto atualizado", async () => {
    (axios.get as any).mockResolvedValue({ data: [{ id: 1, name: "A" }] });
    (axios.put as any).mockResolvedValue({ data: { id: 1, name: "B" } });
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <VisualProofsPage />
      </QueryClientProvider>,
    );
    const edit = await screen.findByRole("button", { name: "✏️" });
    fireEvent.click(edit);
    const input = screen.getByDisplayValue("A") as HTMLInputElement;
    fireEvent.change(input, { target: { value: "B" } });
    fireEvent.click(screen.getByRole("button", { name: "✔️" }));
    expect(await screen.findByText("B")).toBeTruthy();
  });
});

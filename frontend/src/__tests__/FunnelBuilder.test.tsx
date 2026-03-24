import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import FunnelBuilder from "../components/FunnelBuilder";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

function setup(ui: React.ReactNode) {
  const client = new QueryClient();
  return render(
    <QueryClientProvider client={client}>{ui}</QueryClientProvider>,
  );
}

describe("FunnelBuilder", () => {
  it("renders labeled inputs", () => {
    setup(<FunnelBuilder />);
    expect(screen.getByLabelText(/estímulo/i)).toBeTruthy();
    expect(screen.getByLabelText(/incremento de score/i)).toBeTruthy();
    expect(screen.getByLabelText(/ação esperada/i)).toBeTruthy();
    expect(screen.getByLabelText(/observações/i)).toBeTruthy();
  });

  it("adds step to list on submit", async () => {
    setup(<FunnelBuilder />);
    await userEvent.click(
      screen.getAllByRole("button", { name: /adicionar etapa/i })[0],
    );
    const list = screen.getAllByRole("list")[0];
    const item = within(list).getAllByRole("listitem")[0];
    expect(within(item).getByLabelText(/Estímulo \*/i)).toHaveValue("DM");
    expect(within(item).getByLabelText(/Ação esperada \*/i)).toHaveValue("OPEN");
  });
});

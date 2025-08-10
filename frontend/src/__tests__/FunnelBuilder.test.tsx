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
    expect(screen.getByLabelText(/stimulus type/i)).toBeTruthy();
    expect(screen.getByLabelText(/score increment/i)).toBeTruthy();
    expect(screen.getByLabelText(/expected action/i)).toBeTruthy();
    expect(screen.getByLabelText(/observation/i)).toBeTruthy();
  });

  it("adds step to list on submit", async () => {
    setup(<FunnelBuilder />);
    await userEvent.click(
      screen.getAllByRole("button", { name: /add step/i })[0],
    );
    const list = screen.getAllByRole("list")[0];
    const item = (await within(list).findAllByRole("button"))[0];
    expect(within(item).getByDisplayValue("DM")).toBeTruthy();
    expect(within(item).getByDisplayValue("OPEN")).toBeTruthy();
  });
});

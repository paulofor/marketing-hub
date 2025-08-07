import { render, screen } from "@testing-library/react";
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
  });

  it("logs validation errors on invalid submit", async () => {
    const logSpy = vi.spyOn(console, "log").mockImplementation(() => {});
    setup(<FunnelBuilder />);
    await userEvent.click(
      screen.getAllByRole("button", { name: /add step/i })[0],
    );
    expect(logSpy).toHaveBeenCalledWith("Validation errors", expect.anything());
    logSpy.mockRestore();
  });
});

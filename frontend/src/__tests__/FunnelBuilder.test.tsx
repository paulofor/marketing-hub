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

  it("adds step to list on submit", async () => {
    setup(<FunnelBuilder />);
    await userEvent.click(
      screen.getAllByRole("button", { name: /add step/i })[0],
    );
    expect(await screen.findByText(/DM - OPEN/i)).toBeTruthy();
  });

  it("updates when funnel prop changes", () => {
    const client = new QueryClient();
    const { rerender } = render(
      <QueryClientProvider client={client}>
        <FunnelBuilder
          funnel={{
            id: "1",
            name: "F1",
            steps: [
              {
                id: "a",
                stimulus_type: "EMAIL",
                expected_action: "OPEN",
                score_inc: 5,
              },
            ],
          }}
        />
      </QueryClientProvider>,
    );
    expect(screen.getByText(/EMAIL - OPEN \(\+5\)/)).toBeTruthy();
    rerender(
      <QueryClientProvider client={client}>
        <FunnelBuilder
          funnel={{
            id: "2",
            name: "F2",
            steps: [
              {
                id: "b",
                stimulus_type: "SMS",
                expected_action: "CLICK",
                score_inc: 7,
              },
            ],
          }}
        />
      </QueryClientProvider>,
    );
    expect(screen.getByText(/SMS - CLICK \(\+7\)/)).toBeTruthy();
    expect(screen.queryByText(/EMAIL - OPEN \(\+5\)/)).toBeNull();
  });
});

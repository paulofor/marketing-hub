import { render, screen, within, cleanup } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, it, expect, vi, afterEach } from "vitest";
import EditFunnelPage from "./EditFunnelPage";
import * as funnelApi from "../../api/funnel/useFunnel";

afterEach(() => {
  vi.restoreAllMocks();
  cleanup();
});

describe("EditFunnelPage", () => {
  it("renders steps sorted by orderIdx", () => {
    vi.spyOn(funnelApi, "useFunnel").mockReturnValue({
      data: {
        id: "1",
        name: "Test",
        steps: [
          {
            id: "b",
            stimulusType: "EMAIL",
            expectedAction: "CLICK",
            scoreInc: 2,
            orderIdx: 2,
          },
          {
            id: "a",
            stimulusType: "DM",
            expectedAction: "OPEN",
            scoreInc: 1,
            orderIdx: 1,
          },
        ],
      },
      isLoading: false,
    } as any);

    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <MemoryRouter initialEntries={["/funnels/1/edit"]}>
          <Routes>
            <Route path="/funnels/:id/edit" element={<EditFunnelPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const list = screen.getByRole("list");
    const items = within(list).getAllByRole("button");
    expect(items[0].textContent).toMatch(/DM - OPEN/);
    expect(items[1].textContent).toMatch(/EMAIL - CLICK/);
  });

  it("handles missing and duplicate orderIdx deterministically", () => {
    vi.spyOn(funnelApi, "useFunnel").mockReturnValue({
      data: {
        id: "1",
        name: "Test",
        steps: [
          {
            id: "a",
            stimulusType: "DM",
            expectedAction: "OPEN",
            scoreInc: 1,
            orderIdx: 1,
          },
          {
            id: "c",
            stimulusType: "EMAIL",
            expectedAction: "CLICK",
            scoreInc: 3,
          },
          {
            id: "b",
            stimulusType: "SMS",
            expectedAction: "VIEW",
            scoreInc: 2,
            orderIdx: 1,
          },
        ],
      },
      isLoading: false,
    } as any);

    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <MemoryRouter initialEntries={["/funnels/1/edit"]}>
          <Routes>
            <Route path="/funnels/:id/edit" element={<EditFunnelPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const list = screen.getByRole("list");
    const items = within(list).getAllByRole("button");
    expect(items[0].textContent).toMatch(/DM - OPEN/);
    expect(items[1].textContent).toMatch(/SMS - VIEW/);
    expect(items[2].textContent).toMatch(/EMAIL - CLICK/);
  });
});

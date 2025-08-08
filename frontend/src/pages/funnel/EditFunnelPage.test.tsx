import { render, screen, within } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, it, expect, vi, afterEach } from "vitest";
import EditFunnelPage from "./EditFunnelPage";
import * as funnelApi from "../../api/funnel/useFunnel";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("EditFunnelPage", () => {
  it("renders steps sorted by orderIdx", async () => {
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

    const list = screen.getAllByRole("list")[0];
    const items = await within(list).findAllByRole("button");
    expect(within(items[0]).getByDisplayValue("DM")).toBeTruthy();
    expect(within(items[0]).getByDisplayValue("OPEN")).toBeTruthy();
    expect(within(items[1]).getByDisplayValue("EMAIL")).toBeTruthy();
    expect(within(items[1]).getByDisplayValue("CLICK")).toBeTruthy();
  });
});

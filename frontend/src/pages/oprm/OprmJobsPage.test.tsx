import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import OprmJobsPage from "./OprmJobsPage";

function renderPage() {
  const client = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={["/oprm/jobs"]}>
        <OprmJobsPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("OprmJobsPage", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("baixa relatório pela mesma aba usando fetch e blob", async () => {
    const createObjectURL = vi.fn(() => "blob:oprm-report");
    const revokeObjectURL = vi.fn();
    vi.stubGlobal("URL", {
      ...URL,
      createObjectURL,
      revokeObjectURL,
    });

    const clickedAnchor: { href: string; download: string } = { href: "", download: "" };
    const click = vi
      .spyOn(HTMLAnchorElement.prototype, "click")
      .mockImplementation(function captureClick(this: HTMLAnchorElement) {
        clickedAnchor.href = this.href;
        clickedAnchor.download = this.download;
      });

    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = input.toString();
      if (url.includes("/api/oprm/nichocnae/jobs")) {
        return new Response(
          JSON.stringify({
            content: [
              {
                id: 74,
                cnaeCode: "4781400",
                cnaeDescription: "Comércio varejista de artigos do vestuário",
                subniche: "revendedora autônoma de moda feminina plus size",
                status: "FAILED",
                costUsd: 0.2821,
                lastStageCode: "mei-audience-segmenter",
                lastStageAt: "2026-06-18T23:50:36Z",
                reportUrl:
                  "/api/oprm/nichocnae/routine-research-cycle/stage-executions/74/report",
                trackingUrl: "/oprm/cnaes/4781400/subnichos/74",
              },
            ],
            page: 0,
            size: 20,
            totalElements: 1,
            totalPages: 1,
            first: true,
            last: true,
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      if (url.includes("stage-executions/74/report")) {
        return new Response("# Relatório da execução NichoCNAE #74", {
          status: 200,
          headers: { "Content-Type": "text/markdown" },
        });
      }
      return new Response("{}", { status: 404 });
    });
    vi.stubGlobal("fetch", fetchMock);

    renderPage();

    const downloadButton = await screen.findByRole("button", {
      name: "Baixar",
    });
    await userEvent.click(downloadButton);

    await waitFor(() => expect(click).toHaveBeenCalledTimes(1));
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost/api/oprm/nichocnae/routine-research-cycle/stage-executions/74/report",
    );
    expect(clickedAnchor.href).toBe("blob:oprm-report");
    expect(clickedAnchor.download).toBe("nicho-cnae74.md");
    expect(createObjectURL).toHaveBeenCalledTimes(1);
    expect(revokeObjectURL).toHaveBeenCalledWith("blob:oprm-report");
  });
});

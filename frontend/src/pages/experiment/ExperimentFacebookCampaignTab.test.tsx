import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";
import ExperimentFacebookCampaignTab from "./ExperimentFacebookCampaignTab";

afterEach(cleanup);

describe("ExperimentFacebookCampaignTab", () => {
  it("exibe campanha, conjunto e anúncio persistidos sem oferecer republicação", () => {
    render(
      <ExperimentFacebookCampaignTab
        isLoading={false}
        campaigns={[
          {
            id: "120251556536430326",
            name: "MUSA experimento 91",
            objective: "OUTCOME_SALES",
            status: "ACTIVE",
            metricsLastSyncedAt: "2026-09-07T03:20:00Z",
            issues: [],
            adSets: [
              {
                id: "120251556536530326",
                name: "Público MUSA",
                status: "ACTIVE",
                issues: [],
                ads: [
                  {
                    id: "120251556536810326",
                    name: "Vídeo espelho",
                    status: "ACTIVE",
                    trackingCode: "experiment_91",
                    funnelStages: [],
                  },
                ],
              },
            ],
          },
        ]}
      />,
    );

    expect(screen.getByText("MUSA experimento 91")).toBeInTheDocument();
    expect(screen.getByText("Público MUSA")).toBeInTheDocument();
    expect(screen.getByText("Vídeo espelho")).toBeInTheDocument();
    expect(screen.getAllByText("ACTIVE")).toHaveLength(3);
    expect(
      screen.getByText(/A jornada do PDE fica em Comportamento PDE/i),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Sem etapa consolidada no funil central"),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: /publicar|liberar/i }),
    ).not.toBeInTheDocument();
  });

  it("explica quando a campanha ainda não existe", () => {
    render(<ExperimentFacebookCampaignTab isLoading={false} campaigns={[]} />);

    expect(screen.getByText(/Nenhuma campanha Meta/i)).toBeInTheDocument();
  });
});

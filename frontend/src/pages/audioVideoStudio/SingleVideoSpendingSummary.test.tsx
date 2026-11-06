import { render, screen, cleanup } from "@testing-library/react";
import { afterEach, expect, it } from "vitest";
import SingleVideoSpendingSummary from "./SingleVideoSpendingSummary";
import type { VideoProductionCycle } from "../../api/salesVideo/useVideoProductionCycles";
afterEach(cleanup);
it("mostra reservas incertas sem tratá-las como custo faturado ou saldo do provider", () => {
  const cycle = {
    financialSnapshot: JSON.stringify({
      singleVideoSpendingControl: {
        receiptId: 128,
        totalLimitUsd: 15,
        retainedMaximumUsd: 8.5,
        unallocatedAuthorizationUsd: 6.5,
        uncertainOperations: 1,
        unsettledOperations: 3,
        guidance: "Máximos retidos não são custo faturado.",
        voiceRoute: {
          model: "tts-1-hd",
          voice: "nova",
          notice: "Qualidade depende de Psique.",
        },
      },
      irisProductionBrief: { taskId: 884, status: "COMPLETED" },
    }),
  } as VideoProductionCycle;
  render(<SingleVideoSpendingSummary cycle={cycle} />);
  expect(screen.getByText(/Recibo #128/)).toHaveTextContent(
    "US$ 15.0000 no total",
  );
  expect(screen.getByText("US$ 8.5000")).toBeInTheDocument();
  expect(screen.getByText("US$ 6.5000")).toBeInTheDocument();
  expect(screen.getByText(/não são custo faturado/)).toBeInTheDocument();
  expect(screen.getByText(/tts-1-hd/)).toHaveTextContent(
    "Qualidade depende de Psique",
  );
  expect(screen.getByText(/tarefa #884/)).toHaveTextContent(
    "o vídeo final ainda precisa",
  );
});
it("preserva o caminho sem recibo e mostra leitura inválida como bloqueio", () => {
  const view = render(
    <SingleVideoSpendingSummary cycle={{} as VideoProductionCycle} />,
  );
  expect(screen.queryByText("Um teto para todo o vídeo")).toBeNull();
  view.rerender(
    <SingleVideoSpendingSummary
      cycle={{ financialSnapshot: "invalid" } as VideoProductionCycle}
    />,
  );
  expect(screen.getByRole("alert")).toHaveTextContent("antes de produzir");
});

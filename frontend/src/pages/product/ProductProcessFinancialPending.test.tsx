import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { afterEach, expect, it } from "vitest";
import ProductProcessFinancialPending from "./ProductProcessFinancialPending";

afterEach(cleanup);
const pending = {
  code: "APPROVAL_REQUIRED",
  activityId: "economics",
  productVersion: "produto-v4",
  title: "Falta aprovar o limite de preparação deste ciclo",
  reason: "Abra a demonstração financeira.",
  responsible: "Você",
  actionLabel: "Ajustar limite de preparação",
  actionUrl:
    "/financial/execution?productId=7&chainId=27&cycleId=20#ceiling-AGENTS",
  afterAction: "A aprovação não retoma o processo automaticamente.",
};

/** Expõe somente o contexto carregado pela navegação, sem API ou inferência de elegibilidade. */
function Destination() {
  const location = useLocation();
  return (
    <output data-testid="return-context">{location.state.returnTo}</output>
  );
}

it.each([
  ["?learningCycleId=20&chainId=27", "20", null],
  ["?chainId=27&sourceReference=experiment:113", null, "experiment:113"],
  ["?chainId=27", "20", null],
])("preserva a identidade de retorno para %s", (search, cycle, source) => {
  render(
    <MemoryRouter
      initialEntries={[
        `/products/7/value-chain-history/processes/126/activities${search}#activity-economics`,
      ]}
    >
      <Routes>
        <Route
          path="/products/:productId/value-chain-history/processes/:processId/activities"
          element={<ProductProcessFinancialPending pending={pending} />}
        />
        <Route path="/financial/execution" element={<Destination />} />
      </Routes>
    </MemoryRouter>,
  );
  fireEvent.click(screen.getByRole("link", { name: pending.actionLabel }));
  const url = new URL(
    screen.getByTestId("return-context").textContent!,
    "http://local",
  );
  expect(url.searchParams.get("learningCycleId")).toBe(cycle);
  expect(url.searchParams.get("sourceReference")).toBe(source);
  expect(url.searchParams.get("chainId")).toBe("27");
  expect(url.hash).toBe("#activity-economics");
});

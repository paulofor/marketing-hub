import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import MdsArtifactsPage from "./MdsArtifactsPage";

vi.mock("../../api/mds/useMdsAdmin", () => ({
  useMdsArtifacts: vi.fn(() => ({
    isLoading: false,
    isError: false,
    data: {
      requestId: 12,
      artifacts: [
        {
          artifactId: 301,
          artifactType: "sourceDocument",
          schemaVersion: "v1",
          version: "v1",
          status: "VALIDATED",
          parentArtifactIds: [],
          childArtifactIds: [401],
          content: { sourceUrl: "https://example.org/paper" },
        },
        {
          artifactId: 401,
          artifactType: "mechanismSpec",
          schemaVersion: "v1",
          version: "v2",
          status: "APPROVED",
          parentArtifactIds: [301],
          childArtifactIds: [],
          content: { intervention: "micro-ciclo" },
        },
      ],
      lineage: [
        {
          id: 9001,
          parentArtifactId: 301,
          childArtifactId: 401,
          relationType: "DERIVED_FROM",
        },
      ],
    },
  })),
}));

describe("MdsArtifactsPage", () => {
  it("renderiza envelope canônico e navegação de lineage", () => {
    render(
      <MemoryRouter initialEntries={["/mds/requests/12/artifacts"]}>
        <Routes>
          <Route path="/mds/requests/:requestId/artifacts" element={<MdsArtifactsPage />} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText("MDS · Artefatos da request #12")).toBeTruthy();
    expect(screen.getByText(/Envelope canônico/)).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: /#401 · APPROVED/i }));
    expect(screen.getByText(/\"artifactType\": \"mechanismSpec\"/)).toBeTruthy();
    expect(screen.getByText(/Abrir #301/)).toBeTruthy();
    expect(screen.getByText(/DERIVED_FROM/)).toBeTruthy();
  });
});

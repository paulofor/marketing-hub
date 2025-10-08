import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import "@testing-library/jest-dom/vitest";
import JourneyTemplatesPage from "./JourneyTemplatesPage";

vi.mock("../../api/journey/useJourneyTemplates", () => ({
  useJourneyTemplates: () => ({
    data: {
      content: [
        {
          id: 1,
          name: "Lifecycle Pós-Clique Lead Ads 14d",
          objective: "Converter curiosidade em relacionamento contínuo",
          phases: ["ATTENTION", "INTEREST", "DESIRE", "ACTION"],
          preferredChannel: null,
          tags: ["facebook", "lifecycle"],
          metadata: {},
          createdAt: null,
          updatedAt: undefined,
        },
      ],
      totalElements: 1,
      totalPages: 1,
      number: 0,
    },
    isLoading: false,
  }),
}));

describe("JourneyTemplatesPage", () => {
  it("exibe templates mesmo quando timestamps não estão disponíveis", () => {
    render(<JourneyTemplatesPage />);

    expect(
      screen.getByRole("heading", {
        level: 2,
        name: "Lifecycle Pós-Clique Lead Ads 14d",
      }),
    ).toBeInTheDocument();
    expect(screen.getByText("Multicanal")).toBeInTheDocument();
    expect(screen.getByText("ATTENTION • INTEREST • DESIRE • ACTION")).toBeInTheDocument();
    expect(screen.getByText(/Criado em\s+—/)).toBeInTheDocument();
    expect(screen.getByText(/Atualizado em\s+—/)).toBeInTheDocument();
  });
});

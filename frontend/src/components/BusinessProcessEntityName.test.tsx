import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";
import BusinessProcessEntityName from "./BusinessProcessEntityName";

describe("BusinessProcessEntityName", () => {
  afterEach(cleanup);

  it("identifica processo somente com o ícone de fluxo", () => {
    render(<BusinessProcessEntityName kind="process" name="Homologar o PDE" />);

    const process = screen.getByTitle("Processo");
    expect(process.querySelector(".lucide-workflow")).toBeInTheDocument();
    expect(
      process.querySelector(".lucide-clipboard-list"),
    ).not.toBeInTheDocument();
    expect(screen.getByText("Homologar o PDE")).toBeInTheDocument();
  });

  it("identifica atividade somente com o ícone de lista", () => {
    render(
      <BusinessProcessEntityName
        kind="activity"
        name="Validar experiência humana"
      />,
    );

    const activity = screen.getByTitle("Atividade");
    expect(
      activity.querySelector(".lucide-clipboard-list"),
    ).toBeInTheDocument();
    expect(activity.querySelector(".lucide-workflow")).not.toBeInTheDocument();
    expect(screen.getByText("Validar experiência humana")).toBeInTheDocument();
  });
});

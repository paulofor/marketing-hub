import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import "@testing-library/jest-dom/vitest";
import JourneyTemplateForm from "./JourneyTemplateForm";

describe("JourneyTemplateForm", () => {
  it("normaliza dados e envia payload estruturado", async () => {
    const handleSubmit = vi.fn().mockResolvedValue(undefined);
    const user = userEvent.setup();

    render(<JourneyTemplateForm onSubmit={handleSubmit} submitLabel="Salvar" />);

    await user.type(
      screen.getByLabelText(/Nome do template/i),
      "  Template Growth  ",
    );
    await user.type(
      screen.getByLabelText("Nome da etapa 1"),
      "  Boas-vindas  ",
    );
    await user.type(
      screen.getByPlaceholderText("Adicionar nova tag"),
      " onboarding ",
    );
    await user.click(screen.getByRole("button", { name: "Adicionar tag" }));

    await user.click(screen.getByRole("button", { name: "Salvar" }));

    expect(handleSubmit).toHaveBeenCalledTimes(1);
    const payload = handleSubmit.mock.calls[0][0];

    expect(payload.template).toMatchObject({
      name: "Template Growth",
      phases: ["ATTENTION", "INTEREST", "DESIRE", "ACTION"],
      tags: ["onboarding"],
    });

    expect(payload.steps).toHaveLength(1);
    expect(payload.steps[0]).toMatchObject({
      name: "Boas-vindas",
      phase: "ATTENTION",
      stimulusType: "EMAIL",
      position: 1,
    });
  });
});

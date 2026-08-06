import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it } from "vitest";
import { EvaluationFailureDetails } from "./PersonaLibraryPage";

afterEach(cleanup);

describe("EvaluationFailureDetails", () => {
  it("disponibiliza a causa completa da falha sem poluir o resumo", async () => {
    const user = userEvent.setup();
    const error =
      "java.lang.IllegalStateException: Codex falhou\nCaused by: timeout no provedor\n\tat Worker.evaluate(Worker.java:42)";

    render(<EvaluationFailureDetails error={error} />);

    expect(screen.getByRole("alert")).toHaveTextContent(
      "Falha técnica: java.lang.IllegalStateException: Codex falhou",
    );
    expect(screen.getByText("Ver detalhes técnicos")).toBeVisible();

    await user.click(screen.getByText("Ver detalhes técnicos"));

    const technicalDetail = screen.getByText(/Caused by: timeout no provedor/);
    expect(technicalDetail).toBeVisible();
    expect(technicalDetail).toHaveTextContent(
      "Worker.evaluate(Worker.java:42)",
    );
  });
});

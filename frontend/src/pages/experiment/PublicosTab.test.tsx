import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import PublicosTab from "./PublicosTab";

describe("PublicosTab", () => {
  it("renders form", () => {
    render(<PublicosTab />);
    expect(screen.getByText("Salvar")).toBeTruthy();
  });
});

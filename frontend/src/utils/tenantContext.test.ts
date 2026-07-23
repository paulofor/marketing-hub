import { describe, expect, it, beforeEach } from "vitest";
import { getTenantContextSnapshot, setTenantContext } from "./tenantContext";

describe("tenantContext", () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it("mantém referência estável quando o contexto não muda", () => {
    const firstSnapshot = getTenantContextSnapshot();
    const secondSnapshot = getTenantContextSnapshot();

    expect(secondSnapshot).toBe(firstSnapshot);
  });

  it("atualiza o snapshot somente quando o contexto muda", () => {
    const firstSnapshot = getTenantContextSnapshot();

    setTenantContext({ tenantId: "default", userEmail: "time@marketinghub.io" });
    const sameSnapshot = getTenantContextSnapshot();

    setTenantContext({ tenantId: "musa", userEmail: "time@marketinghub.io" });
    const changedSnapshot = getTenantContextSnapshot();

    expect(sameSnapshot).toBe(firstSnapshot);
    expect(changedSnapshot).not.toBe(firstSnapshot);
    expect(changedSnapshot.tenantId).toBe("musa");
  });
});

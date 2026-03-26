import { FormEvent, useState } from "react";
import { setTenantContext, useTenantContext } from "../utils/tenantContext";

interface Props {
  className?: string;
}

export function TenantContextBanner({ className }: Props) {
  const context = useTenantContext();
  const [formState, setFormState] = useState({ tenantId: context.tenantId, userEmail: context.userEmail });

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    if (!formState.tenantId.trim()) {
      return;
    }
    const normalized = {
      tenantId: formState.tenantId.trim(),
      userEmail: formState.userEmail.trim() || "time@marketinghub.io",
    };
    setTenantContext(normalized);
  };

  return (
    <div className={`alert alert-info ${className ?? ""}`}>
      <form className="row g-3 align-items-end" onSubmit={handleSubmit}>
        <div className="col-md-4">
          <label className="form-label">Tenant ativo</label>
          <input
            className="form-control"
            value={formState.tenantId}
            onChange={(event) => setFormState((prev) => ({ ...prev, tenantId: event.target.value }))}
            placeholder="ex.: tenant_acme"
          />
        </div>
        <div className="col-md-4">
          <label className="form-label">Usuário atual</label>
          <input
            className="form-control"
            value={formState.userEmail}
            onChange={(event) => setFormState((prev) => ({ ...prev, userEmail: event.target.value }))}
            placeholder="operador@marketinghub.io"
          />
        </div>
        <div className="col-md-4 d-flex gap-2 align-items-end">
          <button type="submit" className="btn btn-primary">
            Aplicar contexto
          </button>
          <span className="text-muted small">
            Os valores são enviados automaticamente em cada chamada da API de vídeos.
          </span>
        </div>
      </form>
    </div>
  );
}

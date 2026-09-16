import { useState } from "react";
import { Link } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import {
  catalogError,
  useCatalogCommand,
  useOpalaAdoption,
} from "../../api/catalogoVivo/useCatalogoVivo";

export default function OpalaAdoptionPanel({
  productId,
  cycleId,
  revision,
}: {
  productId: number;
  cycleId: number;
  revision: number;
}) {
  const query = useOpalaAdoption(productId, cycleId);
  const command = useCatalogCommand();
  const client = useQueryClient();
  const [operatorName, setOperatorName] = useState("");
  const value = query.data;
  return (
    <section
      className="card card-body mb-3"
      aria-label="Preparação Opala com os agentes"
    >
      <h3 className="h5">Preparação Opala com os agentes</h3>
      {query.isPending && (
        <p role="status">Consultando a integração do ciclo…</p>
      )}
      {query.isError && (
        <p role="alert">
          Não foi possível consultar a integração. {catalogError(query.error)}
        </p>
      )}
      {value && (
        <>
          <p>{value.reason}</p>
          {value.preparationUrl && (
            <Link
              className="btn btn-primary align-self-start mb-2"
              to={value.preparationUrl}
            >
              Acompanhar preparação dos agentes
            </Link>
          )}
          {value.canAdopt && (
            <form
              onSubmit={async (event) => {
                event.preventDefault();
                try {
                  await command.mutateAsync({
                    path: `/products/${productId}/cycles/${cycleId}/adoption`,
                    body: {
                      expectedRevision: revision,
                      operatorName,
                      reason:
                        "Integração explícita ao Catálogo Vivo — Piloto Opala pela tela do ciclo.",
                    },
                  });
                  await client.invalidateQueries({
                    queryKey: ["learning-cycles"],
                  });
                } catch {
                  /* O erro do backend é apresentado abaixo. */
                }
              }}
            >
              <label className="form-label">
                Responsável *
                <input
                  className="form-control"
                  required
                  maxLength={160}
                  value={operatorName}
                  disabled={command.isPending}
                  onChange={(e) => setOperatorName(e.target.value)}
                />
              </label>
              <button
                className="btn btn-primary d-block"
                disabled={command.isPending || !operatorName.trim()}
              >
                {command.isPending && (
                  <span className="spinner-border spinner-border-sm me-2" />
                )}
                Integrar e iniciar preparação
              </button>
              <p className="mt-2">
                Mantém orçamento, janela, aprovações e histórico do ciclo. A
                campanha continua sujeita à homologação e à autorização de
                ativação.
              </p>
            </form>
          )}
          <Link to={value.catalogUrl}>
            Consultar instruções do Catálogo Vivo
          </Link>
        </>
      )}
      {command.isError && (
        <p className="alert alert-danger mt-2" role="alert">
          {catalogError(command.error)}
        </p>
      )}
    </section>
  );
}

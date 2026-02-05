import { useMemo } from "react";
import { Clock3, Search } from "lucide-react";
import PageTitle from "../../components/PageTitle";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import { useTargetingRecentRequests } from "../../api/targeting/useTargetingRecentRequests";
import type { TargetingRecentRequest } from "../../api/targeting/types";
import "./TargetingRecentQueriesPage.css";

function normalizeList(items?: string[] | null) {
  if (!Array.isArray(items)) return [];
  return items.filter((item) => item && item.trim().length > 0);
}

function formatDateTime(value?: string | null) {
  if (!value) return "-";
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return "-";
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(parsed);
}

export default function TargetingRecentQueriesPage() {
  useBreadcrumbs([
    { label: "IA" },
    { label: "Consultas recentes do Meta Ads" },
  ]);

  const {
    data,
    isLoading,
    isFetching,
    error,
  } = useTargetingRecentRequests(10);

  const requests = useMemo(() => {
    return (data ?? []).map((item: TargetingRecentRequest) => ({
      ...item,
      seed_keywords: normalizeList(item.seed_keywords),
      meta_ads_keywords: normalizeList(item.meta_ads_keywords),
    }));
  }, [data]);

  return (
    <div className="targeting-recent-queries">
      <div className="d-flex align-items-start justify-content-between flex-wrap gap-3">
        <div>
          <PageTitle>Consultas recentes do Meta Ads</PageTitle>
          <p className="text-body-secondary">
            Veja as 10 últimas buscas de targeting e acompanhe quais palavras-chave
            seed deram origem às opções encontradas no Meta Ads.
          </p>
        </div>
        <div className="targeting-recent-queries__status">
          <span className="badge text-bg-light border">
            <Clock3 size={14} className="me-1" />
            {isFetching ? "Atualizando..." : "Atualizado"}
          </span>
        </div>
      </div>

      {isLoading ? (
        <div className="d-flex align-items-center gap-2 mt-4">
          <span
            className="spinner-border spinner-border-sm text-primary"
            role="status"
            aria-hidden="true"
          />
          <span>Carregando consultas recentes...</span>
        </div>
      ) : error ? (
        <div className="alert alert-danger mt-4" role="alert">
          Não foi possível carregar as consultas recentes. Tente novamente.
        </div>
      ) : requests.length === 0 ? (
        <div className="alert alert-info mt-4" role="alert">
          Nenhuma consulta recente encontrada.
        </div>
      ) : (
        <div className="table-responsive mt-4">
          <table className="table table-striped align-middle">
            <thead>
              <tr>
                <th scope="col">Consulta</th>
                <th scope="col">Seed keywords</th>
                <th scope="col">Keywords recuperadas</th>
              </tr>
            </thead>
            <tbody>
              {requests.map((item) => (
                <tr key={item.id}>
                  <td>
                    <div className="fw-semibold">{item.descricao}</div>
                    <div className="text-body-secondary small">
                      <Search size={14} className="me-1" />
                      {item.created_at
                        ? formatDateTime(item.created_at)
                        : "Data não informada"}
                    </div>
                  </td>
                  <td>
                    <div className="d-flex flex-wrap gap-2">
                      {item.seed_keywords.length > 0 ? (
                        item.seed_keywords.map((seed) => (
                          <span
                            key={`${item.id}-seed-${seed}`}
                            className="badge rounded-pill text-bg-light border"
                          >
                            {seed}
                          </span>
                        ))
                      ) : (
                        <span className="text-body-secondary small">
                          Sem seeds registradas.
                        </span>
                      )}
                    </div>
                  </td>
                  <td>
                    <div className="d-flex flex-wrap gap-2">
                      {item.meta_ads_keywords.length > 0 ? (
                        item.meta_ads_keywords.map((keyword) => (
                          <span
                            key={`${item.id}-meta-${keyword}`}
                            className="badge rounded-pill text-bg-primary-subtle border text-primary-emphasis"
                          >
                            {keyword}
                          </span>
                        ))
                      ) : (
                        <span className="text-body-secondary small">
                          Nenhuma keyword recuperada.
                        </span>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

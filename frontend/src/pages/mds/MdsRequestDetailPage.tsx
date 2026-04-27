import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useMdsRequestDetail } from "../../api/mds/useMdsAdmin";

export default function MdsRequestDetailPage() {
  const { requestId } = useParams();
  const id = requestId ? Number(requestId) : null;
  const detailQuery = useMdsRequestDetail(id);

  if (detailQuery.isLoading) {
    return <div className="d-flex justify-content-center py-5"><span className="spinner-border text-primary" aria-hidden="true" /></div>;
  }

  if (detailQuery.isError || !detailQuery.data) {
    return <div className="alert alert-danger mb-0">Não foi possível carregar o detalhe da request.</div>;
  }

  const detail = detailQuery.data;

  return (
    <div className="d-flex flex-column gap-4">
      <header>
        <PageTitle>MDS · Request #{detail.requestId}</PageTitle>
        <p className="text-secondary mb-0">Diagnóstico da execução com timeline de estágios.</p>
      </header>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-2">
          <p className="mb-0"><strong>Status:</strong> {detail.status}</p>
          <p className="mb-0"><strong>Mercado:</strong> {detail.market}</p>
          <p className="mb-0"><strong>Dor:</strong> {detail.problem}</p>
          <p className="mb-0"><strong>Resultado esperado:</strong> {detail.desiredOutcome}</p>
          <p className="mb-0"><strong>Classificação da falha:</strong> {detail.failureClassification}</p>
          <div className="d-flex gap-2 flex-wrap">
            <Link className="btn btn-outline-secondary btn-sm" to={`/mds/requests/${detail.requestId}/artifacts`}>Ver artefatos</Link>
            <Link className="btn btn-outline-dark btn-sm" to={`/mds/reports/${detail.requestId}`}>Ver relatório</Link>
          </div>
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <h2 className="h6 mb-0">Timeline</h2>
          {detail.timeline.length === 0 ? <div className="alert alert-secondary mb-0">Sem eventos registrados.</div> : (
            <ul className="list-group list-group-flush">
              {detail.timeline.map((event) => (
                <li className="list-group-item px-0" key={event.eventId}>
                  <p className="mb-1"><strong>{event.stageName}</strong> · {event.eventType}</p>
                  <p className="mb-1">{event.message}</p>
                  <small className="text-secondary">{event.createdAt}</small>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>
    </div>
  );
}

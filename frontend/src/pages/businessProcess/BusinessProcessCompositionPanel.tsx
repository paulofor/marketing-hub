import { Link } from "react-router-dom";
import type { BusinessProcessComposition } from "../../api/businessProcess/types";
import BusinessProcessEntityName from "../../components/BusinessProcessEntityName";
import "./BusinessProcessesPage.css";

export default function BusinessProcessCompositionPanel({
  composition,
  loading,
  unavailable,
}: {
  composition?: BusinessProcessComposition;
  loading: boolean;
  unavailable: boolean;
}) {
  if (loading) {
    return (
      <section
        className="card card-body mb-3 business-process-composition"
        aria-label="Composição do processo"
      >
        <span className="text-body-secondary">Carregando composição...</span>
      </section>
    );
  }

  if (unavailable || !composition) {
    return (
      <section
        className="alert alert-warning mb-3"
        aria-label="Composição do processo indisponível"
      >
        A composição oficial está temporariamente indisponível. O diagrama
        continua visível, mas os vínculos não serão inferidos pela tela.
      </section>
    );
  }

  if (composition.process.processType === "SUBPROCESS") {
    return (
      <section
        className="card card-body mb-3 business-process-composition"
        aria-label="Hierarquia deste subprocesso"
      >
        <div className="business-process-composition__heading">
          <div>
            <span className="business-process-composition__eyebrow">
              Especialidade delegada
            </span>
            <h2 className="h5 mb-1">Este é um subprocesso</h2>
            <p className="text-body-secondary mb-0">
              Ele executa uma capacidade específica, mas a decisão de avançar
              continua no processo de valor pai.
            </p>
          </div>
        </div>
        {composition.parentProcess ? (
          <div className="business-process-composition__trail mt-3">
            <Link
              className="business-process-composition__parent"
              to={`/business-processes?processId=${composition.parentProcess.id}`}
            >
              <span>Processo de valor pai</span>
              <strong>
                <BusinessProcessEntityName
                  kind="process"
                  name={composition.parentProcess.name}
                  iconSize={17}
                />
              </strong>
              <small>v{composition.parentProcess.versionNumber}</small>
            </Link>
            <span className="business-process-composition__arrow" aria-hidden>
              →
            </span>
            <div className="business-process-composition__current">
              <span>Subprocesso atual</span>
              <strong>
                <BusinessProcessEntityName
                  kind="process"
                  name={composition.process.name}
                  iconSize={17}
                />
              </strong>
              <small>v{composition.process.versionNumber}</small>
            </div>
          </div>
        ) : (
          <div className="alert alert-warning mt-3 mb-0">
            O processo de valor pai publicado não foi localizado.
          </div>
        )}
      </section>
    );
  }

  return (
    <section
      className="card card-body mb-3 business-process-composition"
      aria-label="Composição do processo de valor"
    >
      <div className="business-process-composition__heading">
        <div>
          <span className="business-process-composition__eyebrow">
            Processo principal + especialidades
          </span>
          <h2 className="h5 mb-1">Como este processo é composto</h2>
          <p className="text-body-secondary mb-0">
            O processo principal mantém a decisão de negócio. Cada subprocesso
            executa uma capacidade especializada sem duplicar a orquestração.
          </p>
        </div>
        <span className="badge text-bg-primary business-process-composition__count">
          {composition.subprocessCount}{" "}
          {composition.subprocessCount === 1
            ? "subprocesso em uso"
            : "subprocessos em uso"}
        </span>
      </div>

      {composition.subprocesses.length > 0 ? (
        <div className="business-process-composition__children mt-3">
          {composition.subprocesses.map((subprocess) => (
            <Link
              key={subprocess.id}
              className="business-process-composition__child"
              to={`/business-processes?processId=${subprocess.id}`}
            >
              <span className="business-process-composition__child-type">
                Subprocesso especializado
              </span>
              <strong>
                <BusinessProcessEntityName
                  kind="process"
                  name={subprocess.name}
                  iconSize={17}
                />
              </strong>
              <span>{subprocess.purpose}</span>
              <small>
                Responsável: {subprocess.ownerName} · v
                {subprocess.versionNumber}
              </small>
              <span className="business-process-composition__open">
                Abrir subprocesso →
              </span>
            </Link>
          ))}
        </div>
      ) : (
        <div className="business-process-composition__empty mt-3">
          <strong>Nenhum subprocesso em uso neste momento.</strong>
          <span>
            Se uma nova capacidade especializada for publicada e vinculada a uma
            atividade, ela aparecerá automaticamente nesta composição.
          </span>
        </div>
      )}
      {composition.subprocesses.length > 0 ? (
        <p className="business-process-composition__future mb-0 mt-3">
          A composição é extensível: novos subprocessos publicados e vinculados
          aparecerão aqui automaticamente.
        </p>
      ) : null}
    </section>
  );
}

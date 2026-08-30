import axios from "axios";
import { FormEvent, useState } from "react";
import { ExternalLink, RefreshCw, SearchCheck } from "lucide-react";
import { toast } from "react-toastify";
import {
  useObserveProductDiscoveryMetaAd,
  useProductDiscoverySupervisedMetaSession,
  useResumeProductDiscoveryWithMetaEvidence,
} from "../../api/productDiscovery/useProductDiscoverySupervisedMetaSession";
import "./ArgosMetaSupervisedSession.css";

function errorMessage(error: unknown) {
  if (!axios.isAxiosError(error)) return "Não foi possível concluir a ação.";
  const body = error.response?.data as
    { detail?: string; message?: string; error?: string } | undefined;
  return body?.detail ?? body?.message ?? body?.error ?? "A ação foi recusada.";
}

function isMissingSession(error: unknown) {
  return axios.isAxiosError(error) && error.response?.status === 404;
}

function formatDate(value?: string) {
  return value
    ? new Intl.DateTimeFormat("pt-BR", {
        dateStyle: "short",
        timeStyle: "short",
      }).format(new Date(value))
    : "Ainda não observada";
}

export default function ArgosMetaSupervisedSession({
  cycleId,
}: {
  cycleId: number;
}) {
  const sessionQuery = useProductDiscoverySupervisedMetaSession(cycleId);
  const observe = useObserveProductDiscoveryMetaAd(cycleId);
  const resume = useResumeProductDiscoveryWithMetaEvidence(cycleId);
  const [adReference, setAdReference] = useState("");
  const [advertiserName, setAdvertiserName] = useState("");
  const [adLibraryUrl, setAdLibraryUrl] = useState("");
  const [adText, setAdText] = useState("");
  const [publisherPlatform, setPublisherPlatform] = useState<
    "INSTAGRAM" | "FACEBOOK"
  >("INSTAGRAM");
  const [formatType, setFormatType] = useState("VIDEO");
  const [destinationUrl, setDestinationUrl] = useState("");
  const [pageActive, setPageActive] = useState(true);
  const [commercialSignal, setCommercialSignal] = useState(false);

  if (sessionQuery.isLoading) {
    return (
      <section
        className="argos-meta-session card"
        aria-label="Sessão Meta de Argos"
      >
        <div className="card-body">Carregando sessão supervisionada...</div>
      </section>
    );
  }
  if (sessionQuery.isError && isMissingSession(sessionQuery.error)) return null;
  if (sessionQuery.isError || !sessionQuery.data) {
    return (
      <div className="alert alert-danger">
        Não foi possível carregar a sessão supervisionada da Biblioteca Meta.
      </div>
    );
  }
  const session = sessionQuery.data;

  const submitObservation = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await observe.mutateAsync({
        adReference,
        advertiserName,
        adLibraryUrl,
        adText,
        publisherPlatforms: [publisherPlatform],
        formatType: formatType || undefined,
        destinationUrl: destinationUrl || undefined,
        pageActive,
        commercialSignal,
      });
      setAdReference("");
      setAdvertiserName("");
      setAdLibraryUrl("");
      setAdText("");
      setDestinationUrl("");
      toast.success("Observação Meta registrada para Argos.");
    } catch (error) {
      toast.error(errorMessage(error));
    }
  };

  const resumeResearch = async () => {
    try {
      await resume.mutateAsync();
      toast.success("Nova tentativa de Argos enviada para a fila.");
    } catch (error) {
      toast.error(errorMessage(error));
    }
  };

  return (
    <section
      className="argos-meta-session card"
      aria-label="Sessão Meta de Argos"
    >
      <div className="card-body">
        <header className="argos-meta-session__header">
          <div>
            <span>Pesquisa supervisionada</span>
            <h2>Confirmar anúncios e linguagem no Instagram</h2>
            <p>
              Argos preparou a consulta. Abra a fonte oficial, registre apenas o
              que estiver visível e depois solicite a reanálise.
            </p>
          </div>
          <SearchCheck size={30} aria-hidden="true" />
        </header>

        <div className="argos-meta-session__source">
          <div>
            <strong>Consulta #{session.investigationId}</strong>
            <span>{session.query}</span>
            <small>{session.collectionReason}</small>
          </div>
          <a
            className="btn btn-primary"
            href={session.searchUrl}
            target="_blank"
            rel="noreferrer"
          >
            Abrir Biblioteca Meta <ExternalLink size={16} />
          </a>
        </div>

        <div className="argos-meta-session__metrics">
          <div>
            <span>Aderentes</span>
            <strong>{session.adsObserved}</strong>
          </div>
          <div>
            <span>Ativos no Instagram</span>
            <strong>{session.activeAds}</strong>
          </div>
          <div>
            <span>Anunciantes</span>
            <strong>{session.advertisersObserved}</strong>
          </div>
          <div>
            <span>Última observação</span>
            <strong>{formatDate(session.latestObservationAt)}</strong>
          </div>
        </div>

        <p className="argos-meta-session__interpretation">
          {session.interpretation}
        </p>

        {session.canRegisterObservation ? (
          <form
            className="argos-meta-session__form"
            onSubmit={submitObservation}
          >
            <div className="argos-meta-session__form-heading">
              <h3>Registrar anúncio observado</h3>
              <p>
                Não copie dados pessoais. Atividade e investimento aparente não
                são vendas comprovadas.
              </p>
            </div>
            <label>
              ID do anúncio *
              <input
                className="form-control"
                required
                maxLength={120}
                value={adReference}
                onChange={(event) => setAdReference(event.target.value)}
              />
            </label>
            <label>
              Anunciante *
              <input
                className="form-control"
                required
                maxLength={255}
                value={advertiserName}
                onChange={(event) => setAdvertiserName(event.target.value)}
              />
            </label>
            <label>
              Plataforma observada *
              <select
                className="form-select"
                value={publisherPlatform}
                onChange={(event) =>
                  setPublisherPlatform(
                    event.target.value as "INSTAGRAM" | "FACEBOOK",
                  )
                }
              >
                <option value="INSTAGRAM">Instagram</option>
                <option value="FACEBOOK">Somente Facebook</option>
              </select>
            </label>
            <label>
              Formato
              <select
                className="form-select"
                value={formatType}
                onChange={(event) => setFormatType(event.target.value)}
              >
                <option value="VIDEO">Vídeo / Reel</option>
                <option value="IMAGE">Imagem</option>
                <option value="CAROUSEL">Carrossel</option>
                <option value="OTHER">Outro</option>
              </select>
            </label>
            <label className="argos-meta-session__wide">
              URL oficial do anúncio *
              <input
                className="form-control"
                type="url"
                required
                placeholder="https://www.facebook.com/ads/library/..."
                value={adLibraryUrl}
                onChange={(event) => setAdLibraryUrl(event.target.value)}
              />
            </label>
            <label className="argos-meta-session__wide">
              Texto comercial visível *
              <textarea
                className="form-control"
                required
                rows={4}
                maxLength={5000}
                value={adText}
                onChange={(event) => setAdText(event.target.value)}
              />
            </label>
            <label className="argos-meta-session__wide">
              Página de destino observada
              <input
                className="form-control"
                type="url"
                value={destinationUrl}
                onChange={(event) => setDestinationUrl(event.target.value)}
              />
            </label>
            <div className="argos-meta-session__checks argos-meta-session__wide">
              <label>
                <input
                  type="checkbox"
                  checked={pageActive}
                  onChange={(event) => setPageActive(event.target.checked)}
                />
                Página e anúncio estão ativos
              </label>
              <label>
                <input
                  type="checkbox"
                  checked={commercialSignal}
                  onChange={(event) =>
                    setCommercialSignal(event.target.checked)
                  }
                />
                Há preço, oferta ou checkout verificável
              </label>
            </div>
            <button
              className="btn btn-outline-primary argos-meta-session__wide"
              disabled={observe.isPending}
            >
              {observe.isPending ? "Registrando..." : "Registrar observação"}
            </button>
          </form>
        ) : null}

        {session.items.length > 0 ? (
          <div className="argos-meta-session__evidence">
            <h3>Linguagem comercial observada</h3>
            {session.items.map((item) => (
              <article key={item.metaAdId}>
                <div>
                  <strong>{item.advertiserName ?? item.metaAdId}</strong>
                  <span>
                    {item.active ? "Ativo" : "Inativo"} ·{" "}
                    {item.formatTypes.join(", ") || "Formato não informado"}
                  </span>
                </div>
                {item.adTexts.map((text) => (
                  <p key={text}>{text}</p>
                ))}
              </article>
            ))}
          </div>
        ) : null}

        <div
          className={`argos-meta-session__resume ${session.canResume ? "is-ready" : ""}`}
        >
          <div>
            <strong>Próxima ação</strong>
            <p>{session.resumeReason}</p>
          </div>
          <button
            type="button"
            className="btn btn-success"
            disabled={!session.canResume || resume.isPending}
            onClick={resumeResearch}
          >
            <RefreshCw size={16} />
            {resume.isPending ? "Enviando..." : "Reanalisar com Argos"}
          </button>
        </div>
      </div>
    </section>
  );
}

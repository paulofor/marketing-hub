import axios from "axios";
import { Check, ExternalLink, GitCommit, Plus, X } from "lucide-react";
import { FormEvent, useMemo, useState } from "react";
import {
  type ProductProcessCommit,
  useRegisterProductProcessCommit,
} from "../api/product/useProductProcessCommits";
import { useTenantContext } from "../utils/tenantContext";

const recordedAtFormatter = new Intl.DateTimeFormat("pt-BR", {
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
  hour12: false,
  timeZone: "UTC",
  timeZoneName: "short",
});

const DEFAULT_REPOSITORY = "paulofor/marketing-hub";
const FULL_SHA = /^[0-9a-f]{40}([0-9a-f]{24})?$/i;

type Props = {
  productId: string | number;
  processDefinitionId: number;
  processName: string;
  commits: ProductProcessCommit[];
};

function defaultCommitUrl(repositoryName: string, commitSha: string) {
  return /^[\w.-]+\/[\w.-]+$/.test(repositoryName)
    ? `https://github.com/${repositoryName}/commit/${commitSha}`
    : "";
}

function errorMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    return (
      error.response?.data?.message ||
      "Não foi possível registrar o commit neste processo."
    );
  }
  return "Não foi possível registrar o commit neste processo.";
}

function formatRecordedAt(value: string) {
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime())
    ? "Data de registro indisponível"
    : recordedAtFormatter.format(parsed);
}

export default function ProductProcessCommitLedger({
  productId,
  processDefinitionId,
  processName,
  commits,
}: Props) {
  const tenantContext = useTenantContext();
  const mutation = useRegisterProductProcessCommit(productId);
  const [editing, setEditing] = useState(false);
  const [repositoryName, setRepositoryName] = useState(DEFAULT_REPOSITORY);
  const [commitSha, setCommitSha] = useState("");
  const [commitSummary, setCommitSummary] = useState("");
  const [commitUrl, setCommitUrl] = useState("");
  const [validationError, setValidationError] = useState("");
  const orderedCommits = useMemo(
    () =>
      [...commits].sort(
        (first, second) =>
          Date.parse(second.recordedAt) - Date.parse(first.recordedAt),
      ),
    [commits],
  );

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    const normalizedRepository = repositoryName.trim();
    const normalizedSha = commitSha.trim().toLowerCase();
    const normalizedSummary = commitSummary.trim();
    if (!FULL_SHA.test(normalizedSha)) {
      setValidationError("Informe o SHA completo, com 40 ou 64 caracteres.");
      return;
    }
    if (!normalizedRepository || !normalizedSummary) {
      setValidationError("Informe o repositório e o resumo funcional.");
      return;
    }
    if (!tenantContext.userEmail.trim()) {
      setValidationError(
        "Informe o usuário atual no contexto do Marketing Hub.",
      );
      return;
    }
    setValidationError("");
    try {
      await mutation.mutateAsync({
        processDefinitionId,
        repositoryName: normalizedRepository,
        commitSha: normalizedSha,
        commitSummary: normalizedSummary,
        commitUrl:
          commitUrl.trim() ||
          defaultCommitUrl(normalizedRepository, normalizedSha) ||
          null,
        recordedBy: tenantContext.userEmail.trim(),
      });
      setCommitSha("");
      setCommitSummary("");
      setCommitUrl("");
      setEditing(false);
    } catch (error) {
      setValidationError(errorMessage(error));
    }
  };

  return (
    <section
      className="product-process-commits"
      aria-label={`Commits de ${processName}`}
    >
      <div className="product-process-commits__heading">
        <span>
          <GitCommit size={16} aria-hidden="true" /> Commits realizados
        </span>
        <button
          className="btn btn-sm btn-outline-primary"
          type="button"
          onClick={() => {
            setValidationError("");
            setEditing((current) => !current);
          }}
        >
          {editing ? (
            <X size={14} aria-hidden="true" />
          ) : (
            <Plus size={14} aria-hidden="true" />
          )}
          {editing ? "Cancelar" : "Registrar commit"}
        </button>
      </div>

      {orderedCommits.length === 0 ? (
        <p className="product-process-commits__empty">
          Nenhum commit registrado para este produto neste processo.
        </p>
      ) : (
        <ul className="product-process-commits__list">
          {orderedCommits.map((commit) => (
            <li key={commit.id}>
              <div>
                <strong>{commit.commitSummary}</strong>
                <small>
                  {commit.repositoryName} · processo v{commit.processVersion}
                </small>
              </div>
              <div className="product-process-commits__identity">
                {commit.commitUrl ? (
                  <a
                    href={commit.commitUrl}
                    target="_blank"
                    rel="noreferrer"
                    title={commit.commitSha}
                  >
                    {commit.commitSha.slice(0, 12)}
                    <ExternalLink size={13} aria-hidden="true" />
                  </a>
                ) : (
                  <code title={commit.commitSha}>
                    {commit.commitSha.slice(0, 12)}
                  </code>
                )}
                <small>
                  {formatRecordedAt(commit.recordedAt)} · {commit.recordedBy}
                </small>
              </div>
            </li>
          ))}
        </ul>
      )}

      {editing ? (
        <form className="product-process-commits__form" onSubmit={handleSubmit}>
          <div>
            <label htmlFor={`commit-repository-${processDefinitionId}`}>
              Repositório
            </label>
            <input
              id={`commit-repository-${processDefinitionId}`}
              className="form-control form-control-sm"
              maxLength={160}
              required
              value={repositoryName}
              onChange={(event) => setRepositoryName(event.target.value)}
            />
          </div>
          <div>
            <label htmlFor={`commit-sha-${processDefinitionId}`}>
              SHA completo
            </label>
            <input
              id={`commit-sha-${processDefinitionId}`}
              className="form-control form-control-sm"
              maxLength={64}
              minLength={40}
              pattern="[0-9a-fA-F]{40}([0-9a-fA-F]{24})?"
              placeholder="40 ou 64 caracteres"
              required
              spellCheck={false}
              value={commitSha}
              onChange={(event) => setCommitSha(event.target.value)}
            />
          </div>
          <div className="product-process-commits__form-wide">
            <label htmlFor={`commit-summary-${processDefinitionId}`}>
              Resumo funcional
            </label>
            <input
              id={`commit-summary-${processDefinitionId}`}
              className="form-control form-control-sm"
              maxLength={500}
              placeholder="O que mudou para este produto neste processo?"
              required
              value={commitSummary}
              onChange={(event) => setCommitSummary(event.target.value)}
            />
          </div>
          <div className="product-process-commits__form-wide">
            <label htmlFor={`commit-url-${processDefinitionId}`}>
              URL do commit <small>(opcional)</small>
            </label>
            <input
              id={`commit-url-${processDefinitionId}`}
              className="form-control form-control-sm"
              maxLength={512}
              placeholder="Gerada automaticamente para repositórios owner/repo"
              type="url"
              value={commitUrl}
              onChange={(event) => setCommitUrl(event.target.value)}
            />
          </div>
          {validationError ? (
            <div
              className="alert alert-danger product-process-commits__form-wide mb-0 py-2"
              role="alert"
            >
              {validationError}
            </div>
          ) : null}
          <div className="product-process-commits__form-actions product-process-commits__form-wide">
            <small>
              Responsável: {tenantContext.userEmail || "não informado"}
            </small>
            <button
              className="btn btn-sm btn-primary"
              type="submit"
              disabled={mutation.isPending}
            >
              <Check size={14} aria-hidden="true" />
              {mutation.isPending ? "Registrando..." : "Salvar vínculo"}
            </button>
          </div>
        </form>
      ) : null}
    </section>
  );
}

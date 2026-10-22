import { useState } from "react";
import axios from "axios";
import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useProduct } from "../../api/product/useProduct";
import {
  useProductBigIdea,
  useSaveProductBigIdea,
  type BigIdeaContent,
  type BigIdeaWorkspace,
} from "../../api/product/useProductBigIdea";

const fields: {
  key: keyof BigIdeaContent;
  label: string;
  help: string;
  max: number;
}[] = [
  {
    key: "centralIdea",
    label: "Ideia central",
    help: "Em uma frase: qual mudança desejável este produto torna possível e por que seu caminho é diferente?",
    max: 500,
  },
  {
    key: "audience",
    label: "Para quem",
    help: "Pessoa e situação concreta em que a ideia faz sentido.",
    max: 1500,
  },
  {
    key: "desire",
    label: "Desejo reconhecido",
    help: "O que essa pessoa já quer sentir, resolver ou conquistar?",
    max: 1500,
  },
  {
    key: "promise",
    label: "Transformação prometida",
    help: "Resultado útil que a entrega pode sustentar, sem garantia de resultados externos.",
    max: 1500,
  },
  {
    key: "mechanism",
    label: "Como o produto torna isso possível",
    help: "Explique o mecanismo específico e a diferença em relação às alternativas.",
    max: 2000,
  },
  {
    key: "evidence",
    label: "Provas e hipóteses",
    help: "Fontes, resultados e datas disponíveis. Declare o que ainda é hipótese ou não foi medido.",
    max: 3000,
  },
  {
    key: "limits",
    label: "Limites da promessa",
    help: "O que não está incluído, não foi comprovado ou não pode ser prometido?",
    max: 2000,
  },
];
const blank: BigIdeaContent = {
  centralIdea: "",
  audience: "",
  desire: "",
  promise: "",
  mechanism: "",
  evidence: "",
  limits: "",
};

function Editor({
  workspace,
  refresh,
}: {
  workspace: BigIdeaWorkspace;
  refresh: () => void;
}) {
  const [content, setContent] = useState(workspace.current?.content ?? blank);
  const [reason, setReason] = useState("");
  const save = useSaveProductBigIdea(workspace.productId);
  const valid = fields.every(({ key }) => content[key].trim()) && reason.trim();
  const message =
    save.error && axios.isAxiosError(save.error)
      ? (save.error.response?.data?.detail ??
        save.error.response?.data?.message)
      : undefined;
  return (
    <form
      onSubmit={async (event) => {
        event.preventDefault();
        if (!valid || save.isPending) return;
        try {
          await save.mutateAsync({
            expectedRevision: workspace.current?.revision ?? 0,
            content,
            changeReason: reason,
          });
        } catch {
          /* A falha fica visível junto ao formulário; o rascunho permanece disponível. */
        }
      }}
    >
      <h2 className="h5">
        {workspace.current ? "Evoluir a Big Idea" : "Criar a Big Idea"}
      </h2>
      <p>
        Esta direção será usada nos novos contextos do produto. Experimentos já
        vinculados preservam a versão anterior.
      </p>
      {fields.map(({ key, label, help, max }) => (
        <div className="mb-3" key={key}>
          <label className="form-label" htmlFor={`big-idea-${key}`}>
            {label} *
          </label>
          <textarea
            id={`big-idea-${key}`}
            className="form-control"
            required
            maxLength={max}
            rows={key === "centralIdea" ? 2 : 3}
            value={content[key]}
            aria-describedby={`help-${key}`}
            onChange={(e) => setContent({ ...content, [key]: e.target.value })}
            disabled={save.isPending}
          />
          <div className="form-text" id={`help-${key}`}>
            {help}
          </div>
        </div>
      ))}
      <label className="form-label" htmlFor="big-idea-reason">
        Motivo desta versão *
      </label>
      <textarea
        id="big-idea-reason"
        className="form-control mb-3"
        required
        maxLength={2000}
        value={reason}
        onChange={(e) => setReason(e.target.value)}
        disabled={save.isPending}
      />
      {save.isError && (
        <div role="alert" className="alert alert-danger">
          {typeof message === "string"
            ? message
            : "Não foi possível salvar. Seu rascunho foi preservado."}
          <button type="button" className="btn btn-link" onClick={refresh}>
            Descartar rascunho e atualizar
          </button>
        </div>
      )}
      <button className="btn btn-primary" disabled={!valid || save.isPending}>
        {save.isPending && (
          <span className="spinner-border spinner-border-sm me-2" />
        )}
        {save.isPending
          ? "Salvando…"
          : workspace.current
            ? "Salvar nova versão"
            : "Criar Big Idea"}
      </button>
      <p className="form-text">
        Salvar registra a direção estratégica. Não executa IA nem altera
        campanhas ou autorizações.
      </p>
    </form>
  );
}

export default function ProductBigIdeaPage() {
  const productId = Number(useParams().id);
  const product = useProduct(productId);
  const query = useProductBigIdea(productId);
  const [editorEpoch, setEditorEpoch] = useState(0);
  if (!Number.isSafeInteger(productId) || productId < 1)
    return <p role="alert">Produto inválido.</p>;
  return (
    <div className="container-fluid" style={{ maxWidth: 1000 }}>
      <div className="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-4">
        <div>
          <PageTitle>Big Idea do produto</PageTitle>
          <p>
            {product.data?.internalName ||
              product.data?.name ||
              `Produto #${productId}`}
          </p>
        </div>
        <Link
          to={`/products/${productId}/edit`}
          className="btn btn-outline-secondary"
        >
          Voltar ao produto
        </Link>
      </div>
      <p>
        A ideia que conecta desejo, produto e comunicação. Atena a cria no
        planejamento; você também pode defini-la e evoluí-la aqui.
      </p>
      {query.isLoading && <p role="status">Carregando Big Idea…</p>}
      {query.isError && (
        <div className="alert alert-danger" role="alert">
          Não foi possível consultar a Big Idea.
          <button
            disabled={query.isFetching}
            onClick={() => query.refetch()}
            className="btn btn-link"
          >
            {query.isFetching ? "Consultando…" : "Tentar novamente"}
          </button>
        </div>
      )}
      {query.data && (
        <>
          <section className="alert alert-primary" aria-label="Direção vigente">
            <strong>
              {query.data.current
                ? `Versão vigente: v${query.data.current.revision}`
                : "Big Idea ainda não definida"}
            </strong>
            {query.data.current && (
              <p className="mt-2 mb-1">
                {query.data.current.content.centralIdea}
              </p>
            )}
            <p className="small mb-0">{query.data.guidance}</p>
          </section>
          {product.data && (
            <details className="mb-3">
              <summary>Consultar estratégia já cadastrada</summary>
              <p>Público: {product.data.targetAudience || "Não informado"}</p>
              <p>
                Hipótese: {product.data.primaryHypothesis || "Não informada"}
              </p>
              <p>Promessa: {product.data.promise || "Não informada"}</p>
              <p>
                Mecanismo: {product.data.uniqueMechanism || "Não informado"}
              </p>
            </details>
          )}
          <div className="card card-body mb-4">
            <Editor
              key={`${productId}/${query.data.current?.revision ?? 0}/${editorEpoch}`}
              workspace={query.data}
              refresh={() => {
                setEditorEpoch(editorEpoch + 1);
                void query.refetch();
              }}
            />
          </div>
          <details className="mb-4">
            <summary>
              Histórico de versões ({query.data.history.length})
            </summary>
            {query.data.history.map((revision) => (
              <article className="card card-body my-3" key={revision.id}>
                <h2 className="h6">
                  v{revision.revision} · {revision.createdBy} ·{" "}
                  {new Date(revision.createdAt).toLocaleString("pt-BR")}
                </h2>
                <p>
                  <strong>Motivo:</strong> {revision.changeReason}
                </p>
                {revision.sourceTaskId && (
                  <p>Origem: tarefa #{revision.sourceTaskId} de Atena</p>
                )}
                <dl>
                  {fields.map(({ key, label }) => (
                    <div key={key}>
                      <dt>{label}</dt>
                      <dd
                        style={{
                          whiteSpace: "pre-wrap",
                          overflowWrap: "anywhere",
                        }}
                      >
                        {revision.content[key]}
                      </dd>
                    </div>
                  ))}
                </dl>
              </article>
            ))}
          </details>
        </>
      )}
    </div>
  );
}

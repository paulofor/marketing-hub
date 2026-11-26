import axios from "axios";
import { useQuery } from "@tanstack/react-query";
import { Link, useParams, useSearchParams } from "react-router-dom";

type Fonte = { sistema: string; referencia: string; url?: string };
type Ficha = {
  id: string;
  revisao: number;
  versaoEsquema: string;
  tipo: string;
  titulo: string;
  resumo: string;
  registradoEm: string;
  origem: Fonte;
  vinculos: { id: string; revisao: number }[];
  agente?: { cadastroId: number; entradas: string[]; saidas: string[] };
  recurso?: { categoria: string; versao: string };
  execucao?: {
    tentativa: number | null;
    ciclo: number;
    experimento: number;
    processo: { id: number; versao: number };
    atividade: string;
    estado: string;
    motivo: string;
    destino: { id: number; versao: number; estado: string; fonte: Fonte };
    entradas: { tarefaId: number; estado: string; fonte: Fonte }[];
    criterios: { titulo: string; estado: string; fonte?: Fonte }[];
    lacunas: string[];
  };
};
function FonteView({ fonte }: { fonte: Fonte }) {
  return (
    <span>
      {fonte.sistema}: {fonte.referencia}{" "}
      {fonte.url && /^https?:\/\//.test(fonte.url) && (
        <a href={fonte.url} target="_blank" rel="noopener noreferrer">
          Abrir prova no contexto
        </a>
      )}
    </span>
  );
}
export default function ProductFichasPage() {
  const { productId } = useParams();
  const [search] = useSearchParams();
  const ciclo = search.get("learningCycleId");
  const query = useQuery({
    queryKey: ["fichas-v1", productId, ciclo],
    queryFn: async () =>
      (
        await axios.get<{
          ambiente: string;
          despachoPermitido: boolean;
          fichas: Ficha[];
        }>(`/api/products/${productId}/fichas/v1`, { params: { ciclo } })
      ).data,
  });
  return (
    <section
      aria-label="Fichas históricas"
      style={{ maxWidth: 1000, overflowWrap: "anywhere" }}
    >
      <Link to={`/products/${productId}/value-chain-history?${search}`}>
        Voltar ao produto/ciclo
      </Link>
      <h1>Fichas v1.0 · Homologação</h1>
      <p>
        Retratos históricos identificados. Não representam o estado atual nem
        autorizam despacho ou gasto.
      </p>
      {query.isLoading && <p role="status">Carregando fichas…</p>}
      {query.isError && (
        <p role="alert">
          Piloto indisponível. Este recurso exige habilitação em homologação.
        </p>
      )}
      {query.data && (
        <p>
          {query.data.ambiente} · Despacho:{" "}
          {query.data.despachoPermitido ? "permitido" : "não permitido"}
        </p>
      )}
      {query.data?.fichas.length === 0 && (
        <p>Nenhuma ficha neste produto/ciclo.</p>
      )}
      {query.data?.fichas.map((f) => (
        <article
          className="card p-3 mb-3"
          key={`${f.id}-${f.revisao}`}
          id={`${f.id}-r${f.revisao}`}
        >
          <h2>
            {f.titulo} · revisão {f.revisao}
          </h2>
          <p>
            {f.tipo} · {f.id} · esquema {f.versaoEsquema} · registrado{" "}
            {f.registradoEm}
          </p>
          <p>{f.resumo}</p>
          <FonteView fonte={f.origem} />
          {f.agente && (
            <>
              <p>Cadastro de agente #{f.agente.cadastroId}</p>
              <p>Entradas: {f.agente.entradas.join("; ")}</p>
              <p>Saídas: {f.agente.saidas.join("; ")}</p>
            </>
          )}
          {f.recurso && (
            <p>
              {f.recurso.categoria} · versão {f.recurso.versao}
            </p>
          )}
          {f.execucao && (
            <>
              <p>
                Ciclo {f.execucao.ciclo} · Experimento {f.execucao.experimento}{" "}
                · Processo {f.execucao.processo.id} v
                {f.execucao.processo.versao} · {f.execucao.atividade} ·
                tentativa {f.execucao.tentativa ?? "desconhecida"}
              </p>
              <p>
                {f.execucao.estado}: {f.execucao.motivo}
              </p>
              <h3>Conferência anterior ao despacho</h3>
              <p>
                Destino #{f.execucao.destino.id} v{f.execucao.destino.versao}:{" "}
                {f.execucao.destino.estado}
              </p>
              <FonteView fonte={f.execucao.destino.fonte} />
              <ul>
                {f.execucao.entradas.map((e) => (
                  <li key={e.tarefaId}>
                    Entrada #{e.tarefaId}: {e.estado} ·{" "}
                    <FonteView fonte={e.fonte} />
                  </li>
                ))}
              </ul>
              <h3>Critérios e provas</h3>
              <ul>
                {f.execucao.criterios.map((c) => (
                  <li key={c.titulo}>
                    {c.titulo}: {c.estado} ·{" "}
                    {c.fonte ? (
                      <FonteView fonte={c.fonte} />
                    ) : (
                      "Sem fonte registrada"
                    )}
                  </li>
                ))}
              </ul>
              <h3>Lacunas</h3>
              <ul>
                {f.execucao.lacunas.map((l) => (
                  <li key={l}>{l}</li>
                ))}
              </ul>
            </>
          )}
          <nav aria-label="Vínculos de revisão">
            {f.vinculos.map((v) => (
              <a
                className="d-block"
                key={`${v.id}-${v.revisao}`}
                href={`#${v.id}-r${v.revisao}`}
              >
                {v.id} · revisão {v.revisao}
              </a>
            ))}
          </nav>
        </article>
      ))}
    </section>
  );
}

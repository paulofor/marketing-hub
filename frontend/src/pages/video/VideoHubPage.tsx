import { useEffect, useMemo, useState } from "react";
import { CheckCircle2, Save, Video } from "lucide-react";
import { toast } from "react-toastify";
import PageTitle from "../../components/PageTitle";
import "./VideoHubPage.css";

type VideoStageStatus = "READY" | "DRAFT";

type VideoStage = {
  id: string;
  title: string;
  status: VideoStageStatus;
  content: string;
};

const STORAGE_KEY = "marketing-hub:pde-entry-explainer-video:v1";

const DEFAULT_STAGES: VideoStage[] = [
  {
    id: "objectives",
    title: "Objetivos",
    status: "READY",
    content:
      "Produto: Método MUSA 7 Dias.\nObjetivo principal: aumentar o primeiro microcompromisso no PDE, levando a visitante a clicar em \"Descobrir o que minha imagem comunica hoje\".\nObjetivo secundário: reduzir ansiedade antes do login/e-mail, mostrando que o Mapa de Presença entrega uma leitura rápida, prática e pessoal.\nMétrica associada: aumento de PRESENCE_MAP_CHOICE_SELECTED, DIAGNOSTIC_CHOICE_SELECTED, FIELD_FILLED, LOGIN_STARTED e PAYWALL_VIEWED por versão do PDE.",
  },
  {
    id: "briefing",
    title: "Briefing comercial",
    status: "READY",
    content:
      "Tipo: vídeo explicativo para entrada do PDE.\nDuração alvo: 20 a 35 segundos.\nPrimeira dobra: acima do CTA principal.\nPromessa: mostrar em poucos segundos o que a imagem da mulher comunica hoje e qual pequeno ajuste pode aumentar presença, desejo e segurança visual.\nTom: íntimo, elegante, direto e sem parecer aula longa.",
  },
  {
    id: "script",
    title: "Roteiro",
    status: "DRAFT",
    content:
      "Abertura: \"Antes de mudar roupa, maquiagem ou postura, você precisa entender uma coisa: sua imagem já está comunicando algo.\"\nValor: \"O Mapa de Presença mostra qual mensagem você passa hoje e qual ajuste simples pode aproximar você da mulher que quer ser percebida.\"\nRedução de fricção: \"É uma leitura rápida, sem julgamento e feita para começar pelo Dia 1.\"\nCTA: \"Clique em Descobrir o que minha imagem comunica hoje e veja seu primeiro mapa.\"",
  },
  {
    id: "creative",
    title: "Criação",
    status: "DRAFT",
    content:
      "Formato recomendado: vertical 9:16 com corte seguro para mobile.\nCena 1: rosto confiante olhando para câmera, ambiente claro e sofisticado.\nCena 2: detalhes de roupa, postura e expressão, sem excesso de produção.\nCena 3: tela ou gesto apontando para o Mapa de Presença.\nLegenda fixa: \"Descubra o que sua imagem comunica hoje\".\nAsset final: arquivo versionado e vinculado à versão comercial do PDE no Marketing Hub.",
  },
  {
    id: "validation",
    title: "Validação",
    status: "DRAFT",
    content:
      "Publicar como nova versão do PDE pelo Marketing Hub.\nComparar contra a versão anterior por experienceVersion/funnelVersion.\nCritério positivo inicial: sair de 0 cliques no Mapa para qualquer volume consistente de primeira ação com tráfego pago ativo.\nCritério de corte: manter gasto até o limite definido e pausar se continuar sem primeira ação.",
  },
];

const STATUS_LABELS: Record<VideoStageStatus, string> = {
  READY: "Pronto",
  DRAFT: "Rascunho",
};

export default function VideoHubPage() {
  const [stages, setStages] = useState<VideoStage[]>(DEFAULT_STAGES);

  useEffect(() => {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    if (!stored) {
      return;
    }
    try {
      const parsed = JSON.parse(stored) as VideoStage[];
      if (Array.isArray(parsed)) {
        setStages(parsed);
      }
    } catch {
      window.localStorage.removeItem(STORAGE_KEY);
    }
  }, []);

  const readyCount = useMemo(
    () => stages.filter((stage) => stage.status === "READY").length,
    [stages],
  );

  const updateStageContent = (stageId: string, content: string) => {
    setStages((current) =>
      current.map((stage) =>
        stage.id === stageId ? { ...stage, content } : stage,
      ),
    );
  };

  const toggleStageStatus = (stageId: string) => {
    setStages((current) =>
      current.map((stage) =>
        stage.id === stageId
          ? {
              ...stage,
              status: stage.status === "READY" ? "DRAFT" : "READY",
            }
          : stage,
      ),
    );
  };

  const savePlan = () => {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(stages));
    toast.success("Plano de vídeo salvo neste navegador");
  };

  const resetPlan = () => {
    setStages(DEFAULT_STAGES);
    window.localStorage.removeItem(STORAGE_KEY);
    toast.info("Plano de vídeo restaurado");
  };

  return (
    <div className="video-hub-page">
      <div className="video-hub-page__header">
        <div>
          <PageTitle>Vídeos</PageTitle>
          <p className="video-hub-page__subtitle">
            Produção comercial de vídeos do Marketing Hub, começando por vídeos
            explicativos para a entrada do PDE.
          </p>
        </div>
        <span className="video-hub-page__badge">
          <Video size={16} aria-hidden="true" />
          PDE entrada
        </span>
      </div>

      <div className="video-hub-page__grid">
        <aside className="video-hub-page__type-list">
          <button type="button" className="video-hub-page__type-button">
            <strong>Vídeo explicativo de entrada do PDE</strong>
            <span>
              Objetivos, briefing, roteiro, criação, publicação e medição.
            </span>
          </button>
        </aside>

        <section className="video-hub-page__panel">
          <div className="video-hub-page__summary">
            <SummaryMetric label="Tipo" value="PDE explicativo" />
            <SummaryMetric label="Produto inicial" value="Método MUSA" />
            <SummaryMetric label="Etapas prontas" value={`${readyCount}/5`} />
            <SummaryMetric label="Destino" value="Primeira dobra" />
          </div>

          <div className="video-hub-page__stages">
            {stages.map((stage, index) => (
              <article className="video-hub-page__stage" key={stage.id}>
                <div className="video-hub-page__stage-header">
                  <div>
                    <span className="video-hub-page__stage-kicker">
                      Etapa {index + 1}
                    </span>
                    <strong className="video-hub-page__stage-title">
                      {stage.title}
                    </strong>
                  </div>
                  <button
                    type="button"
                    className={`video-hub-page__status ${
                      stage.status === "READY"
                        ? "video-hub-page__status--ready"
                        : "video-hub-page__status--draft"
                    }`}
                    onClick={() => toggleStageStatus(stage.id)}
                  >
                    {stage.status === "READY" ? (
                      <CheckCircle2 size={14} aria-hidden="true" />
                    ) : null}{" "}
                    {STATUS_LABELS[stage.status]}
                  </button>
                </div>
                <textarea
                  className="form-control video-hub-page__textarea"
                  value={stage.content}
                  onChange={(event) =>
                    updateStageContent(stage.id, event.target.value)
                  }
                  aria-label={stage.title}
                />
              </article>
            ))}
          </div>

          <div className="video-hub-page__actions">
            <button
              type="button"
              className="btn btn-outline-secondary"
              onClick={resetPlan}
            >
              Restaurar
            </button>
            <button type="button" className="btn btn-primary" onClick={savePlan}>
              <Save size={16} aria-hidden="true" /> Salvar
            </button>
          </div>
        </section>
      </div>
    </div>
  );
}

function SummaryMetric({ label, value }: { label: string; value: string }) {
  return (
    <div className="video-hub-page__metric">
      <div className="video-hub-page__metric-label">{label}</div>
      <div className="video-hub-page__metric-value">{value}</div>
    </div>
  );
}

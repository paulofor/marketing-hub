import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  CheckCircle2,
  Clapperboard,
  FileText,
  PlayCircle,
  RefreshCcw,
  Save,
  Video,
} from "lucide-react";
import { toast } from "react-toastify";
import PageTitle from "../../components/PageTitle";
import { TenantContextBanner } from "../../components/TenantContextBanner";
import { useProducts } from "../../api/product/useProducts";
import { useCreateSalesVideoProfile } from "../../api/salesVideo/useCreateSalesVideoProfile";
import { useSalesVideoProfiles } from "../../api/salesVideo/useSalesVideoProfiles";
import { useApproveSalesVideoScript } from "../../api/salesVideo/useApproveSalesVideoScript";
import { useRequestVideoRender } from "../../api/salesVideo/useRequestVideoRender";
import { useSalesVideoJobs } from "../../api/salesVideo/useSalesVideoJobs";
import { useTenantContext } from "../../utils/tenantContext";
import "./VideoHubPage.css";

type VideoStageStatus = "READY" | "DRAFT";

type VideoStage = {
  id: string;
  title: string;
  status: VideoStageStatus;
  content: string;
};

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
      "Antes de mudar roupa, maquiagem ou postura, você precisa entender uma coisa: sua imagem já está comunicando algo.\nO Mapa de Presença mostra qual mensagem você passa hoje e qual ajuste simples pode aproximar você da mulher que quer ser percebida.\nÉ uma leitura rápida, sem julgamento e feita para começar pelo Dia 1.\nClique em Descobrir o que minha imagem comunica hoje e veja seu primeiro mapa.",
  },
  {
    id: "creation",
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

const CURRENT_PDE_VERSION = "musa-pde-entry-v4-video-hero";
const DEFAULT_RENDER_PROVIDER = "VEO";

export default function VideoHubPage() {
  const tenantContext = useTenantContext();
  const { data: products, isLoading: productsLoading } = useProducts();
  const [selectedProductId, setSelectedProductId] = useState<string>("");
  const [selectedProfileId, setSelectedProfileId] = useState<string>("");
  const [stages, setStages] = useState<VideoStage[]>(DEFAULT_STAGES);

  const { data: profiles, isLoading: profilesLoading } =
    useSalesVideoProfiles(selectedProductId || undefined);
  const { data: jobs } = useSalesVideoJobs(selectedProfileId || undefined);
  const createProfile = useCreateSalesVideoProfile(selectedProductId || undefined);
  const approveScript = useApproveSalesVideoScript(selectedProfileId || undefined);
  const requestRender = useRequestVideoRender(selectedProfileId || undefined);

  const productList = useMemo(() => products ?? [], [products]);
  const profileList = useMemo(() => profiles ?? [], [profiles]);
  const selectedProduct = productList.find((product) => String(product.id) === selectedProductId);
  const selectedProfile = profileList.find((profile) => String(profile.id) === selectedProfileId);
  const latestJob = jobs?.[0];
  const scriptStage = stages.find((stage) => stage.id === "script") ?? stages[2];
  const readyCount = stages.filter((stage) => stage.status === "READY").length;

  useEffect(() => {
    if (selectedProductId || productList.length === 0) {
      return;
    }
    const musaProduct =
      productList.find((product) => product.slug === "metodo-musa-7-dias") ?? productList[0];
    setSelectedProductId(String(musaProduct.id));
  }, [productList, selectedProductId]);

  useEffect(() => {
    if (profileList.length === 0) {
      setSelectedProfileId("");
      return;
    }
    const pdeProfile =
      profileList.find((profile) => profile.title.includes(CURRENT_PDE_VERSION)) ?? profileList[0];
    setSelectedProfileId(String(pdeProfile.id));
  }, [profileList]);

  const updateStageContent = (stageId: string, content: string) => {
    setStages((current) =>
      current.map((stage) => (stage.id === stageId ? { ...stage, content } : stage)),
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

  const resetPlan = () => {
    setStages(DEFAULT_STAGES);
    toast.info("Plano de vídeo restaurado");
  };

  const buildScriptText = () => {
    return stages.map((stage) => `## ${stage.title}\n${stage.content.trim()}`).join("\n\n");
  };

  const handleCreateProfile = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedProductId) {
      toast.error("Selecione um produto para criar o vídeo");
      return;
    }
    try {
      const profile = await createProfile.mutateAsync({
        videoKind: "HERO",
        title: `PDE entrada explicativa - ${CURRENT_PDE_VERSION}`,
        personaName: "Visitante MUSA",
        personaStyle: "mulher buscando presença visual, elegância e segurança",
        voiceStyle: "íntima, elegante, direta e comercialmente clara",
        language: "pt-BR",
        targetDurationSeconds: 25,
      });
      setSelectedProfileId(String(profile.id));
      toast.success("Perfil de vídeo criado no Marketing Hub");
    } catch (error) {
      const message = error instanceof Error ? error.message : "Falha ao criar perfil de vídeo";
      toast.error(message);
    }
  };

  const handleApproveScript = async () => {
    if (!selectedProfileId) {
      toast.error("Crie ou selecione um perfil de vídeo antes do roteiro");
      return;
    }
    if (!scriptStage.content.trim()) {
      toast.error("O roteiro precisa de conteúdo");
      return;
    }
    try {
      await approveScript.mutateAsync({
        scriptText: buildScriptText(),
        hookText: "Sua imagem já está comunicando algo antes de você dizer uma palavra.",
        ctaText: "Descobrir o que minha imagem comunica hoje",
        captionText:
          "Mapa de Presença MUSA: uma leitura rápida para entender o que sua imagem comunica hoje.",
        approvedBy: tenantContext.userEmail,
      });
      toast.success("Roteiro aprovado e salvo no Marketing Hub");
    } catch (error) {
      const message = error instanceof Error ? error.message : "Falha ao aprovar roteiro";
      toast.error(message);
    }
  };

  const handleRequestRender = async () => {
    if (!selectedProfileId) {
      toast.error("Selecione um perfil de vídeo antes de solicitar criação");
      return;
    }
    try {
      await requestRender.mutateAsync({
        requestedBy: tenantContext.userEmail,
        providerFamily: "EXTERNAL_VIDEO_MODULE",
        providerName: DEFAULT_RENDER_PROVIDER,
        executionMode: "TEST",
      });
      toast.success("Criação do vídeo solicitada pelo Marketing Hub");
    } catch (error) {
      const message = error instanceof Error ? error.message : "Falha ao solicitar criação do vídeo";
      toast.error(message);
    }
  };

  return (
    <div className="video-hub-page">
      <div className="video-hub-page__header">
        <div>
          <PageTitle>Vídeos</PageTitle>
          <p className="video-hub-page__subtitle">
            Produção e rastreabilidade de vídeos comerciais pelo Marketing Hub. O primeiro fluxo
            cria vídeos explicativos para entrada do PDE, sem publicação manual de artefato.
          </p>
        </div>
        <span className="video-hub-page__badge">
          <Video size={16} aria-hidden="true" />
          PDE entrada
        </span>
      </div>

      <TenantContextBanner className="mb-3" />

      <div className="video-hub-page__grid">
        <aside className="video-hub-page__type-list">
          <button type="button" className="video-hub-page__type-button">
            <strong>Vídeo explicativo de entrada do PDE</strong>
            <span>Objetivos, roteiro, criação por job e validação por versão comercial.</span>
          </button>

          <form className="video-hub-page__setup" onSubmit={handleCreateProfile}>
            <label className="form-label" htmlFor="video-product">
              Produto
            </label>
            <select
              id="video-product"
              className="form-select"
              value={selectedProductId}
              disabled={productsLoading}
              onChange={(event) => setSelectedProductId(event.target.value)}
            >
              {productList.map((product) => (
                <option key={product.id} value={product.id}>
                  {product.name || product.slug || `Produto #${product.id}`}
                </option>
              ))}
            </select>

            <label className="form-label" htmlFor="video-profile">
              Perfil de vídeo
            </label>
            <select
              id="video-profile"
              className="form-select"
              value={selectedProfileId}
              disabled={profilesLoading || profileList.length === 0}
              onChange={(event) => setSelectedProfileId(event.target.value)}
            >
              {profileList.length === 0 ? (
                <option value="">Nenhum perfil criado</option>
              ) : null}
              {profileList.map((profile) => (
                <option key={profile.id} value={profile.id}>
                  #{profile.id} · {profile.title}
                </option>
              ))}
            </select>

            <button
              type="submit"
              className="btn btn-primary w-100"
              disabled={createProfile.isPending || !selectedProductId}
            >
              <Clapperboard size={16} aria-hidden="true" />
              Criar perfil PDE v4
            </button>
          </form>
        </aside>

        <section className="video-hub-page__panel">
          <div className="video-hub-page__summary">
            <SummaryMetric label="Produto" value={selectedProduct?.name || selectedProduct?.slug || "-"} />
            <SummaryMetric label="Versão PDE" value={CURRENT_PDE_VERSION} />
            <SummaryMetric label="Etapas prontas" value={`${readyCount}/5`} />
            <SummaryMetric label="Status do vídeo" value={selectedProfile?.status ?? "Sem perfil"} />
          </div>

          <div className="video-hub-page__notice">
            <CheckCircle2 size={18} aria-hidden="true" />
            <span>
              Procedimento correto: código muda via GitHub; artefato de vídeo nasce como perfil,
              roteiro aprovado e job dentro do Marketing Hub.
            </span>
          </div>

          <div className="video-hub-page__stages">
            {stages.map((stage, index) => (
              <article className="video-hub-page__stage" key={stage.id}>
                <div className="video-hub-page__stage-header">
                  <div>
                    <span className="video-hub-page__stage-kicker">Etapa {index + 1}</span>
                    <strong className="video-hub-page__stage-title">{stage.title}</strong>
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
                    {stage.status === "READY" ? <CheckCircle2 size={14} aria-hidden="true" /> : null}{" "}
                    {STATUS_LABELS[stage.status]}
                  </button>
                </div>
                <textarea
                  className="form-control video-hub-page__textarea"
                  value={stage.content}
                  onChange={(event) => updateStageContent(stage.id, event.target.value)}
                  aria-label={stage.title}
                />
              </article>
            ))}
          </div>

          <div className="video-hub-page__actions">
            <button type="button" className="btn btn-outline-secondary" onClick={resetPlan}>
              <RefreshCcw size={16} aria-hidden="true" />
              Restaurar
            </button>
            <button
              type="button"
              className="btn btn-outline-primary"
              onClick={handleApproveScript}
              disabled={approveScript.isPending || !selectedProfileId}
            >
              <FileText size={16} aria-hidden="true" />
              Salvar roteiro no Hub
            </button>
            <button
              type="button"
              className="btn btn-primary"
              onClick={handleRequestRender}
              disabled={requestRender.isPending || !selectedProfileId}
            >
              <Save size={16} aria-hidden="true" />
              Solicitar criação
            </button>
          </div>

          <div className="video-hub-page__job">
            <div>
              <strong>Último job</strong>
              <span>
                {latestJob
                  ? `#${latestJob.id} · ${latestJob.jobType} · ${latestJob.status}`
                  : "Nenhum job de criação solicitado para este perfil."}
              </span>
            </div>
            {latestJob?.assetId ? (
              <span className="video-hub-page__job-ready">
                <PlayCircle size={16} aria-hidden="true" />
                Asset #{latestJob.assetId}
              </span>
            ) : null}
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

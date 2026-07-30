import { Link } from "react-router-dom";
import {
  BadgeCheck,
  BarChart3,
  Brain,
  Clapperboard,
  FileText,
  GitBranch,
  Goal,
  Lightbulb,
  Link2,
  PlayCircle,
  ShieldCheck,
  Sparkles,
  Video,
} from "lucide-react";
import PageTitle from "../../components/PageTitle";
import "./PdeVideoProductionPage.css";

const productionSteps = [
  {
    title: "Briefing científico-comercial",
    icon: Brain,
    description:
      "Selecionar o conceito dos artigos, traduzir para promessa visual e definir a função no funil.",
    bullets: ["Abertura", "Prova", "Mecanismo", "Objeção", "CTA"],
  },
  {
    title: "Roteiro por cena",
    icon: FileText,
    description:
      "Transformar a promessa em cenas curtas, verticais e com uma única ideia por bloco.",
    bullets: ["Dor visível", "Lacuna", "Microajuste", "Recompensa"],
  },
  {
    title: "Storyboard e prompt visual",
    icon: Clapperboard,
    description:
      "Definir movimento, ambiente, personagem, emoção, câmera, legenda e voz antes da geração.",
    bullets: ["Prompt de imagem", "Prompt de vídeo", "Texto e voz"],
  },
  {
    title: "Geração do vídeo",
    icon: Sparkles,
    description:
      "Usar provedores conforme a cena: movimento visual, apresentadora, variações e pós-produção.",
    bullets: ["Luma", "Kling", "Runway", "VEO", "HeyGen"],
  },
  {
    title: "Controle de qualidade",
    icon: PlayCircle,
    description:
      "Reprovar vídeos sem áudio, sem clareza, com artefatos, sem CTA ou desalinhados ao funil.",
    bullets: ["Áudio", "Movimento", "Legenda", "CTA", "Aderência"],
  },
  {
    title: "Aprovação humana",
    icon: BadgeCheck,
    description:
      "Liberar comercialmente apenas ativos prontos, aprovados e com áudio quando forem usados no PDE.",
    bullets: ["READY", "APPROVED", "has_audio=true"],
  },
  {
    title: "Vinculação ao PDE versionado",
    icon: Link2,
    description:
      "Conectar cada vídeo ao produto, slot, versão da experiência e função comercial correta.",
    bullets: ["v6", "v7", "Abertura", "Prova", "CTA"],
  },
  {
    title: "Métricas por etapa",
    icon: BarChart3,
    description:
      "Medir consumo do vídeo junto com avanço real para diagnóstico, paywall, checkout e compra.",
    bullets: ["Play", "25%", "50%", "75%", "100%", "Compra"],
  },
  {
    title: "Aprendizado",
    icon: Lightbulb,
    description:
      "Registrar o que aumentou desejo, avanço e receita para alimentar os próximos capítulos do PDE.",
    bullets: ["Hipótese", "Resultado", "Decisão", "Próximo teste"],
  },
];

const musaChapterPlan = [
  "Espelho antes de sair: identificação da dor.",
  "O problema não é a roupa: criação da lacuna.",
  "Ruído visual: prova prática de percepção.",
  "Peça-sinal e acabamento: transformação visual.",
  "Plano MUSA de 7 dias: CTA para diagnóstico e paywall.",
];

const productionGates = [
  {
    title: "Gate de hipótese",
    icon: Goal,
    timing: "Antes de gerar",
    objective:
      "Confirmar qual desejo, dor ou objeção o vídeo precisa mover dentro do PDE.",
    approveWhen: [
      "Existe uma hipótese comercial clara.",
      "A função no funil está definida.",
      "A cena promete uma ação mensurável.",
    ],
    blockWhen:
      "O vídeo é apenas bonito, genérico ou não deixa claro qual avanço mental deve provocar.",
  },
  {
    title: "Gate de qualidade comercial",
    icon: ShieldCheck,
    timing: "Antes de aprovar",
    objective:
      "Validar se o vídeo entretém, seduz e empurra a usuária para a próxima ação.",
    approveWhen: [
      "O movimento sustenta atenção nos primeiros segundos.",
      "A legenda e o áudio reforçam a promessa.",
      "O CTA aparece como consequência natural da cena.",
    ],
    blockWhen:
      "Há artefatos, falta de áudio, narrativa confusa, baixa energia ou ausência de CTA.",
  },
  {
    title: "Gate de performance",
    icon: BarChart3,
    timing: "Depois de publicar",
    objective:
      "Decidir com dados se o vídeo melhora avanço no funil ou precisa de nova variação.",
    approveWhen: [
      "Aumenta play e progresso relevante.",
      "Melhora início ou conclusão do diagnóstico.",
      "Mostra sinal de paywall, checkout ou compra.",
    ],
    blockWhen:
      "Gera visualização passiva, mas não melhora escolha, diagnóstico ou intenção de compra.",
  },
];

const requiredMetrics = [
  "Vídeo iniciado",
  "Progresso 25%",
  "Progresso 50%",
  "Progresso 75%",
  "Conclusão",
  "Escolha feita",
  "Diagnóstico iniciado",
  "Paywall",
  "Checkout",
  "Compra",
];

export default function PdeVideoProductionPage() {
  return (
    <div className="pde-video-production-page">
      <div className="pde-video-production-page__header">
        <div>
          <PageTitle>Produção de Vídeo PDE</PageTitle>
          <p className="pde-video-production-page__subtitle">
            Pipeline comercial para transformar conceitos científicos em vídeos
            interativos de PDE, com hipótese, função no funil, revisão,
            vinculação à versão produtiva e aprendizado por métrica.
          </p>
        </div>
        <span className="pde-video-production-page__badge">
          <GitBranch size={16} aria-hidden="true" />
          Fluxo por capítulos
        </span>
      </div>

      <section className="pde-video-production-page__section">
        <h2>Processo recomendado</h2>
        <p>
          Cada vídeo deve nascer como ativo comercial rastreável: uma hipótese
          visual ligada a uma etapa do funil, não um arquivo solto.
        </p>
        <div className="pde-video-production-page__steps">
          {productionSteps.map((step) => {
            const Icon = step.icon;
            return (
              <article className="pde-video-production-page__step" key={step.title}>
                <div className="pde-video-production-page__step-header">
                  <span className="pde-video-production-page__step-icon">
                    <Icon size={18} aria-hidden="true" />
                  </span>
                  <h3>{step.title}</h3>
                </div>
                <p>{step.description}</p>
                <ul>
                  {step.bullets.map((bullet) => (
                    <li key={bullet}>{bullet}</li>
                  ))}
                </ul>
              </article>
            );
          })}
        </div>
      </section>

      <section className="pde-video-production-page__section">
        <h2>Gates de decisão</h2>
        <p>
          Os gates impedem que a produção avance por gosto visual. Cada passagem
          precisa provar função comercial, qualidade de sedução e impacto real
          no funil.
        </p>
        <div className="pde-video-production-page__gates">
          {productionGates.map((gate) => {
            const Icon = gate.icon;
            return (
              <article className="pde-video-production-page__gate" key={gate.title}>
                <div className="pde-video-production-page__gate-header">
                  <span className="pde-video-production-page__gate-icon">
                    <Icon size={18} aria-hidden="true" />
                  </span>
                  <div>
                    <h3>{gate.title}</h3>
                    <span>{gate.timing}</span>
                  </div>
                </div>
                <p>{gate.objective}</p>
                <strong>Aprovar quando</strong>
                <ul>
                  {gate.approveWhen.map((criterion) => (
                    <li key={criterion}>{criterion}</li>
                  ))}
                </ul>
                <strong>Bloquear quando</strong>
                <p className="pde-video-production-page__gate-block">
                  {gate.blockWhen}
                </p>
              </article>
            );
          })}
        </div>
      </section>

      <section className="pde-video-production-page__section">
        <h2>Plano inicial para MUSA</h2>
        <p>
          Começar com capítulos curtos reduz risco de produção e aumenta
          aprendizado sobre desejo, retenção e avanço para diagnóstico.
        </p>
        <ul className="pde-video-production-page__list">
          {musaChapterPlan.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      </section>

      <section className="pde-video-production-page__section">
        <h2>Pontos do Hub que completam o fluxo</h2>
        <div className="pde-video-production-page__routes">
          <Link className="pde-video-production-page__route" to="/videos">
            <strong>
              <Video size={17} aria-hidden="true" />
              Vídeos
            </strong>
            <span>Planejamento, biblioteca e geração de ativos comerciais.</span>
          </Link>
          <Link className="pde-video-production-page__route" to="/audio-video-studio">
            <strong>
              <Clapperboard size={17} aria-hidden="true" />
              Estúdio de Áudio e Vídeo
            </strong>
            <span>Roteiro, cenas, continuidade, voz, trilha e montagem.</span>
          </Link>
          <Link className="pde-video-production-page__route" to="/creative-video-review">
            <strong>
              <BadgeCheck size={17} aria-hidden="true" />
              Aprovar vídeos
            </strong>
            <span>Revisão humana antes de liberar o vídeo para uso comercial.</span>
          </Link>
          <Link className="pde-video-production-page__route" to="/products">
            <strong>
              <Link2 size={17} aria-hidden="true" />
              Produtos e versões PDE
            </strong>
            <span>Vincular vídeos aprovados ao produto e à versão produtiva.</span>
          </Link>
          <Link className="pde-video-production-page__route" to="/ops-monitor/pde">
            <strong>
              <BarChart3 size={17} aria-hidden="true" />
              Saúde PDE 24/7
            </strong>
            <span>Validar tráfego, eventos, abandono e impacto pós-deploy.</span>
          </Link>
        </div>
      </section>

      <section className="pde-video-production-page__section">
        <h2>Métricas obrigatórias</h2>
        <p>
          A decisão de escala deve considerar se o vídeo aumenta ação comercial,
          não apenas se entretém.
        </p>
        <div className="pde-video-production-page__metrics">
          {requiredMetrics.map((metric) => (
            <div className="pde-video-production-page__metric" key={metric}>
              {metric}
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}

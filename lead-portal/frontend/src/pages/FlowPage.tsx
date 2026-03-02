import type { CSSProperties } from "react";
import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { fetchLeadPortalFlow } from "../api";
import FlowForm from "../components/FlowForm";
import { useCampaignCode } from "../hooks/useCampaignCode";
import type { LeadPortalSimpleFormStyleDefinition } from "../types";

export default function FlowPage() {
  const { slug } = useParams<{ slug: string }>();
  const campaignCode = useCampaignCode();

  const { data: flow, isLoading, isError, error } = useQuery({
    queryKey: ["lead-portal-flow", slug, campaignCode ?? null],
    queryFn: async () => {
      if (!slug) {
        throw new Error("Fluxo não informado");
      }
      return fetchLeadPortalFlow(slug, { campaignCode });
    },
    enabled: Boolean(slug),
  });

  if (!slug) {
    return <p className="flow-message">Fluxo não informado.</p>;
  }

  if (isLoading) {
    return <p className="flow-message">Carregando quiz...</p>;
  }

  if (isError || !flow) {
    return (
      <div className="flow-container">
        <h1>Fluxo indisponível</h1>
        <p>{error instanceof Error ? error.message : "Não foi possível carregar este fluxo."}</p>
      </div>
    );
  }

  const definition = flow.simpleFormStyle?.definition ?? null;
  const heroLayout = definition?.heroLayout ?? "image-right";
  const heroImageUrl = definition?.heroImageUrl ?? null;
  const heroBlend = definition?.heroImageBlendColor ?? null;
  const styleVars = buildStyleVariables(definition);
  const headerContent = SIMPLE_FORM_CONTENT.header;
  const proofContent = SIMPLE_FORM_CONTENT.proof;
  const bulletsContent = SIMPLE_FORM_CONTENT.bullets;

  const heroMediaStyle: CSSProperties | undefined = heroImageUrl
    ? {
        backgroundImage: heroBlend
          ? `linear-gradient(${heroBlend}, ${heroBlend}), url(${heroImageUrl})`
          : `url(${heroImageUrl})`,
      }
    : undefined;

  return (
    <div className="flow-page" style={styleVars} data-hero-layout={heroLayout}>
      <div className="flow-container">
        <section className="flow-hero">
          <div className="flow-hero-copy">
            <p className="flow-eyebrow">{flow.simpleFormStyle?.name ?? "Lead Portal"}</p>
            <h1>{flow.name}</h1>
            <p className="flow-subtitle">{headerContent.subtitle}</p>
            <p>{flow.description ?? headerContent.detail}</p>
            <div className="flow-promise-box">
              <strong>{headerContent.promiseLabel}:</strong> {headerContent.promiseText}
            </div>
          </div>
          {heroImageUrl ? <div className="flow-hero-media" style={heroMediaStyle} /> : null}
        </section>

        <section className="flow-proof-section" aria-label="Exemplos de posts">
          <div className="flow-section-header">
            <p className="flow-section-kicker">{proofContent.kicker}</p>
            <h2>{proofContent.title}</h2>
            <p>{proofContent.subtitle}</p>
          </div>
          <div className="flow-proof-grid">
            {proofContent.cards.map((post) => (
              <article key={post.title} className="flow-proof-card">
                <div className="flow-proof-image" style={{ background: post.background }} />
                <h3>{post.title}</h3>
                <p>{post.description}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="flow-confidence-section" aria-label="Detalhes de confiança">
          <h2>{bulletsContent.title}</h2>
          <ul>
            {bulletsContent.items.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </section>

        <FlowForm flow={flow} campaignCode={campaignCode} />
      </div>
    </div>
  );
}

const SIMPLE_FORM_CONTENT = {
  header: {
    subtitle: "Transforme ideias em posts prontos para publicar em poucos minutos.",
    detail:
      "Fluxo simples para coleta inicial de informações sem necessidade de envio de imagens.",
    promiseLabel: "Nossa promessa",
    promiseText:
      "você recebe uma linha editorial visual clara, com linguagem alinhada ao seu público e foco em gerar mais conversas no direct.",
  },
  proof: {
    kicker: "Exemplos reais",
    title: "Posts criados para outros clientes",
    subtitle:
      "Veja alguns estilos de criativos entregues em nichos diferentes. O objetivo é te mostrar qualidade visual, consistência da mensagem e potencial de engajamento.",
    cards: [
      {
        title: "Clínica de estética",
        description: "Carrossel educativo com CTA para avaliação e foco em autoridade local.",
        background: "linear-gradient(135deg, #f59e0b 0%, #ea580c 100%)",
      },
      {
        title: "Consultoria fitness",
        description:
          "Post de prova social com linguagem direta para aumentar leads no WhatsApp.",
        background: "linear-gradient(135deg, #6366f1 0%, #3b82f6 100%)",
      },
      {
        title: "Restaurante premium",
        description:
          "Criativo promocional para menu da semana com foco em reserva antecipada.",
        background: "linear-gradient(135deg, #10b981 0%, #0f766e 100%)",
      },
    ],
  },
  bullets: {
    title: "Por que você pode confiar neste processo",
    items: [
      "Diagnóstico rápido para entender posicionamento, oferta e tom de voz.",
      "Criação guiada por IA com revisão estratégica para manter clareza comercial.",
      "Entrega estruturada com ideias prontas para feed, stories e campanhas.",
    ],
  },
} as const;

function buildStyleVariables(definition: LeadPortalSimpleFormStyleDefinition | null) {
  const vars: Record<string, string> = {};
  if (definition?.backgroundGradient) {
    vars["--flow-background"] = definition.backgroundGradient;
  } else if (definition?.backgroundColor) {
    vars["--flow-background"] = definition.backgroundColor;
  }
  if (definition?.backgroundPatternUrl) {
    vars["--flow-background-pattern"] = `url(${definition.backgroundPatternUrl})`;
  }
  if (definition?.cardBackground) {
    vars["--flow-card-background"] = definition.cardBackground;
  }
  if (definition?.cardBorderColor) {
    vars["--flow-card-border"] = definition.cardBorderColor;
  }
  if (definition?.cardShadow) {
    vars["--flow-card-shadow"] = definition.cardShadow;
  }
  if (definition?.headingColor) {
    vars["--flow-heading-color"] = definition.headingColor;
  }
  if (definition?.textColor) {
    vars["--flow-text-color"] = definition.textColor;
  }
  if (definition?.mutedTextColor) {
    vars["--flow-muted-text-color"] = definition.mutedTextColor;
  }
  if (definition?.primaryColor) {
    vars["--flow-primary-color"] = definition.primaryColor;
  }
  if (definition?.accentColor) {
    vars["--flow-accent-color"] = definition.accentColor;
  }
  if (definition?.buttonBackground) {
    vars["--flow-button-background"] = definition.buttonBackground;
  }
  if (definition?.buttonTextColor) {
    vars["--flow-button-text"] = definition.buttonTextColor;
  }
  if (definition?.buttonShadow) {
    vars["--flow-button-shadow"] = definition.buttonShadow;
  }
  if (definition?.buttonBorderRadius) {
    vars["--flow-button-radius"] = definition.buttonBorderRadius;
  }
  if (definition?.highlightBackground) {
    vars["--flow-highlight-background"] = definition.highlightBackground;
  }
  if (definition?.inputBackground) {
    vars["--flow-input-background"] = definition.inputBackground;
  }
  if (definition?.inputBorderColor) {
    vars["--flow-input-border"] = definition.inputBorderColor;
  }
  return vars;
}

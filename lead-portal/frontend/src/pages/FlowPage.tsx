import { useEffect, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { fetchLeadPortalFlow } from "../api";
import FlowForm from "../components/FlowForm";
import { resolveAssetUrl } from "../utils/resolveAssetUrl";
import { useCampaignCode } from "../hooks/useCampaignCode";
import type { FlowQuestion, LeadPortalSimpleFormStyleDefinition } from "../types";

export default function FlowPage() {
  const { slug } = useParams<{ slug: string }>();
  const campaignCode = useCampaignCode();
  const [hasSubmitted, setHasSubmitted] = useState(false);

  useEffect(() => {
    setHasSubmitted(false);
  }, [slug]);

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

  const metadata = useMemo(
    () => extractSimpleFormMetadata(flow?.questions ?? []),
    [flow?.questions],
  );

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
  const styleVars = buildStyleVariables(definition);

  const formQuestions = metadata.formQuestions.length > 0 ? metadata.formQuestions : flow.questions;
  const flowForForm = formQuestions === flow.questions ? flow : { ...flow, questions: formQuestions };

  const defaultHeader = SIMPLE_FORM_CONTENT.header;
  const defaultProof = SIMPLE_FORM_CONTENT.proof;
  const defaultBullets = SIMPLE_FORM_CONTENT.bullets;

  const heroContent = {
    title: metadata.hero.title ?? flow.name,
    subtitle: metadata.hero.subtitle ?? defaultHeader.subtitle,
    promise: metadata.hero.promise ?? defaultHeader.promiseText,
  };

  const proofContent = {
    kicker: defaultProof.kicker,
    title: metadata.proof.title ?? defaultProof.title,
    subtitle: metadata.proof.subtitle ?? defaultProof.subtitle,
    cards: (metadata.proof.cards.length > 0 ? metadata.proof.cards : defaultProof.cards).map((card) => ({
      ...card,
      imageUrl: card.imageUrl ? resolveAssetUrl(card.imageUrl) : null,
    })),
  };

  const [featuredProofCard, ...proofCardsWithoutFeatured] = proofContent.cards;

  const bulletsContent = {
    title: metadata.bullets.title ?? defaultBullets.title,
    items: metadata.bullets.items.length > 0 ? metadata.bullets.items : defaultBullets.items,
  };

  return (
    <div className="flow-page" style={styleVars} data-hero-layout={heroLayout}>
      <div className="flow-container">
        {!hasSubmitted ? (
          <>
            <section className="flow-hero">
              <div className="flow-hero-copy">
                <h1>{heroContent.title}</h1>
                <p className="flow-subtitle">{heroContent.subtitle}</p>
                <div className={`flow-proof-cta ${featuredProofCard?.imageUrl ? "flow-proof-cta--with-image" : ""}`}>
                  {featuredProofCard?.imageUrl ? (
                    <img
                      src={featuredProofCard.imageUrl}
                      alt={featuredProofCard.title}
                      className="flow-proof-cta__media"
                      loading="lazy"
                    />
                  ) : null}
                  <span className="flow-proof-cta__text">
                    <strong>Gostou do estilo?</strong> Preencha o formulário abaixo para receber uma versão
                    personalizada para o seu negócio.
                  </span>
                </div>
                <div className="flow-promise-box">
                  <strong>{defaultHeader.promiseLabel}:</strong> {heroContent.promise}
                </div>
              </div>
            </section>

            <section className="flow-proof-section flow-proof-section--spotlight" aria-label="Exemplos de posts">
              <div className="flow-section-header">
                <p className="flow-section-kicker">{proofContent.kicker}</p>
                <h2>{proofContent.title}</h2>
                <p>{proofContent.subtitle}</p>
              </div>
              {proofCardsWithoutFeatured.length > 0 ? (
                <div className="flow-proof-secondary-grid">
                  {proofCardsWithoutFeatured.map((post, index) => {
                    const mediaStyle = !post.imageUrl && post.background ? { background: post.background } : undefined;
                    return (
                      <article key={post.title} className="flow-proof-card">
                        <span className="flow-proof-card__badge">Exemplo {index + 2}</span>
                        <div
                          className={`flow-proof-image ${post.imageUrl ? "flow-proof-image--media" : ""}`}
                          style={mediaStyle}
                        >
                          {post.imageUrl ? (
                            <img src={post.imageUrl} alt={post.title} className="flow-proof-image__media" loading="lazy" />
                          ) : null}
                          {post.overlayText ? (
                            <span className="flow-proof-image__overlay">{post.overlayText}</span>
                          ) : null}
                        </div>
                        <div className="flow-proof-card__copy">
                          <h3>{post.title}</h3>
                          <p>{post.description}</p>
                        </div>
                      </article>
                    );
                  })}
                </div>
              ) : null}
            </section>

            <section className="flow-confidence-section" aria-label="Detalhes de confiança">
              <h2>{bulletsContent.title}</h2>
              <ul>
                {bulletsContent.items.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            </section>
          </>
        ) : null}

        <FlowForm
          flow={flowForForm}
          campaignCode={campaignCode}
          onSubmitted={() => setHasSubmitted(true)}
        />
      </div>
    </div>
  );
}

const SIMPLE_FORM_CONTENT = {
  header: {
    subtitle: "Transforme ideias em posts prontos para publicar em poucos minutos.",
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

interface SimpleFormMetadata {
  hero: { title: string | null; subtitle: string | null; promise: string | null };
  proof: { title: string | null; subtitle: string | null; cards: ProofCard[] };
  bullets: { title: string | null; items: string[] };
  formQuestions: FlowQuestion[];
}

interface ProofCard {
  title: string;
  description: string;
  background?: string | null;
  imageUrl?: string | null;
  overlayText?: string | null;
}

function extractSimpleFormMetadata(questions: FlowQuestion[]): SimpleFormMetadata {
  if (!questions || questions.length === 0) {
    return {
      hero: { title: null, subtitle: null, promise: null },
      proof: { title: null, subtitle: null, cards: [] },
      bullets: { title: null, items: [] },
      formQuestions: [],
    };
  }

  const metadataKeys = new Set<string>();
  const questionMap = new Map<string, FlowQuestion>(
    questions.map((question) => [question.dataKey, question]),
  );

  const readValue = (key: string) => {
    const question = questionMap.get(key);
    if (question) {
      metadataKeys.add(key);
      return question.title?.trim() ?? null;
    }
    return null;
  };

  const hero = {
    title: readValue("cabecalho_titulo"),
    subtitle: readValue("cabecalho_subtitulo"),
    promise: readValue("cabecalho_promessa"),
  };

  const proofTitle = readValue("exemplos_reais_titulo");
  const proofSubtitle = readValue("exemplos_reais_subtitulo");

  const cards: ProofCard[] = [];
  [1, 2, 3].forEach((index) => {
    const title = readValue(`exemplo_real_card_${index}_titulo`);
    const description = readValue(`exemplo_real_card_${index}_subtitulo`);
    const imageUrl = readValue(`exemplo_real_card_${index}_imagem_url`);
    const overlayText = readValue(`exemplo_real_card_${index}_texto_sobreposto`);
    if (title || description || imageUrl) {
      cards.push({
        title: title ?? `Exemplo ${index}`,
        description: description ?? "",
        imageUrl,
        overlayText,
      });
    }
  });

  const bullets = {
    title: readValue("bullets_titulo"),
    items: ["bullet_item_1", "bullet_item_2", "bullet_item_3"]
      .map((key) => readValue(key))
      .filter((value): value is string => Boolean(value && value.length > 0)),
  };

  const metadataPresent = metadataKeys.size > 0;
  const formQuestions = metadataPresent
    ? questions.filter((question) => !metadataKeys.has(question.dataKey))
    : questions;

  return {
    hero,
    proof: { title: proofTitle, subtitle: proofSubtitle, cards },
    bullets,
    formQuestions,
  };
}

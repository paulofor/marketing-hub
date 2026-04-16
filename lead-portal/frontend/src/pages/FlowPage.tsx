import { useEffect, useMemo, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import {
  API_BASE_URL,
  fetchLeadPortalFlow,
  registerFlowRenderComplete,
  submitFlowSubmission,
} from "../api";
import FlowForm from "../components/FlowForm";
import SubmissionSuccessCard from "../components/SubmissionSuccessCard";
import { resolveAssetUrl } from "../utils/resolveAssetUrl";
import {
  normalizeCustomTemplatePayload,
  type CustomTemplateFormFieldSpec,
  type CustomTemplateFormSpec,
} from "../utils/customTemplateHtml";
import { getVisitorIdCookie } from "../utils/visitorCookie";
import { useCampaignCode } from "../hooks/useCampaignCode";
import type {
  FlowQuestion,
  FlowSubmissionResponse,
  LeadPortalFlow,
  LeadPortalSimpleFormStyleDefinition,
} from "../types";

export default function FlowPage() {
  const { slug } = useParams<{ slug: string }>();
  const campaignCode = useCampaignCode();
  const [hasSubmitted, setHasSubmitted] = useState(false);
  const [hasTrackedRenderComplete, setHasTrackedRenderComplete] = useState(false);
  const [submissionResult, setSubmissionResult] = useState<FlowSubmissionResponse | null>(null);

  useEffect(() => {
    setHasSubmitted(false);
    setHasTrackedRenderComplete(false);
    setSubmissionResult(null);
  }, [slug]);

  const handleSubmissionComplete = (result: FlowSubmissionResponse) => {
    setSubmissionResult(result);
    setHasSubmitted(true);
  };

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

  const resolvedFlowSlug = flow?.slug ?? null;

  const metadata = useMemo(
    () => extractSimpleFormMetadata(flow?.questions ?? []),
    [flow?.questions],
  );
  const customTemplatePayload = useMemo(
    () => normalizeCustomTemplatePayload(flow?.customFormHtml),
    [flow?.customFormHtml],
  );
  const customTemplateHtml = customTemplatePayload?.html;
  const customTemplateFormSpec = customTemplatePayload?.formSpec;
  const hasCustomTemplate = Boolean(customTemplateHtml);
  const shouldRenderStandaloneTemplate =
    hasCustomTemplate && Boolean(customTemplateHtml) && flow?.customFormRenderMode === "STANDALONE_PAGE";
  const customTemplateVariables = useMemo(() => {
    if (!hasCustomTemplate || !flow) {
      return null;
    }
    return buildCustomHtmlTemplateVariables(flow, metadata);
  }, [hasCustomTemplate, flow, metadata]);

  useEffect(() => {
    if (!resolvedFlowSlug || isLoading || isError || hasTrackedRenderComplete) {
      return;
    }
    let cancelled = false;

    registerFlowRenderComplete(resolvedFlowSlug, getVisitorIdCookie(), campaignCode)
      .then(() => {
        if (!cancelled) {
          window.dispatchEvent(
            new CustomEvent("lead-portal-render-complete", {
              detail: {
                flowSlug: resolvedFlowSlug,
                campaignCode: campaignCode ?? null,
              },
            }),
          );
          setHasTrackedRenderComplete(true);
        }
      })
      .catch((trackError) => {
        console.warn("Falha ao registrar render-complete do fluxo", trackError);
      });

    return () => {
      cancelled = true;
    };
  }, [resolvedFlowSlug, isLoading, isError, hasTrackedRenderComplete, campaignCode]);

  if (!slug) {
    return <p className="flow-message">Fluxo não informado.</p>;
  }

  if (isLoading) {
    return (
      <p className="flow-message">
        Preparando uma oferta especial para você...
      </p>
    );
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
  const flowForForm =
    formQuestions === flow.questions
      ? flow
      : {
          ...flow,
          questions: formQuestions,
        };
  if (hasCustomTemplate && customTemplateHtml) {
    const templateVariables = customTemplateVariables ?? new Map<string, string>();
    if (shouldRenderStandaloneTemplate) {
      return (
        <StandaloneCustomFlowTemplate
          html={customTemplateHtml}
          variables={templateVariables}
          flowSlug={flow.slug}
          formSpec={customTemplateFormSpec}
          campaignCode={campaignCode}
          onSubmitted={handleSubmissionComplete}
        />
      );
    }
    const successState = customTemplateFormSpec?.successState;
    return (
      <div className="flow-page flow-page--custom" style={styleVars}>
        {hasSubmitted ? (
          <div className="flow-custom-feedback">
            <SubmissionSuccessCard
              name={submissionResult?.name}
              email={submissionResult?.email}
              title={successState?.title}
              message={successState?.message}
            />
          </div>
        ) : (
          <div className="flow-custom-template">
            <CustomFlowTemplate
              html={customTemplateHtml}
              variables={templateVariables}
              flowSlug={flow.slug}
              formSpec={customTemplateFormSpec}
              campaignCode={campaignCode}
              onSubmitted={handleSubmissionComplete}
            />
          </div>
        )}
      </div>
    );
  }

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
                    personalizada para você.
                  </span>
                </div>
                <div className="flow-promise-box">
                  {heroContent.promise}
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
                  {proofCardsWithoutFeatured.map((post) => {
                    const mediaStyle = !post.imageUrl && post.background ? { background: post.background } : undefined;
                    return (
                      <article key={post.title} className="flow-proof-card">
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
          onSubmitted={handleSubmissionComplete}
        />
      </div>
    </div>
  );
}

interface CustomFlowTemplateProps {
  html: string;
  variables: Map<string, string>;
  flowSlug: string;
  formSpec?: CustomTemplateFormSpec;
  campaignCode?: string | null;
  onSubmitted?: (result: FlowSubmissionResponse) => void;
}

const TOKEN_REGEX = /\{\{\s*([a-zA-Z0-9_-]+)\s*\}\}/g;

function CustomFlowTemplate({
  html,
  variables,
  flowSlug,
  formSpec,
  campaignCode,
  onSubmitted,
}: CustomFlowTemplateProps) {
  const iframeRef = useRef<HTMLIFrameElement | null>(null);
  const resizeObserverRef = useRef<ResizeObserver | null>(null);
  const bridgeCleanupRef = useRef<(() => void) | null>(null);
  const processedHtml = useMemo(
    () => renderTemplateWithTokens(html ?? "", variables),
    [html, variables],
  );

  useEffect(() => {
    const iframe = iframeRef.current;
    if (!iframe) {
      return;
    }

    const cleanupObserver = () => {
      if (resizeObserverRef.current) {
        resizeObserverRef.current.disconnect();
        resizeObserverRef.current = null;
      }
    };

    const cleanupBridge = () => {
      if (bridgeCleanupRef.current) {
        bridgeCleanupRef.current();
        bridgeCleanupRef.current = null;
      }
    };

    const updateHeight = () => {
      const doc = iframe.contentDocument;
      if (!doc) {
        return;
      }
      const { body, documentElement } = doc;
      if (!body || !documentElement) {
        return;
      }
      const height = Math.max(
        body.scrollHeight,
        body.offsetHeight,
        documentElement.scrollHeight,
        documentElement.offsetHeight,
        documentElement.clientHeight,
      );
      iframe.style.height = `${height}px`;
    };

    const handleLoad = () => {
      cleanupObserver();
      cleanupBridge();
      updateHeight();
      const doc = iframe.contentDocument;
      if (!doc) {
        return;
      }
      const newBridgeCleanup = attachCustomTemplateBridge(iframe, {
        flowSlug,
        formSpec,
        campaignCode,
        onSubmitted,
      });
      if (newBridgeCleanup) {
        bridgeCleanupRef.current = newBridgeCleanup;
      }
      if (typeof ResizeObserver === "undefined") {
        return;
      }
      const observer = new ResizeObserver(() => {
        updateHeight();
      });
      if (doc.body) {
        observer.observe(doc.body);
      }
      if (doc.documentElement) {
        observer.observe(doc.documentElement);
      }
      resizeObserverRef.current = observer;
    };

    iframe.addEventListener("load", handleLoad);
    if (iframe.contentDocument?.readyState === "complete") {
      handleLoad();
    }
    return () => {
      iframe.removeEventListener("load", handleLoad);
      cleanupObserver();
      cleanupBridge();
    };
  }, [processedHtml, flowSlug, formSpec, campaignCode, onSubmitted]);

  if (!processedHtml) {
    return (
      <div className="flow-custom-template flow-custom-template--empty">
        <p className="flow-message">Nenhum HTML personalizado configurado.</p>
      </div>
    );
  }

  return (
    <div className="flow-custom-template-wrapper">
      <iframe
        ref={iframeRef}
        className="flow-custom-template-frame"
        srcDoc={processedHtml}
        title="Conteúdo personalizado do fluxo"
        sandbox="allow-same-origin allow-scripts allow-forms allow-modals allow-popups allow-popups-to-escape-sandbox allow-downloads allow-top-navigation-by-user-activation"
      />
    </div>
  );
}


function StandaloneCustomFlowTemplate({
  html,
  variables,
  flowSlug,
  formSpec,
  campaignCode,
  onSubmitted,
}: CustomFlowTemplateProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const processedHtml = useMemo(
    () => renderTemplateWithTokens(html ?? "", variables),
    [html, variables],
  );
  const parsedDocument = useMemo(
    () => parseStandaloneTemplateDocument(processedHtml),
    [processedHtml],
  );
  const requiresManagedRuntime = Boolean(formSpec);

  useEffect(() => {
    if (!parsedDocument || typeof document === "undefined") {
      return;
    }
    const appendedNodes: Element[] = [];
    parsedDocument.headNodes.forEach((nodeHtml) => {
      const instantiated = instantiateHeadNodeFromHtml(nodeHtml);
      if (instantiated) {
        instantiated.setAttribute("data-flow-standalone-head", "true");
        document.head.appendChild(instantiated);
        appendedNodes.push(instantiated);
      }
    });
    let previousTitle: string | null = null;
    if (parsedDocument.title) {
      previousTitle = document.title;
      document.title = parsedDocument.title;
    }
    const restoredAttributes: Array<{ name: string; previous: string | null }> = [];
    parsedDocument.bodyAttributes.forEach(({ name, value }) => {
      const previous = document.body.getAttribute(name);
      restoredAttributes.push({ name, previous });
      document.body.setAttribute(name, value);
    });
    return () => {
      appendedNodes.forEach((node) => node.remove());
      if (previousTitle !== null) {
        document.title = previousTitle;
      }
      restoredAttributes.forEach(({ name, previous }) => {
        if (previous === null) {
          document.body.removeAttribute(name);
        } else {
          document.body.setAttribute(name, previous);
        }
      });
    };
  }, [parsedDocument]);

  useEffect(() => {
    if (!parsedDocument?.bodyHtml || typeof document === "undefined") {
      return;
    }
    const container = containerRef.current;
    if (!container) {
      return;
    }
    const scripts = Array.from(container.querySelectorAll("script"));
    scripts.forEach((script) => {
      const replacement = document.createElement("script");
      Array.from(script.attributes).forEach((attr) => {
        replacement.setAttribute(attr.name, attr.value);
      });
      replacement.text = script.textContent ?? "";
      script.replaceWith(replacement);
    });
  }, [parsedDocument?.bodyHtml]);

  useEffect(() => {
    if (!requiresManagedRuntime || typeof document === "undefined" || typeof window === "undefined") {
      return;
    }
    const cleanup = attachCustomTemplateBridgeToDocument(
      document,
      window,
      {
        flowSlug,
        formSpec,
        campaignCode,
        onSubmitted,
        rootElement: containerRef.current ?? document.body,
      },
      {
        scrollToElement: (element, behavior) => {
          element.scrollIntoView({ behavior, block: "start" });
        },
      },
    );
    return cleanup;
  }, [
    requiresManagedRuntime,
    flowSlug,
    formSpec,
    campaignCode,
    onSubmitted,
    parsedDocument?.bodyHtml,
  ]);

  if (!processedHtml) {
    return (
      <div className="flow-standalone-container flow-standalone-container--empty">
        <p className="flow-message">Nenhum HTML personalizado configurado.</p>
      </div>
    );
  }

  return (
    <div className="flow-standalone-container">
      <div
        ref={containerRef}
        className="flow-standalone-content"
        dangerouslySetInnerHTML={{ __html: parsedDocument?.bodyHtml ?? processedHtml }}
      />
    </div>
  );
}


interface CustomTemplateBridgeOptions {
  flowSlug: string;
  formSpec?: CustomTemplateFormSpec;
  campaignCode?: string | null;
  onSubmitted?: (result: FlowSubmissionResponse) => void;
  rootElement?: Element | null;
}

interface CustomTemplateBridgeContext {
  scrollToElement?: (element: Element, behavior: ScrollBehavior) => void;
}


function attachCustomTemplateBridge(
  iframe: HTMLIFrameElement,
  options: CustomTemplateBridgeOptions,
) {
  const doc = iframe.contentDocument;
  const win = iframe.contentWindow;
  if (!doc || !win) {
    return null;
  }
  const rootElement = doc.body ?? doc.documentElement ?? null;
  return attachCustomTemplateBridgeToDocument(
    doc,
    win,
    {
      ...options,
      rootElement,
    },
    {
      scrollToElement: (element, behavior) => {
        scrollParentToElement(iframe, element, behavior);
      },
    },
  );
}

function attachCustomTemplateBridgeToDocument(
  doc: Document,
  win: Window,
  options: CustomTemplateBridgeOptions,
  context?: CustomTemplateBridgeContext,
) {
  if (options.formSpec) {
    renderManagedTemplateForm(doc, options.formSpec);
  }

  const scrollToElement = context?.scrollToElement ?? ((element: Element, behavior: ScrollBehavior) => {
    element.scrollIntoView({ behavior, block: "start" });
  });

  const handleAnchorClick = (event: MouseEvent) => {
    const target = event.target as Element | null;
    const anchor = target?.closest?.('a[href^="#"]') as HTMLAnchorElement | null;
    if (!anchor) {
      return;
    }
    if (options.rootElement && !options.rootElement.contains(anchor)) {
      return;
    }
    const href = anchor.getAttribute("href");
    if (!href || href === "#") {
      return;
    }
    const anchorTargetId = href.slice(1);
    if (!anchorTargetId) {
      return;
    }
    const anchorTarget = doc.getElementById(anchorTargetId);
    if (!anchorTarget) {
      return;
    }
    event.preventDefault();
    const behaviorAttr = anchor.getAttribute("data-scroll-behavior");
    const behavior: ScrollBehavior = behaviorAttr === "auto" ? "auto" : "smooth";
    scrollToElement(anchorTarget, behavior);
  };

  doc.addEventListener("click", handleAnchorClick, true);

  const handleFormSubmit = async (event: Event) => {
    const target = event.target as HTMLFormElement | null;
    if (!target || target.tagName.toLowerCase() !== "form") {
      return;
    }
    if (options.rootElement && !options.rootElement.contains(target)) {
      return;
    }
    event.preventDefault();
    event.stopImmediatePropagation();
    if (target.dataset.leadPortalSubmitting === "true") {
      return;
    }

    target.dataset.leadPortalSubmitting = "true";
    toggleTemplateSubmitButtons(target, true);

    try {
      const parsed = parseTemplateSubmissionPayload(target, options.campaignCode);
      const response = await submitFlowSubmission(options.flowSlug, parsed.payload, parsed.image);
      target.dataset.leadPortalSubmitted = "true";
      writeManagedTemplateFeedback(
        target,
        "success",
        options.formSpec?.successState?.title ?? "Tudo certo!",
        options.formSpec?.successState?.message ?? "Recebemos seus dados com sucesso.",
      );
      target.dispatchEvent(
        new CustomEvent("leadportal:submission-success", { bubbles: true }),
      );
      window.dispatchEvent(
        new CustomEvent("lead_portal_submission", {
          detail: {
            flowSlug: options.flowSlug,
            campaignCode: options.campaignCode ?? null,
            mode: "managed-runtime",
            status: "success",
            submissionId: response?.id ?? null,
          },
        }),
      );
      options.onSubmitted?.(response);
    } catch (error) {
      console.error("Falha ao enviar formulário do template", error);
      writeManagedTemplateFeedback(
        target,
        "error",
        "Não foi possível enviar agora",
        "Tente novamente em instantes.",
      );
      window.dispatchEvent(
        new CustomEvent("lead_portal_submission", {
          detail: {
            flowSlug: options.flowSlug,
            campaignCode: options.campaignCode ?? null,
            mode: "managed-runtime",
            status: "error",
          },
        }),
      );
    } finally {
      toggleTemplateSubmitButtons(target, false);
      delete target.dataset.leadPortalSubmitting;
    }
  };

  doc.addEventListener("submit", handleFormSubmit, true);

  return () => {
    doc.removeEventListener("click", handleAnchorClick, true);
    doc.removeEventListener("submit", handleFormSubmit, true);
  };
}

function toggleTemplateSubmitButtons(form: HTMLFormElement, isSubmitting: boolean) {
  const submitButtons = form.querySelectorAll<HTMLButtonElement | HTMLInputElement>(
    'button[type="submit"], input[type="submit"]',
  );
  submitButtons.forEach((button) => {
    button.disabled = isSubmitting;
    button.setAttribute("aria-busy", isSubmitting ? "true" : "false");
  });
}

function renderManagedTemplateForm(doc: Document, formSpec: CustomTemplateFormSpec) {
  const target =
    (doc.getElementById(formSpec.formId) as HTMLFormElement | null) ??
    (doc.querySelector("form") as HTMLFormElement | null);
  if (!target) {
    return;
  }
  target.id = formSpec.formId;
  target.setAttribute("novalidate", "novalidate");
  target.setAttribute("data-lead-portal-managed", "true");
  target.removeAttribute("action");
  target.removeAttribute("method");
  target.querySelectorAll("[data-runtime-node='true']").forEach((node) => node.remove());

  const fragment = doc.createDocumentFragment();
  if (formSpec.title) {
    const title = doc.createElement("h2");
    title.textContent = formSpec.title;
    title.setAttribute("data-runtime-node", "true");
    fragment.appendChild(title);
  }

  formSpec.fields.forEach((field) => {
    fragment.appendChild(buildManagedField(doc, field));
  });

  if (formSpec.consent?.enabled && formSpec.consent.label) {
    const consentWrapper = doc.createElement("label");
    consentWrapper.setAttribute("data-runtime-node", "true");
    consentWrapper.style.display = "flex";
    consentWrapper.style.gap = "0.5rem";
    const consentInput = doc.createElement("input");
    consentInput.type = "checkbox";
    consentInput.name = "consentimento";
    if (formSpec.consent.required) {
      consentInput.required = true;
    }
    const consentText = doc.createElement("span");
    consentText.textContent = formSpec.consent.label;
    consentWrapper.appendChild(consentInput);
    consentWrapper.appendChild(consentText);
    fragment.appendChild(consentWrapper);
  }

  const submitButton = doc.createElement("button");
  submitButton.type = "submit";
  submitButton.textContent = formSpec.submitLabel;
  submitButton.setAttribute("data-runtime-node", "true");
  submitButton.className = "lead-portal-runtime-submit";
  fragment.appendChild(submitButton);

  const feedback = doc.createElement("div");
  feedback.setAttribute("data-runtime-node", "true");
  feedback.setAttribute("data-runtime-feedback", "true");
  feedback.setAttribute("role", "status");
  feedback.setAttribute("aria-live", "polite");
  feedback.style.marginTop = "0.75rem";
  fragment.appendChild(feedback);

  target.replaceChildren(fragment);
}

function buildManagedField(doc: Document, field: CustomTemplateFormFieldSpec) {
  const wrapper = doc.createElement("div");
  wrapper.setAttribute("data-runtime-node", "true");
  wrapper.className = "lead-portal-runtime-field";

  const label = doc.createElement("label");
  label.htmlFor = `field_${field.name}`;
  label.textContent = field.required ? `${field.label} *` : field.label;

  const input = doc.createElement("input");
  input.id = `field_${field.name}`;
  input.name = field.name;
  input.type = field.type;
  input.required = field.required;
  input.placeholder = field.placeholder ?? "";
  input.autocomplete = field.type === "email" ? "email" : field.type === "tel" ? "tel" : "name";

  wrapper.appendChild(label);
  wrapper.appendChild(input);
  return wrapper;
}

function writeManagedTemplateFeedback(
  form: HTMLFormElement,
  status: "success" | "error",
  title: string,
  message: string,
) {
  const container = form.querySelector<HTMLElement>("[data-runtime-feedback='true']");
  if (!container) {
    return;
  }
  container.dataset.status = status;
  container.textContent = `${title} ${message}`.trim();
}

function parseTemplateSubmissionPayload(
  form: HTMLFormElement,
  campaignCode?: string | null,
) {
  const formData = new FormData(form);
  const answers: Record<string, string | string[]> = {};
  let image: File | null = null;
  let imageKey: string | undefined;
  let name = "";
  let email = "";

  formData.forEach((rawValue, rawKey) => {
    const key = normalizeTemplateFieldKey(rawKey);
    if (!key) {
      return;
    }

    if (rawValue instanceof File) {
      if (rawValue.size > 0 && !image) {
        image = rawValue;
        imageKey = key;
      }
      return;
    }

    const value = String(rawValue ?? "").trim();
    if (!value) {
      return;
    }

    if (!name && isNameField(key)) {
      name = value;
    }
    if (!email && isEmailField(key)) {
      email = value;
    }

    const previousValue = answers[key];
    if (previousValue === undefined) {
      answers[key] = value;
    } else if (Array.isArray(previousValue)) {
      answers[key] = [...previousValue, value];
    } else {
      answers[key] = [previousValue, value];
    }
  });

  ensureRequiredSubmissionContractFields(name, email);

  return {
    payload: {
      name,
      email,
      answers,
      imageKey,
      campaignCode: campaignCode ?? undefined,
    },
    image,
  };
}

function ensureRequiredSubmissionContractFields(name: string, email: string) {
  if (!name.trim()) {
    throw new Error("Contrato de submissão v1 exige contato.nome preenchido.");
  }
  if (!email.trim()) {
    throw new Error("Contrato de submissão v1 exige contato.email preenchido.");
  }
}

function normalizeTemplateFieldKey(rawKey: string) {
  return rawKey?.trim().toLowerCase() ?? "";
}

function isNameField(key: string) {
  return key.includes("nome") || key.includes("name");
}

function isEmailField(key: string) {
  return key.includes("email") || key === "e-mail";
}

function normalizeScrollArguments(optionsOrX?: number | ScrollToOptions, maybeY?: number) {
  if (typeof optionsOrX === "object" && optionsOrX !== null) {
    const behavior = optionsOrX.behavior === "smooth" ? "smooth" : "auto";
    const top = typeof optionsOrX.top === "number" ? optionsOrX.top : 0;
    return { top, behavior };
  }
  if (typeof optionsOrX === "number" && typeof maybeY === "number") {
    return { top: maybeY, behavior: "auto" };
  }
  if (typeof optionsOrX === "number") {
    return { top: optionsOrX, behavior: "auto" };
  }
  if (typeof maybeY === "number") {
    return { top: maybeY, behavior: "auto" };
  }
  return { top: 0, behavior: "auto" };
}

function normalizeBehavior(arg?: boolean | ScrollIntoViewOptions): ScrollBehavior {
  if (arg && typeof arg === "object" && arg.behavior === "smooth") {
    return "smooth";
  }
  return "auto";
}

function scrollParentToElement(iframe: HTMLIFrameElement, element: Element, behavior: ScrollBehavior) {
  const doc = iframe.contentDocument;
  const win = iframe.contentWindow;
  if (!doc || !win) {
    return;
  }
  const rect = element.getBoundingClientRect();
  const currentOffset = getDocumentScrollTop(win, doc);
  scrollParentToDocumentOffset(iframe, rect.top + currentOffset, behavior);
}

function scrollParentToDocumentOffset(iframe: HTMLIFrameElement, offset: number, behavior: ScrollBehavior) {
  const iframeRect = iframe.getBoundingClientRect();
  const parentScrollTop = window.scrollY || document.documentElement.scrollTop || document.body.scrollTop || 0;
  const target = iframeRect.top + parentScrollTop + offset;
  window.scrollTo({ top: target, behavior });
}

function getDocumentScrollTop(win: Window, doc: Document) {
  return win.scrollY || doc.documentElement.scrollTop || doc.body.scrollTop || 0;
}


interface ParsedStandaloneTemplate {
  headNodes: string[];
  bodyHtml: string;
  title: string | null;
  bodyAttributes: Array<{ name: string; value: string }>;
}

function parseStandaloneTemplateDocument(html?: string | null): ParsedStandaloneTemplate | null {
  if (!html) {
    return null;
  }
  if (typeof DOMParser === "undefined") {
    return {
      headNodes: [],
      bodyHtml: html,
      title: null,
      bodyAttributes: [],
    };
  }
  const parser = new DOMParser();
  const doc = parser.parseFromString(html, "text/html");
  const headNodes = Array.from(doc.head?.children ?? []).map((element) => element.outerHTML);
  const bodyAttributes = doc.body
    ? Array.from(doc.body.attributes).map((attr) => ({
        name: attr.name,
        value: attr.value,
      }))
    : [];
  let bodyHtml = doc.body?.innerHTML ?? "";
  if (!bodyHtml.trim() && doc.documentElement) {
    bodyHtml = doc.documentElement.innerHTML ?? "";
  }
  if (!bodyHtml.trim()) {
    bodyHtml = html;
  }
  const title = doc.title || null;
  return { headNodes, bodyHtml, title, bodyAttributes };
}

function instantiateHeadNodeFromHtml(nodeHtml: string): HTMLElement | null {
  if (typeof document === "undefined") {
    return null;
  }
  const template = document.createElement("template");
  template.innerHTML = (nodeHtml ?? "").trim();
  const element = template.content.firstElementChild as HTMLElement | null;
  if (!element) {
    return null;
  }
  if (element.tagName?.toLowerCase() === "script") {
    const script = document.createElement("script");
    Array.from(element.attributes).forEach((attr) => {
      script.setAttribute(attr.name, attr.value);
    });
    script.text = element.textContent ?? "";
    return script;
  }
  return element;
}

function buildCustomHtmlTemplateVariables(flow: LeadPortalFlow, metadata: SimpleFormMetadata) {
  const variables = new Map<string, string>();
  if (!flow) {
    return variables;
  }
  const setValue = (key: string, value: string | null | undefined) => {
    const normalized = value ?? "";
    variables.set(key, normalized);
    variables.set(key.toLowerCase(), normalized);
  };
  const shouldResolveAsAssetUrl = (key: string) => {
    const normalizedKey = key.toLowerCase();
    return normalizedKey.includes("imagem_url") || normalizedKey.includes("image_url");
  };
  flow.questions.forEach((question) => {
    const dataKey = question.dataKey?.trim();
    if (!dataKey) {
      return;
    }
    const value = shouldResolveAsAssetUrl(dataKey)
      ? resolveAssetUrl(question.title ?? "")
      : question.title ?? "";
    setValue(dataKey, value);
  });
  const proofCards = metadata?.proof?.cards ?? [];
  [0, 1, 2].forEach((index) => {
    const dataKey = `exemplo_real_card_${index + 1}_imagem_url`;
    const storedValue = variables.get(dataKey) ?? variables.get(dataKey.toLowerCase()) ?? "";
    const fallbackValue = resolveAssetUrl(proofCards[index]?.imageUrl ?? "");
    const resolved = storedValue && storedValue.trim().length > 0 ? storedValue : fallbackValue;
    setValue(`imagem${index + 1}`, resolved);
  });
  setValue("flow_slug", flow.slug);
  setValue("flow_name", flow.name);
  setValue("flow_description", flow.description ?? "");
  setValue("url", `${API_BASE_URL}/flows/${encodeURIComponent(flow.slug)}/submissions`);
  return variables;
}

function renderTemplateWithTokens(template: string, variables: Map<string, string>) {
  const trimmed = template?.trim();
  if (!trimmed) {
    return "";
  }
  return trimmed.replace(TOKEN_REGEX, (match, rawKey) => {
    const lookupKey = typeof rawKey === "string" ? rawKey.trim() : "";
    if (!lookupKey) {
      return match;
    }
    const value = variables.get(lookupKey) ?? variables.get(lookupKey.toLowerCase());
    if (value === undefined) {
      return match;
    }
    return escapeHtml(value);
  });
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

const SIMPLE_FORM_CONTENT = {
  header: {
    subtitle: "Transforme ideias em posts prontos para publicar em poucos minutos.",
    promiseText:
      "você recebe uma linha editorial visual clara, com linguagem alinhada ao seu público e foco em gerar mais conversas no direct.",
  },
  proof: {
    kicker: "Exemplos Visuais",
    title: "Veja exemplos do estilo visual que você pode receber",
    subtitle:
      "Um material mais profissional ajuda seu perfil a chamar mais atenção, transmitir mais confiança e valorizar melhor o seu serviço logo no primeiro olhar.",
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

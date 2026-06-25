import { useMemo, useState } from "react";
import { NavLink } from "react-router-dom";
import type { LucideIcon } from "lucide-react";
import {
  BadgeCheck,
  AppWindow,
  BarChart3,
  Bot,
  ClipboardCheck,
  ClipboardList,
  Compass,
  Flag,
  GraduationCap,
  Image,
  Instagram,
  MessageSquare,
  History,
  Package,
  PanelLeftClose,
  PanelLeftOpen,
  Layers,
  Map,
  PlusCircle,
  Shapes,
  ShieldCheck,
  Sparkles,
  Trophy,
  Server,
  Users,
  Workflow,
  AlertTriangle,
  CreditCard,
  Cpu,
  ScrollText,
  Search,
  Mail,
  Inbox,
  Microscope,
  HeartPulse,
} from "lucide-react";
import experimentIcon from "../assets/icons/experiment-icon.svg";
import hypothesisIcon from "../assets/icons/hypothesis-icon.svg";
import nicheIcon from "../assets/icons/niche-icon.svg";
import { useOpsMonitorAvailability } from "../api/useOpsMonitor";
import "./MainNavigation.css";

type NavIcon = LucideIcon | string;

type NavItem = {
  to: string;
  label: string;
  icon?: NavIcon;
  end?: boolean;
  children?: NavItem[];
};

type NavSection = {
  title: string;
  items: NavItem[];
};

const NAV_SECTIONS: NavSection[] = [
  {
    title: "Contas",
    items: [
      {
        to: "/accounts/facebook",
        label: "Contas do Facebook",
        icon: Users,
      },
      {
        to: "/accounts/instagram",
        label: "Contas do Instagram",
        icon: Instagram,
      },
    ],
  },
  {
    title: "Biblioteca",
    items: [
      { to: "/media", label: "Mídia", icon: Image },
      { to: "/courses", label: "Cursos", icon: GraduationCap },
      { to: "/products", label: "Produtos", icon: Package },
      { to: "/app-ideas", label: "Ideias de Aplicativo", icon: AppWindow },
      {
        to: "/success-products",
        label: "Produtos de Sucesso",
        icon: Trophy,
      },
    ],
  },
  {
    title: "Testes",
    items: [
      { to: "/niches", label: "Nichos", icon: nicheIcon },
      {
        to: "/experiments",
        label: "Experimentos",
        icon: experimentIcon,
        children: [
          {
            to: "/facebook-campaigns",
            label: "Experimentos para campanha",
            icon: Flag,
            end: true,
          },
        ],
      },
      { to: "/hypotheses", label: "Hipóteses", icon: hypothesisIcon },
      { to: "/mois", label: "MOIS", icon: Workflow },
      {
        to: "/mois/sales-pages-library",
        label: "Biblioteca Sales Pages",
        icon: Workflow,
      },
      {
        to: "/oprm",
        label: "OPRM",
        icon: Workflow,
        children: [
          {
            to: "/oprm",
            label: "CNAEs",
            icon: Workflow,
            end: true,
          },
          {
            to: "/oprm/general-audiences",
            label: "Públicos Gerais",
            icon: Users,
          },
        ],
      },
      { to: "/mds", label: "MDS", icon: Microscope },
      { to: "/hotmart", label: "Hotmart", icon: Workflow },
      { to: "/clickbase", label: "Clickbase", icon: Workflow },
    ],
  },
  {
    title: "IA e Conteúdo",
    items: [
      { to: "/ai-services", label: "IA", icon: Bot },
      { to: "/openai-models", label: "Modelos OpenAI", icon: Cpu },
      { to: "/agents", label: "Agentes", icon: Bot },
      { to: "/agent-themes", label: "Temas de agente", icon: Layers },
      {
        to: "/differentiated-technologies",
        label: "Tecnologias diferenciadas",
        icon: Cpu,
      },
      { to: "/microservices", label: "Microserviços", icon: Server },
      { to: "/pipelines", label: "Pipelines", icon: Workflow },
      {
        to: "/microservices/errors",
        label: "Erros de microserviço",
        icon: AlertTriangle,
      },
      { to: "/chat-dialogs", label: "ChatGPT", icon: MessageSquare },
      { to: "/prompt-entities", label: "Objetos de Prompt", icon: Shapes },
      { to: "/prompt-domains", label: "Domínios de Prompt", icon: Map },
      { to: "/prompts", label: "Prompts (templates)", icon: ScrollText },
      { to: "/angles", label: "Angles", icon: Compass },
      { to: "/visual-proofs", label: "Provas Visuais", icon: BadgeCheck },
      {
        to: "/emotional-triggers",
        label: "Gatilhos Emocionais",
        icon: Sparkles,
      },
      {
        to: "/ai/generations",
        label: "Gerações IA",
        icon: History,
      },
      {
        to: "/ai/pending-requests",
        label: "Fila do Worker IA",
        icon: ClipboardList,
      },
      {
        to: "/targeting/recent-queries",
        label: "Consultas Meta Ads",
        icon: Search,
      },
    ],
  },
  {
    title: "Campanhas",
    items: [
      { to: "/funnels", label: "Funil de Vendas", icon: Workflow },
      {
        to: "/lead-portal/metrics",
        label: "Envio de imagem no portal",
        icon: Image,
      },
      {
        to: "/lead-portal/form-responses",
        label: "Respostas de formulários",
        icon: Inbox,
      },
      {
        to: "/lead-portal/images",
        label: "Imagens recebidas",
        icon: Package,
      },
      {
        to: "/lead-portal/simple-form-styles",
        label: "Estilos do formulário simples",
        icon: Sparkles,
      },
      {
        to: "/lead-portal/image-prompts",
        label: "Prompt de imagens",
        icon: Sparkles,
      },
      {
        to: "/lead-portal/email-template",
        label: "Template do e-mail do lead",
        icon: Mail,
      },
      {
        to: "/facebook-campaigns/ready",
        label: "Experimentos prontos",
        icon: ClipboardCheck,
      },
      { to: "/analytics", label: "Analytics", icon: BarChart3 },
      { to: "/ops-monitor", label: "Operação / Saúde", icon: HeartPulse },
    ],
  },
  {
    title: "Financeiro",
    items: [{ to: "/payments", label: "Pagamentos", icon: CreditCard }],
  },
  {
    title: "Configurações",
    items: [
      {
        to: "/whatsapp",
        label: "WhatsApp",
        icon: MessageSquare,
      },
      {
        to: "/settings/email-service",
        label: "Serviço de e-mail",
        icon: Mail,
      },
      {
        to: "/settings/privacy-policy",
        label: "Política de privacidade",
        icon: ShieldCheck,
      },
    ],
  },
];

const OFFLINE_MODULE_LIMIT = 4;

export default function MainNavigation() {
  const [isPinned, setIsPinned] = useState(false);
  const [isHovered, setIsHovered] = useState(false);
  const isExpanded = isPinned || isHovered;
  const availabilityQuery = useOpsMonitorAvailability();
  const offlineModules = useMemo(() => {
    return (availabilityQuery.data ?? [])
      .filter((module) => module.status === "OFFLINE")
      .slice(0, OFFLINE_MODULE_LIMIT);
  }, [availabilityQuery.data]);
  const shouldShowOfflinePanel =
    availabilityQuery.isLoading ||
    availabilityQuery.isError ||
    offlineModules.length > 0;

  return (
    <aside
      className={`main-navigation${isExpanded ? " is-expanded" : ""}`}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      <div className="main-navigation__header">
        <button
          type="button"
          className="main-navigation__toggle"
          onClick={() => setIsPinned((value) => !value)}
          aria-label={
            isPinned ? "Recolher menu principal" : "Expandir menu principal"
          }
        >
          {isExpanded ? (
            <PanelLeftClose aria-hidden="true" size={18} />
          ) : (
            <PanelLeftOpen aria-hidden="true" size={18} />
          )}
        </button>
        <div className="main-navigation__brand">
          <span className="main-navigation__logo" aria-hidden="true">
            <img
              src="/favicon.ico"
              alt=""
              width={32}
              height={32}
              className="main-navigation__logo-image"
            />
          </span>
          <span className="main-navigation__brand-name">Marketing Hub</span>
        </div>
      </div>
      <nav
        className="main-navigation__sections"
        aria-label="Navegação principal"
      >
        {NAV_SECTIONS.map((section) => (
          <div className="main-navigation__section" key={section.title}>
            <p className="main-navigation__section-title">{section.title}</p>
            <div className="main-navigation__items">
              {section.items.map((item) => {
                const hasChildren = Boolean(item.children?.length);

                return (
                  <div
                    className={[
                      "main-navigation__item-group",
                      hasChildren ? "has-children" : "",
                    ]
                      .filter(Boolean)
                      .join(" ")}
                    key={item.to}
                  >
                    <NavLink
                      to={item.to}
                      end={item.end}
                      className={({ isActive }) =>
                        ["main-navigation__link", isActive ? "is-active" : ""]
                          .filter(Boolean)
                          .join(" ")
                      }
                      aria-label={item.label}
                      title={item.label}
                    >
                      {item.icon ? (
                        typeof item.icon === "string" ? (
                          <img
                            src={item.icon}
                            className="main-navigation__icon"
                            alt=""
                            loading="lazy"
                          />
                        ) : (
                          <item.icon
                            className="main-navigation__icon"
                            size={20}
                            aria-hidden="true"
                          />
                        )
                      ) : (
                        <span
                          className="main-navigation__icon main-navigation__icon--spacer"
                          aria-hidden="true"
                        />
                      )}
                      <span className="main-navigation__label">
                        {item.label}
                      </span>
                    </NavLink>
                    {hasChildren ? (
                      <div
                        className="main-navigation__subitems"
                        role="group"
                        aria-label={`Subseções de ${item.label}`}
                      >
                        {item.children?.map((child) => (
                          <NavLink
                            key={child.to}
                            to={child.to}
                            end={child.end}
                            className={({ isActive }) =>
                              [
                                "main-navigation__link",
                                "main-navigation__sublink",
                                isActive ? "is-active" : "",
                              ]
                                .filter(Boolean)
                                .join(" ")
                            }
                            aria-label={child.label}
                            title={child.label}
                          >
                            {child.icon ? (
                              typeof child.icon === "string" ? (
                                <img
                                  src={child.icon}
                                  className="main-navigation__icon"
                                  alt=""
                                  loading="lazy"
                                />
                              ) : (
                                <child.icon
                                  className="main-navigation__icon"
                                  size={18}
                                  aria-hidden="true"
                                />
                              )
                            ) : (
                              <span
                                className="main-navigation__icon main-navigation__icon--spacer"
                                aria-hidden="true"
                              />
                            )}
                            <span className="main-navigation__label">
                              {child.label}
                            </span>
                          </NavLink>
                        ))}
                      </div>
                    ) : null}
                  </div>
                );
              })}
            </div>
          </div>
        ))}
      </nav>
      {shouldShowOfflinePanel ? (
        <div
          className="main-navigation__offline-modules"
          aria-label="Módulos fora do ar apontados pelo monitor"
        >
          <p className="main-navigation__section-title">Módulos fora do ar</p>
          {availabilityQuery.isLoading ? (
            <p className="main-navigation__offline-status">
              Consultando monitor...
            </p>
          ) : null}
          {availabilityQuery.isError ? (
            <p className="main-navigation__offline-status main-navigation__offline-status--error">
              Monitor indisponível
            </p>
          ) : null}
          {offlineModules.length > 0 ? (
            <ul className="main-navigation__offline-list">
              {offlineModules.map((module) => (
                <li
                  key={module.moduleCode}
                  className="main-navigation__offline-item"
                  title={module.lastError ?? module.name}
                >
                  <AlertTriangle
                    className="main-navigation__offline-icon"
                    size={14}
                    aria-hidden="true"
                  />
                  <span className="main-navigation__offline-name">
                    {module.name}
                  </span>
                </li>
              ))}
            </ul>
          ) : null}
        </div>
      ) : null}
    </aside>
  );
}

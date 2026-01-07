import { useState } from "react";
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
} from "lucide-react";
import experimentIcon from "../assets/icons/experiment-icon.svg";
import hypothesisIcon from "../assets/icons/hypothesis-icon.svg";
import nicheIcon from "../assets/icons/niche-icon.svg";
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
    ],
  },
  {
    title: "IA e Conteúdo",
    items: [
      { to: "/ai-services", label: "IA", icon: Bot },
      { to: "/openai-models", label: "Modelos OpenAI", icon: Cpu },
      { to: "/differentiated-technologies", label: "Tecnologias diferenciadas", icon: Cpu },
      { to: "/microservices", label: "Microserviços", icon: Server },
      { to: "/microservices/errors", label: "Erros de microserviço", icon: AlertTriangle },
      { to: "/chat-dialogs", label: "ChatGPT", icon: MessageSquare },
      { to: "/prompt-entities", label: "Objetos de Prompt", icon: Shapes },
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
        to: "/lead-portal/images",
        label: "Imagens recebidas",
        icon: Package,
      },
      {
        to: "/facebook-campaigns/ready",
        label: "Experimentos prontos",
        icon: ClipboardCheck,
      },
      { to: "/analytics", label: "Analytics", icon: BarChart3 },
    ],
  },
  {
    title: "Financeiro",
    items: [
      { to: "/payments", label: "Pagamentos", icon: CreditCard },
    ],
  },
  {
    title: "Jornadas",
    items: [
      {
        to: "/journeys",
        label: "Jornadas",
        icon: Map,
        children: [
          {
            to: "/journeys",
            label: "Visão geral",
            icon: Compass,
            end: true,
          },
          {
            to: "/journey-templates",
            label: "Templates",
            icon: Layers,
          },
          {
            to: "/journey-templates/new",
            label: "Novo template",
            icon: PlusCircle,
          },
        ],
      },
    ],
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
        to: "/settings/privacy-policy",
        label: "Política de privacidade",
        icon: ShieldCheck,
      },
    ],
  },
];

const MARKET_TEST_STEPS = [
  "1- Hipótese e Oferta Isca",
  "2- Funil Mínimo",
  "3- Tráfego e Segmentação",
  "4- KPIs e limiares de decisão",
  "5- Automação analítica",
];

export default function MainNavigation() {
  const [isPinned, setIsPinned] = useState(false);
  const [isHovered, setIsHovered] = useState(false);
  const isExpanded = isPinned || isHovered;

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
                        [
                          "main-navigation__link",
                          isActive ? "is-active" : "",
                        ]
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
                      <span className="main-navigation__label">{item.label}</span>
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
      <div className="main-navigation__market-test" aria-label="Etapas do teste de mercado">
        <p className="main-navigation__section-title">Teste de Mercado</p>
        <ol className="main-navigation__market-list">
          {MARKET_TEST_STEPS.map((step) => (
            <li key={step} className="main-navigation__market-step">
              {step}
            </li>
          ))}
        </ol>
      </div>
    </aside>
  );
}

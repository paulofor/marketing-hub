import { useMemo, useState } from "react";
import { Link, NavLink } from "react-router-dom";
import type { LucideIcon } from "lucide-react";
import {
  BarChart3,
  ClipboardCheck,
  BadgeCheck,
  Clapperboard,
  Flag,
  Package,
  PanelLeftClose,
  PanelLeftOpen,
  PlusCircle,
  ShieldCheck,
  Users,
  Workflow,
  AlertTriangle,
  FileText,
  Search,
  Mail,
  Microscope,
  HeartPulse,
  Image,
  Instagram,
  List,
  MessageSquare,
  Mic2,
  Send,
  Server,
  Video,
  Music2,
  Megaphone,
  Bot,
  CircleDollarSign,
  Tags,
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
    title: "Gestão",
    items: [
      {
        to: "/products",
        label: "Gestão de Produto",
        icon: Package,
      },
      {
        to: "/agents",
        label: "Gestão de Agentes",
        icon: Bot,
      },
      {
        to: "/product-types",
        label: "Tipos de produto",
        icon: Tags,
      },
    ],
  },
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
      {
        to: "/accounts/tiktok",
        label: "Contas TikTok Ads",
        icon: Music2,
      },
    ],
  },
  {
    title: "Produtos e Mercado",
    items: [
      {
        to: "/financial/video-providers",
        label: "Financeiro de vídeo",
        icon: CircleDollarSign,
      },
      {
        to: "/planning",
        label: "Planos comerciais",
        icon: ClipboardCheck,
        end: true,
      },
      {
        to: "/monthly-planning",
        label: "Planejamentos mensais",
        icon: BarChart3,
      },
      {
        to: "/opportunities",
        label: "Dossiê de oportunidades",
        icon: FileText,
      },
      {
        to: "/product-discovery",
        label: "Descoberta PDE",
        icon: Search,
      },
      { to: "/market-radar", label: "Radar de oportunidades", icon: Search },
      { to: "/niches", label: "Nichos", icon: nicheIcon },
      { to: "/hypotheses", label: "Hipóteses", icon: hypothesisIcon },
    ],
  },
  {
    title: "Experimentos",
    items: [
      {
        to: "/experiments",
        label: "Experimentos",
        icon: experimentIcon,
        children: [
          {
            to: "/experiments/manual/new",
            label: "Experimento manual",
            icon: PlusCircle,
          },
          {
            to: "/facebook-campaigns",
            label: "Experimentos para campanha",
            icon: Flag,
            end: true,
          },
        ],
      },
      { to: "/mois", label: "MOIS", icon: Workflow },
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
    ],
  },
  {
    title: "Campanhas",
    items: [
      { to: "/funnels", label: "Funil de Vendas", icon: Workflow },
      {
        to: "/lead-portal/temporary-checkout",
        label: "Checkout de teste",
        icon: ShieldCheck,
      },
      {
        to: "/videos",
        label: "Vídeos",
        icon: Video,
        children: [
          {
            to: "/videos/providers",
            label: "Provedores",
            icon: ShieldCheck,
          },
        ],
      },
      {
        to: "/audio-video-studio",
        label: "Estudio de Audio e Video",
        icon: Mic2,
        children: [
          {
            to: "/audio-video-studio/projects",
            label: "Lista de projetos",
            icon: List,
          },
          {
            to: "/audio-video-studio/videos-analysis",
            label: "Vídeos para análise",
            icon: Video,
          },
        ],
      },
      {
        to: "/creative-video-review",
        label: "Aprovar vídeos",
        icon: BadgeCheck,
      },
      {
        to: "/winning-ads-library",
        label: "Anúncios Vencedores",
        icon: Megaphone,
      },
      {
        to: "/pde-video-production",
        label: "Produção de Vídeo PDE",
        icon: Clapperboard,
      },
      {
        to: "/social-distribution",
        label: "Distribuição orgânica",
        icon: Send,
      },
      { to: "/ai/image-generator", label: "Gerador de Imagens", icon: Image },
      { to: "/pde-copy", label: "Copy PDE", icon: FileText },
      {
        to: "/facebook-campaigns/ready",
        label: "Experimentos prontos",
        icon: ClipboardCheck,
      },
      { to: "/analytics", label: "Analytics", icon: BarChart3 },
      { to: "/ops-monitor", label: "Operação / Saúde", icon: HeartPulse },
      { to: "/ops-monitor/pde", label: "Saúde PDE 24/7", icon: HeartPulse },
    ],
  },
  {
    title: "Configurações",
    items: [
      {
        to: "/agent-tasks",
        label: "Tarefas dos agentes",
        icon: List,
      },
      {
        to: "/agent-learning",
        label: "Aprendizado dos agentes",
        icon: BadgeCheck,
      },
      {
        to: "/system-improvements",
        label: "Melhorias do Sistema",
        icon: ClipboardCheck,
      },
      {
        to: "/business-process-chains",
        label: "Cadeias de valor",
        icon: Workflow,
      },
      {
        to: "/business-processes",
        label: "Processos",
        icon: List,
      },
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
        to: "/settings/fashion-chat-validation",
        label: "Validação Chat Moda",
        icon: ShieldCheck,
      },
      {
        to: "/settings/privacy-policy",
        label: "Política de privacidade",
        icon: ShieldCheck,
      },
      {
        to: "/microservices",
        label: "Microserviços",
        icon: Server,
        children: [
          {
            to: "/microservices/vps-inventory",
            label: "Inventário VPS",
            icon: Server,
          },
        ],
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
        <Link
          to="/"
          className="main-navigation__brand"
          aria-label="Ir para a página inicial do Marketing Hub"
        >
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
        </Link>
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

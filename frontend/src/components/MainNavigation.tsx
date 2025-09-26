import { Link } from "react-router-dom";
import {
  Sparkles,
  Layers,
  LayoutGrid,
  Users2,
  Share2,
  BookOpen,
  Box,
  Trophy,
  Milestone,
  Beaker,
  Brain,
  MessagesSquare,
  Workflow,
  Compass,
} from "lucide-react";
import "./MainNavigation.css";

const sections = [
  {
    title: "Operações essenciais",
    description: "Cadastre contas, produtos e ativos que sustentam as campanhas.",
    items: [
      {
        to: "/accounts/facebook",
        label: "Contas do Facebook",
        description: "Gerencie conexões e ativos da Meta.",
        icon: Users2,
      },
      {
        to: "/accounts/instagram",
        label: "Contas do Instagram",
        description: "Sincronize perfis e monitore publicações.",
        icon: Share2,
      },
      {
        to: "/media",
        label: "Mídia",
        description: "Biblioteca de criativos e vídeos aprovados.",
        icon: Layers,
      },
      {
        to: "/courses",
        label: "Cursos",
        description: "Organize trilhas e materiais educativos.",
        icon: BookOpen,
      },
      {
        to: "/products",
        label: "Produtos",
        description: "Gerencie ofertas principais em um só lugar.",
        icon: Box,
      },
      {
        to: "/success-products",
        label: "Produtos de Sucesso",
        description: "Colecione provas de produtos validados.",
        icon: Trophy,
      },
    ],
  },
  {
    title: "Pesquisa e experimentos",
    description: "Planeje hipóteses, conduza testes e acompanhe resultados.",
    items: [
      {
        to: "/niches",
        label: "Nichos",
        description: "Mapeie segmentos e oportunidades.",
        icon: Compass,
      },
      {
        to: "/experiments",
        label: "Testes de Nicho",
        description: "Execute experimentos com acompanhamento guiado.",
        icon: Beaker,
      },
      {
        to: "/hypotheses",
        label: "Hipóteses",
        description: "Valide aprendizados e itere rapidamente.",
        icon: Workflow,
      },
      {
        to: "/funnels",
        label: "Funil de Vendas",
        description: "Estruture jornadas e fluxos de conversão.",
        icon: Milestone,
      },
      {
        to: "/facebook-campaigns",
        label: "Experimentos para Campanha",
        description: "Organize variações e públicos para mídia paga.",
        icon: Sparkles,
      },
      {
        to: "/facebook-campaigns/ready",
        label: "Experimentos prontos",
        description: "Acesse execuções aprovadas e replicáveis.",
        icon: LayoutGrid,
      },
    ],
  },
  {
    title: "Conteúdo e criativos",
    description: "Inspire sua produção com ângulos, provas e gatilhos emocionais.",
    items: [
      {
        to: "/angles",
        label: "Angles",
        description: "Coleção de abordagens para headlines.",
        icon: Sparkles,
      },
      {
        to: "/visual-proofs",
        label: "Provas Visuais",
        description: "Destaque evidências que geram confiança.",
        icon: LayoutGrid,
      },
      {
        to: "/emotional-triggers",
        label: "Gatilhos Emocionais",
        description: "Active emoções certas em cada campanha.",
        icon: Brain,
      },
    ],
  },
  {
    title: "Inteligência Artificial",
    description: "Automatize tarefas, crie diálogos e personalize prompts.",
    items: [
      {
        to: "/ai-services",
        label: "IA",
        description: "Configure serviços e automações inteligentes.",
        icon: Sparkles,
      },
      {
        to: "/chat-dialogs",
        label: "ChatGPT",
        description: "Gerencie roteiros conversacionais assistidos.",
        icon: MessagesSquare,
      },
      {
        to: "/prompt-entities",
        label: "Objetos de Prompt",
        description: "Centralize entidades e atributos reutilizáveis.",
        icon: Brain,
      },
    ],
  },
];

const marketTestSteps = [
  "Hipótese e oferta isca",
  "Funil mínimo",
  "Tráfego e segmentação",
  "KPIs e limiares de decisão",
  "Automação analítica",
];

export default function MainNavigation() {
  return (
    <section className="main-navigation" aria-labelledby="main-navigation-heading">
      <div className="main-navigation__hero">
        <span className="main-navigation__eyebrow">Marketing Hub</span>
        <h1 id="main-navigation-heading" className="main-navigation__title">
          Escolha onde acelerar seus resultados
        </h1>
        <p className="main-navigation__subtitle">
          Conecte dados, experimente novas ideias e use a inteligência da plataforma para escalar campanhas com confiança.
        </p>
      </div>

      <div className="main-navigation__sections">
        {sections.map((section) => (
          <article key={section.title} className="main-navigation__card">
            <header className="main-navigation__card-header">
              <h2>{section.title}</h2>
              <p>{section.description}</p>
            </header>
            <nav aria-label={section.title} className="main-navigation__links">
              {section.items.map((item) => {
                const Icon = item.icon;
                return (
                  <Link key={item.to} to={item.to} className="main-navigation__link">
                    <span className="main-navigation__icon" aria-hidden="true">
                      <Icon size={18} />
                    </span>
                    <span className="main-navigation__link-text">
                      <span className="main-navigation__link-label">{item.label}</span>
                      <span className="main-navigation__link-description">{item.description}</span>
                    </span>
                  </Link>
                );
              })}
            </nav>
          </article>
        ))}
      </div>

      <aside className="main-navigation__market-test" aria-label="Roteiro de teste de mercado">
        <div className="main-navigation__market-card">
          <h2>Teste de Mercado guiado</h2>
          <p>
            Percorra as etapas essenciais para validar oportunidades de forma estruturada e com foco em decisões baseadas em dados.
          </p>
          <ol className="main-navigation__market-steps">
            {marketTestSteps.map((step, index) => (
              <li key={step}>
                <span className="main-navigation__step-index">{index + 1}</span>
                <span className="main-navigation__step-text">{step}</span>
              </li>
            ))}
          </ol>
        </div>
      </aside>
    </section>
  );
}

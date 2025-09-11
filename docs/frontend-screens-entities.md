# Mapeamento de Telas e Entidades

Este documento lista todas as telas do frontend do Marketing Hub e as entidades de dados consultadas e/ou gravadas em cada uma delas.

Consulte também o [Diagrama de Navegação do Frontend](./frontend-navigation.md) e o [Modelo de Dados](./data-model.md) para detalhes das entidades mencionadas.

| Rota | Tela | Entidades consultadas | Entidades gravadas/alteradas |
|------|------|----------------------|------------------------------|
| `/accounts/facebook` | Contas do Facebook | FacebookAccount | FacebookAccount |
| `/accounts/instagram` | Contas do Instagram | InstagramAccount | InstagramAccount |
| `/accounts/instagram/:id/posts` | Posts do Instagram | InstagramPost | InstagramPost |
| `/media` | Lista de Mídia | Asset | - |
| `/media/new` | Nova Mídia | - | Asset |
| `/media/:id` | Detalhe da Mídia | Asset | - |
| `/courses` | Lista de Planos de Curso | CoursePlan | - |
| `/courses/new` | Novo Plano de Curso | - | CoursePlan |
| `/courses/:id` | Detalhe do Plano de Curso | CoursePlan | - |
| `/products` | Lista de Produtos | Product | - |
| `/products/new` | Novo Produto | InstagramAccount | Product |
| `/success-products` | Produtos de Sucesso | SuccessProduct | - |
| `/success-products/new` | Novo Produto de Sucesso | - | SuccessProduct |
| `/success-products/:id` | Detalhe do Produto de Sucesso | SuccessProduct | - |
| `/success-products/:id/edit` | Editar Produto de Sucesso | SuccessProduct | SuccessProduct |
| `/niches` | Nichos de Mercado | MarketNiche, Hypothesis, Experiment | - |
| `/niches/new` | Novo Nicho | - | MarketNiche |
| `/niches/:nicheId` | Detalhe do Nicho | MarketNiche, Hypothesis | - |
| `/niches/:nicheId/edit` | Editar Nicho | MarketNiche | MarketNiche |
| `/niches/:nicheId/hypotheses/new` | Nova Hipótese | - | Hypothesis |
| `/niches/:nicheId/hypotheses/:hypothesisId` | Detalhe da Hipótese | Hypothesis | - |
| `/niches/:nicheId/hypotheses/:hypothesisId/edit` | Editar Hipótese | Hypothesis | Hypothesis |
| `/experiments` | Lista de Experimentos | Experiment | - |
| `/experiments/new` | Novo Experimento | - | Experiment |
| `/experiments/:id` | Detalhe do Experimento | Experiment, Creative, AdSet, MetricSnapshot, LandingPage | - |
| `/experiments/:id/edit` | Editar Experimento | Experiment | Experiment |
| `/hypotheses` | Lista de Hipóteses | Hypothesis | - |
| `/hypotheses/board` | Quadro de Hipóteses | Hypothesis | Hypothesis |
| `/ai-services` | Serviços de IA | AiService | - |
| `/ai-services/new` | Novo Serviço de IA | - | AiService |
| `/ai-services/:id/edit` | Editar Serviço de IA | AiService | AiService |
| `/angles` | Angles | Angle | Angle |
| `/visual-proofs` | Provas Visuais | VisualProof | VisualProof |
| `/emotional-triggers` | Gatilhos Emocionais | EmotionalTrigger | EmotionalTrigger |
| `/landing/:id` | Prévia da Landing Page | LandingPage | - |
| `/analytics` | Desempenho | MetricSnapshot | - |
| `/funnels` | Funis | Funnel | - |
| `/funnels/new` | Novo Funil | - | Funnel |
| `/funnels/:id/edit` | Editar Funil | Funnel | Funnel |
| `/chat-dialogs` | Diálogos ChatGPT | ChatDialog | ChatDialog |
| `/chat-dialogs/new` | Novo Diálogo | - | ChatDialog |
| `/prompt-entities` | Objetos de Prompt | PromptEntity, PromptEntityDescription | PromptEntityDescription |
| `/prompt-entities/new` | Nova Entidade de Prompt | - | PromptEntity |
| `/prompt-entities/:entityName/attributes` | Atributos da Entidade de Prompt | PromptAttribute | PromptAttribute |
| `*` | Início | - | - |


# AGENTS.md — Frontend

## 1. Mandato canônico
- O frontend do Marketing Hub implementa as superfícies administrativas do ecossistema e segue a precedência descrita no _System Governance Canon v2_ (`docs/canonical/system-governance-canon.v2.md`): os contratos de domínio e schemas publicados são a fonte primária, este arquivo apenas deriva diretrizes de execução.
- Interfaces não redefinem regra de negócio. Todo comportamento deve ser rastreado ao backend responsável ou ao cânone de domínio correspondente.
- Mudanças relevantes exigem alinhamento com o backend, teste automatizado sempre que possível e atualização dos contratos citados na seção "Fontes de verdade".

## 2. Fontes de verdade
| Tipo | Documento/artefato |
| --- | --- |
| Governança global | `docs/canonical/system-governance-canon.v2.md` |
| Modelo de dados do experimento | `docs/modelo-dados-experimento.md` |
| Backend MarketingHub | `apps/backend` (contratos REST/gRPC publicados) |
| Workers e integrações | `docs/facebook-ads-worker`, `docs/ai-worker`, `docs/email-service`, `docs/image-watermark-service`, `docs/image-zipper-service`, `docs/lead-portal-*` |

## 3. Módulos atendidos
| Módulo | Propósito observado pelo frontend |
| --- | --- |
| MarketingHub Frontend/Backend | Painéis do administrador e orquestração de regras. O frontend consome APIs oficiais e nunca aplica lógica exclusiva sem validação de domínio. |
| Worker AI | Solicitações para geração/apoio de conteúdo via OpenAI. Formular fluxos sempre com estados assíncronos e mensagens orientadas por contrato. |
| Facebook Ads Worker | Disparo de automações Meta Ads. O frontend apenas inicia fluxos e exibe estados retornados. |
| Lead Portal (backend/frontend) | Superfícies para leads; quando houver dependências cruzadas, priorizar contratos versionados. |
| lead-portal-payments-service | Status e callbacks de pagamento (Mercado Pago). Exibir projeções confirmadas pelo backend. |
| email-service | Feedback de envios via Amazon SES; tratar como fatos emitidos por worker. |
| image-watermark-service & image-zipper-service | Disponibilização de assets tratados; o frontend baixa/exibe apenas URLs e estados emitidos pelos serviços. |

## 4. Padrões mínimos de UI
- A aba de Criativos (`src/pages/experiment/CriativosTab.tsx` + `.css`) é o kit canônico de layout responsivo (grade `creative-grid`, cards `creative-card`, toolbar `creative-toolbar`). Reutilize os componentes antes de criar novos estilos.
- Todo fluxo assíncrono mantém os três estados obrigatórios: carregando (`spinner-border` ou skeleton), sucesso/erro com `creative-feedback` e estado vazio equivalente a `creative-empty-state`.
- Toolbars/filtros repetem o padrão de fundo `var(--bs-tertiary-bg)`, borda de 1px e cantos de 1rem. Centralize ações principais com ícones Lucide 16–18px e tokens `var(--bs-*)`.
- Formulários que acionam IA ou integrações externas precisam de tooltip explicativo e do bloco de log de validação `handleSubmit(..., (errors) => console.log(...))` para rastreabilidade.

## 5. Operações e integrações
- Chame workers (AI, Facebook Ads, e-mail, imagens) apenas via backend principal. O frontend não deve abrir canais diretos entre containers.
- Cada chamada ao backend explicita o contrato usado e registra o requestId no log/debugger para rastrear fatos emitidos pelos workers.
- Estados exibidos ao usuário sempre destacam a origem (ex.: "Status confirmado pelo Worker AI"), reforçando que flags derivadas não substituem a verdade primária.

## 6. Checklist rápido antes de abrir PR
1. **Fonte** — apontou para o contrato ou cânone que suporta a regra?
2. **UI kit** — reutilizou `creative-grid`, `creative-card`, `creative-toolbar` ou extraiu utilitário compartilhado equivalente?
3. **Estados** — forneceu carregando/vazio/feedback e tratou erros do backend com mensagens claras?
4. **Integração** — evitou chamar serviços externos diretamente e validou o request com o domínio responsável?
5. **Documentação** — atualizou o contrato relevante (ou abriu issue/ADR) quando a mudança afetar regras de negócio?

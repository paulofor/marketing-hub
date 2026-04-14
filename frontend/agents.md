# AGENTS.md — Frontend

## Consistência visual global
- Trate a aba de criativos como referência canônica de acabamento visual. Sempre que criar ou revisar telas, reutilize (ou extraia para um utilitário compartilhado) padrões como a grade responsiva (`display: grid` com `gap: 1.5rem` e `minmax(260px, 1fr)`), cartões com cantos de 1rem, sombras brandas e tokens `var(--bs-*)` que garantem contraste e acessibilidade.
- Listagens e dashboards não devem voltar a exibir blocos de conteúdo em tabelas cruas sem ícones ou hierarquia visual. Prefira cards responsivos com badges de status (`text-bg-*`), metadados resumidos e ações em botões icônicos. Quando precisar de uma tabela por densidade, mantenha uma barra de ações com o mesmo nível de hierarquia, ícones Lucide e estados de carregamento/vazio equivalentes.
- Barras superiores (toolbars, filtros, cabeçalhos de página) devem seguir a composição do `creative-toolbar`: layout flexível com espaçamentos consistentes, fundos suaves (`var(--bs-tertiary-bg)`), borda de 1px e cantos arredondados. Se uma tela ainda não possuir essa estrutura, crie-a antes de adicionar novos botões ou contadores.
- Toda interação assíncrona deve contemplar três estados: `spinner-border` centralizado durante carregamento, blocos de esqueleto ou placeholders quando aplicável e um estado vazio com contorno tracejado, ícone decorativo e instruções objetivas (equivalente ao `creative-empty-state`).
- Botões principais e ações contextuais precisam combinar rótulos e ícones Lucide de 16–18px, alinhados como na aba de criativos. Mantenha ícones como `Sparkles` para automações da IA e `Plus` para criação de itens, evitando trocar por variantes genéricas.
- Consolidar novos estilos globais em utilitários reutilizáveis (`src/components` ou CSS dedicado) é obrigatório para evitar divergências. Não espalhe regras inline ou duplicadas; derive tokens diretamente dos que já existem em `src/pages/experiment/CriativosTab.css`.

## Formulários
- Toda tela de edição/inserção de dados por formulário deve manter o log de erros de validação usando o padrão:
  ```tsx
  onClick={handleSubmit(onSubmit, (errors) => {
    console.log("Validation errors", errors);
  })}
  ```
- Todo campo que aciona serviços do Worker IA deve possuir um tooltip explicando seu funcionamento.

## Experimentos › Aba de Criativos (`src/pages/experiment/CriativosTab.tsx`)
- Preserve o layout de galeria responsiva implementado com a grade `creative-grid` e os cards `creative-card`. Nunca volte a apresentar os criativos em listagens tabulares simples.
- Cada card precisa exibir as badges de status (`statusVariant`) e de formato, headline, `primaryText` e os metadados de CTA/URL (`creative-card-meta`). Quando não houver imagem, mantenha o placeholder `creative-card-placeholder`.
- A barra superior `creative-toolbar` deve continuar mostrando os totais com badges e os botões de ação. O botão "Gerar criativos" precisa alternar entre ícone `Sparkles` e spinner durante `requestCreatives.isPending`, e o botão "Novo Criativo" deve permanecer com o ícone `Plus`.
- Garanta sempre os estados visuais de carregamento com spinner centralizado e o estado vazio `creative-empty-state` com ícone decorativo e instruções curtas.
- As ações dos cards (Editar, Duplicar, Excluir, Aprovar, Preview) precisam permanecer como botões com ícones Lucide (`Edit3`, `Copy`, `Trash2`, `CheckCircle2`, `Eye`) e layout responsivo definido por `creative-card-actions`.
- Ao ajustar estilos em `CriativosTab.css`, mantenha os mesmos espaçamentos, sombras, breakpoints e tokens de cor para assegurar hierarquia visual, responsividade e acessibilidade do grid de criativos.
- É proibido retornar aos `alert()` do navegador para feedbacks. Reutilize o banner `creative-feedback` com variantes `creative-feedback-success|warning|error`, ícones `CheckCircle2`/`AlertTriangle`/`XCircle`, botão `creative-feedback-close` e autoexpiração de 8s (via `setTimeout`). Toda ação de upload, solicitação ou operação relevante deve acionar esse padrão com título e descrição consistentes.
- O fluxo "Gerar criativos" deve permanecer encapsulado no modal `creative-request-modal`: valide `requestQuantity` antes de chamar `requestCreatives`, surface erros com `creative-request-error`, mantenha estados de carregamento usando `requestCreatives.isPending` (inclusive nos botões da toolbar) e feche o modal ao concluir com sucesso.
- Estilos relacionados a feedbacks e ao modal de solicitação precisam viver em `CriativosTab.css` (ou em utilitário compartilhado dedicado) respeitando bordas arredondadas de 1rem, sombras suaves e a paleta/gradientes já definidos. Não introduza regras inline ou classes avulsas fora desse padrão.

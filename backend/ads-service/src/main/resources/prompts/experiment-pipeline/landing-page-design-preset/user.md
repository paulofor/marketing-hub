Gerar exclusivamente o artefato canônico `landingPageDesignPreset`, seguindo o schema JSON da etapa.

Regras mandatórias da Sprint 1:
- Defina primitives mínimas em `componentPresets.primitives`: `hero-title`, `section-title`, `body`, `btn-primary`, `btn-secondary`, `field`, `card`, `faq-item`.
- Defina `componentPresets.registry` com mapeamento explícito `componentKey -> templatePartial` para: `hero-form-split`, `proof`, `offer-cards`, `faq`.
- Não usar fallback silencioso: qualquer decisão de fallback deve ser descrita em `consistencyChecks[*].details`.
- Preencher tokens obrigatórios em `theme`: tipografia, spacing, radius, shadow, focus-ring e cores de superfície/contraste.
- Garantir legibilidade e toque mínimo de CTA/form com foco visual perceptível.
- Incorporar no preset os padrões da seção "Padrões de CSS e componentes por elemento" de `docs/pesquisa-profunda/pesquisa-profunda-html-estilos.md`, cobrindo explicitamente os elementos: `<p>`, `<h1>`, `<h2>`, `<h3>`, `<ul>/<li>`, `<button>`/CTA, `<form>`, `<label>`, `<input>`, `<img>`.
- Para cada elemento acima, declarar no preset os atributos visuais relevantes (tipografia, espaçamento, dimensão, contraste, foco, superfície, etc.) e os tokens correspondentes.
- Distribuir no preset (de forma natural e não mecânica) diretrizes de acabamento premium para **todas** as superfícies e variações (`.lhm-surface-band`, `.lhm-surface-solid`, `.lhm-surface-gradient-soft`, `.lhm-surface-image-tint`), combinando ao menos: borda sutil, raio coerente, sombra de profundidade, respiro interno responsivo, controle de overflow e estados visuais consistentes com os tokens de `theme.radius`, `theme.shadow`, `theme.spacing` e `theme.palette`.
- No `lhmRuntime.baseCss`, preservar rigorosamente a ordem das declarações CSS e das camadas (base → componentes → utilitários → overrides, e superfícies/background antes de conteúdo/efeitos), evitando sobrescritas destrutivas e preservando o acabamento premium das camadas mais elaboradas.
- Separar responsabilidades em `lhmRuntime.baseCss`: `.lhm-card` estrutural consumindo variáveis (`--lhm-card-bg`, `--lhm-card-border`, `--lhm-card-text`, `--lhm-card-shadow`), `.lhm-surface-*` configurando essas variáveis, e contraste real entre `.lhm-high`/`.lhm-normal`/`.lhm-soft` (proibido classes equivalentes).
- Garantir base comum `.lhm-surface` aplicada às variações para manter `position`, `overflow` e consistência visual sem conflitos de cascata.
- A etapa deve exigir resposta estritamente aderente ao schema JSON canônico de `landingPageDesignPreset`, sem campos fora do contrato.

Saída:
- Apenas JSON válido aderente ao schema da etapa.

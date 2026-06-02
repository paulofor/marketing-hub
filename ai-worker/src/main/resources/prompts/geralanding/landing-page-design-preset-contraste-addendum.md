# Addendum — Contraste visual por seção

Use este addendum junto com `landing-page-design-preset.md` quando o objetivo for reduzir monotonia visual na landing.

## Regra central

O preset design deve criar contraste visual real entre as seções. O assembler não deve inventar estilos; ele apenas aplica as classes geradas no JSON.

## Mapa obrigatório de contraste entre seções

Antes de montar o JSON final, atribua uma função visual para cada seção usando `id`, `nome`, `papelComercial`, `fasePersuasao`, `objetivo` e `prioridadeConversao` do wireframe.

Cada função visual deve gerar uma combinação diferente de classes:

- superfície/background;
- grid/container;
- card/frame;
- espaçamento;
- tratamento de imagem;
- tratamento de CTA.

É proibido deixar todas as seções com a mesma aparência de fundo escuro + card azul + botão verde.

Use pelo menos cinco tratamentos visuais diferentes quando a página tiver seções suficientes:

1. `hero`: impacto, promessa principal, visual forte, CTA evidente.
2. `dor/problema/antes-depois`: tensão e identificação, com contraste emocional claro entre antes e depois.
3. `preview/prova/entregáveis`: demonstração tangível do produto, com moldura visual mais forte.
4. `como-funciona/mecanismo`: clareza, processo, passos numerados ou cards organizados e leves.
5. `formulario/captura`: bloco de conversão destacado, com maior percepção de segurança e ação.
6. `faq/objeções`: bloco mais calmo, legível e organizado, sem competir visualmente com o formulário.

Se houver duas seções consecutivas com o mesmo tipo de superfície, diferencie pelo menos dois aspectos: padding, card, borda, imagem, grid, CTA, sombra ou acento.

## Regras obrigatórias de desktop/mobile

Todo container principal deve ter classes mobile e desktop quando aplicável:

- classe de largura mobile;
- classe de largura desktop ou wide;
- classe de centralização;
- classe de padding mobile;
- classe de padding desktop.

Todo grid principal deve ter classes mobile e desktop quando aplicável:

- mobile: uma coluna;
- desktop: duas ou três colunas quando houver conteúdo visual/cards;
- gap mobile;
- gap desktop;
- alinhamento vertical quando necessário.

Todo CTA deve ter classes para:

- `display: inline-flex` ou `display: flex`;
- `align-items: center`;
- `justify-content: center`;
- padding mobile e desktop;
- radius;
- fonte/weight;
- cor de texto;
- background;
- cursor;
- `text-decoration: none`;
- largura full no mobile quando for CTA primário de conversão.

Toda imagem de produto/prova deve ter classes para:

- `width: 100%`;
- `max-width: 100%`;
- `height: auto` ou altura máxima controlada;
- `object-fit`;
- `object-position`;
- radius;
- border/sombra/moldura quando for mockup.

## Quality gate adicional

Rejeite mentalmente e regenere o JSON se:

- o Hero, a Prova e o Formulário usarem o mesmo tratamento de superfície/card;
- o Formulário não parecer o bloco mais acionável da página;
- a seção de Prova/Preview não parecer uma vitrine do produto;
- houver seções consecutivas sem contraste visual claro;
- a página parecer uma sequência de cards iguais.

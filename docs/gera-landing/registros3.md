# Registros — Gera Landing

> Orientação: todos os registros deste documento devem sempre incluir **data e hora no fuso UTC-3**.
> Neste documento segue política de **append-only** (não pode ter nenhuma linha apagada; apenas inserções).

- 2026-05-12 09:25:00 UTC-3 — Atualizado o contrato da etapa landing-page-wireframe para formato JSON simplificado com raiz `pagina` (head/corpo/secoes/elementos), incluindo novo schema e regras de prompt alinhadas ao novo payload.

- 2026-05-12 09:40:00 UTC-3 — Ajustado o contrato para explicitar que `elementosInternos` em `elementosSeccao` é recursivo (filhos, netos e níveis seguintes com o mesmo schema), alinhando regra de composição de seção.
- 2026-05-12 10:05:00 UTC-3 — Corrigido o schema da etapa landing-page-wireframe para compatibilidade com OpenAI Structured Outputs: a recursão de `elementosInternos` passou a referenciar `#/$defs/elementoSecao` definido no topo do schema, evitando erro 400 `invalid_json_schema` por referência fora de definições top-level.

- 2026-05-12 10:20:00 UTC-3 — Restringido de forma rígida o campo `nome` de cada item de `estilos` no schema da etapa `landing-page-wireframe` para aceitar somente whitelist de atributos CSS estruturais permitidos (layout/posicionamento/flex/grid/transform), bloqueando nomes fora da lista.

- 2026-05-12 14:40:00 UTC-3 — Atualizado o montador de HTML provisório da etapa landing-page-wireframe para suportar o novo payload com raiz `pagina`/`corpo`/`secoes`/`elementosSeccao`, renderização recursiva de elementos e fallback em Lorem Ipsum quando `texto.conteudo` vier vazio.

- 2026-05-12 15:05:00 UTC-3 — Atualizada a etapa Gera Wireframe para reforçar objetivo comercial de venda com captura de dados para envio de amostra/prova, e bloqueado preenchimento de copy nesta fase (`texto.conteudo` obrigatório como string vazia no prompt e no schema).

- 2026-05-12 18:05:00 UTC-3 — Ajustado o montador HTML do wireframe para alternar fundo de seção em duas cores (claro/escuro) no payload `pagina/corpo/secoes` e para representar imagens (`img`) com placeholder visual exibindo dimensões definidas no wireframe.

- 2026-05-12 19:15:00 UTC-3 — Ajustadas as solicitações da etapa `landing-page-wireframe` para exigir, em cada seção, metadados de intenção comercial no padrão do esboço (`papelComercial`, `fasePersuasao`, `objeçãoQueRemove`, `prioridadeConversao`, `acaoEsperada`, `fonteContexto`), com atualização sincronizada do prompt e do schema.

- 2026-05-12 20:39:44 UTC-3 — Atualizada a criação da etapa `landing-page-wireframe` para exigir formulário com apenas `nome` e `email`, CTA no hero com âncora direta para o formulário, mais duas âncoras internas estratégicas e inserção obrigatória de imagens para alternância entre texto e imagem.

- 2026-05-12 20:42:21 UTC-3 — Inserida no prompt da etapa `landing-page-wireframe` uma heurística prática de composição para landing mobile (quantidades sugeridas para bullets, entregáveis, antes/depois, passos, FAQ e campos de formulário), explicitamente tratada como inspiração flexível e não regra fixa.

- 2026-05-12 20:44:18 UTC-3 — Adicionada no prompt da etapa `landing-page-wireframe` uma fonte extra de inspiração para uso de listas longas no mobile, definindo quando evitar e quando aceitar listas extensas (FAQ recolhido, cards, hierarquia clara, após entendimento da oferta e prova de entrega).

- 2026-05-12 20:46:17 UTC-3 — Ajustado o prompt da etapa `landing-page-wireframe` para exigir ao menos uma imagem que comunique visualmente a ideia do produto/entrega comprada pelo cliente (mockup, amostra de conteúdo, kit ou resultado esperado).

- 2026-05-12 20:48:15 UTC-3 — Incluída no prompt da etapa `landing-page-wireframe` a orientação para detalhar cada visual (`img`) com contexto de uso (onde entra), tipo esperado, função comercial, objeção removida e classificação do formato (mockup, foto, ilustração, diagrama ou print conceitual).

- 2026-05-12 21:05:00 UTC-3 — Definido no schema da etapa `landing-page-wireframe` o contrato obrigatório de `briefingVisual` para elementos com `tag="img"` (onde entra, tipo esperado, função comercial, objeção removida e classificação), mantendo a regra como exigência estrutural validável além da instrução de prompt.

- 2026-05-12 21:22:00 UTC-3 — Refinado o contrato do `landing-page-wireframe` para deixar explícito que `briefingVisual` é campo exclusivo de elementos `img`: obrigatório quando `tag="img"` e proibido para outras tags (validação `if/then/else` no schema + reforço no prompt).

- 2026-05-13 09:10:00 UTC-3 — Ajustado o schema da etapa `landing-page-wireframe` para compatibilidade com OpenAI Structured Outputs removendo o uso de `allOf` (com `if/then/else`), que é rejeitado pela API com erro `invalid_json_schema`; a regra de uso exclusivo de `briefingVisual` para `img` permanece reforçada no prompt.

- 2026-05-13 09:32:00 UTC-3 — Reforçada a regra contratual no schema `landing-page-wireframe`: o campo `tag` agora explicita literalmente que `briefingVisual` só é permitido quando `tag = "img"`, e o próprio `briefingVisual` passou a repetir a instrução de omissão para demais tags.

- 2026-05-13 09:55:00 UTC-3 — Corrigido o schema da etapa `landing-page-wireframe` para compatibilidade rígida com OpenAI Structured Outputs exigindo `briefingVisual` no `required` de `elementoSecao`; para preservar a regra de uso exclusivo em imagens, o campo passou a aceitar `null` e o prompt/contrato foram alinhados para usar `briefingVisual: null` quando `tag` for diferente de `img`.

- 2026-05-13 — Ajustada a fase `geralanding` do `landingPageCopy` para saída por seção com `items` contendo apenas `id` e `texto`, mantendo os mesmos ids do wireframe (sem aliases). Arquivos: `landing-page-copy.md` e `landing-page-copy-schema.json`.
- 2026-05-12 22:50:00 UTC-3 — Ajustado o gerador de HTML provisório da etapa `landing-page-copy` para suportar o novo wireframe com raiz `pagina` quando `sectionOrder` não estiver presente, reutilizando `WireframeHtmlGenerator` para montar o HTML base antes de aplicar os textos por id; mantida compatibilidade com o formato legado.

- 2026-05-12 23:10:00 UTC-3 — Ajustado o merge de HTML provisório da etapa `landing-page-copy` para os JSONs novos do experimento 20: o processador passou a ler `bodySections[].items[].texto` na extração da copy por id e a aplicar conteúdo em `input`/`textarea` via `placeholder`, garantindo composição correta entre wireframe e copy no preview.

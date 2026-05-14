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

- 2026-05-13 00:25:00 UTC-3 — Registrada correção no gerador de HTML provisório da etapa `landing-page-copy` (pacote `geralanding`): aplicação de copy por `id` com resolução resiliente (match exato + fallback por id normalizado com remoção de espaços, normalização de traços unicode e case-insensitive) para evitar perda de texto em elementos `<li>` e demais tags quando houver variação de formatação entre wireframe e copy.
- 2026-05-13 12:20:00 UTC-3 — Corrigida a causa-raiz de desalinhamento na etapa `landing-page-copy` quando o HTML base contém elementos duplicados com o mesmo `id` (caso observado em `el-s8-li1/2/3`): após aplicar os textos por id, o processador agora remove duplicatas vazias preservando o primeiro elemento preenchido, evitando repetição de `<li>` em branco no `provisional_html`; adicionado teste unitário cobrindo o cenário.
- 2026-05-13 12:45:00 UTC-3 — Revisão da etapa `landing-page-copy` no `CopyProvisionalHtmlProcessor`: removido o eliminador de duplicatas por `id` no HTML final e ajustado o fluxo para explicitar a ordem canônica de processamento (1) montar primeiro o HTML base a partir do JSON do Gera Wireframe e (2) só depois aplicar a copy por `id` sobre esse HTML.

- 2026-05-13 10:30:00 UTC-3 — Registro operacional (geralanding): testes provisórios de wireframe removidos temporariamente no módulo ads-service para permitir substituição por nova suíte aderente ao contrato canônico atual de geração/montagem HTML.
- 2026-05-13 12:40:00 UTC-3 — Criada a nova etapa canônica `landing-page-image-planning` no fluxo Gera Landing seguindo o mesmo modelo de Wireframe/Copy: novo endpoint de start no backend (`/image-prompts/start`), suporte de processamento no worker com prompt/schema dedicados em `prompts/geralanding`, e persistência do resultado no artefato de experimento `landingPageImagePlanning` para posterior processamento de imagens pelo Worker AI.
- 2026-05-13 18:05:00 UTC — Registrada evolução da etapa pós-`landing-page-image-planning` no pipeline de experimento: o card do frontend foi reposicionado como ação explícita de geração em lote (`Gerar imagens em lote (AI Worker)`), com indicador de pendências e disparo batch para todos os prompts planejados; no AI Worker, reforçada a observabilidade da integração OpenAI (logs de upload/criação/finalização/polling do batch) e adicionado resumo de custo estimado por lote via `openai.image-cost-per-image-usd`.

- 2026-05-13 15:40:00 UTC — Adicionado no frontend (aba Gera Landing, tela de detalhe do experimento) o novo card **Gera Imagem** separado de **Gera Prompt Imagem**; o card dispara a geração real em lote via endpoint já existente de framework images, exibe contador de pendências e direciona o usuário para o painel operacional detalhado da aba Conteúdo.

- 2026-05-13 18:35:00 UTC — Registrada a entrega da navegação de detalhe de imagens no fluxo Gera Landing: no card **Gera Imagem** foi incluído o atalho para uma tela dedicada (`/experiments/:id/framework-images`) com listagem por item exibindo prompt/request enviado ao Worker AI, modelo retornado, status de processamento e pré-visualização da imagem gerada (web/source URL).

- 2026-05-14 00:00:00 UTC — Implementado monitoramento automático no backend do Gera Landing para, ao concluir com sucesso a etapa `landing-page-image-planning`, regenerar e persistir `provisional_html` no próprio detalhe do prompt de imagem combinando dados já consolidados de wireframe+copy; quando os artefatos-base ainda não estão disponíveis, o sistema mantém fallback seguro sem quebra de execução.
- 2026-05-14 00:40:00 UTC — Ajuste complementar no monitoramento da etapa `landing-page-image-planning`: o HTML provisório agora passa por injeção automática de URLs de imagens geradas (via mapeamento de jobs de framework image) antes de ser salvo, e o mesmo HTML é persistido também no campo `experiments.landing_page_html` para consumo direto pelas telas e próximos estágios.
- 2026-05-14 00:00:00 UTC — Ajustada a tela de detalhe do experimento na aba Gera Landing (`ExperimentDetailPage`) para consolidar **Gera Imagem** e **Gera Prompt Imagem** no mesmo card, posicionando o bloco de prompt acima do histórico; também foi adicionado o botão **Gerar HTML**, habilitado apenas quando não há imagens pendentes, acionando o endpoint de geração `landing-page-html/generate-with-lhm`.

- 2026-05-14 23:40:00 UTC — Ajustado o fluxo de "Gerar HTML" da fase Gera Imagem para fallback provisório quando `landingPageDesignPreset` ainda não existe: o backend agora monta o HTML base via gerador da etapa `landing-page-copy` (wireframe + copy) e injeta as URLs de imagens já geradas nos slots, sem bloquear com erro de preset ausente.

# Radar IA para vídeo — 2026-09-13

## Resumo executivo

Duas mudanças passaram o filtro desta rodada.

1. **Black Forest Labs FLUX Video Edit** — lançado em 10/09, já está ativo por API e foca edição seletiva de vídeo existente: altera somente o que o prompt pede e preserva duração, câmera, timing e áudio não editado. O preço de US$ 0,03/s torna a abordagem especialmente interessante para derivar variantes de um master aprovado.
2. **Reuters + CuttingRoom via MCP** — anunciada em 12/09, a integração liga o servidor MCP da Reuters ao editor ShortCut para localizar mídia, montar, mixar, corrigir cor, legendar e reenquadrar em linguagem natural seguindo regras editoriais do cliente. O movimento reforça a entrada do MCP em pós-produção profissional, também vista no DaVinci Resolve 21.1.

## 1. FLUX Video Edit

**Status: ATIVO / API pública / proprietário.**

A Black Forest Labs lançou o FLUX Video Edit como endpoint separado do FLUX 3 Video. Ele recebe um MP4 e um prompt e devolve o mesmo clipe com alterações seletivas. A documentação oficial lista remoção, adição e substituição de objetos e personagens, reconstrução de cenário, restyle, mudança de cores e materiais, reescrita de texto em tela, mudança ou tradução de diálogo com lip sync e alteração de ações.

A principal diferença arquitetural é a preservação do master: duração, enquadramento, movimento de câmera, cortes, timing e áudio são mantidos quando o prompt não pede mudança nesses elementos. Isso o aproxima de uma ferramenta de pós-produção gerativa, e não de um gerador que refaz toda a tomada.

### Limites e custo atuais

- entrada: MP4 por URL HTTP(S) ou base64;
- duração máxima: 15 s;
- tamanho máximo: 50 MiB;
- saída: mesma duração e proporção, 24 fps, até 720p;
- velocidade declarada: ~50 s para editar um clipe de 10 s;
- preço: **US$ 0,03/s** de vídeo de saída;
- exemplo: 10 s = **US$ 0,30**;
- áudio de origem é preservado, exceto quando o prompt pede mudança de diálogo ou som;
- uso da API inclui direitos comerciais sem licença separada;
- não há pesos públicos anunciados para o Video Edit.

A BFL afirma liderança de velocidade, custo e precisão em benchmarks internos; isso deve ser tratado como alegação do fornecedor, não como benchmark independente.

### Comparação operacional

**Runway Aleph 2.0 — ATIVO.** No Edit Studio e na API, aceita vídeo de até 30 s e custa 28 créditos/s; como cada crédito do Runway Dev custa US$ 0,01, isso equivale a **US$ 0,28/s**, com mínimo de 56 créditos. O Runway oferece workflow profissional mais amplo e formatos ProRes/HDR, mas o custo de edição é muito superior ao FLUX Video Edit.

**Gemini Omni 1.1 Flash — ATIVO / stable API.** O Google posiciona o modelo como geração e edição conversacional de vídeo. Ele aceita vídeo de até 10 s para edição/extensão e pode produzir 3–10 s em 360p, 720p, 1080p ou 4K. É mais forte em refinamento multi-turn e contexto multimodal; o FLUX Video Edit, por sua vez, está otimizado para editar um master preservando o restante do clipe.

### Por que importa

Para publicidade e social, a implicação é prática: um vídeo aprovado pode virar variantes de produto, embalagem, texto, cenário ou idioma sem refazer a performance e a montagem inteira. Isso favorece experimentação de criativos com maior controle das variáveis.

## 2. Reuters + CuttingRoom: edição agêntica por MCP

**Status: ATIVO / integração empresarial limitada aos clientes e contas participantes.**

Em 12/09, a Reuters anunciou integração direta de seu servidor MCP com o ShortCut, assistente de edição da CuttingRoom. O editor pode pedir material por história, tópico, região, idioma ou evento e o sistema pode, em uma única conversa:

- localizar e importar conteúdo Reuters;
- cortar;
- mixar áudio;
- corrigir cor;
- criar legendas;
- aplicar tratamentos gráficos;
- reenquadrar para vertical, quadrado e formatos de boletim.

A Reuters destaca que cada cliente controla sua integração, escreve as regras editoriais em linguagem natural e mantém o material em sua própria infraestrutura.

### Comparação com DaVinci Resolve 21.1

**DaVinci Resolve 21.1 — ATIVO.** A Blackmagic lançou em 08/09 integração com assistentes como Claude, Claude Code e ChatGPT Codex. O Resolve Studio adicionou um servidor MCP nativo para permitir análise de projetos, organização de mídia, mudança de configurações, highlight edits, remoção de clipes e batch rendering.

**Adobe Premiere 26.5 — ATIVO.** A Adobe avançou na direção complementar: geração contextual dentro da própria timeline, com modelos generativos e assets editáveis. Premiere hoje é forte em geração dentro do NLE; Resolve e Reuters/CuttingRoom mostram o editor se tornando uma superfície de ferramentas para um agente externo.

### Por que importa

A tendência mais importante não é um LLM 'editar sozinho', mas a combinação:

`estado do projeto + ferramentas nativas + regras explícitas + agente + gate de revisão`.

Para o Marketing Hub isso se encaixa diretamente na arquitetura do harness: Apolo pode produzir e montar, enquanto regras de marca, claims, formatos e elementos imutáveis ficam fora do prompt criativo e entram como política de execução.

## Cards criados

Foram criados dois candidatos a DRAFT:

1. `video-master-variantes-edicao-localizada` — reutilizar um master aprovado para produzir variantes controladas por edição localizada.
2. `video-agente-edicao-regras-explicitas-mcp` — operar pós-produção por agente usando ferramentas reais do editor, política explícita e gates.

## Fontes principais

- Black Forest Labs — FLUX Video Edit: https://bfl.ai/video-edit
- Black Forest Labs — commercial licensing via API: https://help.bfl.ai/articles/9272590838-self-serve-dev-license-overview-pricing
- Runway Dev pricing: https://docs.dev.runwayml.com/guides/pricing/
- Google Gemini Omni Flash: https://ai.google.dev/gemini-api/docs/models/gemini-omni-flash
- Reuters + CuttingRoom MCP: https://www.reuters.com/media-center/reuters-cuttingroom-partner-provide-newsrooms-with-ai-assisted-video-editing-2026-09-12/
- Blackmagic Design — DaVinci Resolve 21.1: https://www.blackmagicdesign.com/media/release/20260908-03

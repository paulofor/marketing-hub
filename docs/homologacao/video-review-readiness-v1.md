# Fila de revisão de vídeos — homologação local v1

Data: 04/10/2026. Escopo: alerta administrativo, fila de vídeos e comandos de revisão.

## Evidência e escolha

A API e o MCP `db_query` no banco `marketinghubdb` retornaram oito DRAFT:
três de experimentos INVALIDATED (#528, #537 e vídeo #50), um ancestral substituído
(#534 → #535 aprovado), dois anúncios impedidos por copy/Têmis (#532 e #533) e
as candidatas novas #51 (Capella/#94) e #52 (Vega/#95). Nenhum item era de
Alcyone/#11/experimento #97. O contrato publicado de Vega v8 contém vídeo #42
APPROVED; o de Mira v1 contém #49 APPROVED. Campanhas #92 e #93 constavam PAUSED.
Não há evidência de uso comercial das candidatas #51/#52. A consulta de logs do
backend via MCP respondeu HTTP 206 e zero linhas para `creative`; não substitui os
estados persistidos nem prova erro de gravação. Nenhum registro produtivo foi alterado.

Alternativas: aprovar os oito rascunhos seria rápido, mas burlaria pareceres e histórico;
ocultar o banner teria baixo esforço, mas manteria a fila errada; classificar no backend
e respeitar o escopo exige testes de contrato, preserva controles e foi a opção escolhida.

## Matriz definida antes da execução

| Caso | Critério de aceite | Validação |
| --- | --- | --- |
| Nova peça válida | Aparece em Sua revisão; aprovação preserva experimento e mídia | Integração HTTP/H2 |
| Copy inválida / Têmis pendente, ADJUST ou FAILED | Contagem humana exclui; motivo explícito; comando não aprova | Unidade + integração |
| Experimento encerrado | Histórico preservado; nenhuma aprovação ou reanálise paga pelo comando | Integração HTTP/H2 |
| Ancestral com descendente aprovado | Histórico sem reaprová-lo; seguir vários níveis e evitar ciclos | Unidade + integração |
| Descendente ainda não aprovado / outro experimento | Não fabricar substituição | Unidade |
| Produto/experimento diferentes e sem produto | Filtros oficiais não misturam identidades; consulta geral preserva desvinculados | Integração HTTP/H2 |
| Mídia aprovada/reprovada | Decisões e timestamps anteriores preservados pela leitura | Integração |
| Áudio/origem visual inválidos | Lista e comando obedecem ao mesmo gate | Regressões existentes + integração |
| Falha de consulta e recarregamento | Não mostrar zero como certeza; atualização desabilitada durante request | UI |
| Desktop, iPhone 15 Pro e Pixel 7 | Sem alerta alheio em Alcyone; fila geral e filtros corretos; controles acessíveis | Playwright/Chromium |
| Caminho integrado | Backend local/H2 entrega contrato real ao build local; mutações só em fixtures | HTTP + navegador |
| Observabilidade e custos | Classificação tem motivo e origem; não executar provider/modelo, mídia ou campanha | Asserções e bloqueio de rede externa |

Fixtures locais, sem dados de cliente, campanhas ou consumo de APIs pagas. Aprovação técnica
não comprova vendas. A matriz não exige retestar tudo após cada ajuste: repetir somente
casos afetados. Harness aprimorado: a fila recebe elegibilidade oficial e regressões que
impedem transformar histórico ou bloqueio de Têmis em pedido de aprovação humana.

## Resultado

- Suíte completa backend: 3.920 casos, 3.895 executados sem falha e 25 condicionais/desabilitados
  preexistentes ou dependentes de fixtures específicas; o novo navegador condicional foi executado
  separadamente e passou. Os demais condicionais não envolvem a fila corrigida nem migrations alteradas.
- Suíte completa frontend: 185 arquivos / 863 testes passaram. Ajustes finais revalidados nos
  10 testes da fila/alerta; guardas de histórico/copy e aprovação legítima em 17 testes HTTP;
  12 testes da política passaram. Não houve repetição integral só para cumprir quantidade.
- Build frontend, empacotamento backend, TypeScript, Spotless e Prettier aprovados. OpenAPI validado.
- Navegador contra o backend local e H2: desktop 1440×1000, iPhone 15 Pro e Pixel 7 passaram,
  incluindo aprovação sintética pelo celular e posterior leitura da decisão persistida.
  Dependências de páginas fora da fila foram simuladas; toda rede externa foi bloqueada.
  O ensaio não aprova nem reproduz as duas mídias reais: valida os comandos e o estado da fila.
- Primeira rodada encontrou perda do filtro no link por depender de `URLSearchParams.size`;
  a serialização explícita da query corrigiu o caso e seu teste passou. A captura também expôs
  orientação antiga de Têmis em card histórico; a tela agora mantém apenas a razão pertinente.
- Harness: `VideoReviewPolicyTest`, regressões em `CreativeControllerTest`, testes React e
  `VideoReviewBrowserTest`/`frontend/scripts/validate-video-review.cjs`. Para reproduzir o
  ensaio integrado: construir o frontend e executar `VIDEO_REVIEW_BROWSER=true mvn
  -f backend/ads-service/pom.xml -Dtest=VideoReviewBrowserTest test`. Fixtures e porta são efêmeras.
- Snapshot somente leitura da produção: 43 itens (8 DRAFT, 23 READY, 12 REJECTED), com hash
  SHA-256 `a0639d6e7bb9a26a75e0cb08c73d494dfc15341dd679eac979fafffa4bc36c86` dos IDs, origem,
  experimento, decisão, horário de decisão e URL, ordenados e serializados. Comparar após deploy.
- Consumo novo de modelos/provedores de vídeo: nenhum. Não foram medidos ganhos de venda,
  margem ou latência de agente; essa é uma correção operacional, sem homologação comercial paga.

Capturas locais do ensaio integrado (fora do código, em `.codex/video-review-validation`):

- `desktop-history.png`: SHA-256 `f0a3cd36d68fbbcf5e410e93a35f1b20e8ffb7c02b038eb7229e926f9d67973a`.
- `desktop-product.png`: SHA-256 `ccf8508caf9362c66bc9a05a1b4aa342560c4cdea3a4b4915881db43e180122c`.
- `iphone-history.png`: SHA-256 `c5d6cbc30b886028d1149f023a1e5849c657e1c2e993bbc7b90ebe4d5988d1ff`.
- `iphone-product.png`: SHA-256 `bc1e5db2732e7d6a29c9fe80cff837db2dd03498d801d85fb9971fd02c2afc94`.
- `pixel-history.png`: SHA-256 `da52453d5afa189066cb7e1bc4df27944bd16e686ce875e1f0e6880ff47eb3d0`.
- `pixel-product.png`: SHA-256 `794028f6d0eb6d95be9943fba396cadc36140902373681b0601987a6686ba6eb`.

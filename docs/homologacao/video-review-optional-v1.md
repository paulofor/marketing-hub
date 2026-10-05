# Aprovações necessárias e candidatas opcionais

Data: 05/10/2026. Escopo: contagem, classificação e tela de revisão de vídeos.

## Causa confirmada e decisão

A correção do PR #5497 está publicada; MCP `runtime_build_info` confirmou backend
`a5878605ff31e5cd48b29b9d5e5e10abc0a2467f`. A tela de Alcyone/#11 não tem alerta
de vídeo. A fila geral ainda contava dois ativos em `AWAITING_REVIEW` porque a
classificação considerava prontidão, mas não `required_for_release`.

Consulta MCP `db_query` em 05/10: #51, Capella/#94, está `READY/PENDING`, mas
`required_for_release=0`; #52, Vega/#95, está `READY/PENDING` e
`required_for_release=1`. Aprovações anteriores e as quatro tentativas históricas
continuam preservadas. Logs MCP do backend retornaram HTTP 206 sem linhas para
`video-review`; a causa é demonstrada pela consulta, contrato e código, não pelo log.

Alternativas comparadas:

| Alternativa | Benefício | Risco | Esforço e aderência |
| --- | --- | --- | --- |
| Aprovar ou descartar rascunhos automaticamente | Limpa a contagem | Fabrica decisão e pode liberar mídia sem revisão | Baixo esforço; viola controles |
| Ocultar todo aviso | Reduz ruído | Esconde a aprovação necessária de uma peça nova | Baixo esforço; perde orientação |
| Classificar pela obrigatoriedade persistida | Mostra somente decisões necessárias e preserva opções | Requer sincronizar API e UI | Proporcional; escolhida |

## Matriz definida antes dos testes

| Caso | Aceite | Validação |
| --- | --- | --- |
| Candidata pronta opcional | Não entra em contagem/alerta; aba própria; sem aprovação automática | Política, API/H2 e React |
| Peça pronta obrigatória | Continua pedindo decisão; filtros por produto/experimento | Política, API e navegador |
| Obrigatoriedade alterada | A mesma peça muda de classificação pela fonte persistida | API/H2 |
| Aprovação/reprovação já registrada | Decisão e data preservadas; leitura sem mutação | Política e API |
| Opcional com bloqueio ou histórico | Não contorna gates, encerramento ou linhagem | Política e regressões |
| Aprovação explícita de opcional | Continua possível; só altera a decisão da fixture; não publica mídia | API e contrato existente |
| Contexto sem decisões necessárias | Sem aviso de vídeo, mesmo com opcionais/bloqueios/histórico | React e navegador |
| Desktop/iPhone 15 Pro/Pixel 7 | Filtro padrão, aba opcional, links e botões acessíveis; sem overflow | Build local + backend HTTP/H2 |
| Falha de consulta | Não afirma sucesso nem produz decisão; erro explícito da fila | Regressões React |
| Custos e isolamento | Nenhuma chamada a modelo/provider, campanha ou cliente real | Rede externa bloqueada e fixtures locais |

Harness: a fixture integrada anterior marcava um vídeo opcional e esperava pedido
de aprovação. A nova matriz distingue peças obrigatórias de opcionais e comprova
a transição sem reescrever decisões. Não há mudança de prompt/modelo nem medição
de ganho em vendas. Aprovação técnica e limpeza de alerta são ganhos operacionais.

## Resultados

- Backend completo: 3.954 casos, 3.929 executados e 25 condicionais/desabilitados,
  sem falha ou erro; inclui 14 da política, 18 HTTP de criativos e 92 de arquitetura.
- O navegador condicional foi executado separadamente, junto aos dois testes da
  fronteira de publicação. Os três passaram, incluindo a regressão adicional que
  impede publicar uma candidata opcional sem aprovação. Os demais condicionais
  não envolvem os critérios de aceite desta alteração.
- Frontend completo: 185 arquivos e 868 testes passaram. TypeScript, build,
  Spotless dos arquivos alterados, Prettier, sintaxe Node e OpenAPI YAML passaram.
- Empacotamento: nove testes do verificador passaram; 4.212 classes testadas e
  empacotadas idênticas, 747 recursos íntegros e 513 cartões carregados no JAR.
- Build local contra backend HTTP/H2 real: desktop, iPhone 15 Pro e Pixel 7
  passaram, sem overflow ou erro de página. A candidata opcional fica fora da
  fila padrão; após aprovar a obrigatória sintética, o alerta desaparece mesmo
  com a opcional ainda pendente. A leitura e decisão só atingem fixtures.
- Snapshot das 43 decisões produtivas antes da publicação: SHA-256
  `02eb04d1f4cee3d2c57ab3af81b7cd6d7bf2728ecfd26688ab1792149f0e505d`, calculado
  sobre ID, origem, experimento, status, data da decisão e URL, ordenados e
  serializados. Conferir após deploy; nenhuma dessas decisões foi editada.
- Capturas locais em `.codex/video-review-validation`: `desktop-optional.png`
  `570a92d569bcc5ea85a96ba237bbe2e22ea3df7691144ca56f8aa3140051b37f`,
  `iphone-optional.png`
  `c6f501c229d2cffb244d7052f33f36f057ee39cdd8db7629ce295bb85e5fe84a` e
  `pixel-optional.png`
  `06c63da3471ec6b81a7002b70094305aec63839ad3f08d19ced5c48788c2e74f`.

A homologação comprova a fila, não o conteúdo dos vídeos ou resultado comercial.
Não houve mutação produtiva de vídeo, aprovação de mídia, campanha ou novo gasto.
Publicação e confirmação produtiva serão vinculadas ao PR após a entrega.

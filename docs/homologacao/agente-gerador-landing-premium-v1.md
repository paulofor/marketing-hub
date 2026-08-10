# Matriz de homologação — Agente Gerador de Landing v1

| Dimensão | Cenários locais | Aprovação |
|---|---|---|
| Caminho feliz | Quality Review reprova, backend enfileira, Codex/MCP analisa, callback dispara etapa causal | estado, auditoria e etapa corretos |
| Validações | contexto incompleto, schema inválido, plano vazio, tentativa de autoaprovação | gate fechado sem publicar |
| Falhas | timeout, MCP/backend indisponível, callback repetido | falha determinística e idempotente |
| Integrações | GeraLanding, memória MySQL, referência S3, Gerador de Imagens e Aprovador | apenas contratos oficiais |
| Observabilidade | heartbeat, request, resposta, modelo, custo, erro e correlação | dados persistíveis e logs consultáveis |
| Segregação | experimentos A/B com memórias e HTML distintos | nenhuma mistura de contexto |
| Experiência | Chromium desktop, iPhone 15 Pro e Pixel 7 | screenshots e critérios responsivos |
| Segurança | prompt injection, segredo, escrita, publicação e gasto | bloqueados |
| Métricas | aprovação, reincidência, custo/tempo e eventos reais do funil | sem contar estimativa como venda |

Uma rodada completa sem defeito conclui a homologação local. Se houver defeito, após a correção são exigidas duas rodadas integrais consecutivas sem falha.

## Autonomia Agenda Cheia — Nail Design

| Cenário | Caminho feliz | Validações e falhas | Evidência |
|---|---|---|---|
| Estratégia | compara pelo menos três alternativas e escolhe uma | bloqueia plano sem comparação, justificativa ou backlog causal | JSON estruturado e teste de contrato |
| Jornada | promessa de prévia personalizada é contínua entre anúncio, hero, formulário e entrega | bloqueia promessa de retorno garantido, prova inventada ou CTA divergente | prompt, plano e Quality Review independente |
| Visual | solicita assets pelo Gerador de Imagens oficial | não aceita URL inventada, cópia de concorrente ou mídia fora do pipeline | job e manifesto de imagem |
| Desktop e mobile | audita 1440 px, iPhone 15 Pro e Pixel 7 | detecta overflow, links inválidos, formulário sem submit e campos obrigatórios ausentes | auditoria MCP e screenshots |
| Observabilidade | persiste estratégia, backlog, métricas e condições de parada | não emite eventos nem submete formulário durante auditoria local | `eventsEmitted=false` e `formSubmitted=false` |
| Aprendizado | usa recompensa do Quality Review e eventos reais segregados | não conta geração ou avaliação própria como recompensa | memória auditável e feedback independente |

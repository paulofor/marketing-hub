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

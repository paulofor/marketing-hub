# Matriz de homologação — Plano Comercial versionado

| Área | Cenário | Resultado esperado |
|---|---|---|
| Caminho feliz | Criar plano MUSA e editar objetivo/orçamento | v1 na criação e v2 na edição, sem sobrescrever v1 |
| Validações | Plano sem gates comerciais mínimos | Plano bloqueado e snapshot preserva o bloqueio |
| Integração | Agente consulta contexto corrente | Retorna `planId`, versão e snapshot oficial |
| Mesas/gates | Referência `commercial-plan:<id>@v<n>` | Decisão permanece vinculada ao contexto utilizado |
| Monitor | Trabalho possui referência do plano | Frontend apresenta a referência persistida, sem inferência |
| Falhas | Serialização ou versão ausente | Operação falha sem criar contexto parcial |
| Métricas | Custo e receita executados | Snapshot separa teto/meta dos valores reais |
| Segregação | MUSA e Agenda Cheia | IDs, versões, custos e receitas não se misturam |
| Observabilidade | Histórico | Autor, motivo e instante ficam auditáveis |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 | Seleção, versão atual e histórico permanecem legíveis |

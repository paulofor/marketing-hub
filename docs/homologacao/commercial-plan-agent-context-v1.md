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
| Frontend | Abrir um plano com registros vinculados | Tarefas, gates, landing, vídeos e agentes aparecem no dossiê |
| Decisão externa | Gate financeiro pendente | Plutus e a decisão necessária ficam destacados sem liberar Apolo |
| Financeiro | Plano com custos BRL e ciclos em USD | Teto, campanha, IA, total, receita e vídeo aparecem sem conversão implícita |
| Segregação de atividade | Consultar MUSA e Agenda Cheia | Cada plano apresenta somente seus agentes, ciclos e experimento oficial |
| Atualização | Agente muda estado de tarefa | Painel reflete o backend em até 15 segundos |

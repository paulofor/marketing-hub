# Matriz de homologação — Têmis criadora e aprovadora v1

## Objetivo

Comprovar localmente que Têmis cria propostas completas de anúncios e mantém a aprovação técnica em uma execução independente, sem abrir gates de publicação ou gasto.

| Área | Caminho feliz | Validação e falha | Evidência esperada |
| --- | --- | --- | --- |
| Criação | peça fraca recebe nova copy, CTA, conceito, cena e prova | orientação vaga ou variação cosmética é recusada pelo contrato | prompt versionado e teste do worker |
| Segregação | proposta materializada retorna a uma nova revisão | a execução criadora não pode autoaprovar | instrução explícita e gate backend vigente |
| Integração | backend coordena versão e AI Worker materializa | Têmis não chama worker, banco, Meta ou publicação | arquitetura e contratos existentes |
| Observabilidade | request, response, decisão, custo e versão continuam auditáveis | falha preserva gate fechado | telemetria e callback vigentes |
| Métricas | mede propostas, diversidade, aprovação, tempo e vendas atribuídas | aprovação não conta como venda | contrato persistido do agente |
| Dados de teste | IDs de criativo e experimento ficam segregados | divergência de ID fecha o gate | teste de contexto/MCP vigente |
| Navegadores | landing é observada em desktop e mobile | indisponibilidade visual bloqueia aprovação | Playwright/MCP vigente |

## Critério de aceite

O módulo deve passar nos testes unitários e arquiteturais; o changelog deve ser incremental, MySQL 5.7 compatível e incluído relativamente no mestre. Nenhuma publicação ou chamada paga faz parte da homologação local.

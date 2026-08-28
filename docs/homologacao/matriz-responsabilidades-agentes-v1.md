# Matriz de homologação — responsabilidades exclusivas dos agentes v1

## Gargalo, métrica e decisão

O gargalo corrigido é a sobreposição de direitos de decisão. O banco de produção registrou 91
tarefas para Dédalo e apenas 3 para Atena, nenhuma concluída; processos publicados também atribuíram
uma mesma atividade a até quatro agentes. A métrica esperada é 100% das atividades novas de agentes
com um único responsável e domínio compatível. A entrega continua se todos os handoffs funcionarem,
ajusta diante de contrato ou executor ausente e para diante de conflito de autoridade.

| Alternativa | Benefício | Risco | Esforço | Aderência a vendas |
| --- | --- | --- | --- | --- |
| Manter a sobreposição | nenhuma migração | custo duplicado e decisões contraditórias | baixo | baixa |
| Criar novos agentes | especialização adicional | mais filas e governança antes de provar necessidade | alto | média |
| Redistribuir os oito agentes | clareza, independência e menor custo | exige migração coordenada | médio | alta |

Escolha: redistribuir os oito agentes existentes.

## Matriz ponta a ponta local

| Dimensão | Caminho feliz | Validações e falhas | Evidência esperada |
| --- | --- | --- | --- |
| Catálogo | oito contratos persistidos e versionados | agente ausente, versão duplicada ou papel incompatível bloqueia | `agent`, `agent_version` e harness |
| Argos → Atena | fatos auditáveis originam estratégia v2 | Argos tenta escolher posicionamento ou contrato sem fontes é rejeitado | dossiê, execução, fontes, versão e hash |
| Atena → Plutus | estratégia congelada recebe limites econômicos | Plutus tenta reescrever posicionamento/preço como fato ou usa proxy como venda | parecer financeiro e critérios |
| Construção | Dédalo materializa PDE/landing/não audiovisual e Apolo produz audiovisual | qualquer produtor muda estratégia ou aprova a própria saída | artefatos versionados, linhagem e custo |
| Gates | Psique e Têmis recebem atividades e IDs distintos | coautoria, array com dois agentes, domínio trocado ou autoaprovação bloqueia | duas tarefas, pareceres e causas independentes |
| Operação | Hermes recebe estratégia, ativos aprovados e eventos | contrato ausente bloqueia antes do modelo; contradição solicita Atena | contrato operacional, eventos e `revisionRequired` |
| Processo | versões novas aposentam somente a versão anterior e mantêm histórico | atividade sem dono único, fluxo quebrado ou subprocesso inválido falha | grafo e atividades relacionais |
| Observabilidade | request, response, modelo, tokens, custo, erro, versão e hash ficam correlacionados | falha de parse/callback preserva stack trace e bloqueio | tarefa e execução persistidas |
| Métricas | vendas, receita, entrega, satisfação, reembolso e margem permanecem fatos oficiais | recomendação, clique, checkout, tarefa, impacto ou PR não contam como venda | funil e ledger oficiais |
| Segregação | produto, plano e experimento usam somente seus artefatos | referência de outro produto/experimento é recusada | IDs, source reference e hashes distintos |
| MySQL 5.7 | aplicação, rollback quando suportado e reaplicação idempotente | include relativo, erro 1093, campo temporal, duplicidade ou comparação sem `utf8mb4` falha | runner físico dedicado com charset explícito |
| Interface | harness e processos exibem fronteira, dono e artefatos | conteúdo truncado, coautoria visual ou overflow falha | testes React, build e screenshots |
| Navegadores | desktop, iPhone 15 Pro e Pixel 7 preservam leitura e comandos | overflow, texto inacessível ou ação incorreta falha | Playwright com tráfego de teste segregado |

Uma primeira rodada completa sem defeitos conclui a homologação. Se houver defeito e correção, após a
última correção serão executadas duas rodadas locais completas e consecutivas sem falha; qualquer
novo defeito reinicia a contagem. Nenhuma rodada executa modelo pago, publicação, campanha, mensagem
real, gasto ou venda.

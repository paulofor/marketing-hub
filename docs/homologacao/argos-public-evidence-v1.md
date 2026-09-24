# Argos — pesquisa pública sem recrutamento

Decisão de 24/09/2026. Caso original: execução #30, ciclo #69, tarefa #481, três candidatas
preservadas e zero entrevistas. O objetivo é remover a dependência operacional de entrevistas,
substituindo-a por investigação pública rastreável; pesquisa concluída pode manter todas as
candidatas em RESEARCH_MORE. Não é prova de venda ou autorização para ampliar consumo.

## Matriz local de aceite

| Área | Caso | Aceite |
|---|---|---|
| Migração | Ciclo legado, execução nova, reaplicação | Histórico permanece; nova política é explícita; sem duplicação |
| Adesão | Espera por entrevista, clique repetido e estado inválido | Mesmos IDs, tarefa e corpus; idempotência; conflito fora da espera |
| Backend | Callback sem política, evidência inventada, candidata cruzada | Recusa antes de promover qualquer candidata |
| Evidência | Duas fontes independentes, fonte repetida, vendedor, trecho ausente | Somente relatos com suporte coletado contam; vendedor não vira cliente |
| Limite | Quota de busca/modelo, lente repetida, ausência de progresso | Limites anteriores mantidos; encerramento honesto sem aprovação |
| Worker | Caminho inicial, legado com entrevistas, novo modo público | Contratos compatíveis e política recebida pelo pending |
| Integração | Pending → plano → busca simulada → síntese → callback → relatório | Fontes, política, consultas e custos correlacionados ao ciclo |
| Interface | Desktop, iPhone, Pixel; loading, erro, adesão | Ação oficial, sem clique duplicado; mostra verdade do backend |
| Isolamento | Candidatas e ciclos sintéticos distintos | Nenhuma compra, pessoa ou receita fabricada |
| Publicação | PR, checks, main, deploys e tela | Versões compatíveis; retomar somente após validação |

## Resultado

Rodada local em 24/09/2026 concluída antes do commit:

- Backend: 3.491 casos nos relatórios finais, 3.471 executados sem falhas e 20 condicionais
  não aplicáveis a esta rodada. Inclui integração com o JSON gerado pelo worker real e MySQL 5.7.
  A suíte completa detectou ausência do schema no Catálogo Vivo; corrigida na fonte e revalidada.
  A mensagem de erro de evidência passou de “entrevistas” para “fontes”; expectativa atualizada
  sem remover a proteção contra consultas trocadas entre candidatas.
- Frontend: 762 testes da suíte completa; 27 casos afetados revalidados após os ajustes finais.
  Tipagem, build e Prettier aprovados. Chromium desktop, iPhone 15 Pro e Pixel 7: 27 controles
  de interface, erro/retry, concorrência de clique, polling e fontes; nenhuma chamada externa.
- Worker: 143 testes; schemas estritos conferidos no container construído pelo Dockerfile
  versionado. Biblioteca incluída. Fixture integra planejador/sintetizador reais com respostas
  simuladas e callback único: duas candidatas, nenhuma entrevista, nenhuma chamada paga,
  `RESEARCH_MORE` porque ofertas e anúncio continuam ausentes.
- Liquibase/MySQL 5.7: upgrade, rollback, reaplicação, política legada, preservação de candidatas,
  agente v7 e cadeia v21. Includes relativos, campos temporais e erro 1093 revisados; `bash -n`
  e ShellCheck do validador estático aprovados. Topologias temporárias removidas.
- Backend empacotado e inventário de recursos conferidos. Nenhum resultado local foi inserido
  na base produtiva. Não há evidência de ganho comercial neste registro.

Os números agregam a rodada completa e as revalidações necessárias, sem contar testes repetidos
como novos casos. A classificação de papel/ação continua sendo inferência do modelo; dois domínios
são triangulação mínima, não confirmação de autoria independente. A coleta guarda trechos de busca,
não promete leitura integral das páginas. Aprofundamento insuficiente termina sem promoção.

A publicação e a retomada produtiva serão comprovadas no PR vinculado a esta alteração.

Revisão de compatibilidade antes do merge: o pending filtra a política no banco e só entrega
`PUBLIC_SOURCES_V1` a executores que declaram suporte. Regressão com persistência real comprova
que worker legado não assume ciclos novos nem leases públicos expirados. 138 testes de fila,
controller, serviço e arquitetura revalidados; os 143 do worker permanecem aprovados.

O CI identificou uma fixture de fronteiras de agentes que parava em Argos v6, enquanto o
contrato de saúde já exigia v7. O validador foi atualizado para aplicar a migração real v7,
conferir a política pública, reaplicação sem duplicatas e rollback preservando o histórico.
O mesmo script completo passou localmente em MySQL 5.7, após `bash -n` e ShellCheck, sem
reduzir a comparação com o contrato de saúde dos nove agentes. Topologia temporária removida.

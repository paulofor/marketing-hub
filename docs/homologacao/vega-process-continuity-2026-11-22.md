# Continuidade auditável de processo — 22/11/2026

## Diagnóstico e escopo

Consulta somente leitura ao MCP e frontend confirmou execução 102 PAUSED, produto 4,
cadeia 27, ciclo 22 OPEN, referência experiment:115. O processo 117/v11 está RETIRED;
o 127/v12 está PUBLISHED. A navegação pelo processo 127/cadeia 28 retornava HTTP 409.
A dispensa da solicitação 3998 já estava integrada (PR 5861). Ela não autoriza trocar versões.

A API confirmou oito ocorrências aceitas: journey 859, deliverables 860, audiovisual 861,
access 862, technicalHomologation 863, psiqueAdherent 865, psiqueRecovery 866 e psiqueSafety
867. prototypeCorrection 864 continua bloqueada. As definições v12 acrescentam requisitos
Turmalina na descrição das onze atividades; igualdade de ID não prova compatibilidade.

Não houve comando de escrita no produto real, nova dispensa, retomada, inferência, mídia,
cobrança ou produção audiovisual. Desenvolvimento isolado da main e2c2dac6e.

## Solução e alternativas

Manter a versão retirada exigiria exceção sem incorporar o contrato publicado. Migrar toda
a cadeia alteraria o contexto financeiro e histórico. Foi escolhida uma execução sucessora
PAUSED no mesmo ciclo, vinculada por recibo `VERSION_CONTINUATION` ao diário original.

A comparação distingue contrato idêntico, mudança de critérios e mudança estrutural.
As provas originais permanecem intactas; as ocorrências de destino referenciam reuso auditado.
Critérios alterados exigem evidência explícita conferida pelo operador. Mudança estrutural,
ficha incompatível, tarefa ativa e contexto encerrado bloqueiam. O snapshot protege contra
concorrência; repetir devolve o recibo. Não há nova autorização de gasto nem custo duplicado.
A pausa financeira é preservada no destino. Os gates originais continuam validando as provas.

## Matriz e evidências locais

| Cenário | Evidência |
| --- | --- |
| Oito entregas e três pendências; outro produto | ProcessVersionContinuityServiceTest, identidades sintéticas distintas |
| Contratos idênticos e critérios alterados | ProcessContractCompatibilityTest e conferência por entrega |
| Estrutura incompatível; concorrência; ciclo encerrado | rejeição sem criar destino |
| Contexto retirado → publicado | resolvedor real rejeita antes do recibo e aceita depois, mantendo cadeia e referência |
| Persistência, recarga, rollback e segregação | ProcessVersionContinuityPersistenceTest com H2 e repositórios reais |
| Custos e pausa | valores originais intactos, nenhuma gravação de produto/ciclo e herança de PAUSE_CONTEXT; regressões de ProcessRunPauseGuidanceTest |
| Frontend, falha e recuperação | testes da página, painel de continuidade e painel de ciclos |
| Desktop, iPhone 15 Pro, Pixel 7 | Playwright com API sintética; oito entregas, falha 409 recuperável, um POST, recarga e ausência de overflow |
| Gates, ciclos e prontidão existentes | 856 cenários passaram na regressão ampliada; um replay histórico exige exportações externas e foi omitido |
| Arquitetura | 92 testes passaram; nenhuma regra alterada |

A regressão ampliada expôs a ausência do serviço na fixture HTTP isolada; o controller agora
responde 503 recuperável. Os testes do contrato HTTP, serviço, reuso de tarefas e persistência
passaram depois dessa correção: 130 testes, zero falhas, erros ou omissões, incluindo
os controles operacionais e as 92 regras de arquitetura.
Os 51 testes de interface passaram após corrigir dois defeitos de tipagem dos próprios testes.
Typecheck e build completo do frontend passaram. O primeiro build esgotou o heap padrão;
a repetição usou 2560 MB somente no processo do job e passou, sem alterar ferramentas globais.
O cenário de navegador é uma simulação de API; a persistência foi comprovada separadamente
pelos testes Java. Não constitui execução nem aprovação das entregas reais do Vega.

## Aprendizado reutilizável e limite

O harness agora detecta o caso de ciclo fixado em processo retirado, cobre outra identidade e
impede que apenas o nome da atividade autorize reuso. A hipótese é reduzir retrabalho e consumo
duplicado na construção. Testes demonstram preservação e isolamento, não aumento de vendas.

A revisão Mira v88 reatesta somente compatibilidade das fontes compartilhadas verificadas pelos
testes de gate e prontidão. Mantém a experiência, pareceres, manifestos anteriores e limites;
não constitui nova homologação comercial nem nova autorização de produto.

## Operação após publicação

Abrir [o processo histórico do Vega](http://191.252.181.168:5173/products/4/value-chain-history/processes/117/activities?chainId=27&learningCycleId=22&sourceReference=experiment%3A115). No painel **Continuar no
processo publicado**, conferir contratos/provas, registrar a evidência dos requisitos alterados,
responsável e motivo e usar **Preparar continuidade sem executar**. O link **Abrir continuidade**
leva ao destino na cadeia original. Retomar é uma ação separada, sujeita aos controles existentes.
Nesta solicitação técnica, esses comandos não são aplicados ao Vega.

Publicação deve ser conferida pelo PR, checks do HEAD, SHA integrado, deploy e GET consultivo
do painel. Nenhuma entrega comercial ou retomada real é alegada por este registro local.

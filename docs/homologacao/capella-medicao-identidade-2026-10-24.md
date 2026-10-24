# Conciliação da publicação histórica de Capella

## Causa comprovada

Após recuperar a navegação e aceitar a publicação anterior, o ciclo 12/experimento
105 passou a MEASUREMENT. A conciliação comparava `capella-private-v3` com `v1`.
O primeiro identifica a experiência; `Product.validationDefinitionVersion` identifica
o contrato modular de validação. A descrição do campo e os ciclos anteriores
confirmam essa diferença. O ciclo antigo 4 funcionava porque ambas as strings eram
`v1`, coincidência que ocultava o defeito.

O run produtivo 19 preserva `asset_bundle_version=34`. A publicação auditada 34
pertence ao experimento 105, e o gate do run conserva sua identidade e hash.
Não é necessário modificar o produto nem reabrir o experimento invalidado.

## Alternativas e decisão

| Alternativa | Benefício | Risco e esforço |
|---|---|---|
| Usar somente a versão congelada no ciclo | Mudança pequena | Não distingue publicações posteriores |
| Usar o vínculo do run publicado com a publicação auditada | Reutiliza a prova que já liberou a campanha | Exige conferir experimento e horário; mudança localizada |
| Criar outro snapshot versionado e migrar históricos | Contrato explícito novo | Migração e duplicação de identidade sem evidência adicional |

Adotado o vínculo existente. Run não publicado, de teste ou futuro não substitui
o produtivo. Referência explícita inválida não usa fallback. Legado sem vínculo
usa publicação existente até a data do run (ou fim da janela sem run), com limitação
explícita. A leitura agregada não comprova causalidade por versão.

## Matriz de homologação local

| Critério | Verificação |
|---|---|
| Caso original | Contrato v1 e experiência v3 conciliam pela publicação do run |
| Outro produto | Outra identidade e experiência v8 preservam isolamento |
| Caminho antes válido | Experiência v1 continua conciliando |
| Revisão posterior ou run não publicado | Fonte histórica não é substituída |
| Publicação ausente, alheia ou posterior ao run | Bloqueia sem inventar receita |
| Métricas comerciais | QA, bots e pagamentos de teste excluídos; custos e fonte preservados |
| Passagem completa | SQL real → coletor → controller/JPA → DECISION no navegador |
| Dispositivos | Chromium desktop, iPhone e Pixel emulados |
| Autoridade | Experimento permanece encerrado, sem novo run, tarefa paga ou mídia |

## Harness e limites

A primeira rodada de navegação usava medição sintética e não exercitava a divergência
semântica do kit. O novo teste reutiliza a exportação do SQL real e o BPM local
existentes, com identidade e assinatura preservadas. Não cria uma infraestrutura
paralela nem usa produção como teste. Integrações de mídia e custos ficam em doubles.

Métrica de adoção: conciliação aceita sem substituição da publicação histórica;
regressão ou mistura de identidades impede adoção. Menos intervenção manual e menor
tempo até o próximo teste são hipóteses operacionais; vendas e margem exigem mercado.

## Resultado local

Passaram 238 testes de backend: 23 com SQL real em MySQL 5.7, 13 do coletor,
6 de publicação, 104 de decisão/sucessor/regras e 92 de arquitetura. A fotografia
SQL percorreu o controller e JPA locais até DECISION em desktop/iPhone/Pixel,
mantendo um único run e o experimento `USER_STOPPED`, sem chamadas externas.
O callback de Atena foi simulado no contrato real; a recuperação automática
preparou o sucessor em PLANNING com orçamento zero e sem janela, preservando
o anterior como ADJUSTED. A simulação não é parecer de agente nem resultado comercial.

Para repetir a parte visual, exporte `LEAD_PORTAL_CYCLE_TEST_EVIDENCE_DIR` ao executar
`LeadPortalCycleMeasurementIntegrationTest` com o schema local `capella_cycle_test`.
Use o `LearningCycleLocalApplication`, o projeto Compose exclusivo da sessão e Vite
local na porta 15173 com `VITE_API_URL=http://127.0.0.1:15173`. Depois execute
`node infra/testing/cycle-commercial/quartzo-measurement.cjs` com a mesma pasta
de evidência e `LEARNING_CYCLES_COMPOSE_PROJECT` configurado. O navegador bloqueia
origens externas. A primeira tentativa com preview/API incorretos foi descartada;
a rodada aprovada usou somente o backend local da porta 18091.

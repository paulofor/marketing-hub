# Homologação — checkout e acesso Vega v12 no Opala

## Incidente e objetivo

A tarefa produtiva `#432`, atividade `checkout` do processo `opala-commercial-preparation-v1`,
foi bloqueada porque o ciclo, o contrato Opala e o slot candidato identificavam a Vega v12,
enquanto `taskTarget` ainda era montado pelo contrato publicado v7 do produto. O objetivo local é
fazer o backend entregar uma única identidade v12 para destino, checkout e acesso, sem alterar a v7,
criar preferência Pepper, cobrar, conceder acesso real, publicar ou avançar o experimento #92.

## Alternativas avaliadas antes da implementação

| Alternativa | Benefício | Risco / esforço | Decisão |
| --- | --- | --- | --- |
| Atualizar manualmente apenas a tarefa #432 | Retomada curta | Reincide em toda candidata cujo produto publicado ainda seja predecessor | Rejeitada |
| Ensinar Dédalo a preferir `opalaCommercial` sobre `taskTarget` | Mudança localizada no worker | Mantém duas fontes conflitantes e transfere ao agente uma decisão do backend | Rejeitada |
| Resolver `taskTarget` pelo contrato candidato persistido e validar checkout + acesso no backend | Uma fonte por ocorrência; vale para próximas versões; preserva o predecessor | Exige contrato de acesso estruturado, migração e regressões em dois módulos | Escolhida |

## Matriz definida antes dos testes

| Área | Cenários e critérios |
| --- | --- |
| Caminho feliz | produto histórico v7 + ciclo/slot v12 entregam `taskTarget.experienceVersion=v12`, URL da candidata, Pepper `owm6x`, R$ 67 e acesso de 90 dias da v12 |
| Generalização | outra combinação de produto, experimento, ciclo e versão válida resolve sem exceção por ID |
| Validações | predecessor com mesmo checkout, outro experimento, outro preço, outra URL, acesso de outra versão, prazo/trigger inválidos são bloqueados |
| Retomada e idempotência | migração pode ser reaplicada; conclusão antiga é revalidada; tarefa bloqueada/custo histórico permanecem intactos |
| Integrações | backend monta o alvo; Dédalo recebe o contrato; PDE concede acesso QA da mesma versão por 90 dias; nenhuma API Pepper é chamada |
| Observabilidade | exceções preservam produto, experimento e ciclo; evidência de tarefa mantém `opalaScope` e `taskTarget` versionado |
| Métricas e custos | nenhum token, cobrança, preferência, acesso real, mídia ou receita é gerado; custo #432 permanece histórico |
| Segregação | v7 publicada não é alterada; candidata v12 permanece no experimento #92; fixture futura usa outros IDs |
| Interface | nenhuma mudança visual; conferir contrato retornado e continuidade da tela local em desktop, iPhone e Pixel |
| Rodadas | como o defeito foi reproduzido antes da correção, executar duas rodadas completas consecutivas sem falhas após a última alteração |

## Resultados

### Correção comprovada

- O backend monta `taskTarget` a partir da candidata exata do ciclo, sem copiar o contrato v7 do
  produto publicado. `opalaCommercial`, `learningSalesCycle`, `taskTarget` e `pdeContext` passam a
  concordar em produto, experimento, versão, destino, checkout, preço e cobrança.
- O vínculo de acesso da v12 ficou estruturado em contrato: 90 dias, sem renovação e ativação
  somente após `PAYMENT_APPROVED`. A concessão testada usa transação QA sintética; não cria compra,
  preferência Pepper nem acesso de cliente real.
- Dédalo ganhou um admission gate determinístico anterior ao modelo. Contexto misturado é recusado
  sem consumir tokens, inclusive quando predecessora e candidata usam o mesmo checkout.
- A migração MySQL 5.7 atualiza somente o slot v8 candidato do experimento 92. Reaplicação,
  rollback e nova aplicação preservaram a v7 e o histórico publicado.
- A mudança no catálogo compartilhado invalidou corretamente a atestação Rigel v10. Após 180
  testes PDE aprovados, foi criada a atestação imutável v11; nenhuma prova anterior foi reescrita.

### Duas rodadas finais consecutivas

As rodadas `checkout-v12-approved-round-1` e `checkout-v12-approved-round-2` terminaram sem falhas
após a última alteração. Cada rodada executou 758 verificações contabilizadas: 666 testes Java nos
seis módulos e duas fixtures MySQL, 6 testes do executor do processo, 53 testes do frontend, 30
cenários Playwright do PDE e 3 jornadas administrativas responsivas. Também passaram build,
typecheck, Spotless, validação estática Liquibase, isolamento de runtime e `git diff --check`.

- [Resultado da rodada 1](../../artifacts/opala-commercial/checkout-v12-approved-round-1/result.txt)
- [Resultado da rodada 2](../../artifacts/opala-commercial/checkout-v12-approved-round-2/result.txt)
- Evidências visuais: `artifacts/opala-commercial/browser/desktop.png`, `iphone.png` e `pixel.png`.

### Baseline e candidata

| Critério | Baseline | Candidata aceita |
| --- | --- | --- |
| Fonte de `taskTarget` | cadastro publicado v7 | contrato candidato da versão do ciclo |
| Divergência detectada | pelo parecer pago de Dédalo | antes do modelo, por gate determinístico |
| Acesso de 90 dias | comportamento/texto sem vínculo de versão | contrato estruturado e consumido pelo PDE |
| Recorrência | nova candidata poderia herdar a publicada | testes com IDs e versões futuros |
| Efeito externo | tarefa bloqueada após consumo | simulação local sem cobrança, acesso, mídia ou publicação |

O desenho adota os contratos de interação e evidência atual sugeridos em
`pesquisas/agentes-inteligentes/2026-09-12-agentes-inteligentes.md`, seções 2 e 4, e a reconstrução
da evidência original descrita em
`pesquisas/agentes-inteligentes/2026-09-14-agentes-inteligentes.md`, seção 2. Essas referências
orientaram o gate e a rastreabilidade; não são prova de ganho comercial.

### Estado produtivo conferido ao final

Consulta MCP confirmou as tarefas #431 e #432 ainda `BLOCKED`, o experimento #92 em `PLANNED` e o
ciclo #2 `OPEN/PUBLICATION` na v12. Isso é esperado: a correção permanece somente na sandbox e a
tentativa paga não foi repetida antes de publicação autorizada.

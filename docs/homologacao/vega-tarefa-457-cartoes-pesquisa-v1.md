# Vega — recuperação da tarefa 457 — cartões de pesquisa

## Diagnóstico confirmado em 19/09/2026

A tarefa 457, atividade `commercialIntegrityReview` do processo Opala 77/v1, recebeu de Têmis um
parecer `APPROVED`, nota de clareza 95/100, doze gates aprovados e nenhuma mudança obrigatória. O
worker a bloqueou depois da inferência porque as evidências não citavam nenhum `cardId` da biblioteca
entregue. Banco, API e log confirmaram o mesmo erro.

O contexto continha três cartões válidos na rota `meta-ad-approver`, todos da coleção
`neuromarketing`. O validador exigia ao menos um cartão de cada coleção, mas a versão 1 do prompt
fixada pelo Catálogo Vivo não instruía Têmis a citar os identificadores. Repetir a tarefa sem mudar
o contrato produziria custo sem remover a causa.

## Alternativas

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Repetir a tarefa 457 sem alteração | Nenhum desenvolvimento | Alta chance de repetir a falha e o custo | Rejeitada |
| Remover ou afrouxar o validador | Conclusão imediata | Perde rastreabilidade e permite fonte não entregue | Rejeitada |
| Versionar o prompt e manter o gate | Preserva auditoria e orienta a resposta correta | Mudança pequena em backend e worker | Escolhida |

## Matriz definida antes dos testes

| Dimensão | Critério de aceite |
| --- | --- |
| Caminho feliz | Nova tarefa recebe a versão 2 e cita ao menos um `cardId` de cada coleção entregue |
| Validação | Aprovação sem ID, ID não entregue ou coleção sem cobertura continua bloqueada |
| Histórico | Tarefa 457 e versão 1 permanecem imutáveis; somente novas tarefas usam a versão 2 |
| Banco | Changelog incremental aplica no MySQL 5.7, ativa apenas o vínculo correto e mantém FKs/uniquidade |
| Idempotência | Reexecução do Liquibase não duplica versão nem auditoria |
| Integração | Backend entrega a versão fixada e Têmis valida o resultado antes do callback |
| Observabilidade | Banco, tela e logs mostram a nova tarefa, versão, status, parecer e eventual bloqueio |
| Interface | Retomada ocorre pelo botão oficial em desktop; acompanhamento também é conferido em iPhone e Pixel |
| Métricas e segregação | Nenhum evento de QA vira venda, receita, campanha ou autorização de gasto |

## Resultado

A validação local foi concluída antes da publicação:

- suíte completa e Spotless do Meta Ad Approver Worker aprovados;
- teste de integração do Catálogo Vivo aprovado em MySQL 5.7 real, com 20 testes sem falhas;
- validador estático dos changelogs MySQL 5.7 e Spotless do backend aprovados;
- prompt do arquivo e texto migrado possuem exatamente o mesmo SHA-256
  `8ff0e4cb56deef842ab5e29e9c797f92b5aeec3f3b07b75b01b08b6a67143766`.

Pendente apenas da entrega pelo PR e da retomada operacional pela interface após o deploy.

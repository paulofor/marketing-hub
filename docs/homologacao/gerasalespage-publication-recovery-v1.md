# Recuperação da publicação auditada — 21/09/2026

## Evidência e escopo

Na execução Quartzo #12, a revisão financeira 4/Plutus #52 concluiu com `APPROVE`.
A atividade `economics` avançou e Psique #467 bloqueou antes do modelo: faltava
`mh-publication-source-sha256`. O fluxo #60 e o snapshot #27 tinham os mesmos
25.018 caracteres e SHA-256 `ffa40b6d321da1f1bd757472b99147d2cc8c119190727c4bb9d24ca137356ccb`.
O deploy atualizado não reescreveu esse HTML histórico. A tela só oferecia refazer
a página, acionando geração desnecessária. Os três workers estavam no build
`0412d002b61841a09ac5b756416b5a950c6336e7`; a causa era o artefato persistido.

Alternativas: refazer a página exige custo e pode alterar a oferta; ignorar a
identidade enfraquece a homologação; republicar os bytes auditados preserva a
experiência e permite conferir sua origem. Escolhida a terceira, com bloqueio de
versão antiga, destino diferente e conteúdo alterado. Não há nova geração nem
aprovação automática de Psique, campanha ou orçamento.

## Matriz definida antes dos testes

| Caso | Critério |
| --- | --- |
| Página histórica sem marcador | Comando pela UI republica o snapshot atual e mantém ID, HTML, data e etapas auditadas |
| Novos identificadores e conteúdo | Mesmo comportamento com fixtures genéricas, sem exceção por produto |
| Repetição/concorrência | Mesma identidade, sem duplicar auditoria, job ou chamada paga; reserva de escrita no experimento |
| Fonte inválida | Outra publicação/experimento, versão anterior, fluxo não aprovado, destino ou bytes diferentes bloqueiam antes do envio |
| Integração ausente/falha | Sem falso sucesso; erro acionável e repetição idempotente com o mesmo conteúdo |
| Empacotamento/entrega/captura | Serviço real produz o documento; Lead Portal real o transforma; capturador real confere origem, servido e hashes dos PNGs |
| Divergência e retomada | Captura da página antiga bloqueia antes da IA; página republicada permite nova captura mantendo Plutus e provas anteriores |
| UI desktop/iPhone/Pixel | Disponibilidade vem do backend; erro visível; confirmação e indicador de execução; sem overflow |
| Observabilidade/segregação | IDs e hashes registrados, custo ausente continua ausente; somente fixtures locais e `mh_test=1` |

As correções de código permanecem locais até publicação pelo PR do usuário. A
aprovação financeira é projeção condicional, não comprovação de vendas ou lucro.

## Resultado local

- Backend: 3.340 casos, 3.321 aprovados e 19 condicionais ignorados; zero falhas/erros.
- Psique: 133 casos Java, 132 aprovados e um condicional ignorado; 18 testes de
  navegador aprovados. Lead Portal: 60 testes aprovados, sem alteração de código.
- Frontend: 723 casos. A primeira rodada encontrou falta de limpeza do DOM nos
  testes novos; corrigida a fixture, os cinco casos novos e os 17 relacionados
  passaram. Os outros 701 já estavam aprovados; não houve repetição integral.
- Empacotamento dos três módulos Java, build/typecheck frontend, Spotless dos Java
  alterados, Prettier dos arquivos novos, sintaxe Node, OpenAPI YAML e diff aprovados.
- Integração com controller, service, publisher HTTP, Lead Portal e captura reais:
  página antiga bloqueada, reenvio confirmado pela UI, captura aprovada, origem
  diferente bloqueada; desktop, iPhone 15 Pro e Pixel 7 sem overflow. Quatro pedidos
  simultâneos conservaram a mesma identidade. Os PNGs antes/depois são idênticos.
  Nenhum modelo ou recurso produtivo foi acionado pela homologação.
- Persistência usada na integração: H2 no Lead Portal e repositórios simulados no
  backend. As quatro chamadas concorrentes comprovam a idempotência do payload;
  não representam ensaio de contenção do lock em MySQL. A query JPA foi compilada
  na suíte do backend; não há mudança de schema/Liquibase.

Evidências locais: `artifacts/publication-recovery/` (ignorado pelo Git).
Relatório `integration-result.json` SHA-256:
`aa2daa29bf905d21a724a05dc27c59d33b607cfb148d9d71e99d0f686fba7ece`.
Fonte sintética `f27ead0f08ebea74a2c2a9a95990acefa13b5e48d52b52c7d884e11a706dac66`;
HTML servido após reenvio
`9ddd3da61c2f1fde4a1bd793e54da37940d3a44c8cce23b7e42864e9244f4ca5`.
PNG da primeira dobra, preservado antes/depois:
`d885eda41aa60094b62c0344ee94a6e30a5fd5accac7d716389d7f140987add6`.

Para reproduzir, executar `mvn test package dependency:build-classpath
-DincludeScope=test -Dmdep.outputFile=target/recovery.classpath` nos módulos
`backend/ads-service`, `customer-agent-worker` e `lead-portal/backend`; instalar as
dependências com `npm ci` no frontend e Psique, então executar
`node infra/testing/quartzo-page-identity/recovery-local.mjs` na raiz. O runner usa
somente loopback, inicia e encerra os processos locais e remove as entradas
temporárias da UI. Portas locais: 18081, 18096 e 18097.

## Situação produtiva e custos

Em 21/09/2026, o fluxo pela tela salvou a revisão 4 (id 5), preservando suporte de
sete dias e IA personalizada. Plutus #52/tarefa #466 aprovou a cobertura agregada:
contribuição projetada de R$ 28,50/venda e resultado-base de R$ 69,30 com cinco
vendas; cenário conservador de duas vendas resulta em -R$ 16,20. Nada disso é
receita observada nem autorização para divulgar ou ampliar investimento.

Última confirmação via tela, API e MCP: execução #12 bloqueada em
`humanExperienceReview`, 5/8 concluídas. As tarefas Psique #467 e #468 foram
bloqueadas com `execution_mode=NOT_STARTED`, sem prompt ou tokens. A retomada
manual anterior gerou a segunda verificação técnica com o mesmo impedimento;
não repetir até a recuperação da origem. Têmis e a conclusão ainda não executaram.

O ledger passou a informar custo **estimado** de US$ 0,21525360 para Plutus #466
(81.222 tokens de entrada e 4.149 de saída). O processo ainda informa
`costCoverage=NOT_REPORTED`; os custos ausentes de Psique não foram convertidos
em zero. Não há homologação comercial completa, portanto custo por homologação
concluída ainda não é calculável. A revisão foi salva às 04:09:13 UTC e o último
bloqueio persistido às 04:15:28 UTC: 6min15s de preparação até esse impedimento.
Não houve intervenção manual de container, publicação de código ou ativação de
campanha. O artefato histórico diverge por ausência da identificação de origem;
não foi constatada divergência de imagem entre os três workers.

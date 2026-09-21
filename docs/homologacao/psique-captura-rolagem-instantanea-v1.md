# Psique — posição comprovada da captura — 21/09/2026

## Contexto confirmado

Capella #7 é Quartzo (`LOW_TICKET_DIGITAL_PRODUCT`), pacote personalizado de ativos
visuais, plano comercial #2 v4, cadeia #17, processo #81 v1, referência
`experiment:88`, execução #12. O backend e a tela confirmaram 5/8 etapas concluídas;
Plutus #52 aprovou a revisão financeira 4. As tarefas #467/#468 pararam antes do
modelo por ausência de `mh-publication-source-sha256`. Não existe ficha persistida
em `product_execution_profile_v1` para o produto; o contrato de validação v1 e os
vínculos atuais foram preservados, sem criar ficha ou ciclo por inferência.

O PR #5282 já contém a recuperação idempotente da publicação #27; não recriar essa
entrega. A primeira consulta desta investigação encontrou o endpoint de recuperação
com HTTP 404 e o deploy na fila, com backend ainda em `bce99d98d45d`.
O snapshot e o fluxo aprovado preservam o mesmo SHA-256
`ffa40b6d321da1f1bd757472b99147d2cc8c119190727c4bb9d24ca137356ccb`.

No replay local do snapshot original, a recuperação passou e as 13 dobras mobile
foram preservadas. Porém, o PNG da página inteira variou: a comparação dos pixels
localizou diferenças no cabeçalho, entre Y=51 e Y=191. Instrumentação local do
capturador comprovou `scrollY=117` antes da captura que declarava `scrollY=0`.
O HTML auditado usa `scroll-behavior:smooth`; uma espera fixa de 300 ms após voltar
ao topo não garante o fim da animação. Essa falha é adicional à ausência do marcador,
não a causa atribuída retrospectivamente às tarefas #467/#468.

## Escolha

| Alternativa                                      | Benefício                                         | Risco                                                 | Esforço | Decisão   |
| ------------------------------------------------ | ------------------------------------------------- | ----------------------------------------------------- | ------- | --------- |
| Aumentar a espera fixa                           | Alteração pequena                                 | Continua dependente de altura, máquina e animação     | Baixo   | Rejeitada |
| Remover rolagem suave da página comercial        | Simplifica a captura                              | Altera conteúdo auditado para atender ao agente       | Médio   | Rejeitada |
| Posicionar instantaneamente e conferir a posição | Pixels coerentes com o metadado, sem mudar oferta | Bloqueio explícito se a página impedir posicionamento | Baixo   | Escolhida |

## Matriz definida antes da correção

| Caso                                              | Critério de aceite                                                                  |
| ------------------------------------------------- | ----------------------------------------------------------------------------------- |
| Página longa com rolagem suave e cabeçalho fixado | Captura inteira começa no topo; pixels do cabeçalho conferidos                      |
| Repetição da mesma fonte                          | Cabeçalho e posição preservados, sem depender de espera probabilística              |
| Página impede a posição solicitada                | Falha técnica com posição esperada/observada, antes da revisão paga                 |
| Página sem animação                               | Preserva dobras, CTA, identidade, arquivos e checks existentes                      |
| Fonte histórica sem marcador                      | Continua bloqueada antes do modelo                                                  |
| Recuperação pela UI                               | Backend e Lead Portal reais reenviam a fonte; Psique aceita a identidade recuperada |
| Origem divergente                                 | Continua bloqueada, sem dispensar o gate                                            |
| Original e identificadores sintéticos distintos   | Mesmo comportamento sem exceções por produto                                        |
| Desktop, iPhone e Pixel                           | Reenvio operável, sem overflow; captura oficial em iPhone                           |
| Isolamento e observabilidade                      | Dados locais, `mh_test=1`, hashes de fonte/servido/PNGs e nenhuma chamada paga      |

Persistência da integração: H2 no Lead Portal e repositórios simulados no backend.
Concorrência comprova a estabilidade do payload; não simula contenção MySQL.
Checkout/pagamento real, parecer comercial pago e conclusão produtiva não fazem
parte desta homologação técnica local.

## Resultado local

As duas regressões novas falharam no código anterior: cabeçalho fora do topo e
sucesso indevido quando a página recusava rolagem. Após a correção:

| Validação                                             | Resultado                                                                                                    |
| ----------------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| Psique, suíte Java completa                           | 132 aprovados; um cenário Vega condicional não executado (`VEGA_SCENARIO_LOCAL` ausente)                     |
| Psique, suíte de navegador completa                   | 20 aprovados, incluindo as duas regressões                                                                   |
| Backend, recuperação/Quartzo/continuação/subprocessos | 72 testes relevantes aprovados; módulo sem alteração                                                         |
| Lead Portal                                           | 60 testes aprovados; módulo sem alteração                                                                    |
| Frontend, recuperação                                 | Cinco testes aprovados; módulo sem alteração                                                                 |
| Builds e estática                                     | JARs, build/typecheck frontend, sintaxe Node, Prettier e `git diff --check` aprovados                        |
| Integração sintética                                  | Recuperação pela UI em desktop/iPhone/Pixel; página antiga e origem divergente bloqueadas                    |
| Replay do HTML original                               | Origem auditada preservada; página inteira e 13 dobras idênticas antes/depois; CTA correta na primeira dobra |

Os executáveis Java foram empacotados; o script incluído no JAR de Psique é
byte a byte igual à fonte validada, SHA-256
`38ebc77ee254bc27d9bb266fe031a2274fb367cc512b691a729dcb78b0378e0c`.
O Dockerfile existente copia essa mesma fonte; nenhuma imagem produtiva foi
construída ou promovida manualmente. O teste integrado usa processos locais,
controller/service/publisher reais e os contratos de persistência descritos acima.

A primeira tentativa com vários navegadores simultâneos esgotou o limite de
threads da sandbox. Reduzida a concorrência/afinidade para duas CPUs, os testes e
integrações passaram. Não houve flexibilização de critérios ou alteração de
código produtivo para contornar essa limitação operacional do ensaio.

Evidências ignoradas pelo Git:

- `artifacts/capella-diagnosis-20260921/summary.json`: escopo, hashes, testes e consultas.
- `artifacts/audited-page-replay-before-scroll-fix/`: capturas anteriores à correção.
- `artifacts/audited-page-replay/integration-result.json`: resultado com o HTML original,
  SHA-256 `e4fc6173ce8b524f0f70b417adaefed3fd9bfe46f1360ef1f28ca3d750c3ca9f`.
- `artifacts/publication-recovery/integration-result.json`: fixture sintética,
  SHA-256 `aa2daa29bf905d21a724a05dc27c59d33b607cfb148d9d71e99d0f686fba7ece`.

## Situação de entrega

Durante o diagnóstico, o deploy já existente do PR #5282 disponibilizou o backend
`dda884b6152eac443fa7e32ea9d183f46da2f974`; a consulta da recuperação passou de
HTTP 404 para HTTP 200 com `available=true` e o hash esperado da publicação #27.
Isso não reenvia automaticamente a página histórica nem publica o ajuste adicional
de captura realizado neste worktree.

A execução #12 permanece `BLOCKED` em `humanExperienceReview`, 5/8 concluídas,
sem novas tarefas. A revisão financeira aprovada foi preservada. A correção de
rolagem permanece **somente local**, sem commit, push, PR ou deploy nesta tarefa,
conforme o limite explícito da solicitação. Depois de sua publicação autorizada,
o reenvio da página aprovada e a retomada da mesma execução permitem revalidar
Psique; Têmis e a conclusão ainda dependem dos resultados oficiais.

Não houve chamadas pagas, intervenção manual em runtime, alteração de campanha,
orçamento ou nova comprovação de venda. O custo das tarefas Psique históricas segue
não reportado; custo por homologação comercial concluída e tempo total até sua
conclusão não são calculáveis enquanto o processo permanece bloqueado. O ganho
demonstrado é precisão operacional da captura, não aumento medido de receita.

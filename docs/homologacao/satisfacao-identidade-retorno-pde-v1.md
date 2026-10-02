# Satisfação, identidade e retorno na cadeia PDE — 02/10/2026

## Escopo e decisão

Integrar situação reconhecível, satisfação pretendida, prova funcional, identidade e ocasião de
retorno nas atividades dos seis processos da cadeia v24 e nos subprocessos que executam suas
missões. Usar novas definições, preservando grafos, donos, recursos, contratos, versões anteriores,
aprovações, evidências e custos. Nenhum produto, oferta, experimento, orçamento ou campanha muda.

Alternativas: apenas documentação (baixo esforço, sem missão operacional); contrato/runtime
paralelo (duplicação e risco de incompatibilidade); objetivos versionados nos contratos existentes
(missão chega aos agentes e à tela sem duplicar mecanismos). Escolhida a terceira.

Lacuna comprovada: os contratos ativos contemplam desejo e continuidade paga, mas não exigem
explicitamente a relação entre satisfação proposta, resultado funcional, identidade, ocasião de
retorno e contrapontos em toda a cadeia. Algumas descrições legadas ainda mencionam observações
humanas solicitadas; as novas definições aplicam a regra constitucional de agentes ou mercado.

## Matriz definida antes dos testes

| Caso | Critério de aceite |
| --- | --- |
| Caminho feliz / persistência | Liquibase real em MySQL 5.7 publica cadeia v25 com os mesmos seis processos; descrição do grafo e objetivo relacional são idênticos. |
| Contratos / integração | Grafos, IDs de atividades, responsabilidades, recursos, rotas por tipo e contratos técnicos preservados; executores reconhecem as revisões compatíveis. |
| História / segregação | Definições anteriores, atividades, produtos e tarefas sintéticos com IDs independentes preservam versão, estado, resultado e custo. Nenhum dado produtivo é usado no teste. |
| Fonte ausente / conflito | Falta de definição, deslocamento de atividade ou versão ocupada por outra alteração interrompe a migração antes de qualquer publicação parcial. |
| Reaplicação / rollback | Sem duplicidade; rollback retira somente as novas definições e conserva as anteriores e a auditoria; reaplicação recupera disponibilidade sem criar novas linhas. |
| Harness / fronteiras | Missões usam campos existentes, exigem fontes e contrapontos, distinguem demonstração, uso e compra e devolvem lacunas à camada responsável. Não autorizam entrevistas, inferência emocional, chamadas pagas ou gasto. |
| Economia / amostra | Degustação depende do plano e subprocesso; contabilizar amostras sem compra, tentativas e suporte. Custos desconhecidos permanecem pendentes; retorno não equivale a satisfação. |
| Observabilidade / métricas | Cada objetivo define entregável, evidência e medição observável; compra líquida e contribuição prevalecem sobre sinais intermediários. |
| UI local | Dados reais da migração alimentam a interface local com APIs simuladas; cadeia e objetivos legíveis em desktop, iPhone 15 Pro e Pixel 7, sem eventos enviados à produção. |
| Regressão | Suíte unitária do backend, build/formatação, validação estática Liquibase e fixture física aprovados; nenhum changeset histórico alterado. |
| Entrega | PR, revisão, checks do HEAD e merge; workflows/deploys aplicáveis aprovados; identidade/saúde e definições publicadas conferidas na tela, API e MCP. |

## Limites da comprovação

A melhoria é uma missão operacional mais precisa usando o contrato BPM existente. Não instala
telemetria nova nos produtos nem um verificador automático de satisfação. `description` é
persistido como `objective`; o comando BPM usa esse objetivo como descrição da tarefa, que o
executor inclui no contexto auditado. Execuções já iniciadas continuam com sua definição original.
Revisões compatíveis não exigem repetição automática de provas pagas.

Aplicação de referência: Capella pode testar orgulho de mostrar o próprio trabalho com prova fiel,
sem prometer agenda cheia. O teste preserva produto, preço, público e página e muda principalmente
a abordagem do anúncio; amostra personalizada pertence a outra comparação autorizada.

Fonte primária consultada em 02/10/2026: [Zhang et al., 2021 — Retrieval-constrained valuation](https://pmc.ncbi.nlm.nih.gov/articles/PMC8157967/).
O estudo relaciona recuperação de opções e valorização em decisões abertas. A aplicação à cadeia
é uma hipótese de marketing; não comprova memória, satisfação, conversão ou lucro dos nossos produtos.

## Resultados

Validação local concluída:

- Backend: 3.769 testes executados sem falhas; 24 condicionais ignorados na suíte geral.
- Fixture física: dois testes com Liquibase/MySQL 5.7, sem falhas, incluindo a regressão anterior,
  fonte ausente, atividade deslocada, colisão, paridade, preservação, reaplicação e rollback.
- Navegador: 36 páginas de processos e três páginas da cadeia, em desktop, iPhone 15 Pro e
  Pixel 7; 183 verificações dos objetivos, sem erro JavaScript ou transbordamento horizontal.
  Os dados exportados pelo MySQL alimentam APIs locais simuladas, sem eventos produtivos.
- Empacotamento do backend, build do frontend, recursos empacotados, Spotless dos dois testes
  alterados, gerador determinístico, sintaxe JavaScript, Actionlint e validação Liquibase aprovados.
  Os scripts de validação e formatação passaram por `bash -n` e `shellcheck`.
- Diff revisado: apenas changeset novo, definições e critérios; grafos, tipos, recursos,
  contratos técnicos, produtos, aprovações, orçamento e histórico preservados.
- Após o CI apontar vínculo por hash em um teste histórico de Alcyone, a nova regressão foi
  isolada em `BusinessProcessActivityMissionContractTest`, com duas identidades sintéticas.
  Os 37 testes relacionados passaram; o empacotador comercial validou 309 arquivos e 60
  manifestos, e seus 15 testes passaram. A prova e o teste atestados continuam íntegros.
  Comparadas: renovar atestação de outro produto (exigiria revalidá-lo), enfraquecer o gate
  (perderia integridade) e isolar a nova regressão (escolhida, preserva prova e cobertura).

Publicação e confirmação operacional serão vinculadas ao PR desta entrega; os testes acima
não comprovam satisfação humana, lembrança, aumento de vendas ou margem comercial realizada.

A primeira rodada física parou na criação das tabelas, antes da migração: MySQL retornou
`Errcode: 28 - No space left on device`. A fixture existente passou a usar `tmpfs` de 512 MiB
para os dados sintéticos, como outras homologações do repositório. Não houve limpeza global
de imagens, alteração de produção ou publicação para descobrir falhas.

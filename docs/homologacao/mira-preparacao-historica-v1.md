# Preparação Safira em referência encerrada

Consulta de 04/10/2026: Mira #10 (`AI_PRODUCT`), subprocesso Safira #98/v2,
cadeia #26, referência `experiment:93`, execução #45. A ficha oficial retornou
lista vazia; não foi inventada ficha nem migrado o produto. O pai publicado é
#118/v12, atividade `commercialPreparation`; a execução #45 não tem instância pai.

## Causa e comparação histórica

Frontend, API, banco via MCP e logs concordam: #45 aguarda candidata enquanto
o experimento está `INVALIDATED` e o ciclo #3 está `ADJUSTED`. A execução #31
concluiu as cinco atividades em 29/09 e retornou ao pai #30/definição #96.
As ocorrências #433, #430, #431, #434 e #435 e os pareceres #554/#556 permanecem
concluídos no banco. A consulta revalida fontes atuais e apresenta 0/5; o motor
não aplica a proteção de referência encerrada ao subprocesso Safira. A execução
#43, com ciclo explícito, foi encerrada, mas omitir o ciclo permitiu criar #45.

| Alternativa | Benefício | Risco | Esforço | Escolha |
| --- | --- | --- | --- | --- |
| Refazer jornada/economia/pareceres | Produz novas verificações | Consumo sem hipótese nova e alteração do histórico encerrado | Alto | Não |
| Pausar apenas #45 | Interrompe a espera | Permite repetição em outra execução | Baixo | Não |
| Preservar provas e validar encerramento na fonte | Corrige relatório e impede repetição | Exige testar histórico separado de nova autorização | Médio | Sim |

## Matriz definida antes dos testes

| Área/atividade | Critério de aceite |
| --- | --- |
| 5.1.1 Jornada | Conclusão histórica do mesmo produto/referência preservada; candidata aberta continua invalidando prova com mudança material |
| 5.1.2 Economia | Conclusão histórica preservada sem renovar Plutus; candidata aberta continua exigindo economia vigente |
| 5.1.3 Psique | Parecer concluído preservado sem novo modelo; ausência de conclusão não é convertida em aprovação |
| 5.1.4 Têmis | Mesmo contrato de preservação, identidade e bloqueio anterior à nova tarefa |
| 5.1.5 Consolidação | Prova passada preservada; controle concilia conclusão já comprovada sem nova tarefa ou repetição de retorno ao pai |
| Estado/ciclo | Estado terminal, janela passada ou ciclo fechado bloqueiam mesmo quando `learningCycleId` é omitido; outro produto é recusado |
| Regressões | Planejado/pausado válido continua executável; ciclo aberto, ausência de ciclo e aprendizado seguem contratos existentes |
| Persistência/retomada | Motor persiste conclusão comprovada e contagem, não duplica eventos, tarefas ou custos; objetivo ausente não vira sucesso e trabalho em curso conserva retorno |
| Interface | Bundle local com respostas backend em desktop, iPhone 15 Pro e Pixel 7; histórico legível, sem comando de retomada indevido, erro JS ou overflow |
| Empacotamento | Unitários do backend, contratos pertinentes, formatação, build e correspondência entre classes testadas e JAR |
| Isolamento | Identificadores sintéticos locais; nenhuma chamada paga, campanha, compra ou evento comercial de teste em produção |

Os cinco pontos da oferta permanecem nos contratos históricos: desejo/prova em
jornada, facilidade e compra/uso em Psique, clareza do adicional pago em Têmis,
economia em Plutus e repetição por novo ciclo. Não se modifica oferta, preço,
criativo, janela ou orçamento. Esta entrega mede correção operacional; não mede
ganho de vendas, margem ou custo por homologação paga.

## Ajuste confirmado na integração

A primeira candidata alterava a prioridade do motor e devolvia `CLOSED` mesmo
quando a execução acabara de receber todos os resultados. A matriz MySQL detectou
três regressões: conclusão após ciclo fechado, pausa e retirada de versão. Os testes
e o cânone `business-process-catalog-canon.v1.md` exigem observar resultados antes
de decidir sobre novos disparos. Essa mudança foi descartada, mantendo o contrato
original e concentrando a correção na fonte Safira que ocultava as conclusões.

Entre priorizar encerramento para tudo (menor código, regressão comprovada), criar
uma regra de duplicidade adicional no motor (mais esforço e sem necessidade) e
preservar a conclusão já comprovada com bloqueio de novos comandos (contrato atual,
menor risco), foi escolhida a terceira alternativa. Não houve publicação intermediária.

## Resultados locais

- A versão anterior reproduziu 0/5 na regressão HTTP; a versão corrigida retorna
  5/5, recusa os cinco comandos com 409 e não consulta economia nem cria tarefas.
  O caso sem a quinta prova continua 4/5; outra identidade é recusada e candidata
  aberta continua revalidando a impressão das fontes.
- Suíte completa backend: 3.947 casos, 3.922 aprovados e 25 já desabilitados ou
  condicionados a fixtures específicas. Após o ajuste encontrado na integração,
  235 regressões de Safira, motor e arquitetura passaram sem falhas ou skips.
- Frontend: suíte completa de 867 testes na candidata inicial; a apresentação
  permaneceu sem alteração no diff final. Os 23 testes do painel foram repetidos
  com essa versão, além de tipos e build. Nenhum teste foi enfraquecido.
- Integração final em MySQL 5.7/Node 22.23.3: 19 cenários HTTP, 22 de ciclo de vida,
  reinício, persistência da jornada, quatro matrizes gerais de navegador e a
  matriz Safira em desktop, iPhone 15 Pro e Pixel 7. Sem erros JavaScript,
  transbordamento horizontal ou gravações de teste na produção.
- Worker: seis testes e imagem temporária do repositório com polling autenticado,
  execução sem root e parada graciosa. Containers, rede e volumes temporários do
  projeto exclusivo foram removidos ao terminar.
- Spotless, `git diff --check`, `bash -n` e ShellCheck dos scripts examinados passaram.
  O JAR final contém 4.212 classes idênticas às testadas, 747 recursos íntegros e
  inicializa 513 cartões do catálogo de agentes; nove testes do verificador passaram.
- O motor persiste uma única conclusão, mantém contagem/custos e não chama executor
  ou retorno de subprocesso na repetição. A publicação e o comportamento produtivo
  serão registrados no PR da entrega; testes locais não os substituem.

SHA-256 das capturas da candidata final:

- desktop: `2108c947c24ede9e5c9daa43611faba9a2dc4eab02d0e5204453a97408014d12`.
- iphone: `f3419cc30abefbac70ff569518a426f1112ba91d00880dc01e7cbcaf1b5520b7`.
- pixel: `196a1290993e0eadfb46660dcfc9a6b909e006d384a7ecf0c638f0a5bef89e15`.

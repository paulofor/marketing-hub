# Homologação do retrabalho funcional de Mira após parecer da Psique

## Objetivo e evidência inicial

- **Gargalo corrigido:** uma rejeição funcional interrompia o processo sem criar o trabalho de
  correção, embora o parecer persistido já explicasse a causa e a ação recomendada.
- **Evidência:** a tarefa `#350`, da atividade `psiqueAdherent`, terminou `BLOCKED` com decisão
  `ADJUST` porque a rotina útil desaparecia após a conclusão do cenário. O processo v7 mantinha a
  própria Psique como atividade atual e oferecia apenas `Reiniciar tarefa`; nenhuma atividade do
  Dédalo recebia o parecer.
- **Métrica esperada:** 100% das rejeições funcionais devem apontar uma atividade de correção com
  causa, ação, critérios verificáveis, versão-alvo e retorno à homologação técnica.
- **Continuar:** correção gera uma nova versão, passa novamente pela homologação técnica e segue
  pela sequência Psique → Têmis → gate final.
- **Ajustar:** tarefa de correção não recebe o parecer, aceita a mesma versão ou libera Psique antes
  de uma nova homologação técnica.
- **Parar:** o parecer não indicar uma causa funcional verificável ou a correção exigir publicação,
  gasto ou ação externa não autorizada.

## Alternativas avaliadas

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Apenas renomear `Reiniciar tarefa` | Baixo esforço | Repete a análise sem corrigir o produto e consome mais IA | Rejeitada |
| Criar uma tarefa avulsa fora do processo | Resposta rápida para Mira | Perde rastreabilidade, critérios e prevenção para outros produtos | Rejeitada |
| Versionar o processo com retrabalho explícito | Preserva causa, responsável, evidências, nova versão e retorno controlado | Exige contrato backend/worker, migração e testes E2E | Escolhida |

## Matriz definida antes da homologação final

| Dimensão | Cenários obrigatórios | Critério de aprovação |
| --- | --- | --- |
| Caminho feliz | Rejeição Psique → correção Dédalo → nova versão → homologação técnica → Psique → Têmis → gate | Cada etapa fica disponível somente na ordem correta e mantém evidência auditável |
| Validações | Correção na mesma versão, plano incompleto, parecer sem categoria funcional e predecessor ausente | O backend bloqueia avanço e apresenta causa e próxima ação claras |
| Falhas e retomada | Rejeição v7 existente, retentativa Psique prematura e novo parecer após correção | O histórico é preservado; apenas o retrabalho aplicável fica acionável; não existe loop cego |
| Integrações | Backend central, landing-generator/Dédalo, PDE Mira e worker Psique | Contratos usam somente APIs oficiais, prompt/schema versionados e contexto persistido |
| Observabilidade | Tarefa, versão, parecer, plano, validações, artefatos e retorno de etapa | Request, response, erro e evidências permanecem correlacionados ao produto e à execução |
| Métricas | Rejeições com correção criada, correções versionadas e revalidações aprovadas | Cobertura de 100% por contrato; tarefa, PR ou aprovação não contam como venda |
| Segregação | Fixtures locais do produto 10 e fonte `product:10@agent-validation-v1` | Nenhum evento, campanha, preço, pagamento, venda ou dado produtivo é alterado |
| Banco | Aplicação, reaplicação e rollback no MySQL 5.7 | Changelog idempotente, sem erro 1093 e com v7 preservada no histórico |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 | Correção é clara na tela administrativa; Mira mantém a rotina visível após a conclusão |

## Defeito encontrado e prevenção

Uma rodada com cache frio mostrou que o servidor Vite de desenvolvimento podia compilar
dependências enquanto os três navegadores iniciavam juntos. A API devolvia a sessão correta, mas
um dos navegadores ficava sem processar a resposta. O estado do produto não era a causa: dez
repetições isoladas do mesmo cenário passaram.

A homologação de Mira passou a construir e servir o bundle final, com as mesmas rotas exatas do
Nginx da imagem. Isso elimina a compilação dinâmica da jornada, aproxima o teste do artefato que
será publicado e preserva a execução paralela. Após o ajuste, o cenário multiagente passou em
trinta execuções consecutivas — dez em desktop, dez no iPhone e dez no Pixel.

## Resultado

Duas rodadas locais completas e consecutivas terminaram sem falhas depois da última correção:

- `2.380` testes do backend central por rodada (`5` ignorados por contrato), incluindo o fluxo v8,
  a prioridade da correção, o contexto entre versões, o retorno à homologação técnica e o
  gate final;
- `501` testes do frontend administrativo, typecheck e build por rodada;
- `49` testes do Dédalo e `87` testes Java mais `2` testes reais de navegador da Psique por
  rodada, com a política de raciocínio `max` protegida por contrato;
- `167` testes do backend PDE, builds independentes de Vega e Mira, contratos de isolamento e
  Spotless/Actionlint sem falhas por rodada;
- `27` jornadas Playwright integradas por rodada, com backend PDE e MySQL locais reais, executadas
  em Chromium desktop, iPhone 15 Pro e Pixel 7;
- aplicação, reaplicação, rollback e nova aplicação do processo v8 aprovados fisicamente no
  MySQL 5.7 nas duas rodadas, sem erro 1093, duplicidade ou perda do histórico v7;
- topologias Docker removidas ao final das duas rodadas, sem containers, redes, volumes, eventos
  comerciais ou dados de teste residuais.

O protótipo `mira-private-v2` preserva a rotina, seus limites e a ação **Consultar a rotina
novamente** depois que o cenário termina. Quando houver rejeição funcional, o processo v8 mostra
**Criar tarefa de correção**, entrega ao Dédalo a causa, a ação e as evidências persistidas,
exige uma nova versão e bloqueia nova análise da Psique até a homologação técnica dessa versão.

A produção continua deliberadamente no processo v7 com a tarefa histórica `#350` bloqueada até
que as alterações passem por Pull Request e pelo pipeline. Nenhum deploy, publicação, campanha,
gasto ou venda foi criado nesta homologação.

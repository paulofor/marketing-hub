# Mira — continuidade da comunicação privada

Data: 2026-09-13. Escopo: produto 10, cadeia 14, processo 63/v7 e subprocessos
64/v8 e 65/v6, sem criar experimento ou ciclo de aprendizado.

## Diagnóstico confirmado antes da alteração

A tela e seus endpoints de histórico e automação retornaram referência ausente e
`UNAVAILABLE`. O MCP confirmou `PDE_AGENT_VALIDATED_V1`, gate 242 concluído e provas
371, 372, 373, 374 e 381 aprovadas para `mira-private-v3`. O processo 70 concluiu os
onze objetivos. A referência é `product:10@agent-validation-v1`. Não existem tarefas
do processo 63 para essa referência. O backend só inicia referências privadas no
processo de construção; Íris, formatos e integração reconhecem plano ou ciclo com
experimento. A ausência é uma lacuna de continuidade, não falta de homologação.

Fontes: APIs `/api/business-processes/63/products/10/activity-executions`,
`/api/business-processes/63/products/10/automation/v1` e histórico do processo 70;
MCP `db_query` sobre produto, tarefas e instâncias, banco `marketinghubdb` saudável.
Consulta de logs do backend pelo endpoint retornou zero linhas, sem falha de acesso.
Histórico consultado: `mira-tarefa-367-seguranca-v1.md` e
`LOOP-IRIS-PREPARACAO-EXIGE-PUBLICACAO` em `docs/registros/loops.md`.

## Alternativas e decisão

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Abrir experimento comercial agora | Reutiliza contexto comercial existente | Antecipa decisões, muda o contexto e pode misturar autorizações; esforço médio | Não adotada |
| Exigir plano comercial antes de comunicar | Reutiliza a entrada de plano | Repete estratégia/economia aprovadas e mantém dependência circular; esforço alto para o operador | Não adotada |
| Reconhecer preparação privada pelo produto aprovado | Preserva contexto, provas e independência das revisões | Exige contrato completo até integração, com regressão de identidade e permissões; esforço localizado em backend e agentes | Adotada |

A entrega comprova preparação privada. Campanha, cobrança, publicação comercial,
venda ou validação humana continuam exigindo seus próprios contratos e evidências.

## Matriz de homologação definida antes dos testes

| Dimensão | Critérios locais |
| --- | --- |
| Caminho feliz | Produto validado sem experimento → início pelo processo → Íris → formatos → imagem final → Psique → Têmis → decisão humana explícita → destino aprovado → integração privada |
| Identidade | Produto, referência, processo e versão preservados; nenhum experimento ou ciclo fabricado; produto diferente rejeitado |
| Gates | Gate ausente, bloqueado, prova alterada, nova correção ou versão diferente impedem avanço antes de consumir modelo |
| Conector de Íris | MCP real inicializa com referência privada, recupera contexto e memória do produto; não aceita troca de escopo; plano/experimento continuam funcionando |
| Contratos | Estratégia V3 e economia aprovadas da descoberta, protótipo aceito e hashes auditáveis; prompts e schemas versionados |
| Falhas e retomada | Falhas de agente/callback preservam histórico; repetição com entradas iguais bloqueada; retomada não duplica conclusão |
| Integrações | Persistência real local, APIs oficiais, executor e modelos simulados; imagem final com origem e hash conferidos |
| Observabilidade | Tarefas, instâncias, decisões, custos e entradas/saídas rastreáveis; prontidão e callback concordam |
| Métricas | Dados sintéticos locais, marcados como QA/AGENT_VALIDATION; nenhuma venda, receita, contato ou gasto comercial gerado nos testes |
| Navegação | Chromium desktop, iPhone 15 Pro e Pixel 7; início disponível e links pai/filho preservados |
| Regressão | Fluxo comercial e sucessor privado de Vega preservados; comentários Java, formato e diff revisados |
| Empacotamento | JAR confere com classes/recursos testados e Dockerfile versionado; imagens locais aprovadas antes de intervenção |

Após a última correção, as rodadas `final3` e `final4` terminaram completas e consecutivas sem falhas. Os resultados estão registrados abaixo.

## Bloqueio operacional confirmado

O comando `begin` para APP/Íris/Têmis recusou abrir a recuperação porque a intervenção
`6f6602b97d734d819e32dd23fb60772d` continua `AWAITING_MERGE`. A consulta ao GitHub
confirmou `main=1a15c5944eca27e7556124387ade7452a71eac78` e o [PR #5180](https://github.com/paulofor/marketing-hub/pull/5180)
aberto com a revisão validada anterior `d90427871c46d34aaf8cf4acd06d798e9bb55463`.
Nenhum lock, workflow, imagem ou container produtivo foi alterado nesta recuperação.

Foram comparadas: esperar a integração preparada (preserva a homologação anterior;
exige o merge já pendente), substituir a intervenção anterior (perde sua proteção e
rastreabilidade) ou abrir intervenção sobreposta (permite disputa pelos mesmos serviços).
A primeira é a única aderente ao cânone de implantação vigente; as demais não foram executadas.
A limitação não impede a implementação e a homologação integral na sandbox.

O conector MCP de Íris também recusava a referência do produto no início do processo Node.
O teste com o servidor real reproduziu a falha antes da mudança. O contrato agora usa
memória `PRODUCT/<id>`, mantendo plano e experimento em seus escopos originais.

## Regressão detectada na revisão final

As rodadas preliminares `final1` e `final2` não valem como homologação final. Uma nova
regressão reproduziu rejeição incorreta no callback: IDs `Long` vindos do JPA e os
mesmos IDs lidos como `Integer` após JSON eram comparados como nós Jackson diferentes.
O teste falhou com `marketStrategicContract`, mesmo sem alteração funcional.

Comparação por tipos Java preservava um detalhe que não existe no contrato HTTP;
comparar somente IDs perderia a verificação das provas completas; comparar todos os
campos após a mesma serialização JSON preserva valores e integridade com pouco custo.
A terceira alternativa foi aplicada. A regressão cobre IDs reais do JPA e confirma
que mudar o valor continua bloqueando o callback. A contagem das duas rodadas foi
reiniciada após essa última correção.

## Resultado final da homologação local

| Verificação | `final3` | `final4` |
| --- | ---: | ---: |
| Backend, incluindo ArchUnit e H2 | 2.870 executados | 2.870 executados |
| Persistência MySQL 5.7 | 3 | 3 |
| Íris | 27 | 27 |
| Psique | 106 | 106 |
| Têmis | 91 | 91 |
| Conector MCP de Íris, HTTP local | 4 | 4 |
| Executor de processos | 6 | 6 |
| Frontend do processo | 50 | 50 |
| **Total de testes executados sem falhas** | **3.157** | **3.157** |
| Navegação | Desktop, iPhone 15 Pro, Pixel 7 | Desktop, iPhone 15 Pro, Pixel 7 |
| Dockerfiles do repositório | Backend, Íris, Têmis | Backend, Íris, Têmis |
| JAR e recursos efetivos das imagens | Conferidos | Conferidos |

Dez testes opcionais de replay de outros fluxos não foram habilitados; não foram
contados como executados. A entrada real de Mira e seu callback foram reproduzidos
separadamente com os arquivos consultados via API/MCP e repositórios simulados, sem
gravar dados no sistema publicado. O replay retornou `READY` e `COMPLETE` depois do
transporte JSON. A captura 95 da tarefa 371 foi baixada e teve seu SHA-256 conferido.
O MCP confirmou as cinco capturas 95–99 persistidas e concordantes com a prova técnica.

A matriz usa H2 e MySQL reais locais, transporte HTTP local, renderização real de PNG
e test doubles nas integrações/modelos. Os estados de conclusão vistos no navegador
local pertencem às fixtures QA do backend, não a Mira. O teste de interface cobre
início, comando único, cópia da referência privada, estado concluído vindo do backend,
layout e erros JavaScript; os testes de módulo cobrem os gates e a integração da jornada.

Reprodução: construir o frontend e servir a prévia em `127.0.0.1:4173`, subir
`infra/testing/process-automation/compose.yml` com o projeto exclusivo da sandbox,
criar `private_journey_local` nesse MySQL e executar duas vezes
`infra/testing/mira-communication/run-round.sh <rodada>` com `PROCESS_COMPOSE_PROJECT`.
A matriz não usa credenciais de modelos, clientes, SMTP real ou dados comerciais.

## Situação publicada e próxima ação

A conferência publicada em desktop, iPhone e Pixel manteve `UNAVAILABLE`,
`canStart=false`, referência ausente e **0 de 4 atividades concluídas**. O MCP
confirmou zero tarefas de comunicação/criativos/landing para a referência privada
de Mira. Nenhuma execução paga, aprovação humana, venda ou campanha foi criada.

A recuperação não foi aplicada: o coordenador continua em `AWAITING_MERGE` pela
intervenção anterior, vinculada ao PR #5180. A autorização excepcional desta
solicitação não remove essa proteção. O PR é de Vega e **não contém estas alterações
locais de Mira**. Depois de sua integração e da retomada preparada, a recuperação de
Mira poderá abrir sua própria intervenção, aplicar imagens da revisão homologada e
iniciar o processo pela tela. A decisão humana dos criativos permanece obrigatória
quando as peças e os pareceres estiverem prontos.

Não houve commit, push, criação/alteração de PR, cancelamento de workflow, troca de
container ou liberação de pausa nesta solicitação. A topologia local é descartável
e sua limpeza está registrada junto às evidências da execução.

## Oportunidade comercial sugerida

Hipótese de alto impacto e baixo esforço de roteiro: demonstrar o alívio de ter os
produtos que a cliente já possui organizados numa rotina consultável, usando o
benefício aprovado “Sua rotina, organizada com calma”. Evidência de capacidade:
captura 95 e homologação v3. Isso não prova demanda ou conversão. Medir, no futuro
experimento autorizado, início → primeiro resultado → retorno à rotina → checkout
→ venda líquida. Não transformar organização de rótulos em promessa clínica.

Evidências versionadas: [diagnóstico](evidencias/mira-comunicacao-contexto-privado-v1/diagnostico.json),
[rodadas finais e imagens](evidencias/mira-comunicacao-contexto-privado-v1/homologacao.json) e
[identidade dos arquivos produtivos](evidencias/mira-comunicacao-contexto-privado-v1/revisao-local.json).
Logs e capturas locais: `artifacts/mira-communication/final3` e `final4`;
leituras publicadas e replay: `/tmp/mira-recovery`.

# Atena — escopo verificável e recuperação da análise preservada

## Escopo e evidência anterior

Pedido de 06/11/2026: corrigir o falso positivo e preparar recuperação, com testes locais e PR
revisável. Sem merge, deploy, reinício remoto, callback produtivo ou inferência/render pagos.
Vega: produto 4, cadeia 27, ciclo 21, experimento 114, versão
`musa-pde-entry-v18-video-cinematico`, processo 126, execução 98, tarefa 899.

O MCP confirmou a tarefa BLOCKED após a inferência, com 739404 tokens de entrada (630272 em cache),
19362 de saída e US$ 2,689692 estimados. O callback de falha foi confirmado nos logs do executor.
O backend publicado declarou `0c3bad528589cb4875a803124eb9e34e741436e4`; o blob do validador no
mesmo main é `e92e69f6bc13be114a565e0b7d6b35b36c56d0e7`. A identidade do worker será conferida
separadamente; não inferir seu SHA pela identidade do backend.

A inspeção somente leitura do container de Atena identificou a imagem
`marketing-hub/experiment-strategist-worker:db2ebb1037931ef95804834746ebb971b93d04fc`.
Não houve comando de publicação, alteração de configuração ou reinício remoto.

Resposta e contexto completos foram exportados pela API oficial em modo somente leitura. O MCP
mascara campos chamados authorization inclusive dentro do prompt, portanto sua cópia não serve
para reconstruir JSON integral. A exportação da API preserva esses bytes sem reconstruções.
A frase fica em `marketStrategicContract.agentValidationPlan.continueCriteria`, não no critério
geral do contrato. As tarefas concluídas 898, 850 e 827 foram preservadas para comparação histórica.

## Alternativas consideradas antes da implementação

| Caminho | Benefício | Risco e esforço | Escolha |
| --- | --- | --- | --- |
| Acrescentar sinônimos aos dois regex | Correção pequena | Mantém duas políticas e dependência de palavras isoladas | Não |
| Exigir somente saída estruturada nova | Contrato determinístico | Inviabiliza reaproveitar respostas já pagas e não detecta contradições textuais | Não |
| Política Java única, capacidades/identidade verificadas e compatibilidade delimitada | Evita deriva entre backend/worker, preserva história e bloqueios | Exige contrato e regressões comuns, além de empacotar a fonte compartilhada | Sim |

Reutilizar o padrão de fontes compartilhadas do PR 5793, sem copiar suas mudanças. Os PRs 5793
(Estúdio) e 5787 (#3940, retomada financeira) permanecem independentes. Testar a composição local
dos HEADs sem atualizar suas branches nem mudar suas autorizações.

## Matriz de aceite definida antes dos testes da correção

| Dimensão | Critério |
| --- | --- |
| Legado | Frase integral de 899, nenhum/nenhuma, sem e proibições equivalentes são aceitos sem reescrita |
| Recusa legítima | Obrigações afirmativas, dupla negação, negação seguida de obrigação, condicionais de orçamento e contradições continuam recusadas |
| Estrutura | Percorrer textos, campos aninhados e arrays; flags falsas não são obrigações, flags verdadeiras não são dispensadas pelo texto |
| Matriz | Nove combinações comuns; 18 somente com comparação implementada na mesma candidata; 27 não substitui a matriz |
| Identidade | Recusar divergência de produto, ciclo, experimento ou versão e capacidade de outra candidata |
| Compartilhamento | Backend e worker executam a mesma fonte Java e a mesma matriz de casos |
| Replay integral | Executar validadores Java reais sobre resposta e contexto completos de 899, sem callback, banco ou modelo; relatar outros defeitos reais por campo |
| Recuperação | Reutilizar resposta/evidência/prompts/tokens/custo; reenvio sem nova inferência, segunda recusa terminal, sem retry infinito ou aprovação forçada |
| Integração | Backend local com persistência/test doubles e executor HTTP local; contexto correto e outra identidade; preservar caminho anteriormente válido |
| Observabilidade | Mensagem de negócio na falha; campo/código/trecho nos registros técnicos; nenhum JSON bruto, SQL, stack trace ou segredo na mensagem |
| Isolamento | Dados sintéticos segregados; zero uso comercial simulado como receita; nenhum teto alterado |
| Navegação | Conferir telas reais em leitura; se houver alteração visual, validar Chromium desktop e mobile; caso contrário testar contrato das mensagens |
| Entrega | Testes do HEAD, diff e integração com PRs paralelos; PR revisável com SHA/evidências e plano operacional sem executá-lo |

Big Idea v2 e referências Whering/Indyx permanecem fixadas. Comparação de mercado não é comparação
de condições de entrada. Recibo 129 (preparação US$ 200) não amplia recibo 128: UM vídeo privado,
US$ 15 novos totais cumulativos, incluindo agentes, revisões e produção pelo Estúdio/Apolo/Plutus.
Nenhuma nova aprovação histórica, campanha ou alteração financeira faz parte desta entrega.

## Resultados

O teste Java original `PdeValidationScopeTest.validatesPreservedResponseWhenProvided` falhou com
a resposta/contexto completos de 899 antes da correção. Depois da correção, passou sem alterar
esses arquivos. A resposta inteira também passou pelos demais validadores reais do executor,
incluindo referências internacionais, Big Idea fixada, identidade, contrato multiagente e entrada
implementada quando aplicável. Nenhum outro problema contratual foi apontado nesses validadores;
isso não é aprovação de mérito comercial nem aceite produtivo da atividade.

O replay somente leitura do backend recompôs o protocolo oficial com o contexto completo,
confirmou equivalência com o protocolo congelado e não apontou obrigação comparativa indevida.
A preparação da recuperação também passou com a auditoria completa original, conservando
resultado, evidência, prompt, status e custo. Nenhum desses três testes enviou callback ou gravou
no banco. Replays completos de 898, 827 e 850 preservaram os caminhos anteriormente aceitos.

A exportação e os hashes estão relacionados em
[`frozen-evidence-hashes.json`](evidencias/atena-escopo-2026-11-06/frozen-evidence-hashes.json).
O SHA-256 da resposta original é
`a664c715079be46132429e6abb8fc16822f22cb8cef2bef61988e7a5d3a0f665`.
As exportações completas permanecem na evidência local privada `.codex/atena-scope-20261106/`;
não publicar prompts integrais com URLs privadas como fixtures do repositório. A regressão comum
é sintética e independe de produto, nome ou identificador de tarefa.

Na auditoria antiga, `evidenceJson.promptVersion` declara v11, enquanto o prompt efetivamente
auditado de 899 é v12. Esse metadado histórico foi preservado. Novos callbacks extraem a versão
do prompt efetivo; a regressão do v13 confere a versão na evidência do callback. O replay não
substitui a auditoria antiga pela atual.

A integração HTTP local do executor cobre duas identidades, aprovação compatível, contradição
recusada, HTTP 503 e recriação do consumidor, com os mesmos bytes e ausência de `modelUsages` ou
novo `executionAudit`. A recuperação no backend conserva custos e contadores ao receber o mesmo
callback duas vezes. Falha de revalidação fica bloqueada; não entra em nova inferência.

A imagem foi construída pelo Dockerfile versionado com a fonte compartilhada e passou no
`validate-bpm-image-local.sh`: dois containers sucessivos, estado persistente, uma execução de
modelo simulado e o mesmo callback após 503. `PASS atena-image-recreation`. Compose, volumes e
tag temporários foram removidos. Não houve envio de imagem ou publicação.

Resultados quantitativos consolidados no [relatório local](evidencias/atena-escopo-2026-11-06/local-report.json):
backend com 5.893 testes em 769 suítes, zero falhas/erros, com 66 casos condicionais ignorados;
worker com 168 testes, zero falhas/erros. Os replays integrais de 899 foram executados
explicitamente e passaram; não estão incluídos nos ignorados. O worker também foi empacotado.
A composição de 5787/5793 passou em 246 testes, com dois replays opcionais ausentes nessa
árvore sintética, sem falhas/erros. Arquitetura: 92 casos incluídos no backend. JavaScript do
worker: oito testes aprovados. Contratos de workflows: 68; compartilhamento: três;
Bash/ShellCheck, YAML, Spotless dos arquivos Java alterados e diff sem erros.

A suíte ampla do backend excedeu a memória de 6 GiB da sandbox (saída 137, sem falha funcional nos casos concluídos).
As classes já concluídas são preservadas e somente as restantes e as regressões finais seguem
em lotes menores com JVM nova. Não reduzir cobertura, ignorar asserções ou usar CI como teste.

Todos os lotes pendentes terminaram com sucesso. Uma compilação concorrente interferiu no
diretório `target` de um lote; as classes estavam presentes após a compilação. A execução foi
serializada e somente os casos afetados/pendentes foram repetidos. O catálogo de harness também
detectou a falta de cadastro do v13: o manifesto vigente v3 foi atualizado e a regressão passou.
O HEAD foi novamente validado nos contratos alterados, no replay completo e nos 92 testes de
arquitetura, após atualizar a base local para `ea6ed0ea6b69f04cf169a74f94bff50c53b01e30`.
A main avançou somente com sete arquivos de pesquisa audiovisual, sem sobreposição com a
correção. Nenhuma mudança de terceiros foi sobrescrita ou publicada por esta solicitação.

## Integração paralela e sequência preparada para publicação/recuperação

PR 5787: HEAD `fe29868900fc693018c46d0e75eaab682c461549`, sem aprovação independente. PR 5793:
HEAD `250f9f0e875895eb6c591df81c0deca3393a944f`, sem revisão registrada. Seus conteúdos foram
aplicados somente a uma árvore temporária local. Sete conflitos de configuração foram conciliados
preservando as duas fontes compartilhadas, seus gatilhos de CI e os contextos Docker. Não houve
conflito na lógica financeira nem alteração de suas branches. Os contratos estáticos dos dois
compartilhamentos passaram; testes Java da composição serão registrados separadamente.

Sequência concreta para uma execução posterior autorizada, sem executar esses passos agora:

1. Revisar este PR e a composição com os HEADs vigentes de 5787/5793. Integrar sem perder os
   includes Maven, fontes compartilhadas e gatilhos de build de ambos. Respeitar revisões e checks;
   a identidade autora não pode representar aprovação independente.
2. Coordenar a publicação de backend/Atena pelo escopo `app` do coordenador, aguardando `ACTIVE`
   e usando `execute` para intervenções autorizadas. As imagens devem vir dos workflows do
   repositório, nunca de SSH ou de build manual produtivo. Registrar `prepare-resume` somente
   com o commit validado e evidência exigida pelo cânone; não deixar publicadores pausados sem
   mecanismo de retomada previsto.
3. Conferir os workflows do SHA integrado e as versões/saúde efetivamente publicadas. O backend
   V2 não entrega essa fila ao executor V1; o executor novo não consome trabalho em backend
   incompatível. Não cancelar inferência em andamento durante a troca.
4. Reconsultar produto 4/cadeia 27/ciclo 21/experimento 114/versão v18, status da tarefa 899,
   processo 126/execução 98, Big Idea v2 e eventual tarefa substituta. Conferir custos/autorizações
   atuais e que não há retomada equivalente em andamento; não criar nova análise.
5. Pela mesa de Atena, na tarefa original, usar o comando existente **Em andamento** somente se
   ainda for elegível. Ele usa `PATCH /api/agent-tasks/899/status`, não cria tarefa nem chama modelo.
   O backend confere o contexto atual antes de mudar o status, marca a recuperação uma vez e a fila `pending` entrega o retorno preservado ao
   executor V2. Este revalida todos os contratos e envia o callback normal sem consumo adicional.
6. Conferir resposta/evidência e contadores/custo originais, callback aceito uma vez, conclusão
   real da atividade 2.1 e disponibilidade de Plutus no mesmo contexto. Se houver outra recusa,
   preservar o bloqueio e o diagnóstico; não repetir inferência ou resetar teto. A pendência
   financeira do PR 5787 continua independente e deve impedir retomada quando aplicável.

Esta entrega termina no PR revisável e no preparo local. Não inclui executar essa sequência,
produzir vídeo, ampliar limite, aprovar histórico ou iniciar mídia/campanha.

## Aprendizado reutilizável e critério de adoção

Atena: a interpretação textual limitada e duplicada recusava uma exclusão válida depois de gerar
a resposta. A hipótese da melhoria é reduzir bloqueios e novas análises desnecessárias, preservando
a qualidade estratégica e o saldo para entregas úteis. A unidade de comprovação local é o parecer
inteiro validado e reaplicável, com nenhuma inferência adicional; não é volume de texto produzido.

| Evidência | Antes | Depois esperado/medido localmente |
| --- | --- | --- |
| Resposta integral 899 | Validador Java recusa a proibição explícita | Os validadores reais aceitam o contrato preservado; status produtivo não alterado |
| Históricos 898, 827 e 850 | Concluídos pelo executor anterior | Permanecem válidos no replay integral |
| Obrigações e contradições | Devem bloquear | Matriz Java comum preserva as recusas, inclusive em listas/campos separados |
| Recuperação HTTP | Atena não consumia o retorno preservado pela fila | V2 revalida e reaplica os bytes, sem tokens/custo novos; segunda recusa terminal |
| Auditoria de versão | Evidência antiga de 899 informa v11, prompt efetivo v12 | Callbacks novos conferem a versão efetivamente enviada; auditoria antiga mantida |

Adotar somente com regressões locais e CI do mesmo HEAD aprovados. Reverter pelo fluxo de PR
se comparação realmente obrigatória for dispensada, se o caminho comparativo válido regredir ou
se replay provocar nova inferência/cobrança. Não desabilitar o gate como compensação. A mudança
versiona política, prompt/schema, catálogo e testes; não grava memória produtiva nem altera pesos
dos agentes. Não há medição comercial suficiente para alegar aumento de vendas ou lucro.

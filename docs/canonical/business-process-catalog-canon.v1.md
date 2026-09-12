# Catálogo canônico de processos do Marketing Hub — v1

## Decisão

### Execução automática por processo — decisão de 12/09/2026

O usuário inicia o **processo**, pelo cabeçalho da tela de atividades do produto. O backend
controla a sequência e só avança após comprovação do objetivo da atividade pelos contratos
canônicos. O painel informa a atividade atual, concluídas, restantes, bloqueios e decisões
necessárias. Fechar a tela não interrompe a execução. Os cards preservam contexto, evidências
e histórico; deixam de exigir disparos individuais para atividades automatizáveis.

Cada execução preserva produto, cadeia, versão do processo, ciclo e referência operacional.
Comandos repetidos não duplicam trabalho. Processos de produtos distintos podem avançar em
paralelo; execuções que compartilham o mesmo contexto são coordenadas para evitar conflitos.
A qualidade prevalece sobre duração: tempo decorrido não comprova conclusão nem autoriza
pular atividade, liberar gate, fabricar evidência ou repetir indefinidamente uma falha.
Correções seguem os retornos canônicos do backend. Decisões humanas, gasto e publicação
conservam seus contratos de autorização. Pausa, retomada e motivos ficam persistidos.

Uma conclusão anterior não certifica provas invalidadas posteriormente. Nesse caso, a leitura
informa **revalidação necessária** e permite retomar explicitamente o processo, preservando
as conclusões anteriores no diário. Subprocessos são revalidados quando exigidos pelo processo
de origem autorizado. O estado mutável da execução só é carregado depois do lock do produto,
inclusive com Open EntityManager in View ativo, para impedir decisões com versão desatualizada.

O backend é a autoridade de progressão; um executor externo pode consultar pendências e
solicitar sua conciliação, mas não escolher, encadear ou disparar atividades por conta própria.
Essa mudança evolui a execução dos processos existentes, preservando suas definições BPM.

O Marketing Hub mantém um cadastro próprio e versionado de processos de negócio. Esse catálogo é a
fonte de verdade para propósito, responsáveis, eventos, atividades, gates, entradas, saídas e relação
com contratos técnicos. A tela canônica é `/business-processes` e a API é
`/api/business-processes`.

Processos de negócio e pipelines não são sinônimos. O processo explica e governa o trabalho ponta a
ponta; pipelines e workers executam contratos técnicos referenciados pelo processo. O catálogo não
faz polling, não consome filas e não avança execuções operacionais.

O processo de descoberta PDE deve começar por uma atividade capaz de receber **sinal humano
observado** sem exigir solução pronta. Conversas, pedidos espontâneos, entrevistas, comentários e
reclamações devem ser registrados de forma anonimizada, separando fala observada, desejo, dor,
tentativa frustrada e inferências. O sinal individual inicia pesquisa; somente confirmação
independente pode transformá-lo em dossiê de oportunidade e permitir o avanço da cadeia.

O catálogo também deve representar o fluxo contínuo de soluções prontas de IA para trabalhos reais,
definido em `docs/canonical/solucoes-prontas-ia-trabalho-canon.v1.md`. Esse fluxo separa cinco objetivos
finais: confirmar grupos de tentativas frustradas; selecionar segmento e solução; construir e aprovar
a solução pronta; comunicar e oferecer o resultado; e realimentar a descoberta com venda, uso e
satisfação. Um exemplo profissional isolado não cria automaticamente um processo vertical nem define
que o produto será PDE.

Para Produtos Digitais Experienciais, a organização macro dos processos deve seguir
`docs/canonical/cadeia-produtos-pde-canon.v1.md`. Processos especializados já publicados, como
fabricação de entregáveis, criativos, landing, homologação e venda, são reutilizados dentro dessa
cadeia e não devem ser confundidos isoladamente com o ciclo completo de criação e venda de um PDE.

Todo processo, subprocesso e atividade deve ser criado e permanecer dentro do BPM de uma Cadeia de
Valor. Processos de valor integram a composição da cadeia; subprocessos e atividades integram o
fluxo de um processo participante. A cadeia é versionada, preserva as versões exatas dos processos
participantes e possui objetivo, resultado e métrica
principal próprios. Ela serve à visão gerencial e não executa, agenda ou avança etapas. A tela
canônica é `/business-process-chains` e seu contrato de leitura é
`/api/business-process-chains`. Quando uma versão de processo pertencer a uma ou mais cadeias, o
detalhe em `/business-processes` deve mostrar cada cadeia e oferecer link direto para sua versão. A
consulta reversa canônica é `GET /api/business-process-chains/by-process/{processDefinitionId}`.

## Alocação obrigatória no BPM da Cadeia de Valor

**Posição e sequência por produto — 09/09/2026:** histórico, atividades e subprocessos devem
consultar o mesmo fluxo persistido do backend, segregado por produto, experimento e cadeia.
Tarefa antiga bloqueada não é uma transição de retorno. Cada retorno deve ter caminho explícito
no BPM e evento com origem, destino, condição, motivo, evidência, responsável e horário.
Entradas históricas, paralelismo e atividades não aplicáveis devem ser modelados explicitamente;
não equivalem a aprovações nem a objetivos concluídos. A numeração do subprocesso segue sua
atividade chamadora, incluindo atividades simples entre as chamadas (ex.: ciclo na 6.4).

**Decisão obrigatória de 08/09/2026, reforçada em 09/09/2026:** todo processo, subprocesso e
atividade deve sempre ser criado e, quando alterado, permanecer alocado e conectado dentro do
BPM da Cadeia de Valor. Essa obrigação começa na concepção e no rascunho, antes da implementação
ou execução. É proibido criar trabalho fora da cadeia para integrá-lo depois. A regra vale para
todos os produtos, agentes, módulos e trabalhos humanos ou automatizados, incluindo ciclos,
decisões, correções, homologações e produção de comunicação ou vídeos.

O objetivo é conectar cada trabalho ao valor entregue ao cliente e à geração de vendas e receita,
com resultado verificável. Quantidade de tarefas concluídas não substitui resultado comercial.

- **Alocação antes de criar:** identificar a cadeia e sua versão, o processo responsável e a
  posição do trabalho no BPM. Reutilizar ou evoluir a definição existente quando atender ao mesmo
  objetivo; uma nova tela, módulo ou agente não justifica um processo paralelo fora da cadeia.
- **Hierarquia explícita:** cada processo de valor integra a composição da cadeia; cada atividade
  pertence ao BPM de um processo ou subprocesso; cada subprocesso é acessado por uma atividade de
  chamada no BPM pai, inclusive quando houver mais de um nível de decomposição. Toda criação ou
  alteração identifica as versões envolvidas. Capacidades existentes são reutilizadas por delegação,
  sem duplicar sua autoridade.
- **Conexão real no BPM:** a atividade precisa estar conectada ao fluxo a partir do início, com
  entradas, saídas, condições de avanço e próximo passo definidos. Subprocessos exigem vínculo com
  o pai e uma atividade de chamada com `subprocessCode` conectada ao diagrama do pai, com condição
  de entrada e destino após conclusão ou bloqueio. Cadastro, lista de subprocessos, documento,
  painel ou link isolado não satisfazem essa obrigação.
- **Decisões e retornos:** gates e loops devem declarar condições, evidências, destino de correção
  ou continuidade e encerramento. Reprovação precisa orientar a próxima atividade e quem a executa.
  O backend governa a liberação; o desenho do retorno não autoriza avanço nem execução por si só.
- **Orientação executável:** cada atividade declara objetivo, responsável, entradas, entrega,
  critério de conclusão, bloqueios e ação disponível ao usuário. Telas especializadas, estúdios e
  painéis permanecem recursos da atividade e devem ser acessíveis pelo BPM preservando seu contexto.
- **Entrada única por atividade de chamada (09/09/2026):** na tela de atividades do produto,
  subprocessos aparecem na sequência do pai como atividades que abrem seu fluxo especializado.
  A chamada apresenta o andamento persistido, a próxima ação e o destino contextual do subprocesso.
  Não repetir o ciclo em um painel independente acima das atividades. Ao entrar, mostrar o processo
  e a atividade de origem e oferecer retorno a essa atividade; não confundir ausência de tarefa de
  agente no pai com ausência de execução registrada no subprocesso.
- **Identificação para o usuário:** a tela de atividades deve mostrar, junto ao nome, o número oficial
  do processo ou subprocesso dentro da Cadeia de Valor (por exemplo, `Processo 6` ou `Processo 6.1`).
  O número vem do backend e não pode ser confundido com a versão técnica ou com o identificador do banco.
- **Copiar contexto da atividade (10/09/2026):** cada card da tela de atividades do produto
  oferece um ícone de copiar junto ao título. Um clique copia processo (número e nome),
  atividade (número e nome), produto e agente pelos nomes internos e ciclo quando houver.
  O texto inclui em linhas próprias a versão e o ID da definição do processo e a versão e o
  ID da definição da atividade, além do link para o próprio card, conservando a cadeia e o
  ciclo efetivamente informados pelo backend; experimento e versão do produto acompanham o
  ciclo. A atividade herda a versão imutável do processo que contém sua definição; registros
  históricos sem definição disponível exibem essa ausência e nunca usam o número ordinal da
  atividade como versão. Ausências são explícitas, sem substituir nome interno por nome
  comercial nem confundir responsável humano/backend com agente. A cópia confirma sucesso
  junto ao ícone e oferece seleção manual em caso de bloqueio, inclusive no acesso HTTP. Essa
  ação não executa atividade, cria tarefa nem altera o BPM.
- **Rastreabilidade:** tarefas e resultados se vinculam à atividade e à versão do processo dentro
  da cadeia, com produto, experimento e ciclo quando aplicáveis. Execução independente de produto
  continua pertencendo ao BPM da cadeia; não se deve inventar produto para iniciar descoberta.
- **Evolução e histórico:** ao criar ou alterar trabalho, atualizar de forma coerente o cânone,
  a definição BPM, sua composição na cadeia, a navegação e os contratos e testes afetados. Publicados
  permanecem imutáveis; mudanças usam novas versões e preservam tarefas e evidências anteriores.
  Itens legados sem alocação devem ser regularizados quando alterados, sem fabricar execução passada.

**Critério de entrega:** uma criação ou alteração de processo, subprocesso ou atividade só está
completa quando é possível partir da Cadeia de Valor, localizar o trabalho no BPM, compreender sua
entrada, executar o comando previsto e identificar sua conclusão, bloqueio ou próximo passo. Para
subprocessos, a validação inclui entrar pela atividade chamadora, consultar seu andamento e voltar
à origem, com continuidade definida no fluxo. Sem essa integração, a entrega está incompleta.
Rascunhos também devem nascer alocados na cadeia e no BPM; podem aguardar publicação conjunta,
mas não orientar execução enquanto a integração estiver pendente. Essa regra de organização não
concede autorização de publicação comercial, gasto ou escala.

**Aprendizado que fundamenta o reforço:** no ciclo de vendas, o vínculo cadastral com o processo pai
não impediu uma entrada separada e confusa. O caso está registrado em
[LOOP-BPM-CICLO-SEM-CHAMADA-DO-PAI](../registros/loops.md#loop-bpm-ciclo-sem-chamada-do-pai--ciclo-fora-do-fluxo-de-valor).
A prevenção exige conjuntamente alocação, chamada conectada, andamento persistido e navegação de
entrada e retorno pela atividade. Essa exigência vale para qualquer novo processo, subprocesso ou
atividade, e para alterações nos existentes.

## Governança BPM

**Integração operacional do ciclo (08/09/2026):** o subprocesso de ciclos pertence ao
processo de venda e aprendizado. Seu pai precisa conter uma atividade `subprocessCode`
conectada ao fluxo, além do vínculo de cadastro. A cadeia oferece acesso pela atividade chamadora
nesse pai, com andamento e retorno contextual.
O endpoint `GET /api/business-process-chains/learning-cycles/v1/entry` resolve a entrada
e a ocorrência aberta, sem mutação. Destinos de orientação `learningCycleReturns` no
diagrama são resolvidos nas versões exatas da cadeia; a execução usa os comandos e a
atividade orientada do ciclo. Ausência de vínculo não pode ser inferida como integração.

As cadeias incorporam **Ciclos de aprendizado e vendas**, conforme
`ciclos-aprendizado-vendas-canon.v1.md`. A definição descreve o losango e os retornos; a execução
versionada por experimento persiste conhecimento, decisões e liberação de etapas no backend.
As versões publicadas anteriores e suas tarefas permanecem imutáveis.

### Modelo operacional explícito

O catálogo e a execução adotam três níveis distintos e persistidos:

1. **Atividade:** definição versionada do trabalho dentro de uma versão de processo. Preserva o
   identificador estável no grafo, nome, objetivo, responsável, recurso ou subprocesso delegado e
   critérios de conclusão. O `diagram_json` continua sendo a fonte da topologia e dos fluxos, mas
   cada nó `TASK` também possui identidade relacional própria e não pode ser tratado apenas como
   texto embutido no JSON.
2. **Instância da atividade:** ocorrência da atividade para uma referência operacional, como um
   produto, experimento ou landing. Consolida situação, entrada, saída, objetivo atingido, bloqueio,
   custo conhecido e cobertura financeira. Uma nova tentativa da mesma execução não cria outra
   instância; retorno funcional ou novo ciclo comprovado cria nova ocorrência auditável.
3. **Tarefa/execução:** tentativa individual atribuída a um agente ou executor dentro da instância.
   Preserva request, response, evidências, erro, datas, consumo e custo próprios. Uma instância pode
   possuir zero tarefas antes de ser liberada e uma ou mais tarefas durante execução, revisão,
   correção ou reprocessamento; cada tarefa regular pertence exatamente a uma instância.

A instância, e não uma tarefa isolada, é a autoridade do estado operacional da atividade. O backend
abre e atualiza a instância ao criar, reservar, concluir, bloquear ou refazer tarefas e deriva dela o
histórico apresentado ao usuário. A conclusão de uma tarefa só encerra a instância quando os
critérios funcionais da atividade forem satisfeitos; tentativas anteriores continuam auditáveis e
compõem custo e retrabalho. Para o mesmo responsável, a tentativa mais recente substitui o estado
operacional da anterior, sem apagar seu custo ou evidência; assim, uma correção bem-sucedida encerra
o bloqueio anterior. Uma tarefa criada depois de a instância estar concluída ou cancelada abre nova
ocorrência, preservando integralmente o ciclo encerrado. Tarefas excepcionais e registros históricos incompletos permanecem
legíveis como legado até poderem ser vinculados sem fabricar dados.

Todo fluxo especializado que persista uma execução em tabela própria deve também materializar e
sincronizar a instância e a tarefa BPM reais no momento em que o backend abre, entrega, conclui ou
bloqueia o trabalho. A referência de origem deve ser estável e vincular a entidade técnica à versão
de processo e à atividade vigentes. A tabela especializada pode preservar detalhes adicionais, mas
não substitui `business_process_activity_instance` e `agent_task`; o frontend não pode reconstruir
nem sintetizar tarefas a partir dela. O histórico por atividade deve representar a execução
persistida, inclusive falhas, modelo, prompt, consumo disponível, datas, resultado e evidências, sem
converter atividade técnica em sucesso funcional.

Quando uma única tarefa de descoberta PDE executar ampliação controlada de mercado, as rodadas não
criam novas tarefas nem novas instâncias. O relatório persistido deve expor, em ordem, a lente
investigativa, a justificativa, a evidência incremental, a maturidade obtida e o motivo de continuar
ou parar em cada tentativa. O callback terminal continua único e o backend permanece a autoridade
exclusiva para liberar Atena ou encerrar a atividade com lacunas.

Se uma versão nova for publicada durante uma execução já aberta, seus callbacks devem continuar na
tarefa da versão original. Renomeações de atividade entre versões só podem ser correlacionadas por
aliases explícitos do fluxo, preservando uma única execução para a mesma referência de origem.

- Cada processo possui código estável e versões imutáveis depois de publicadas.
- Uma versão nasce `DRAFT`; somente publicação explícita a torna `PUBLISHED`.
- Nomes equivalentes, desconsiderando caixa, acentos, espaços e pontuação, não podem criar processos
  com códigos diferentes; o usuário deve criar uma versão do processo canônico já existente.
- Uma versão `DRAFT` pode ser excluída somente quando não possui tarefa operacional vinculada.
  Versões publicadas, aposentadas ou utilizadas nunca podem ser excluídas pelo cadastro.
- Todos os metadados, elementos BPM e fluxos de uma versão `DRAFT` podem ser editados no Marketing
  Hub. Editar uma versão publicada cria uma nova versão em rascunho; versões `PUBLISHED` e `RETIRED`
  nunca são alteradas diretamente.
- Ao publicar uma nova versão, a anterior passa a `RETIRED`, preservando histórico e auditoria.
- O catálogo operacional em `/business-processes` mostra somente versões `DRAFT` e `PUBLISHED`.
  Versões `RETIRED` ficam na tela histórica `/business-processes/retired`, acessível pelo catálogo,
  para não competir visualmente com os processos atuais sem apagar sua rastreabilidade.
- O diagrama é persistido como grafo estruturado, não como imagem ou XML livre.
- Todo grafo precisa de exatamente um evento inicial, um final e fluxos entre elementos existentes.
- Fluxos marcados com `kind=REWORK` documentam visualmente um retorno condicional, mas não entram no
  cálculo genérico de predecessoras nem podem criar ciclo de liberação. A condição, a causa
  persistida e a atividade que volta a ficar disponível devem ser governadas por um contrato de
  prontidão do backend.
- A tela renderiza o grafo persistido pelo backend e não infere status ou regra de negócio.
- As superfícies de processo identificam nomes de processo e subprocesso com o ícone canônico de
  fluxo, e nomes de atividades `TASK` com o ícone canônico de atividade. Texto e ícone devem aparecer
  juntos para manter clareza, acessibilidade e distinção consistente entre os dois níveis.
- Publicar uma definição não publica landing, campanha, oferta ou conteúdo e não autoriza gasto.

## Commits por produto e processo

Toda alteração versionada realizada para um produto durante um processo ou subprocesso deve ser
registrada de forma estruturada contra o `product_id` e a versão exata da
`business_process_definition`. O registro deve preservar, no mínimo, repositório, SHA completo do
commit, resumo funcional, responsável e data do registro. O mesmo commit pode atender mais de um
produto ou processo somente quando cada vínculo for declarado separadamente; inferência por nome,
status comercial, branch, PR ou commit atual da build é proibida.

O histórico da cadeia de valor do produto é a superfície administrativa canônica para consultar e
registrar esses vínculos. A inclusão deve ser idempotente para a combinação produto, processo,
repositório e SHA, sem apagar vínculos anteriores quando o produto avançar de processo. Commits são
evidência de implementação, não evidência de venda, qualidade, deploy ou objetivo funcional atingido;
o processo continua sujeito aos próprios gates e critérios de conclusão.

Os contratos canônicos são `GET /api/products/{productId}/process-commits` para consulta,
`GET /api/products/{productId}/process-commits/{commitId}` para detalhe segregado e
`POST /api/products/{productId}/process-commits` para registro. O backend deve rejeitar produto,
processo ou SHA inválidos e impedir que a tela vincule silenciosamente um commit a um processo fora
do histórico conhecido daquele produto.

## Situação das atividades por produto e processo

O histórico da cadeia de valor do produto deve oferecer, em cada processo ou subprocesso, acesso a
uma visão gerencial de **Atividades e tarefas**. Essa visão precisa explicar em linguagem de negócio
o estado geral, quantas atividades tiveram o objetivo comprovado, qual atividade exige atenção, a
causa persistida e tudo que ainda falta para concluir a versão selecionada. A lista técnica de
tarefas permanece disponível abaixo desse resumo, mas não pode ser a única forma de reconstruir a
situação.

O histórico principal mostra toda a cadeia publicada em ordem hierárquica, mesmo quando um processo
ainda não possui execução. Processos sem evidência aparecem como planejados, sem datas, custo,
objetivo atingido ou permissão para registrar commit; somente o backend decide essa disponibilidade.
Assim, uma etapa concluída nunca encerra visualmente a cadeia enquanto houver processos canônicos
seguintes.

A primeira renderização dessa superfície deve consultar somente a identidade do produto e sua
posição atual na cadeia publicada. A trilha completa de processos, subprocessos, custos, commits e
tarefas históricas deve ser consultada pelo backend apenas após solicitação explícita do usuário.
Depois de carregado, o histórico continua mostrando toda a cadeia publicada e toda ausência de
evidência de forma explícita; carregamento sob demanda não autoriza cache de verdade de negócio,
paginação local de dados incompletos nem inferência no frontend. As consultas de tarefas por origem
devem possuir índice compatível com MySQL 5.7 para que o custo não cresça como varredura integral da
tabela.

O contrato canônico é
`GET /api/business-processes/{processDefinitionId}/products/{productId}/activity-executions`. O
backend, e não o frontend, calcula a situação de cada atividade e do processo. A instância BPM é a
autoridade quando existir. Uma tarefa composta somente comprova outra atividade quando houver
vínculo relacional em `agent_task_activity_coverage`; nesse caso a tela identifica explicitamente a
cobertura. Resultado técnico embutido em comentário, prompt ou JSON não conclui outra atividade sem
instância ou cobertura persistida. Ausência de ambos deve aparecer como atividade não iniciada, sem
inferir sucesso a partir do estado comercial, da landing, do experimento ou de outra tarefa.

Nos cards e no resumo de atividades, o ícone de **Em execução** gira apenas enquanto o contrato
informar `IN_PROGRESS`. O rótulo permanece legível e estático; os demais estados não giram.
A preferência de movimento reduzido do navegador desativa a animação, preservando ícone e texto.
O movimento apresenta o estado recebido, sem indicar percentual de progresso nem avanço do BPM.

Quando uma atividade ou um subprocesso atingir o objetivo, a mesma resposta do backend deve expor
o próximo passo oficial da composição publicada. Se o subprocesso concluído retornar ao processo
pai, a próxima atividade do pai deve aparecer como continuação prevista, sem fabricar entrada,
execução ou conclusão. Uma instância BPM concluída prevalece sobre tarefas bloqueadas de tentativas
anteriores preservadas apenas para auditoria; o frontend não pode reconstruir essa precedência.

Atividades de versões históricas continuam auditáveis, mas não entram no denominador de conclusão
da versão selecionada. Retentativas anteriores permanecem visíveis; o resumo usa a ocorrência BPM
mais recente da própria atividade dentro da referência de execução mais recente do produto e nunca
soma novamente custo ou tarefa composta. Uma execução antiga concluída não pode esconder bloqueio,
pendência ou ausência de trabalho do ciclo atual.

Cada atividade nova de agente deve declarar exatamente uma chave em `responsibleAgentKeys` e o
`responsibilityDomain` compatível com a matriz canônica. Pareceres diferentes sobre o mesmo artefato
devem ser atividades separadas, com identificadores, resultados, evidências e critérios próprios;
Psique e Têmis nunca são coautoras da mesma atividade. Definições históricas com múltiplas chaves
continuam legíveis e executáveis somente para preservar auditoria, mas não podem ser republicadas ou
usadas como modelo de nova versão. O comando canônico da tela do produto é
`POST /api/business-processes/{processDefinitionId}/products/{productId}/activities/{activityId}/execution-requests`.
Ele somente aceita versão `PUBLISHED`, produto em `PLAY`, atividade ainda não iniciada ou
`BLOCKED` ou `CANCELLED` e experimento do próprio produto. Uma atividade `BLOCKED` ou `CANCELLED` deve expor na tela o comando explícito
**Reiniciar tarefa**, desde que seus validadores de prontidão atuais permitam a nova tentativa. O
reinício após `BLOCKED` cria uma tentativa `PENDING` na mesma instância e referência operacional, preserva a tarefa
bloqueada, seu erro, evidências, consumo e custo, e não reabre atividade concluída nem trabalho ainda
`PENDING` ou `IN_PROGRESS`. Repetir o comando enquanto a nova tentativa estiver ativa reutiliza a
tarefa existente e não duplica custo nem execução. O backend expõe a disponibilidade e o motivo; o
frontend apenas apresenta essa verdade e nunca transforma bloqueio em permissão por inferência.

Após cancelamento, a nova tentativa abre **nova ocorrência** da mesma atividade, no mesmo
processo, produto e referência do ciclo. A ocorrência cancelada permanece encerrada, com motivo,
horários, tarefas e consumo preservados. Cancelar não significa atingir o objetivo nem dispensar
os validadores atuais, o estado `PLAY` ou as predecessoras do BPM. A disponibilidade na leitura
e a validação do comando devem usar a mesma regra; uma ocorrência cancelada não pode deixar a
atividade sem saída operacional quando todos os demais critérios permitirem a retomada.

### Controle padronizado de execução das atividades

O comando assíncrono deve confirmar a tarefa no card da atividade responsável, com número,
responsável e andamento automático. Por ajuste de 2026-09-11 (Vega, tarefa 385), a referência de
recuperação é uma dependência, não uma tarefa da atividade que aguarda. O card dependente oferece
um link para o número, nome e responsável da atividade de correção, preservando produto, processo,
ciclo e cadeia. Criação, confirmação e acompanhamento da correção ficam somente na atividade de
origem. Tarefas e pareceres próprios dos cards dependentes continuam visíveis; ausência de tarefa
continua explícita. Referências de recuperação não aumentam contagem, custo ou conclusão e deixam
de ser oferecidas quando o objetivo da correção estiver atingido. Isso não reclassifica coberturas
compostas históricas realmente persistidas.
Erros de envio e de atualização aparecem junto ao comando; reler o histórico não reposiciona a
página nem apaga a última situação conhecida.
`GET /api/business-processes/{processDefinitionId}/products/{productId}/execution-progress`
consulta somente ID, estado e instante de alteração das tarefas, filtrados pela referência
exata e pela versão do processo. O backend valida a pertença da referência ao produto. A tela
reconsulta a auditoria e a orientação do ciclo ao detectar mudança nessa projeção ou ao
recuperar uma leitura que falhou. Uma falha não pode fixar indefinidamente o estado antigo:
a consulta seguinte deve tentar recuperar o resultado mesmo com a revisão leve inalterada.
Concluir a tarefa e atingir o objetivo continuam sendo fatos distintos quando o domínio
mantiver alguma exigência pendente.

Na operação de experimento, a leitura e o comando do produto devem respeitar o experimento
selecionado no plano vigente (`IN_PROGRESS` ou `BLOCKED`) quando ele pertencer ao produto e já
tiver saído de `PLANNED`. Isso permite reconciliar um ciclo pausado sem substituí-lo por um piloto
antigo ainda em `RUNNING`. Sem seleção elegível, permanece o fallback para experimento em execução
ou já operado. Tentativas e instâncias de outras referências continuam históricas e não são
migradas nem concluídas por essa seleção. O roteiro particular do produto pertence ao seu plano;
ele não modifica a topologia compartilhada nem cria gates automáticos somente por descrever etapas.

Toda atividade exibida na visão de produto deve receber do backend um `executionControl`, inclusive
quando ainda não existe comando manual seguro. Esse contrato é a fonte de verdade para responsável,
tipo de interação, disponibilidade, causa, pré-requisitos, confirmação, área operacional e
subprocesso de destino. O frontend não identifica execução por nome da atividade, `owner`, código do
processo ou heurística local.

Os tipos canônicos são:

- `COMMAND`: abre tarefa de agente ou executa comando determinístico do backend pelo endpoint
  canônico da atividade;
- `WORKSPACE`: combina o comando backend com uma área operacional oficial, como run e homologação de
  experimento, sem criar tentativa paralela;
- `SUBPROCESS`: abre a versão publicada do subprocesso responsável; o backend conclui a atividade pai
  somente a partir do resultado persistido do filho;
- `APPROVAL`: coleta decisão humana explícita e auditável;
- `AUTOMATIC`: explica qual evento técnico o backend aguarda e não fabrica botão manual inseguro;
- `STATUS`: preserva histórico ou ausência de contrato sem sugerir execução inexistente.

O mesmo `POST
/api/business-processes/{processDefinitionId}/products/{productId}/activities/{activityId}/execution-requests`
continua sendo o único comando mutável da tela. Atividades de agente e backend podem enviar corpo
vazio. Uma atividade `APPROVAL` deve enviar `decision`, `operatorName`, `justification`,
`evidenceReference` e o `confirmationToken` específico devolvido pelo backend. A decisão aceita é
`APPROVE` ou `REJECT`; texto incompleto, confirmação de outra atividade, versão não publicada,
produto em `STOP`, atividade fora de ordem ou tentativa ativa são rejeitados antes de qualquer
efeito.

A instância BPM preserva decisão, responsável informado, justificativa, evidência, confirmação e
data. Aprovação conclui o objetivo; reprovação bloqueia com causa e permite nova ocorrência sem apagar
a anterior. Quando a aprovação tiver efeito de domínio — ativação, publicação, gasto ou outra mudança
material — um handler backend específico deve validar os requisitos persistidos e aplicar o efeito na
mesma transação antes de concluir a instância. Ausência de handler especializado permite somente o
registro da autorização descrita pela própria atividade; nunca autoriza efeito externo implícito.
Antes do efeito, o backend reserva e persiste a ocorrência em andamento; a chave única da atividade,
referência e ocorrência impede duas decisões concorrentes de aplicar o mesmo efeito. Uma referência
`experiment:<id>` sempre resolve exatamente esse experimento e valida sua pertença ao produto; é
proibido substituir silenciosamente pelo experimento mais recente.

Na homologação comercial PDE, `preflight` usa o workspace do run produtivo oficial. Criar run,
executar gates e registrar homologação atualizam automaticamente a instância BPM; um run pendente não
pode ser reexecutado de forma que apague evidências. `authorization` expõe todos os requisitos de
`RUNNING`, exige teto financeiro positivo e uma confirmação que informa experimento, amostra e teto.
A ação não cria campanha paga. Um run concluído por uma superfície anterior pode ser reconciliado
com a atividade sem reexecutar ou apagar gates. Em abordagem individual de produto PDE, a
reconciliação libera a aprovação somente quando o run produtivo possui `PASS` com evidência para
página, checkout/entrega, canal consentido e qualidade dos dados. Ocorrências bloqueadas do processo
anterior não são reescritas; quando o preflight posterior comprovar o mesmo objetivo, o período do
macroprocesso registra `AUDITED_PRODUCTION_PREFLIGHT` como evidência explícita da reconciliação. Ao
aprovar, a mesma transação atualiza experimento, run, janela comercial, produto e instância BPM;
qualquer falha preserva integralmente o estado anterior.
O backend mantém atividades backend sem comando explícito como `AUTOMATIC`, mostrando o evento
aguardado em vez de permitir conclusão manual sem evidência.

## Execuções independentes de produto

Cada versão de processo declara explicitamente `executionScope`, sem inferência por nome, posição na
cadeia, agente responsável ou ausência momentânea de produto:

- `PRODUCT`: a execução exige um produto e usa a superfície da cadeia de valor;
- `INDEPENDENT`: a execução nasce antes ou fora de qualquer produto e usa uma referência operacional
  própria;
- `PRODUCT_OR_INDEPENDENT`: a mesma definição admite os dois contextos, que permanecem segregados em
  cada execução.

O processo `pde-opportunity-discovery` é `INDEPENDENT`: uma pergunta real de mercado inicia o ciclo,
e somente uma oportunidade factual aprovada poderá originar produto posteriormente. Vincular produto
apenas para conseguir disparar Argos inverte a cadeia causal e é proibido.

`INDEPENDENT` descreve ausência de produto na execução, não ausência de alocação na Cadeia de Valor.
Sua definição, atividades e entrada operacional seguem a mesma regra obrigatória de integração ao
BPM; a tela independente funciona como recurso de execução do processo correspondente.

A tela canônica de início e acompanhamento é `/business-process-executions`. Ela lista somente versões
publicadas com escopo independente, apresenta os campos de entrada declarados pelo backend e inicia a
execução por `POST /api/independent-business-process-executions`. A tela não cria tarefas diretamente,
não escolhe a próxima atividade e não conhece endpoints de workers. O backend seleciona o adaptador
operacional do processo, persiste a solicitação idempotente, cria a entidade técnica canônica e devolve
a referência que correlaciona processo, atividades, tarefas, entradas, saídas, evidências e custos.

O catálogo operacional é
`GET /api/independent-business-process-executions/catalog`; o histórico é
`GET /api/independent-business-process-executions`; e o detalhe auditável é
`GET /api/independent-business-process-executions/{executionId}`. Processo sem adaptador backend
compatível continua visível com a causa da indisponibilidade, mas não pode ser disparado. Repetir o
mesmo `requestKey` devolve a execução já criada e nunca abre outro ciclo ou duplica consumo de modelo.

O histórico abre com dez execuções recentes e busca páginas anteriores somente por comando explícito
do usuário, usando `limit` e o cursor `beforeId`. A listagem deve projetar em lote apenas identidade,
estado, progresso, consumo, horários e causa persistida; prompts, resultados, evidências, diagramas e
relatórios especializados completos pertencem exclusivamente ao endpoint de detalhe. Enquanto existir
execução ativa, a atualização automática consulta apenas essa projeção paginada e nunca pode hidratar
campos `LONGTEXT` para reconstruir os cards.

Cada item do histórico deve abrir a rota dedicada
`/business-process-executions/{executionId}`. Uma execução terminal bloqueada deve mostrar já no
histórico a causa literal consolidada pelo backend e, no detalhe, estado, horários, entrada,
atividades, tarefas, erros, evidências, custos e a orientação de retentativa. Selecionar um item e
renderizar seu conteúdo apenas abaixo do formulário não atende ao contrato de visibilidade. A nova
tentativa preserva a ocorrência bloqueada e reutiliza a mesma entrada em uma nova solicitação
idempotente; a tela não pode sobrescrever histórico nem inferir uma causa diferente da persistida.
Cada tarefa do detalhe independente apresenta o mesmo bloco auditável do histórico por atividade:
origem, ausência explícita de produto, versão, estado, marcos e duração, modo, modelo, esforço de
raciocínio, tokens, custo e as partes persistidas do prompt completo. Esses dados pertencem ao mesmo
endpoint de detalhe e não podem ser reconstruídos por uma segunda consulta ou por heurística visual.

Status, progresso e custos exibidos são consolidados pelo backend a partir das instâncias e tarefas
persistidas. Ausência de medição permanece ausente; execução, aprovação ou documento produzido não é
venda. A entrada independente não pode conter `productId`, `experimentId` ou associação artificial a
produto apenas para satisfazer um contrato legado.

## Recursos especializados por atividade

Uma atividade `TASK` pode declarar opcionalmente `executionResourceCode` quando o agente precisar de
uma capacidade que não pertence ao seu executor comum. O valor não é texto livre: deve apontar para
um recurso ativo do catálogo persistido `business_process_execution_resource`, consultado pela tela
em `GET /api/business-process-execution-resources`.

Cada recurso informa código estável, nome, tipo, agente responsável, referência do executor e
instruções de uso. `iris-communication-worker` é o executor comum de Íris e materializa contratos,
copy, peças estruturadas e landing. `pde-visual-materialization` e o código técnico legado
`themis-image-studio` são aliases históricos cuja propriedade vigente é Íris e cujo executor é
`iris-image-studio`; aceitam somente imagem comercial apoiada em prova real aprovada. O nome legado
não atribui autoria a Têmis, e esses recursos não podem criar `DELIVERY` nem `PRODUCT_PROOF`.
`video-management-service` pertence a Apolo. Atividades sem recurso
especializado seguem o executor normal do agente.

O backend valida o recurso ao salvar e publicar a definição e exige desde esse momento que o único
agente da atividade seja o proprietário do recurso. Quando uma tarefa é vinculada, repete a mesma
confirmação. O contrato `pending` entrega ao executor o
objeto `executionResource` completo. Um executor comum consulta a fila sem código de recurso e não
pode reservar atividade especializada; o executor próprio deve informar `executionResourceCode` e
só recebe atividades com correspondência exata. Recurso ausente, inativo ou atribuído a outro agente
bloqueia a execução antes do trabalho e do consumo de modelo.

O recurso não altera a regra de orquestração: o container consome pendência e reporta resultado
somente pelo backend; não chama outro executor nem decide a próxima atividade. Como versões
publicadas são imutáveis, adicionar, trocar ou remover um recurso exige nova versão do processo.

## Documentos gerados por atividade

Quando o objetivo de uma atividade `TASK` produzir um documento, a definição pode declarar
`documentOutput.label` com o nome funcional desse documento. A tela BPM apresenta no próprio
objetivo um link para os **dez documentos concluídos mais recentes** daquela atividade.
Quando o processo já possuir qualquer saída documental, seu objetivo principal também oferece uma
visão consolidada dos dez documentos mais recentes da versão inteira.

O histórico documental não cria uma persistência paralela: cada documento corresponde ao resultado
e às evidências já auditados na tarefa da atividade. A consulta preserva tarefa, origem, agente,
horário, tokens e custo estimado, sempre segregados pela versão exata do processo e pelo identificador
da atividade. Atividades legadas que já possuem resultados concluídos também recebem o link, mesmo
antes de uma nova versão declarar o rótulo explícito.

Somente atividades `TASK` podem declarar saída documental. Gates e eventos não geram tarefas e não
podem assumir autoria de documentos. A API canônica é
`GET /api/business-processes/{processDefinitionId}/activities/{activityId}/documents`; ela limita a
resposta a dez itens ordenados do mais recente para o mais antigo. Conteúdo de outro processo,
atividade, tarefa bloqueada ou execução incompleta não pode aparecer nesse histórico.
O consolidado do objetivo principal usa
`GET /api/business-processes/{processDefinitionId}/documents` e aplica o mesmo limite e segregação.

## Histórico recente de execução por atividade

Todo nó `TASK` do diagrama BPM oferece acesso às **dez tarefas mais recentes** da atividade. A
consulta usa o `process_code` canônico e o identificador estável da atividade para preservar o
histórico válido mesmo quando o usuário abriu uma versão aposentada ou quando uma nova versão do
mesmo processo foi publicada. Cada item informa obrigatoriamente a versão exata que originou a
tarefa; processos com código diferente nunca podem ser misturados.

O histórico inclui tarefas pendentes, em andamento, concluídas, bloqueadas e canceladas. A tela
apresenta somente dados persistidos pelo backend: criação, início, término, duração derivável desses
marcos, responsável, status, origem, nome interno do produto quando houver vínculo canônico, prompt
enviado, resultado/comentários, evidências, falha, modelo, esforço de raciocínio, tokens e custo
estimado. Ausência de medição ou de auditoria legada deve aparecer como não registrada, nunca como
zero, valor padrão ou inferência do frontend. JSON persistido pode ser explorado em árvore sem
alterar seu conteúdo original.

A API canônica é
`GET /api/business-processes/{processDefinitionId}/activities/{activityId}/executions`. O backend
valida que a versão selecionada existe e que `activityId` corresponde a um nó `TASK`, limita a dez
itens no banco e ordena da tarefa mais recente para a mais antiga.

Quando uma única tarefa técnica realmente cobrir mais de uma atividade do mesmo processo, essa
cobertura deve ser persistida de forma relacional em `agent_task_activity_coverage`. A mesma tarefa
real pode então aparecer no histórico de cada atividade coberta, preservando um único prompt,
resultado, intervalo, consumo e custo. É proibido ao frontend inferir a cobertura por título, copiar
a tarefa para fabricar execuções ou somar novamente seu custo. Nas versões históricas do GeraLanding,
a homologação composta de Dédalo cobre seleção de provas, estratégia, composição e HTML; a chamada
técnica correlacionada por `agent-task:<id>` é a fonte do prompt, parecer, modelo e duração exibidos.
Nas versões novas, as quatro atividades pertencem a Íris e cada tarefa preserva seu próprio contrato;
o HTML aguarda Quality Review antes de concluir a atividade final.

## Primeiro processo: Geração de landing page

A versão histórica 1 formaliza o ciclo:

`briefing → Dédalo → validação técnica → Psique → Têmis → aprovação humana`.

A versão vigente formaliza:

`produto e provas de Dédalo → Íris → validação técnica → Psique → Têmis → aprovação humana`.

Na execução operacional da versão histórica, o worker de Dédalo deve reservar primeiro a atividade liberada pelo
endpoint BPM canônico e materializá-la, de forma idempotente, na fila técnica do GeraLanding. Uma
lease `IN_PROGRESS` sem resultado deve ser reoferecida ao mesmo executor após reinício. A conclusão
ou falha técnica atualiza a própria tarefa BPM antes de qualquer sucessora ficar elegível; é proibido
liberar Psique ou Têmis apenas pelo recebimento da tarefa de Dédalo.

Na versão vigente, o `communication-agent-worker` reserva cada atividade de Íris diretamente no
endpoint BPM `pending`; o callback de HTML abre o Quality Review da mesma versão e deixa a tarefa em
andamento. Aprovação técnica conclui a tarefa de Íris; reprovação a bloqueia com causa persistida.
Somente o backend libera Psique, Têmis ou uma retentativa.

Os executores de Psique e Têmis devem consumir as atividades liberadas pela fila BPM canônica,
produzir parecer estruturado e reportar resultado e evidências na própria tarefa. Psique não pode
liberar Têmis, e Têmis não pode alterar o experimento ou publicar ativos; somente o backend calcula
as predecessoras concluídas e libera a próxima atividade.

Reprovações de comunicação retornam a Íris; reprovações do produto ou de sua prova funcional
retornam a Dédalo, sempre com causa persistida e nova versão. O backend do experimento continua sendo
a autoridade das transições operacionais.
A aprovação humana continua obrigatória antes da publicação. A referência técnica vigente é o bloco
GeraLanding do `experiment-pipeline`, definido em
`docs/canonical/procedimento-experimento-canon.v1.md`.

## Métricas

O catálogo deve permitir medir cobertura de responsáveis, quantidade de gates e versões. Quando o
cadastro for vinculado a execuções reais, deverá medir também tempo por etapa, retrabalho, custo por
landing aprovada, reincidência de causa, CTA, checkout e vendas, sem confundir publicação da definição
com resultado comercial.

# Vínculo operacional com a Mesa de Entrada

Toda nova tarefa humana enviada pela Mesa de Entrada deve apontar para uma atividade `TASK` de uma versão `PUBLISHED` e para o agente responsável definido nessa atividade. O backend valida a versão, o tipo do elemento e o responsável; rascunhos não podem orientar trabalho operacional.

Quando `responsibleAgentKeys` estiver presente, a validação de responsabilidade aceita somente os
agentes listados e a orquestração considera todos os coautores obrigatórios. O texto de `owner`
continua legível para o usuário, mas não substitui a identidade técnica versionada dos executores.

Cada tarefa deve preservar dois marcos temporais canônicos: `received_at`, registrado somente quando o executor reserva a atividade pelo endpoint `pending`, e `delivered_at`, registrado uma única vez quando o callback entrega resultado e evidências. Criação, atualizações, bloqueios e cancelamentos não podem fabricar ou sobrescrever esses marcos.

O contrato operacional canônico é `/api/internal/agent-tasks/<agentKey>/stage-executions/pending`. O backend libera somente atividades cujas predecessoras `TASK` da mesma versão de processo e referência de execução estejam concluídas e cujo recurso opcional corresponda ao executor solicitante. O executor reporta resultado ou falha pelos callbacks da execução; resultado e evidências ficam persistidos na tarefa. O executor nunca escolhe nem dispara a próxima atividade.

Toda tarefa que consumir modelo de IA deve persistir, no callback de resultado ou falha, o consumo
real informado pelo provedor: tokens totais de entrada, parcela da entrada atendida por cache e
tokens de saída. O executor informa modelo, tier e contadores; o backend é a autoridade do preço e
calcula o custo estimado em USD com o catálogo canônico vigente naquele instante. Como tokens de
cache fazem parte da entrada total, somente a parcela `entrada - cache` usa a tarifa de entrada
normal e a parcela em cache usa a tarifa própria. A tarefa preserva o custo calculado como histórico,
sem recalculá-lo retroativamente quando o catálogo mudar.

A ausência de preço para um modelo não pode apagar os tokens nem transformar falha de
instrumentação em sucesso econômico: a tarefa deve ficar com o consumo persistido, custo
indisponível e status explícito de preço ausente. Tarefas sem modelo podem registrar custo zero;
tarefas legadas que não reportaram consumo permanecem identificadas como não informadas. A tela da
tarefa deve mostrar entrada, saída, cache e custo estimado vindos do backend, sem estimativa local.

Uma tarefa excepcional pode ser vinculada posteriormente a uma atividade regular somente enquanto estiver `PENDING`, ainda não tiver sido recebida e a definição estiver `PUBLISHED`. O vínculo preserva o mesmo identificador e histórico da tarefa, remove a excepcionalidade e valida se a atividade pertence ao agente responsável. Tarefas recebidas, concluídas ou já vinculadas não podem ser migradas por esse contrato.

Uma demanda ainda sem correspondência no catálogo deve orientar a criação ou revisão da atividade
no BPM da Cadeia de Valor antes de originar novo trabalho operacional. A partir da decisão de
08/09/2026, `Atividade excepcional` não pode ser usada para criar ou alterar processos e atividades
fora dessa organização. Registros excepcionais anteriores permanecem legíveis como legados; sua
regularização respeita os limites de vínculo e preservação do histórico descritos acima.

A tela do experimento deve expor a instância do processo vinculada à referência da entidade. A situação
de cada atividade é calculada pelo backend com o mesmo grafo usado pelo endpoint `pending`, distinguindo
atividade liberada, atividade em execução, bloqueio por predecessora, falha e conclusão. Quando já existe
uma instância BPM para a entidade, tarefas sem vínculo de processo da mesma referência são apresentadas
como legado substituído e não competem visualmente com o trabalho canônico.

## Vídeos no ciclo comercial PDE — 08/09/2026

O BPM v2 de Ciclos de aprendizado e vendas explicita briefing, produção de vídeo AD de campanha,
produção de LANDING_HERO de entrada e revisão/integração antes da homologação. Usa o Estúdio
existente, com Apolo, Plutus, Psique e Têmis nas responsabilidades canônicas. O backend confere
ativos, criativo e contrato da mesma versão; o usuário recebe links para as telas oficiais.
A execução assistida e os limites estão em [Ciclos de aprendizado e vendas](ciclos-aprendizado-vendas-canon.v1.md).
Publicar o BPM não produz vídeo, não cria tarefa paga nem ativa campanha.

## Medição automática no ciclo comercial PDE — 09/09/2026

O BPM v3 de Ciclos de aprendizado e vendas mantém a atividade `MEASUREMENT` dentro do subprocesso
do processo 6, mas transfere sua execução ao backend: fontes atribuídas são conciliadas e persistidas
antes do losango de decisão. A tela não solicita métricas, fonte, período, responsável ou justificativa.
Uma leitura coerente avança para a decisão; indisponibilidade, desatualização ou conflito persiste
o bloqueio na própria atividade, sem fabricar zero. Versões anteriores e seus eventos permanecem
auditáveis. O contrato completo está em
[Ciclos de aprendizado e vendas](ciclos-aprendizado-vendas-canon.v1.md#conciliação-automática-de-resultados--decisão-de-09092026).

## Contrato de execução automática v1

A decisão de 12/09/2026 substitui o disparo individual de atividades automatizáveis na tela do
produto. Os endpoints de atividade continuam como contratos de domínio usados pelo coordenador;
formulários e decisões humanas continuam acessíveis nos cards. O painel oficial ocupa o espaço
do cabeçalho à direita dos atalhos, reorganizado verticalmente em celular.

Comandos `WORKSPACE` do backend, como criar e executar o preflight técnico, também são
automáticos quando o contrato os libera sem confirmação. A existência de uma área de trabalho
não é uma decisão humana. Formulários de evidências e aprovações nessa área continuam exigindo
seus contratos; a automação não preenche provas nem autoriza publicação por inferência.

- Pacote backend: `businessprocess.automation.v1`; executor: `process-execution-worker/src/v1`.
- Entrada administrativa: `/api/business-processes/{processId}/products/{productId}/automation/v1`.
- Entrada do conciliador: `/api/internal/business-processes/automation/v1/stage-executions/pending`.
- Persistência: `product_process_run_v1` e `product_process_run_event_v1`; chaves de escopo e de
  entrada impedem duplicação. O lock do produto ordena comandos concorrentes e interage com STOP.
- Uma autorização conserva cadeia, processo/versionamento, produto, ciclo e referência. Publicar
  outra versão não migra automaticamente a execução. A definição retirada pode terminar seus
  callbacks existentes, mas novos disparos continuam sujeitos ao contrato de versão publicada.
- Produtos diferentes avançam em paralelo. Processos do mesmo produto aguardam uma execução
  anterior terminar ou pausar; subprocessos da mesma raiz compartilham a autorização da chamada.
- Pausa primeiro aguarda tarefas em curso no processo e nas delegações. Só depois libera o produto
  para outro processo. Não cancela tarefas, não as conclui e não abandona sua auditoria.
- Esperas de entrada, evento ou decisão permanecem duráveis. Nenhum limite de duração conclui
  atividade. Falha técnica de conciliação tem diagnóstico preservado fora da transação revertida.
- Observar resultados e autorizar novos disparos são decisões separadas. Ciclo encerrado, STOP,
  pausa ou conclusão do pai não podem ocultar provas já recebidas: o backend registra a conclusão
  somente quando todos os objetivos aplicáveis estiverem comprovados e não restarem tarefas
  pendentes. Se um ciclo fechado ainda tiver objetivos pendentes e nenhuma tarefa em curso,
  encerra o controle como `CLOSED`, preservando as pendências e liberando a reserva do produto.
  Esse estado não equivale a sucesso e não permite retomar o ciclo fechado. Nunca reabre o ciclo
  nem inicia outra atividade para conseguir atualizar o acompanhamento.
- Retornos de correção exigem declaração no BPM e prontidão no contrato do domínio. A mesma
  entrada sem progresso não é reenviada indefinidamente: o painel mostra a causa e a retomada
  explícita. Nova versão/entrada ou prova concluída permite reavaliar a tentativa sem apagar falhas.
- O progresso usa objetivos comprovados; histórico e dispensa explícita são apresentados
  separadamente. Custo desconhecido permanece desconhecido, e conclusão técnica não é venda.
- Todas as atividades TASK do BPM precisam ter definição e contrato de acompanhamento, inclusive
  as condicionais atualmente dispensadas. Contrato ausente, duplicado ou fora do BPM impede
  autorização/conclusão, sem omitir silenciosamente uma obrigação do processo.
- O acompanhamento do cabeçalho lê a projeção persistida e o diário paginado sob demanda; não
  retransmite prompts. A auditoria integral continua na tarefa e nos endpoints do módulo original.

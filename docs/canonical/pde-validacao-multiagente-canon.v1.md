# Validação multiagente de Produtos Digitais Experienciais — v1

> [!IMPORTANT]
> Este cânone aplica a
> [regra global de validação por agentes ou mercado](system-governance-canon.v3.md#31-regra-global-de-validação-por-agentes-ou-mercado).
> Recrutamento, entrevistas, convites para teste, leituras privadas e opiniões solicitadas não são
> apenas dispensáveis: são proibidos como tarefa, gate ou próximo passo de qualquer produto.

## Decisão

Por decisão de produto de 2026-09-06, o Marketing Hub não depende de convites, recrutamento ou
leituras privadas de pessoas para homologar um PDE antes da comunicação comercial. O gate anterior
à comunicação passa a ser uma **validação multiagente**, executada pelos agentes existentes e por
testes determinísticos, com evidências segregadas de qualquer métrica de mercado.

Essa validação comprova prontidão técnica, coerência da experiência, segurança e integridade
comercial. Ela **não comprova** preferência humana, intenção de compra, satisfação, product-market
fit, venda ou receita. Essas evidências só podem vir do comportamento voluntário do mercado no
experimento comercial, com origem e métricas persistidas.

Execuções históricas permanecem imutáveis. A primeira versão multiagente entrou em
`pde-construction-approval` v7, com a referência `product:<id>@agent-validation-v1`; atividades
históricas de leitura privada não podem ser reescritas nem concluídas artificialmente por agentes.

Por decisão de 2026-09-07, a versão v8 torna operacional o retrabalho funcional: uma rejeição de
harness, Psique ou Têmis não pode oferecer somente a repetição do mesmo parecer. O backend deve
destacar uma atividade condicional de correção, preservar a rejeição como entrada, orientar o
usuário e exigir versão nova quando a implementação ou o contrato do produto mudar. Uma lacuna
exclusiva de prova segue a regra de suplemento abaixo. A v7 permanece imutável como histórico.

Por correção de 2026-09-30, o Processo 2 `pde-commercial-plan-offer` v10 passa a produzir
`MARKET_STRATEGY_V4` e `PDE_AGENT_VALIDATION_V1` desde a origem. Materializações novas não podem
reintroduzir `privateValidationPlan`, leituras humanas ou checkout atribuído a pessoa fictícia. Uma
execução histórica completa pode ser convertida apenas em plano multiagente sem alegar que seus
critérios humanos aconteceram; o resultado bruto original permanece imutável na tarefa de origem.

## Alternativas consideradas

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Recrutar duas pessoas antes da comunicação | Evidência humana direta antes do lançamento | Depende de uma capacidade operacional inexistente, atrasa aquisição e consome orçamento sem testar venda | Não adotar |
| Registrar QA ou resposta de agente como leitura humana | Mudança pequena e avanço rápido | Fabrica evidência, mistura tráfego interno com mercado e permite prova social falsa | Proibida |
| Homologar com agentes e validar valor no mercado | Fluxo executável, auditável e rápido; preserva gates independentes | Não antecipa preferência ou compra humana; exige disciplina na leitura dos resultados | Adotar |
| Publicar sem homologação | Menor tempo inicial | Expõe pessoas e orçamento a falhas de produto, segurança e mensuração | Não adotar |

## Processo canônico

O macroprocesso passa a se chamar **Protótipo, validação multiagente e aprovação do PDE** e segue:

`Dédalo → homologação técnica determinística → Psique → Têmis → gate do backend → comunicação`

1. **Dédalo materializa o PDE**: jornada, componentes, audiovisual quando previsto, acesso,
   continuidade, instrumentação e versão imutável do protótipo.
2. **Psique executa a homologação técnica com o harness**: o harness é a estrutura de
   ferramentas, contratos, cenários, testes e evidências usada pelo executor; não é um agente
   nem deve constar como responsável no BPM. Essa execução é determinística, sem parecer de IA,
   e permanece separada dos três pareceres de experiência de Psique. Executa a versão real em desktop e nos perfis móveis
   suportados, testa caminho feliz, retomada, entradas inválidas, falhas de integração, privacidade,
   acessibilidade básica e emissão dos eventos esperados. Também comprova que a superfície pertence
   a imagem, container, porta, proxy e ciclo de deploy exclusivos daquele produto.
3. **Psique valida a experiência por cenários**: usa o protótipo real em contexto novo e isolado,
   sem herdar a resposta de Dédalo, e avalia compreensão, esforço, utilidade, confiança, prazer,
   objeções e clareza do próximo passo.
4. **Têmis revisa integridade independentemente**: confirma fidelidade ao produto e à estratégia,
   limites honestos, segurança, privacidade, direitos, ausência de prova fabricada e segregação das
   métricas internas.
5. **O backend calcula o gate**: somente evidências persistidas, versionadas e aprovadas liberam o
   produto para `COMUNICACAO_E_JORNADA`. O produto permanece em `STOP`; essa passagem não publica,
   cobra, cria campanha nem autoriza gasto.
   Na revalidação de um produto já comercial em um único ciclo de aprendizado aberto, nas etapas
   de ajuste/homologação e para a versão exata declarada, o gate renova as evidências sem regredir
   o estado comercial ou alterar o STOP/RUN do produto. Todos os critérios e contratos do gate
   continuam obrigatórios. A exceção não autoriza publicação nem mídia.
6. **O mercado valida o valor**: depois da comunicação, homologação comercial e autorização de
   mídia, Hermes mede tráfego e compradores reais desde o anúncio até compra, entrega, uso,
   recompra, suporte iniciado pelo cliente e reembolso, sem recrutar participantes nem solicitar
   testes ou opiniões.

Nenhum agente pode criar e aprovar o mesmo artefato. Atena e Plutus continuam responsáveis,
respectivamente, pela estratégia e pela economia anteriores à construção; Íris e Apolo continuam
responsáveis pela comunicação e pelo audiovisual; autorização humana permanece obrigatória para
preço, publicação, campanha e gasto, não para representar uma pessoa fictícia em teste privado.

### Entrada da homologação técnica

Por correção de 06/10/2026, confirmada pela tarefa 595 de Capella, o contexto da construção
deve preservar separadamente a identidade do produto e seu tipo de catálogo. O tipo
`Quartzo`, por exemplo, não representa outro produto. O vínculo vem dos dados estruturados
do backend; texto livre não resolve divergência entre identificadores.

As atividades `journey`, `deliverables` e `access` no executor atual produzem contratos JSON.
Seu `READY` significa especificação completa: não afirma arquivos implementados nem testes
executados. Elas recebem o contexto persistido e não precisam de shell para ler o workspace.
URL, imagem, manifesto, hashes e provas da implementação permanecem obrigatórios na entrada
de `technicalHomologation`; antecipá-los como pré-requisito de uma especificação completa cria
dependência circular. Ausência ou contradição nas entradas da própria atividade continua
bloqueante. Essa distinção não se aplica a `prototypeCorrection`, cujo `READY` exige a versão
corrigida executável, nem permite concluir a construção inteira sem implementação real.

Por correção de 2026-09-10 (Vega, tarefa #377), concluir especificações de jornada,
componentes ou acesso não comprova que o protótipo esteja implementado. Antes de liberar o
comando de homologação, o backend deve resolver o mesmo alvo entregue à fila e exigir URL
executável, identidade e versão compatíveis com a aceitação privada persistida. Um plano
`PLANNED` com URL ausente bloqueia a execução com orientação de concluir a implementação e
registrar sua aceitação. Repetir a tarefa, usar uma URL histórica ou aceitar qualquer URL
não resolve essa ausência.

O responsável da atividade 3.5 é Psique; o modo determinístico e o uso do harness aparecem
na explicação do comando. A tarefa bloqueada permanece no histórico, com sua causa técnica
original. O resumo da atividade apresenta a pendência atual informada pelo backend.

O executor só pode executar cenários implementados para o produto e a superfície declarados.
O script `pde-agent-validation-harness.mjs` atual implementa Mira; seu nome genérico não
autoriza usá-lo em Vega ou em outro PDE. A referência deve identificar o mesmo produto do
alvo, e o contrato do ciclo precisa ser compatível com o executor antes de liberar sua fila.

## Rejeição, correção e nova validação

Por ajuste de 2026-09-10, uma falha `TECHNICAL_FAILURE` da atividade
`technicalHomologation`, causada pelo comportamento ou contrato executável do protótipo, também
disponibiliza **Criar tarefa de correção** para Dédalo. O card bloqueado deve indicar essa correção
com destino, responsável e disponibilidade fornecidos pelo backend, usando a mesma referência de
produto/ciclo. O modo legado `ON_FUNCTIONAL_REJECTION` inclui essa recuperação técnica da
homologação; falhas técnicas de outras atividades não são promovidas a parecer funcional.

Por decisão de 2026-09-30, indisponibilidade de catálogo, configuração, processo ou integração do
próprio harness usa `EXECUTOR_FAILURE`. Ela preserva a tentativa original na auditoria, não abre
`prototypeCorrection`, não solicita Dédalo e não exige nova versão do protótipo. Depois de corrigir
e publicar o executor, o backend cria uma nova tentativa de `technicalHomologation` para a mesma
versão aceita. O erro legado “O harness instalado não possui cenários próprios para este produto”
recebe a mesma regra, mesmo quando já foi persistido como `TECHNICAL_FAILURE`. Uma homologação
posterior aprovada supera a falha do executor; uma falha atribuída ao protótipo ou uma rejeição
funcional continua exigindo a correção versionada conforme as regras abaixo.

A atividade condicional `prototypeCorrection` nunca integra a sequência progressiva normal. Ela só
concorre com a próxima atividade quando existe uma rejeição acionável e o próprio comando de
correção está disponível. Uma tarefa histórica bloqueada ou cancelada não pode manter o controlador
nessa atividade quando o backend já classificou a origem como `EXECUTOR_FAILURE`; nesse caso, a
execução repete `technicalHomologation` na mesma versão aceita.

Por decisão de 2026-09-10, refinada em 2026-09-11 após a tarefa 385 do Vega, a atividade bloqueada
apresenta **uma única ação principal**: um link para a atividade de correção quando esse for o
caminho vigente, ou o comando da própria atividade quando seus requisitos estiverem satisfeitos.
O link identifica número, nome e responsável; preserva produto, processo, cadeia e ciclo.
A tarefa de Dédalo pertence somente a `prototypeCorrection`; os cenários de Psique e a revisão
de Têmis mantêm tarefas, resultados e estados próprios. A referência `recoveryAction.latestTask`
nunca substitui o acompanhamento próprio nem transforma um card sem tarefa em execução.
Criar e acompanhar a correção acontece somente no card responsável, com confirmação, número,
agente e atualização automática até o resultado persistido. Falhas de envio ou de acompanhamento
também ficam visíveis junto à ação; o usuário não precisa procurar mensagens no topo nem recarregar.
Uma tarefa já ativa é acompanhada sem duplicação. Reabrir a página restaura esse acompanhamento
pelo backend. A tarefa encerrada e seu motivo continuam visíveis, com a auditoria sob demanda.

Criar a tarefa de correção não comprova implementação nem aprovação: Dédalo deve identificar
a tarefa bloqueada e o aprendizado do ciclo, e só pode declarar prontidão após comprovar
a versão executável aceita. Uma tarefa ativa de correção impede duplicação pelo card.

Quando uma atividade técnica ou de revisão retornar bloqueio funcional corrigível, o processo deve
seguir esta sequência auditável:

1. o backend mantém a tarefa rejeitada, a causa-raiz, a ação recomendada, as evidências e o custo;
2. a tela aponta **Corrigir o protótipo a partir do parecer** como próxima atividade e mostra o
   comando **Criar tarefa de correção**; a mesma revisão fica indisponível enquanto a correção não
   for concluída;
3. Dédalo recebe o parecer estruturado, compara exatamente três alternativas e devolve instruções
   executáveis, mudanças, critérios e versão anterior/nova;
4. `READY` só é válido quando a versão corrigida difere da rejeitada e preserva resultado útil,
   valor, limites e próximo passo visíveis; enquanto código, imagem, publicação ou prova estiverem
   pendentes, a tarefa permanece `BLOCKED` com a ação restante;
5. a versão nova retorna obrigatoriamente à homologação técnica; depois dela, os cenários de Psique
   são executados em sequência e Têmis revisa o conjunto antes do gate do backend.

As setas `kind=REWORK` do diagrama expressam retorno visual e não são predecessoras operacionais.
Quem libera tarefas continua sendo o backend, combinando a sequência principal, a rejeição
persistida e a validade da nova versão. Nenhum executor escolhe ou dispara a próxima etapa.

Uma tentativa de `prototypeCorrection` bloqueada por implantação ou evidência pendente não é
um novo parecer independente. No contexto de outra tentativa, `blockedActivities` preserva
os pareceres de origem; os bloqueios anteriores de correção ficam em `correctionAttempts`,
com tarefa, causa, ação e resultado estruturados. Assim, a repetição não troca a versão
rejeitada nem exige corrigir a própria atividade de correção.

A conclusão do cenário deve preservar também seu título semântico, consumido pelo harness
e pelas tecnologias assistivas, junto à rotina e à ação de consulta. A homologação local deve
executar o harness real contra a imagem final; um teste com API simulada isoladamente não
comprova compatibilidade entre frontend e executor.

## Cenários mínimos de Psique

Psique deve executar no mínimo três jornadas isoladas da mesma versão:

1. **Caminho aderente**: entrada válida e representativa do público, primeiro resultado utilizável,
   uso do resultado e compreensão do próximo passo.
2. **Fricção e recuperação**: pessoa sem conhecimento de IA, entrada incompleta ou ambígua, erro
   recuperável, retomada da sessão e instruções suficientes para continuar.
3. **Limite e segurança**: pedido fora do escopo, dado sensível ou situação de risco; o produto deve
   bloquear, explicar o limite e oferecer orientação segura sem inventar resultado.

A conclusão e a retomada de um cenário bloqueado devem manter na tela a causa persistida,
o limite e uma próxima ação funcional e acessível. A confirmação administrativa não pode
substituir esse conteúdo. Encerrar o acesso no navegador não reabre nem apaga a evidência.
O harness deve verificar também esse estado terminal, sem depender somente do evento de bloqueio.
Em Mira, a `mira-private-v3` corrige esse encerramento; sessões novas registram sua versão
na criação e as projeções/eventos mantêm essa versão após atualizações do serviço. Checkpoints
legados sem versão preservam o contrato v2 já exposto antes desta migração, sem reclassificá-los
como prova da v3. A revalidação da v3 exige sessões frescas e retorno explícito no BPM.

Os cenários são avaliações sintéticas e devem usar `trafficClass=AGENT_VALIDATION` e marcador
interno equivalente a `mh_internal_test`. Podem explorar personas do público, mas nunca recebem
nome, consentimento, depoimento ou identificador de participante humana.

Na passagem de um ciclo de vendas, a referência canônica pode ser `experiment:<id>`.
O consumidor deve validar produto, experimento, ciclo e versão fornecidos pelo backend;
a forma da referência nunca pode desviar uma atividade sintética para o prompt de leitura
humana ou para capturas anônimas. Identidade inconsistente bloqueia antes de qualquer
inferência. Prompt, schema e validação do callback devem aceitar o mesmo contrato.
O backend publica `validationPolicy` na fila para a versão multiagente do processo,
distinguindo gate vigente de exigências humanas presentes nos pareceres históricos.
Cada cenário deve vincular suas capturas persistidas e expor entrada, saída e eventos
da sessão segregada realmente executada, sem segredos ou evidências de outros produtos.

Têmis e o gate final também devem reconhecer a referência `experiment:<id>` do ciclo,
validando produto, experimento, cadeia, ciclo aberto, versão e aceitação com a mesma fonte
usada pela fila. O plano técnico do ciclo usa o recurso versionado
`contracts/pde-agent-validation-plan-v1.json`, exposto no contexto antes das execuções.
Não importar catálogos globais nem trocar essa referência pela referência histórica do produto.
Estratégia, arquitetura, versões e pareceres anteriores são linhagem: devem ser preservados,
mas não substituem as provas atuais nem reintroduzem exigências humanas retiradas do gate.

No ciclo, a aprovação final permanece na ocorrência BPM, com as tarefas e os hashes utilizados;
não sobrescreve o cadastro comercial, a referência da passagem ou o PLAY/STOP do produto.
Uma tentativa recente bloqueada/em execução impede usar a aprovação anterior da atividade.
A última correção exige homologação posterior. Repetir o mesmo gate é idempotente; novas provas
ou versão produzem nova ocorrência auditável.

### Sucessão da candidata privada no mesmo bundle

Quando a candidata privada é publicada dentro da imagem exclusiva já existente do produto,
seu manifesto pode declarar `publicationContract.publishedByManifest`, mantendo
`automaticDeployOnMerge=false`. A sucessão de experiências só é válida quando o manifesto
publicador possui status de revisão, deploy automático, mesmo produto/slug/alvo/hash de fonte
e uma prova que vincula o caminho e SHA-256 exatos da candidata. Builder e Têmis conferem
esses dados independentemente. Um caminho solto, imagem de outro produto, hash divergente ou
publicador sem autorização bloqueia; nenhum manifesto antigo é reescrito. Esse vínculo
publica código privado e não ativa experimento, cobrança, campanha ou gasto comercial.

### Contagem e limites da homologação

Correção de 07/10/2026, comprovada no planejamento de Mira (tarefas 613–617): três pareceres
de experiência não significam três repetições completas da matriz técnica. O recurso
`contracts/pde-agent-validation-plan-v1.json` declara uma execução técnica e três avaliações
isoladas de Psique antes de Têmis; o backend o entrega também antes do planejamento. Para a
comparação documental implementada, uma matriz contém 18 combinações: três cenários, três
dispositivos e duas condições de entrada. Essa quantidade não se aplica a superfícies sem
esse contrato. Após um defeito, repetir apenas os critérios necessários para comprovar a
correção e prevenir regressões relacionadas; não executar novas matrizes para atingir uma contagem.

Hipóteses econômicas para uma futura entrega comercial não redefinem automaticamente funções,
franquias ou duração do protótipo privado. Distinguir limites implementados, lacunas reais e
propostas comerciais; não exigir quotas de integração paga ou atendimento de uma função ausente.
Consulta idempotente e duas organizações úteis continuam verificáveis. O prazo de execução de
uma tarefa não é o período de validade da credencial nem uma condição de venda.

Na comparação documental V3, os sinais EXPERIENCE_STARTED, VALUE_MOMENT, READY_RESULT_USED,
PREFERRED_OVER_FREE e CHECKOUT_STARTED são persistidos com a sessão segregada. Os dois últimos
são simulações explicitamente internas, realizadas somente depois de consultar o resultado.
O percurso inseguro registra o bloqueio e não a continuidade. Contratos V1/V2 históricos
permanecem válidos segundo seus critérios; não recebem eventos fabricados nem são sobrescritos.

## Evidência obrigatória

Ao recriar o backend compartilhado, o deploy deve revalidar a ligação de todos os frontends PDE
já publicados a esse backend, preservando suas imagens, versões e containers. Saúde de HTML estático
não comprova disponibilidade da API. Os proxies novos usam resolução dinâmica do DNS Docker; os
legados recebem reload validado após a saúde do backend, com sonda pela API do próprio produto.
Uma falha nessa reconexão bloqueia o deploy e identifica a superfície afetada. A homologação local
deve trocar de fato o IP do backend e verificar recuperação, URI/query, POST, autorização de materiais
e isolamento entre Mira e Vega, sem aceitar 502 nem recarregar serviços alheios ao PDE.

No executor de navegador, o clique não equivale a evento persistido. Antes de emitir uma
ação dependente ou recarregar a página, aguardar a resposta do evento exato iniciado pela
tela e conferir sua presença no estado devolvido pelo backend. Em particular,
`RECOVERY_COMPLETED` exige confirmação prévia de `READY_RESULT_USED`. HTTP de erro,
JSON inválido ou resposta sem o evento devem bloquear com operação e status identificáveis,
sem expor tokens. Não adicionar espera fixa nem afrouxar o gate para acomodar latência.
O teste de contrato deve introduzir atraso antes da persistência e comprovar a ordem.

Cada execução deve persistir:

- produto, URL e versão exatos, além de imagem, digest/tag, container, porta e proxy próprios;
- agente, execução, modelo, versão do prompt e versão do schema;
- cenário, entradas, saída funcional e decisão estruturada;
- request enviado e response bruto recebido do modelo;
- dispositivo, viewport, horários, duração, screenshots e artefatos;
- sequência de eventos, falhas, bloqueios, retomada e custo;
- critérios aprovados ou reprovados e causa-raiz do ajuste;
- confirmação de que pagamento, publicação, campanha e gasto permaneceram desativados.

Eventos de `AGENT_VALIDATION`, QA, smoke test e automação devem ser excluídos de visitantes,
conversão, checkout, venda, receita, CAC, satisfação, reviews e depoimentos. Agentes não podem
declarar “pessoas preferiram”, “clientes aprovaram” ou expressão equivalente.

## Gate para avançar

O backend libera a comunicação somente quando:

- a mesma versão passa em desktop e nos perfis móveis suportados;
- imagem, container, porta, proxy, diagnóstico e deploy da superfície não são compartilhados com
  outro produto;
- o caminho feliz entrega resultado pronto em até dez minutos e sem prompting ou montagem externa;
- os três cenários de Psique possuem evidência completa e nenhum bloqueio crítico;
- Têmis aprova segurança, verdade, privacidade e fidelidade aos contratos anteriores;
- eventos e artefatos internos estão segregados das métricas comerciais;
- não há pagamento, publicação, mídia ou gasto decorrente da homologação.

**Continuar:** todos os gates aprovados; preparar comunicação e experimento comercial.

**Ajustar:** há fricção, inconsistência ou falha corrigível; retornar à autoridade do artefato e
reexecutar somente depois da correção.

**Parar:** produto inseguro, promessa sem sustentação, economia inviável, falha de privacidade ou
resultado que transfere à cliente o trabalho de operar a IA.

## Prova comercial posterior

A primeira evidência externa do PDE passa a ser o comportamento voluntário do mercado, nunca uma
leitura privada solicitada. O experimento deve separar tráfego interno e medir, no mínimo:

- impressão, clique e sessão atribuída;
- início e conclusão da experiência;
- chegada e uso do primeiro resultado útil;
- CTA e checkout iniciados;
- pagamento aprovado, receita e CAC;
- entrega concluída, uso ou recompra, suporte iniciado pelo cliente e reembolso.

Sem tráfego humano suficiente, o resultado é `EVIDÊNCIA_INSUFICIENTE`. Sem pagamento reconciliado,
não existe venda; sem contribuição positiva, não existe escala comprovada. Parecer de agente pode
liberar o teste, mas nunca substituir esses fatos.


### Evidência técnica da versão corrigida

O alvo e o contexto enviado ao agente devem usar a mesma aceitação canônica de
`validationDefinitionJson.privatePrototypeAcceptance`. A cópia histórica em `pdeExperienceJson`
não pode reintroduzir versão anterior no contexto.

Antes de repetir uma correção, o backend mantém `prototypeCorrection` pendente e reconcilia
automaticamente o runtime, sem edição manual do produto e sem entregar a tarefa ao executor. O
contrato `PDE_TECHNICAL_DEPLOYMENT_EVIDENCE_V2` registra `observedAt`, `httpStatus`,
`reconciliationTrigger`, `sourceTaskId` e `diagnosticSnapshot` bruto. O diagnóstico precisa
identificar produto, URL privada, imagem, commit, hash de fonte e uma versão diferente da aceita,
com status `UP` e efeitos comerciais desligados. Reutilizar a versão com outro build permanece
bloqueado.

Depois da prova, o backend atualiza atomicamente `privatePrototypeAcceptance` na definição e na
experiência, preservando a versão anterior, a tarefa de correção e a identidade publicada. Somente
então o claim é liberado. A reentrega da mesma tarefa e do mesmo build é idempotente; outra tentativa
de correção exige nova versão. Recibos de outro produto, endereço, versão ou tarefa não entram no
contexto.

Disponibilidade técnica de protótipo privado não muda `published`, não autoriza distribuição,
cobrança, campanha ou gasto e não substitui `technicalHomologation`, Psique ou Têmis. O agente
deve avaliar a evidência atual; um workflow antigo com `frontend_version=none` não a invalida.


### Revalidação depois da correção

Uma conclusão de outra versão do processo/protótipo, ou anterior à última correção aplicável,
não encerra a atividade da versão atual. O gate do domínio declara `requiresFreshExecution` e
o motor BPM representa a nova validação como pendente, mantendo tarefas e instâncias antigas
na auditoria. O mesmo critério governa a tela e o comando de criação. Uma nova tarefa gera nova
ocorrência; execução pendente/em andamento permanece idempotente e não pode ser duplicada.

Toda rejeição funcional em uma revisão da v8 bloqueia a repetição das revisões até nova correção
ou suplemento de prova compatível, conforme a regra abaixo.
A homologação técnica anterior não libera nova tentativa de Psique sobre o mesmo defeito.

Rejeições superadas por correção válida ficam na auditoria e não definem o bloqueio atual
da tela. Falhas posteriores à correção continuam vigentes. Para liberar uma revisão, o backend
considera a tentativa mais recente da predecessora; uma aprovação antiga não compensa uma
falha mais nova nem uma execução ainda pendente.

### Suplemento de prova sem mudança do produto

Uma lacuna de cobertura não exige mudar a versão do produto quando implementação, entradas,
saídas e limites permanecem iguais. O suplemento deve vir de testes executados, identificar
produto/versão, fonte, origem e hash, e declarar cada critério com resultado rastreável.
Testes locais com MySQL real e cadastros simulados declaram essa fronteira; não representam
ações em ciclos publicados, parecer independente, comportamento humano ou prova comercial.

O backend entrega o suplemento em `pdeContext.operationalControlEvidence` por catálogo versionado
e integridade dos bytes. Prova nova, posterior ao horário persistido do bloqueio, libera somente
uma nova revisão independente de Têmis. Bloqueios usam `updatedAt`; `deliveredAt` pertence às
entregas concluídas e pode estar ausente. Prova anterior, de outra versão ou incompleta não libera
a retentativa. Novo bloqueio posterior ao suplemento não pode gerar repetição automática sem
outra mudança comprovada.

A matriz técnica e os três pareceres válidos da mesma implementação permanecem vigentes. Têmis
continua responsável pelo novo aceite e o gate exige esse parecer aprovado, posterior ao
suplemento e à tentativa bloqueada que referencia a rejeição original. Correção em andamento,
prova velha ou parecer do próprio construtor continuam bloqueados. Após todos os
objetivos atendidos, a tentativa condicional de correção cancelada ou bloqueada, sem comando
disponível, aparece como histórico registrado; sua tarefa e seu bloqueio original permanecem
na auditoria. Isso não dispensa objetivo obrigatório nem autoriza publicação, cobrança ou mídia.

O gate persiste `PDE_EVIDENCE_CORRECTION_RESOLUTION_V1` quando essa resolução preserva as revisões
anteriores. O recibo vincula a rejeição, a tentativa bloqueada e o novo parecer independente por
identidade e hashes, além da versão, referência e relatório usado. O contexto de Íris confere esse
mesmo recibo e a prova atual antes de consumir o gate; não exige correção funcional nova para uma
lacuna já conciliada, nem aceita a tentativa bloqueada isoladamente. Ausência ou divergência,
correção posterior, mudança funcional ou tarefa em andamento mantém a passagem bloqueada.


### Mesma política de versões na matriz e no gate — 07/10/2026

O gate deve encaminhar a comparação documental ao mesmo validador que reconhece suas versões
V1, V2 e V3. V3 exige os sinais executados além das medições e das dezoito combinações; seu
resultado não pode cair na regra histórica de cinco cenários. Contrato explícito desconhecido
permanece bloqueado. Matrizes históricas sem `fixtureContract` e a estática V1 de nove cenários
conservam seu contrato próprio, sem ampliar autorização de mercado ou dispensar revisão.

A regressão deve atravessar gate, recibo persistível, contexto e prontidão real de Íris com
payloads persistidos compatíveis, repetir com outra identidade e preservar o caminho antes
válido. Aprovar apenas o validador isolado não comprova a passagem do processo. Registro:
[`mira-gate-matriz-v3-2026-10-07.md`](../registros/mira-gate-matriz-v3-2026-10-07.md).

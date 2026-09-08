# Validação multiagente de Produtos Digitais Experienciais — v1

## Decisão

Por decisão de produto de 2026-09-06, o Marketing Hub não depende de convites, recrutamento ou
leituras privadas de pessoas para homologar um PDE antes da comunicação comercial. O gate anterior
à comunicação passa a ser uma **validação multiagente**, executada pelos agentes existentes e por
testes determinísticos, com evidências segregadas de qualquer métrica de mercado.

Essa validação comprova prontidão técnica, coerência da experiência, segurança e integridade
comercial. Ela **não comprova** preferência humana, intenção de compra, satisfação, product-market
fit, venda ou receita. Essas evidências só podem vir do experimento comercial e da entrega a pessoas
reais, com origem e métricas persistidas.

Execuções históricas permanecem imutáveis. A primeira versão multiagente entrou em
`pde-construction-approval` v7, com a referência `product:<id>@agent-validation-v1`; atividades
históricas de leitura privada não podem ser reescritas nem concluídas artificialmente por agentes.

Por decisão de 2026-09-07, a versão v8 torna operacional o retrabalho funcional: uma rejeição de
harness, Psique ou Têmis não pode oferecer somente a repetição do mesmo parecer. O backend deve
destacar uma atividade condicional de correção, preservar a rejeição como entrada, orientar o
usuário e exigir versão nova antes de qualquer revalidação. A v7 permanece imutável como histórico.

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
2. **O harness homologa tecnicamente**: executa a versão real em desktop e nos perfis móveis
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
   mídia, Hermes mede pessoas reais desde o anúncio até compra, entrega, satisfação e reembolso.

Nenhum agente pode criar e aprovar o mesmo artefato. Atena e Plutus continuam responsáveis,
respectivamente, pela estratégia e pela economia anteriores à construção; Íris e Apolo continuam
responsáveis pela comunicação e pelo audiovisual; autorização humana permanece obrigatória para
preço, publicação, campanha e gasto, não para representar uma pessoa fictícia em teste privado.

## Rejeição, correção e nova validação

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

Os cenários são avaliações sintéticas e devem usar `trafficClass=AGENT_VALIDATION` e marcador
interno equivalente a `mh_internal_test`. Podem explorar personas do público, mas nunca recebem
nome, consentimento, depoimento ou identificador de participante humana.

## Evidência obrigatória

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

A primeira evidência humana do PDE passa a ocorrer no mercado, não em uma leitura privada
artificial. O experimento deve separar tráfego interno e medir, no mínimo:

- impressão, clique e sessão atribuída;
- início e conclusão da experiência;
- chegada e uso do primeiro resultado útil;
- CTA e checkout iniciados;
- pagamento aprovado, receita e CAC;
- entrega concluída, satisfação, suporte e reembolso.

Sem tráfego humano suficiente, o resultado é `EVIDÊNCIA_INSUFICIENTE`. Sem pagamento reconciliado,
não existe venda. Parecer de agente pode liberar o teste, mas nunca substituir esses fatos.


### Evidência técnica da versão corrigida

O alvo e o contexto enviado ao agente devem usar a mesma aceitação canônica de
`validationDefinitionJson.privatePrototypeAcceptance`. A cópia histórica em `pdeExperienceJson`
não pode reintroduzir versão anterior no contexto.

Antes de repetir uma correção por ausência de implantação, registrar pela edição do produto a
prova operacional em `validationDefinitionJson.technicalDeploymentEvidence`, contrato
`PDE_TECHNICAL_DEPLOYMENT_EVIDENCE_V1`: `observedAt`, `diagnosticUrl`, `httpStatus`,
`diagnosticSnapshot` bruto e referência à verificação. O diagnóstico precisa identificar produto,
URL privada, imagem e versão atuais com status `UP`. O backend expõe a evidência apenas quando
as identidades conferem com a aceitação vigente; recibos de outra versão/produto/endereço não
entram no contexto. O registro deve ser renovado após nova implantação.

Disponibilidade técnica de protótipo privado não muda `published`, não autoriza distribuição,
cobrança, campanha ou gasto e não substitui `technicalHomologation`, Psique ou Têmis. O agente
deve avaliar a evidência atual; um workflow antigo com `frontend_version=none` não a invalida.


### Revalidação depois da correção

Uma conclusão de outra versão do processo/protótipo, ou anterior à última correção aplicável,
não encerra a atividade da versão atual. O gate do domínio declara `requiresFreshExecution` e
o motor BPM representa a nova validação como pendente, mantendo tarefas e instâncias antigas
na auditoria. O mesmo critério governa a tela e o comando de criação. Uma nova tarefa gera nova
ocorrência; execução pendente/em andamento permanece idempotente e não pode ser duplicada.

Toda rejeição funcional em uma revisão da v8 bloqueia a repetição das revisões até nova correção.
A homologação técnica anterior não libera nova tentativa de Psique sobre o mesmo defeito.

Rejeições superadas por correção válida ficam na auditoria e não definem o bloqueio atual
da tela. Falhas posteriores à correção continuam vigentes. Para liberar uma revisão, o backend
considera a tentativa mais recente da predecessora; uma aprovação antiga não compensa uma
falha mais nova nem uma execução ainda pendente.

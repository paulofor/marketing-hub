# Prompt v12 — estratégia, identidade e homologação multiagente no Processo 2

Identifique a entrada da tarefa antes de decidir. Na descoberta, receba o dossiê factual de Argos.
No retorno de um ciclo de vendas, receba o aprendizado e o brief persistidos do mesmo produto e
experimento. Defina público prioritário, problema, desejo, comportamento
estratégico, concorrência, diferenciação, posicionamento, tese de oferta, portfólio e hipótese
prioritária.

Classifique a entrada em exatamente um destes modos antes da análise:

- `DISCOVERY`: `sourceReference: product-discovery-cycle:*`, com candidatas factuais de Argos;
- `SUCCESSOR`: existe `processContextJson.learningSalesCycle` do experimento exato;
- `INITIAL_PLANNED_EXPERIMENT`: `taskTarget.pdeContext.contractVersion` é
  `PDE_COMMERCIAL_PLANNING_INPUT_V1` e seu `mode` é `INITIAL_PLANNED_EXPERIMENT`.

Uma referência `experiment:*`, sozinha, não torna a tarefa sucessora. Se não houver ciclo nem o
contrato inicial completo, use `ADJUST` sem chamar isso de baixa amostra. Nunca fabrique um ciclo,
um dossiê ou aprendizado de venda para preencher a lacuna.

Esta versão exige `productIdentity` no topo da resposta. Na descoberta, use a política
`productIdentityPolicy` recebida no contexto: compare pelo menos três nomes internos de estrelas
que não estejam em `reservedInternalNames` e pelo menos três tipos de
`activeProductTypes`. Escolha o tipo pelo mecanismo de valor, pela entrega e pelo modelo de receita;
webapp, página, aplicativo ou interface são formatos e não justificam outro tipo. Retorne
`contractVersion: PRODUCT_IDENTITY_V1`, `mode: CREATE`, o `internalName` escolhido e exatamente o
`productTypeCode` e o `productTypeInternalName` da mesma entrada do catálogo. Explique a escolha em
`classificationRationale`. Nome provisório, título do dossiê, “PDE planejado” e tipo genérico usado
por conveniência são proibidos.

Em `SUCCESSOR` ou `INITIAL_PLANNED_EXPERIMENT`, o produto já existe: use `mode: PRESERVE` e copie
sem alterar `taskTarget.productInternalName`,
`taskTarget.pdeContext.product.productTypeCode` e
`taskTarget.pdeContext.product.productTypeInternalName`. Esta atividade não renomeia nem
reclassifica produto existente. Em `ADJUST` ou `REJECT`, use `mode: NOT_APPLICABLE` e strings vazias
nos quatro campos descritivos. Escolher identidade organiza o ativo comercial, mas não comprova
demanda, venda, receita ou margem.

Quando o contexto contiver duas ou três candidatas, priorize no máximo uma. Informe os
`selectedDossierId` e `selectedOpportunityId` exatamente como recebidos. Em `ADJUST` ou `REJECT`,
use `null` quando nenhuma candidata puder avançar. Não misture fatos entre dossiês. Fora da
descoberta autônoma, use `null` para esses dois campos.

Somente na descoberta autônoma (`sourceReference: product-discovery-cycle:*`), candidatas com
`maturity: DOSSIER_READY` são elegíveis. Essa maturidade autoriza apenas planejamento, economia,
arquitetura e homologação multiagente de um protótipo não público. Ela não significa produto
validado, pronto para operar ou pronto para venda.

Para um sucessor (`sourceReference: experiment:*` e `processContextJson.learningSalesCycle`),
use o `brief`, `inheritedLearning` e `currentDecisions` do ciclo exato. Exija que `experimentId`
corresponda à referência da tarefa e `productId` ao produto; divergência ou contexto ausente é
bloqueio. Essa entrada já possui produto e decisão de ajuste: não exigir nova descoberta, dossiê
Argos ou maturidade DOSSIER_READY. Os identificadores de seleção ficam nulos. Compare três maneiras
de executar a melhoria aprovada, sem substituir público, preço, canal ou variável principal.

No primeiro planejamento (`INITIAL_PLANNED_EXPERIMENT`), use somente produto, hipótese,
experimento e plano comercial persistidos em `taskTarget.pdeContext`. Não exija dossiê novo,
`learningSalesCycle`, venda anterior ou decisão de ajuste. Compare três formas de concretizar a
tese já cadastrada e congele no máximo uma. Preço cadastrado continua hipótese; CAC, custo,
contribuição, margem e orçamento desconhecidos seguem para Plutus e nunca viram zero. A ausência
dessas medições não bloqueia o desenho do protótipo quando problema, promessa, mecanismo, público,
canal e limites formam um contrato coerente.

Respeite o modo de validação persistido do produto. Quando `validationDefinitionVersion` for
`PDE_AGENT_VALIDATION_V1` ou `PDE_AGENT_VALIDATED_V1`, não proponha recrutamento, convite, contato
ou leitura humana. Reutilize somente a prova técnica/multiagente ainda compatível e preserve
`humanEvidenceClaimed: false` e `commercialEvidenceClaimed: false`; validação por agentes não é
comportamento de cliente nem venda. Qualquer contato, tráfego ou compra continua fora desta tarefa.

A versão `learningSalesCycle.productVersion` é o alvo ainda a construir e homologar. A URL/versão
já publicada de `taskTarget` pode ser a referência histórica: nunca declará-la como o novo artefato.
Use os registros de medição e decisão como fontes internas com seus identificadores. Diferencie
sinais de uso, hipótese de valor e resultado comercial; não invente fontes, comportamento pago ou
validação humana. Campos de cena de compra ainda não observados devem declarar a hipótese e sua
lacuna. O valor ao cliente e os caminhos de evidência devem referenciar fatos persistidos,
distinguindo métricas observadas de proposta comercial. Aprovar aqui congela somente a construção e
a homologação multiagente, sem contato com pessoas.

No sucessor, use `APPROVE/READY_FOR_AGENT_VALIDATION` se o aprendizado conciliado, o ajuste aprovado
e a hipótese explícita sustentarem um protótipo limitado, mesmo com poucas sessões e nenhuma venda.
A baixa amostra não comprova causa do abandono e não impede planejar um teste para aprender.
Use ADJUST/INSUFFICIENT_EVIDENCE quando o contexto for incompatível, não existir aprendizado/decisão
rastreável ou o mecanismo for inviável. As regras de dossiê abaixo aplicam-se somente à descoberta.

Compare exatamente três alternativas estratégicas por benefício, risco, esforço e aderência a
vendas com entrega satisfatória. Preserve fatos, inferências, hipóteses e lacunas em categorias
distintas. Tente refutar a alternativa escolhida e registre evidências rastreáveis.

Na descoberta, use `APPROVE` com status `READY_FOR_AGENT_VALIDATION` quando uma candidata
`DOSSIER_READY` tiver base factual suficiente para Atena congelar a estratégia e Dédalo projetar um
protótipo não público limitado. No primeiro planejamento, use `APPROVE` quando o contrato persistido
sustentar um protótipo limitado, mesmo sem vendas anteriores; deixe explícito o que continua
hipótese e o que Plutus deve limitar. A ausência do próprio protótipo, de preferência observada ou
de checkout de teste é uma lacuna esperada desta fase e nunca deve, isoladamente, causar `ADJUST`.

Em `agentValidationPlan`, predeclare a hipótese, a cena de compra, a alternativa gratuita mais
forte, a vantagem que o protótipo precisa demonstrar e a entrega de valor ao cliente. Exija os três
cenários `ADHERENT`, `RECOVERY` e `SAFETY`, nos dispositivos `DESKTOP_1440`, `IPHONE_15_PRO` e
`PIXEL_7`, com resultado pronto em no máximo 600 segundos. Use `contractVersion:
PDE_AGENT_VALIDATION_V1`, `trafficClass: AGENT_VALIDATION` e
`internalMarker: mh_internal_test`. Preserve `humanEvidenceClaimed: false`,
`commercialEvidenceClaimed: false`, pagamento, publicação e campanha desativados e gasto de mídia
igual a zero. Se as fontes comerciais precisarem de atualização, marque `sourceRefreshRequired:
true` e descreva a atualização em `sourceRefreshAction`; essa lacuna bloqueia o gate factual, não o
desenho do protótipo.

Nunca proponha entrevista, recrutamento, convite, leitura privada, opinião solicitada ou checkout
simulado por pessoa fictícia. Psique e Têmis comprovam somente compreensão, utilidade, recuperação,
segurança e integridade do artefato. Agentes nunca viram participante nem prova de demanda. Preserve
a cena de compra nos seis campos estruturados e copie `customerValueDelivery` somente de evidências
públicas rastreáveis. Declare `sourceMaxAgeDays` entre 1 e 90; o backend registrará o instante em que
os critérios foram congelados e recalculará os resultados a partir dos fatos, sem confiar em um
booleano do modelo.

Use `ADJUST` ou `REJECT` com status `INSUFFICIENT_EVIDENCE` quando não existir candidata
`DOSSIER_READY`, a cena ou o mecanismo não forem plausíveis, ou o risco não permitir sequer uma
homologação automatizada. Nunca use `READY_FOR_OPERATION` nesta atividade e nunca invente confiança,
venda, receita, validação humana ou pagamento.

A aprovação desta atividade não autoriza contato, publicação, campanha, orçamento, gasto, checkout
real nem venda. O backend deve manter o produto em `PLANNED` e execução em `STOP`. A preparação
comercial só pode ocorrer depois de protótipo utilizável, fontes vigentes, homologação técnica,
três cenários de Psique e revisão independente de Têmis; somente o mercado comprova demanda.


O protocolo de execução vem de `learningSalesCycle.agentValidationExecution`, fornecido pelo
backend antes do planejamento. Declare `technicalMatrixRuns=1` e
`independentExperienceReviewCount=3`: são uma homologação técnica e três pareceres isolados de
Psique (ADHERENT, RECOVERY e SAFETY), seguidos de Têmis. Não multiplique a matriz pelos pareceres.
Use comparação documental somente quando `agentValidationExecution.inputComparisonApplicable`
for `true` e houver `documentedInputComparison` da versão exata. Nesse caso são 18 combinações:
três cenários, três dispositivos e as duas condições recebidas. Quando for `false`, não imponha
REFERENCE/REDUCED, redução de campos ou uma referência artificialmente mais trabalhosa. Preserve
os cenários e dispositivos comuns; não importe um protocolo de outro produto. Não substitua por
27, não exija três matrizes completas e reteste somente critérios alterados ou falhos.
Os critérios adicionais da hipótese podem avaliar qualidade, sem redefinir esse protocolo.

Diferencie capacidades atuais comprovadas e hipóteses de uma futura entrega comercial. Um
protótipo documental determinístico sem provedor externo nem suporte automatizado pago deve ser
avaliado por seus limites implementados, consulta idempotente e bloqueios. Não invente quotas de
chamadas de IA, atendimento ou consultas para funções inexistentes, nem converta uma hipótese
econômica em mudança obrigatória de produto. Mudanças comerciais continuam propostas, com fontes
e lacunas, e dependem da decisão própria. Preserve os limites e a duração privada informados;
o prazo operacional de uma tarefa não altera a validade da credencial nem a oferta.

Quando o sucessor trouxer `learningSalesCycle.implementedInput`, essa descrição foi vinculada
pelo backend ao produto, ciclo, experimento e versão implementada. Preserve a entrada: copie
integralmente `implementedInput.minimumCustomerInput` em
`agentValidationPlan.customerValueDelivery.minimumCustomerInput`. Não complete campos com
descrições de versões anteriores. Use os campos obrigatórios e opcionais recebidos para descrever
o mecanismo, o esforço e a jornada. Esse contrato descreve a implementação; não constitui parecer,
prova de demanda nem autorização de consumo. Se a hipótese realmente precisar mudar a entrada,
retorne ADJUST com a divergência e a mudança necessária, sem afirmar que ela já existe.

Contexto da tarefa:

{{TASK_CONTEXT}}

## Big Idea do produto — PRODUCT_BIG_IDEA_V1

Entregue `bigIdea` com `centralIdea`, `audience`, `desire`, `promise`, `mechanism`, `evidence` e `limits`.
Uma ideia central não é lista de funcionalidades nem slogan genérico: liga uma situação reconhecível
e um desejo a uma transformação específica por mecanismo plausível. Explique a diferença que torna
o produto desejável e a prova disponível; se só houver hipótese, declare isso. Não invente resultados.

Quando `processContextJson.productBigIdea.revision.content` existir, copie exatamente esse conteúdo:
é a versão fixada para o experimento. Produto, oferta e comunicação devem se alinhar a ela. Se as
fontes contradisserem a ideia, use ADJUST com motivo e mudança recomendada; não a reescreva.
Na ausência, formule a primeira versão a partir da estratégia selecionada. Não autoriza publicação,
campanha ou gasto. O backend preserva a revisão e o operador pode evoluí-la com motivo e histórico.


## Modelagem por referências existentes

Todos os retornos incluem `referenceModelingTrace` e `internationalAssessment` para cumprir o
formato estrito. Sem modelagem, use `referenceModelingTrace: []` e `internationalAssessment: null`.
Sem escopo internacional, ou em `ADJUST`/`REJECT` sem avaliação suficiente, use `internationalAssessment: null`.
Não preencha referências inexistentes para satisfazer o formato. Em aprovação internacional,
o objeto completo e a rastreabilidade abaixo continuam obrigatórios e serão validados pelo executor.

Quando processContextJson.referenceModeling.contractVersion=MARKET_REFERENCE_MODELING_V1,
use o dossiê factual de Argos, factualSources e a intenção congelada como complemento
ao modo DISCOVERY ou ao ciclo escolhido. Não mude o modo contratual da tarefa.
Para produto existente, preserve a identidade (PRESERVE) e Big Idea fixadas. Não exija
um experimento anterior nem crie cópia do produto. Para produto novo, mantenha CREATE
com a política do catálogo. Este modo é preparação, não autorização de publicação ou gasto.
Compare três vantagens próprias: benefício percebido, esforço inicial, experiência sensorial,
personalização e continuidade paga. Explique por que a cliente pagaria em vez de usar a
alternativa gratuita. Não transforme existência de concorrentes em nossas vendas comprovadas.

Ao aprovar, inclua referenceModelingTrace, uma lista com sourceUrl (existente em referenceModeling.factualSources e também em sources),
observedFact, limitation, learning, decision, deliverable e acceptanceMetric. A métrica
separa prova executável de Dédalo e posterior compra/entrega/contribuição de Hermes.
Toda decisão deve conduzir a algo utilizável, não apenas a um relatório. Plutus recebe
custos desconhecidos como lacunas, não zero. Proposta que exige mudar a Big Idea deve
retornar ADJUST e explicar a revisão necessária; não sobrescrever a direção vigente.

### Referências internacionais → experiência brasileira

Quando `referenceModeling.researchScope=INTERNATIONAL_TO_BR_V1`, ao aprovar entregue também
`internationalAssessment`. Em `references`, avalie cada `referenceUrls` exatamente uma vez:
`referenceUrl`, `sourceUrl` existente em `factualSources`, `observedEvidence` copiado literalmente
da `observation` preservada (não traduza esse trecho), `originMarket` (ou desconhecido),
`signalType` (ADOPTION, PAID_OFFER, REPORTED_REVENUE, RETENTION ou UNKNOWN),
`evidenceLevel` (SELLER_CLAIM, PUBLIC_CUSTOMER_REPORT, INDEPENDENT_REPORT ou UNKNOWN) e
`limitation`. O trecho é evidência do que a fonte registra, não confirmação independente.
Preço anunciado não é receita; número de usuários não é rentabilidade; presença internacional
não comprova sucesso no Brasil. Não invente país, números, faturamento nem avaliações.

Em `brazilAdaptation`, preencha `recognizedDesire`, `firstValue`, `localization`,
`localAlternatives`, `paidValue`, `economicsHypotheses`, `mainRisk` e `validationPlan`.
Conecte esses campos à alternativa escolhida e à Big Idea. Compare três alternativas com
benefício, esforço, risco e economia. Considere pt-BR, rotina, clima e preferências pertinentes,
acesso por celular, preço em reais, pagamento e suporte locais. Custos Codex são estimativas
identificadas; não exigir fatura da API nem definir a meta de margem pelo operador.
A facilidade e a personalização devem ser demonstráveis, não apenas promessa. Explique por
que pagar após o primeiro valor e como medir entrega útil, compras líquidas e contribuição.
Se faltarem dados, classifique a lacuna e proponha teste limitado; não fabrique alta demanda.
Atena decide a estratégia; Plutus avalia economia e Dédalo constrói, sem novos agentes ou gates.

## Base disponível e candidata nova

`learningSalesCycle.availableImplementations` descreve versões disponíveis do próprio produto,
com fontes e limites. São referências de construção, não a candidata nova já implementada, nem
aprovação herdada. Compare a hipótese com essa base antes de prometer redução de esforço. Não
transforme campo opcional em obrigatório na referência para fabricar vantagem. Na modelagem
internacional, use as evidências e a Big Idea para propor diferença útil e verificável de
personalização, orientação ou experiência; simplicidade já existente não é uma inovação nova.
Se o benefício proposto já existir, explicite a lacuna e devolva à estratégia; não repita a
arquitetura contraditória nem aprove para avançar. Registre a base, a mudança proposta e como
a cliente perceberá o valor, sem usar teste interno como comprovação de vendas.

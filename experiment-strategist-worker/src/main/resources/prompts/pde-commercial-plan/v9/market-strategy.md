# Atividade — estratégia e identidade do produto no Processo 2 v9

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
`maturity: DOSSIER_READY` são elegíveis. Essa maturidade autoriza apenas
planejamento, economia e arquitetura de um protótipo privado. Ela não significa produto validado,
pronto para operar ou pronto para venda.

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
dessas medições não bloqueia o desenho privado quando problema, promessa, mecanismo, público,
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
lacuna. O valor humano e os caminhos de evidência devem referenciar fatos persistidos, distinguindo
métricas observadas de proposta comercial. Aprovar aqui congela somente o próximo teste privado.

No sucessor, use APPROVE/READY_FOR_PRIVATE_VALIDATION se o aprendizado conciliado, o ajuste aprovado
e a hipótese explícita sustentarem um protótipo limitado, mesmo com poucas sessões e nenhuma venda.
A baixa amostra não comprova causa do abandono e não impede planejar um teste para aprender.
Use ADJUST/INSUFFICIENT_EVIDENCE quando o contexto for incompatível, não existir aprendizado/decisão
rastreável ou o mecanismo for inviável. As regras de dossiê abaixo aplicam-se somente à descoberta.

Compare exatamente três alternativas estratégicas por benefício, risco, esforço e aderência a
vendas com entrega satisfatória. Preserve fatos, inferências, hipóteses e lacunas em categorias
distintas. Tente refutar a alternativa escolhida e registre evidências rastreáveis.

Na descoberta, use `APPROVE` com status `READY_FOR_PRIVATE_VALIDATION` quando uma candidata
`DOSSIER_READY` tiver base factual suficiente para Atena congelar a estratégia e Dédalo projetar um
protótipo privado limitado. No primeiro planejamento, use `APPROVE` quando o contrato persistido
sustentar um protótipo limitado, mesmo sem vendas anteriores; deixe explícito o que continua
hipótese e o que Plutus deve limitar. A ausência do próprio protótipo, de preferência observada ou
de checkout de teste é uma lacuna esperada desta fase e nunca deve, isoladamente, causar `ADJUST`.

Em `privateValidationPlan`, predeclare a hipótese, a cena de compra, a alternativa gratuita mais
forte, a vantagem que o protótipo precisa demonstrar e os critérios de duas leituras independentes.
Use obrigatoriamente os sinais `EXPERIENCE_STARTED`, `VALUE_MOMENT`, `READY_RESULT_USED`,
`PREFERRED_OVER_FREE` e `CHECKOUT_STARTED`. Se as fontes comerciais precisarem de atualização,
marque `sourceRefreshRequired: true` e descreva a atualização em `sourceRefreshAction`; essa lacuna
bloqueia o início das leituras, não o desenho do protótipo.

Quando o contrato persistido exigir leitura humana, cada leitura representa uma pessoa consentida.
Quando o modo for multiagente, não converta cenários automáticos em participante nem evidência
humana; preserve no texto do plano que os limiares estruturados não autorizam contato e que Psique e
Têmis apenas comprovam qualidade, segurança e integridade. Em ambos os casos use
`minimumEligibleParticipantsPerReading: 1` e taxa mínima `1` para cada um dos cinco sinais somente
como contrato de aceite futuro: os dois usos elegíveis precisam chegar ao valor, usar o resultado
pronto, preferi-lo à alternativa gratuita e escolher avançar no checkout simulado. Preserve a cena
de compra nos seis campos estruturados e copie o `humanValueDelivery` somente de evidências
rastreáveis. Declare `sourceMaxAgeDays` entre 1 e 90; o backend registrará o instante em que os
critérios foram congelados e recalculará os resultados a partir dos fatos, sem confiar em um
booleano do modelo.

Use `ADJUST` ou `REJECT` com status `INSUFFICIENT_EVIDENCE` quando não existir candidata
`DOSSIER_READY`, a cena ou o mecanismo não forem plausíveis, ou o risco não permitir sequer uma
validação privada. Nunca use `READY_FOR_OPERATION` nesta atividade e nunca invente confiança,
venda, receita, validação humana ou pagamento.

A aprovação desta atividade não autoriza contato, publicação, campanha, orçamento, gasto, checkout
real nem venda. O backend deve manter o produto em `PLANNED` e execução em `STOP`. A priorização
comercial final só pode ocorrer depois de protótipo utilizável, fontes vigentes e duas leituras
independentes acima dos critérios, sem bloqueio de Psique ou Têmis.

Contexto da tarefa:

{{TASK_CONTEXT}}

# Atividade — arquitetura do protótipo privado PDE v7

Consuma a estratégia de Atena e os limites econômicos de Plutus como contratos imutáveis. Defina
arquitetura do produto, jornada de valor, entregáveis, acesso, superfícies pós-compra e critérios
técnicos.

Compare exatamente três arquiteturas por benefício, risco, esforço e aderência à estratégia
aprovada. Escolha uma que esconda a complexidade da IA e entregue um resultado pessoal pronto e
aplicável ao cotidiano, com entrada mínima e primeiro valor em até dez minutos.

Nesta atividade, a aprovação de Atena significa `READY_FOR_AGENT_VALIDATION`, não prontidão para
operação. Portanto, a arquitetura deve começar por um harness interno pequeno, instrumentável e
reversível. O harness deve permitir cenários sintéticos independentes dos sinais
`EXPERIENCE_STARTED`, `VALUE_MOMENT`, `READY_RESULT_USED`, `PREFERRED_OVER_FREE` e
`CHECKOUT_STARTED`. Esses sinais comprovam somente a integridade técnica da instrumentação: não
representam visita, preferência, intenção, checkout, venda ou validação de mercado. O checkout é
somente simulado e nunca pode cobrar. Preserve os critérios predeclarados por Atena sem
substituí-los.

O protótipo não pode exigir que a pessoa conheça prompts ou opere um modelo de IA. O harness é o
produto: recebe uma entrada simples, usa o modelo nos bastidores e devolve uma experiência
sensorial, personalizada e utilizável. Declare explicitamente o que fica fora do protótipo para
evitar construir produto completo antes de comprovar valor.

Implemente a estratégia sem alterar os contratos recebidos. Audiovisual pertence a Apolo. Psique
avalia a experiência sintética nos cenários `ADHERENT`, `RECOVERY` e `SAFETY`, enquanto Têmis revisa
integridade comercial sem fabricar comportamento de mercado. O campo
legado `nonAudiovisualSurfaces` descreve apenas superfícies funcionais usadas depois da compra.
Copy, landing, anúncios, e-mails e direção visual pré-compra pertencem a Íris.

Não proponha recrutamento, convite, entrevista, leitura privada, opinião solicitada nem piloto
humano. Agentes podem provar prontidão interna e falhas determinísticas; somente compra voluntária
reconciliada e contribuição positiva podem provar demanda e valor comercial.

Use `audiovisualRequired=true` somente quando a entrega do protótipo exigir áudio ou vídeo que
precise ser produzido ou homologado por Apolo. Imagens estáticas do próprio resultado, telas,
cartões e fixtures visuais determinísticas pertencem ao harness e não acionam o pipeline de vídeo.
Declare `staticResultFixtures` em toda arquitetura. Quando o resultado pronto depender apenas
dessas imagens, mantenha `audiovisualRequired=false`, use `required=true`, modo
`DETERMINISTIC_HOMOLOGATION_ONLY`, quantidade e dimensões explícitas, zero chamadas de provider,
efeitos externos falsos e elegibilidade comercial falsa. Quando não depender, use `required=false`,
modo `NOT_REQUIRED`, `artifactType=NONE`, `generator=NONE` e quantidade/dimensões zero. Fixtures de
agente comprovam somente prontidão técnica.

A aprovação cria somente um produto `PLANNED`, em `STOP`, e o encaminha à construção governada. Não
autoriza contato, publicação, campanha, orçamento, gasto, pagamento ou venda.


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

Contexto da tarefa:

{{TASK_CONTEXT}}

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

## Retorno à origem

Se a decisão for `ADJUST` porque um contrato anterior precisa mudar, preencha
`upstreamCorrection` com `contractVersion=PROCESS_UPSTREAM_CORRECTION_V1`,
`targetActivityId=marketStrategy` (Atena) ou `economics` (Plutus), `reason` e
`acceptanceCriterion` verificável. Use o identificador real do BPM e a primeira causa
não atendida; não escolha a própria arquitetura nem outra execução. Quando não houver
correção de predecessor, use `null`. `requiredChanges` continua preservando todos os ajustes.
O backend valida o destino, reabre a origem e revalida dependências; o agente nunca
dispara a próxima tarefa. Não peça nova arquitetura com a mesma entrada rejeitada.
No máximo dois retornos por par/ciclo são permitidos; troca de justificativa não é progresso.
Falha de transporte recupera a resposta existente. Não transfira aprovação, autorização
ou orçamento, nem altere a Big Idea para contornar o bloqueio.

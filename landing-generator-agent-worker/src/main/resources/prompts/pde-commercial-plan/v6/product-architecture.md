# Atividade — arquitetura do protótipo privado PDE v6

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

Contexto da tarefa:

{{TASK_CONTEXT}}

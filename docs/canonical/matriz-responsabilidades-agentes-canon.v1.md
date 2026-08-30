# Matriz canônica de responsabilidades dos agentes v1

## Objetivo

Por decisão de 2026-08-28, os nove agentes do Marketing Hub possuem direitos de decisão exclusivos.
Um agente pode consumir o artefato de outro, mas não pode recriar, corrigir silenciosamente ou
aprovar a própria decisão em outro nome. A finalidade é reduzir custo duplicado, preservar
independência dos gates e localizar com precisão qual hipótese precisa ser revista para gerar vendas
com entrega satisfatória.

Na primeira revisão foram consideradas três organizações:

1. manter os papéis atuais, com menor mudança e decisões contraditórias;
2. criar novos agentes para cada tipo de ativo, aumentando filas, custo e governança antes de existir
   evidência de necessidade;
3. redistribuir os oito agentes então existentes, com um domínio exclusivo e handoffs auditáveis.

A terceira organização foi adotada. A revisão posterior comprovou, porém, que Dédalo ainda acumulava
duas capacidades reutilizáveis com objetivos diferentes: construir o valor usado depois da compra e
materializar a persuasão antes da compra. Foram então comparados manter o acúmulo, separar apenas as
atividades sob a mesma identidade e criar uma agente de comunicação. A decisão vigente cria Íris
como nona agente, com executor e contrato próprios. O cânone detalhado está em
`docs/canonical/iris-communication-agent-canon.v1.md`.

## Direitos de decisão

| Agente | Código técnico | Domínio exclusivo | Entrada canônica | Saída canônica | Não pode decidir |
| --- | --- | --- | --- | --- | --- |
| Argos | `market-radar` | `MARKET_EVIDENCE` — fatos de mercado, demanda, concorrentes, linguagem pública, ofertas, preços visíveis e fontes | pergunta e recorte de pesquisa | dossiê factual com fatos, lacunas e qualidade das fontes | público prioritário, posicionamento, oferta, formato ou canal |
| Atena | `experiment-strategist` | `MARKET_STRATEGY` — mercado, desejo, comportamento estratégico, diferenciação, posicionamento, portfólio, tese de oferta e hipótese prioritária | dossiê factual de Argos e contexto do portfólio | Contrato Estratégico de Mercado versionado, com execução autora e SHA-256 | coleta factual primária, simulação humana, preço final, construção, distribuição ou campanha |
| Plutus | `financial-agent` | `FINANCIAL_VALIDATION` — preço como hipótese econômica, margem, CAC, orçamento, custos, receita e risco financeiro | estratégia, custos e eventos financeiros oficiais | parecer econômico, limites e gates financeiros | posicionamento, comunicação, construção, publicação ou movimentação financeira |
| Dédalo | `landing-generator` | `PDE_CONSTRUCTION` — arquitetura do PDE, experiência funcional, jornada pós-compra, entregáveis, personalização, acesso e prova real do produto | estratégia e economia aprovadas | PDE e provas funcionais versionados | criar comunicação pré-compra, escolher mercado, posicionamento ou preço, aprovar o próprio trabalho ou operar aquisição |
| Íris | `communication-director` | `COMMUNICATION_MATERIALIZATION` — mensagem, copy, landing, peças estáticas, e-mails, direção sensorial e briefings por canal | estratégia, economia, PDE e provas reais aprovados | pacote de comunicação e superfícies pré-compra versionados | redefinir estratégia, preço ou produto, produzir audiovisual final, revisar o próprio trabalho, publicar ou gastar |
| Apolo | `videomaker` | `AUDIOVISUAL_PRODUCTION` — roteiro, cenas, áudio, montagem, legendas e entrega técnica de vídeo | estratégia, briefing e provas aprovadas | pacote audiovisual versionado | redefinir estratégia, criar landing, aprovar integridade ou publicar mídia |
| Psique | `customer-agent` | `HUMAN_EXPERIENCE_REVIEW` — compreensão, reação afetiva, sensorial e estética, prazer, desejo, esforço, confiança e objeções | artefato real e evidências técnicas determinísticas | parecer humano estruturado com evidências e ajustes | inventar fatos de mercado, decidir compliance, alterar preço ou materializar o artefato revisado |
| Têmis | `meta-ad-approver` | `COMMERCIAL_INTEGRITY_REVIEW` — verdade, prova, fidelidade à estratégia, direitos, compliance e segurança da comunicação | estratégia, artefato real e relatório técnico | gate independente de integridade com causa e correção requerida | criar copy, imagem, vídeo, landing ou produto; redefinir estratégia; aprovar trabalho produzido sob sua identidade |
| Hermes | `growth-operator` | `GROWTH_OPERATION` — distribuição, instrumentação, atribuição, funil, gargalo, otimização e decisão de continuar, ajustar ou parar | estratégia imutável, ativos aprovados e eventos reais | contrato operacional e diagnóstico de crescimento | público, problema, desejo, posicionamento, tese de oferta, preço ou construção |

## Fluxo e handoffs

O fluxo canônico é:

`Argos → Atena → Plutus → Dédalo → Íris/Apolo → Psique → Têmis → autorização humana → Hermes`

- Argos entrega fatos; Atena é a única autora da estratégia.
- Plutus valida a viabilidade econômica sem reescrever a proposta de valor.
- Dédalo materializa a experiência usada depois da compra e suas provas reais.
- Íris transforma estratégia e produto em comunicação pré-compra; Apolo materializa vídeo e áudio
  a partir do briefing aprovado.
- Psique avalia se a pessoa entende, deseja e percebe valor.
- Têmis avalia, em atividade diferente, se o material é verdadeiro, comprovável e comercialmente
  seguro. Têmis não produz o ativo que revisa.
- O backend exige os gates aplicáveis, registra a decisão humana e somente então libera trabalho
  operacional para Hermes.
- Se eventos contradisserem segmento, desejo, posicionamento ou tese de oferta, Hermes registra a
  evidência e solicita nova execução da Atena; não altera o contrato estratégico.

O Estúdio de Imagens é um recurso técnico de produção, não uma segunda personalidade de Têmis. O
código de recurso legado `themis-image-studio` e `pde-visual-materialization` permanece no banco para
preservar referências históricas, mas sua propriedade vigente, executor e PLAY/STOP são de Íris sob
o runtime `iris-image-studio`. Ele produz somente `LANDING`, `ADS` e `SOCIAL` a partir de prova real
aprovada. Entregáveis e provas do PDE continuam sob Dédalo e não podem ser fabricados como efeito
colateral do estúdio comercial. Toda imagem de Íris volta a gates independentes de Psique e Têmis.

## Contrato de atividades

Toda nova atividade BPM atribuída a agente deve declarar:

- exatamente uma identidade em `responsibleAgentKeys`;
- o único `responsibilityDomain` permitido para essa identidade;
- um `owner` legível que represente somente esse agente;
- entrada, saída, evidência, critério de conclusão e critério de bloqueio;
- versão/hash dos artefatos predecessores quando a decisão depender deles.

Revisões independentes possuem `activityId` distintos. É proibido representar Psique e Têmis como
coautores de uma atividade, usar `e`, `ou`, vírgula ou barra para combinar um agente com outro dono,
ou permitir que recomendação, tarefa, score, impacto estimado ou PR seja contado como venda.

O backend valida esse contrato ao criar, atualizar ou publicar processos. Changelogs que publicam
processos devem ser homologados fisicamente no MySQL 5.7 e testar que as versões vigentes não contêm
coautoria nem domínio incompatível. Versões históricas permanecem imutáveis e auditáveis.

Na operação vigente, Dédalo aceita somente os seis pares de arquitetura, jornada, entregáveis,
acesso, degustação e personalização definidos no cânone de Íris. Íris aceita somente contrato de
comunicação, pacote não audiovisual e as quatro atividades da landing. O worker deve testar o par
completo `processCode/activityId`, porque nomes isolados podem existir em processos de domínios
diferentes. Filas técnicas legadas não podem criar uma segunda autoridade nem concluir tarefas BPM
de outro executor.

## Métrica e critérios

A métrica principal da organização é **tempo até venda entregue com satisfação**, acompanhada de
custo por artefato aprovado, aprovação na primeira tentativa, retrabalho por causa-raiz, CAC,
margem, reembolso e satisfação.

- continuar: handoffs íntegros, gates independentes e evidência real avançando para venda/entrega;
- ajustar: retrabalho recorrente, atividade sem executor, contrato ausente ou gargalo operacional;
- parar: conflito de autoridade, produto inseguro, economia inviável ou limite comercial atingido.

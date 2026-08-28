# Matriz canônica de responsabilidades dos agentes v1

## Objetivo

Por decisão de 2026-08-28, os oito agentes do Marketing Hub possuem direitos de decisão exclusivos.
Um agente pode consumir o artefato de outro, mas não pode recriar, corrigir silenciosamente ou
aprovar a própria decisão em outro nome. A finalidade é reduzir custo duplicado, preservar
independência dos gates e localizar com precisão qual hipótese precisa ser revista para gerar vendas
com entrega satisfatória.

Foram consideradas três organizações:

1. manter os papéis atuais, com menor mudança e decisões contraditórias;
2. criar novos agentes para cada tipo de ativo, aumentando filas, custo e governança antes de existir
   evidência de necessidade;
3. redistribuir os oito agentes atuais, com um domínio exclusivo e handoffs auditáveis.

A organização adotada é a terceira. Um novo agente somente será considerado quando uma capacidade
reutilizável não couber em nenhum domínio abaixo e o histórico comprovar perda comercial ou
operacional causada por essa ausência.

## Direitos de decisão

| Agente | Código técnico | Domínio exclusivo | Entrada canônica | Saída canônica | Não pode decidir |
| --- | --- | --- | --- | --- | --- |
| Argos | `market-radar` | `MARKET_EVIDENCE` — fatos de mercado, demanda, concorrentes, linguagem pública, ofertas, preços visíveis e fontes | pergunta e recorte de pesquisa | dossiê factual com fatos, lacunas e qualidade das fontes | público prioritário, posicionamento, oferta, formato ou canal |
| Atena | `experiment-strategist` | `MARKET_STRATEGY` — mercado, desejo, comportamento estratégico, diferenciação, posicionamento, portfólio, tese de oferta e hipótese prioritária | dossiê factual de Argos e contexto do portfólio | Contrato Estratégico de Mercado versionado, com execução autora e SHA-256 | coleta factual primária, simulação humana, preço final, construção, distribuição ou campanha |
| Plutus | `financial-agent` | `FINANCIAL_VALIDATION` — preço como hipótese econômica, margem, CAC, orçamento, custos, receita e risco financeiro | estratégia, custos e eventos financeiros oficiais | parecer econômico, limites e gates financeiros | posicionamento, comunicação, construção, publicação ou movimentação financeira |
| Dédalo | `landing-generator` | `PDE_CONSTRUCTION` — arquitetura do PDE, jornada, entregáveis, acesso, landing e materialização não audiovisual da estratégia aprovada | estratégia e economia aprovadas | produto, landing e artefatos não audiovisuais versionados | escolher mercado, posicionamento, preço, aprovar o próprio trabalho ou operar aquisição |
| Apolo | `videomaker` | `AUDIOVISUAL_PRODUCTION` — roteiro, cenas, áudio, montagem, legendas e entrega técnica de vídeo | estratégia, briefing e provas aprovadas | pacote audiovisual versionado | redefinir estratégia, criar landing, aprovar integridade ou publicar mídia |
| Psique | `customer-agent` | `HUMAN_EXPERIENCE_REVIEW` — compreensão, reação afetiva e sensorial, prazer, desejo, esforço, confiança e objeções | artefato real e evidências técnicas determinísticas | parecer humano estruturado com evidências e ajustes | inventar fatos de mercado, decidir compliance, alterar preço ou materializar o artefato revisado |
| Têmis | `meta-ad-approver` | `COMMERCIAL_INTEGRITY_REVIEW` — verdade, prova, fidelidade à estratégia, direitos, compliance e segurança da comunicação | estratégia, artefato real e relatório técnico | gate independente de integridade com causa e correção requerida | criar copy, imagem, vídeo, landing ou produto; redefinir estratégia; aprovar trabalho produzido sob sua identidade |
| Hermes | `growth-operator` | `GROWTH_OPERATION` — distribuição, instrumentação, atribuição, funil, gargalo, otimização e decisão de continuar, ajustar ou parar | estratégia imutável, ativos aprovados e eventos reais | contrato operacional e diagnóstico de crescimento | público, problema, desejo, posicionamento, tese de oferta, preço ou construção |

## Fluxo e handoffs

O fluxo canônico é:

`Argos → Atena → Plutus → Dédalo/Apolo → Psique → Têmis → autorização humana → Hermes`

- Argos entrega fatos; Atena é a única autora da estratégia.
- Plutus valida a viabilidade econômica sem reescrever a proposta de valor.
- Dédalo materializa produto, landing e peças não audiovisuais; Apolo materializa vídeo e áudio.
- Psique avalia se a pessoa entende, deseja e percebe valor.
- Têmis avalia, em atividade diferente, se o material é verdadeiro, comprovável e comercialmente
  seguro. Têmis não produz o ativo que revisa.
- O backend exige os gates aplicáveis, registra a decisão humana e somente então libera trabalho
  operacional para Hermes.
- Se eventos contradisserem segmento, desejo, posicionamento ou tese de oferta, Hermes registra a
  evidência e solicita nova execução da Atena; não altera o contrato estratégico.

O Estúdio de Imagens é um recurso técnico de produção, não uma segunda personalidade de Têmis. O
código legado `themis-image-studio` pode permanecer temporariamente por compatibilidade operacional,
mas sua direção de produção pertence ao contrato de construção de Dédalo e todo resultado continua
obrigado a passar por revisão independente de Psique e Têmis.

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

## Métrica e critérios

A métrica principal da organização é **tempo até venda entregue com satisfação**, acompanhada de
custo por artefato aprovado, aprovação na primeira tentativa, retrabalho por causa-raiz, CAC,
margem, reembolso e satisfação.

- continuar: handoffs íntegros, gates independentes e evidência real avançando para venda/entrega;
- ajustar: retrabalho recorrente, atividade sem executor, contrato ausente ou gargalo operacional;
- parar: conflito de autoridade, produto inseguro, economia inviável ou limite comercial atingido.


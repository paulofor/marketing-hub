-- pde-commercial-plan-offer: v6 -> v7; somente objetivos, preservando contratos, responsáveis e fluxos.
INSERT INTO business_process_definition
 (process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,
  technical_reference,process_type,parent_process_code,execution_scope,diagram_json,created_at,published_at)
SELECT source.process_code,source.name,source.purpose,source.owner_name,source.trigger_description,
 source.outcome_description,7,'PUBLISHED','docs/canonical/cadeia-produtos-pde-canon.v1.md / cinco criterios comerciais',
 source.process_type,source.parent_process_code,source.execution_scope,
 JSON_SET(source.diagram_json,
  '$.nodes[1].description',
  'Entregar: um desejo reconhecível, público/situação, resultado desejado e linguagem real com fonte/data; separar fato, hipótese de mensagem e explicação concorrente. Aceite: selecionar no máximo uma candidata, fiel ao tipo e à ficha aprovada, sem provocar inadequação; congelar a referência e predeclarar duas leituras privadas. Medir: visitantes humanos atribuídos → primeira interação por variante e janela. Participação não comprova compra; não liberar venda.',
  '$.nodes[2].description',
  'Entregar: preço como hipótese, receita líquida, CAC, custo integral por cliente/resultado e custos fixos, cenários conservador, esperado e uso intenso, teto e travas. Se houver degustação, incluir custo por uso, limite total e fronteira gratuita/paga. Aceite: fonte e validade de cada premissa; custo desconhecido não é zero; contribuição e margem atendem à política aprovada. Medir: custo por resultado útil, compras líquidas e margem; projeção não é receita nem autorização de gasto.',
  '$.nodes[3].description',
  'Entregar: caminho mínimo do formato aprovado até um resultado útil, finalidade de cada pergunta, primeira ação e próximo botão evidentes, recuperação e continuidade paga. Aceite: protótipo privado e harness com desktop/iPhone/Android, entrada aderente e falhas; não impor quatro perguntas, cadastro ou webapp a todo produto. Medir: início → conclusão → resultado recebido, abandono por etapa e tempo até o benefício; testes e leituras humanas separados.',
  '$.commercialCriteriaVersion',
  'PDE_COMMERCIAL_PRINCIPLES_V1'),UTC_TIMESTAMP(),UTC_TIMESTAMP()
FROM business_process_definition source
LEFT JOIN business_process_definition existing ON existing.process_code=source.process_code AND existing.version_number=7
WHERE source.process_code='pde-commercial-plan-offer' AND source.version_number=6 AND existing.id IS NULL;

-- pde-construction-approval: v8 -> v9; somente objetivos, preservando contratos, responsáveis e fluxos.
INSERT INTO business_process_definition
 (process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,
  technical_reference,process_type,parent_process_code,execution_scope,diagram_json,created_at,published_at)
SELECT source.process_code,source.name,source.purpose,source.owner_name,source.trigger_description,
 source.outcome_description,9,'PUBLISHED','docs/canonical/cadeia-produtos-pde-canon.v1.md / cinco criterios comerciais',
 source.process_type,source.parent_process_code,source.execution_scope,
 JSON_SET(source.diagram_json,
  '$.nodes[1].description',
  'Entregar: jornada privada do formato aprovado com primeira ação evidente, apenas entradas necessárias e benefício aplicável. Aceite: justificar cada pergunta, manter próximo botão e botão final alcançáveis no celular sem rolagem inesperada ou conteúdo bloqueante; cumprir a promessa inicial e explicar o próximo passo. Medir: início, conclusão das entradas e resultado recebido separadamente, abandono por etapa e tempo até o benefício. A quantidade de escolhas depende do benefício e do formato, sem padrão universal.',
  '$.nodes[4].description',
  'Entregar: acesso de teste, salvamento/retomada quando previstos, tratamento de erro, privacidade e eventos correlacionados à versão. Aceite: cadastro não equivale a login; resultado exibido não equivale a uso; registrar consentimento e excluir testes da amostra comercial. Medir: resultado → interesse em salvar → cadastro → acesso concluído → retorno, sem inferir etapas ausentes.',
  '$.nodes[5].description',
  'Entregar: evidências da mesma versão em desktop, iPhone e Android, com caminho feliz, falha e retomada. Aceite: primeira ação, próximo botão e botão final visíveis/acessíveis após cada passo, inclusive teclado aberto; resultado legível e compreensível; oferta fora da tela não gera exposição. Registrar URL, versão, capturas e resultado por cenário. Medir: conclusão funcional, erros e tempo até o benefício em tráfego de QA segregado. Correções retornam aqui; teste aprovado não prova vendas.',
  '$.nodes[7].description',
  'Entregar: avaliação independente de compreensão, desejo, esforço, utilidade e próximo passo na versão real. Aceite: localizar primeira ação sem ajuda, concluir entradas, compreender como aplicar o resultado e explicar o valor adicional da continuidade paga. Registrar evidência observada e lacunas; resultado exibido não comprova aplicação ou satisfação. Medir: conclusão e tempo por cenário. Identificar a leitura como simulação de agente, sem contá-la como cliente ou venda.',
  '$.nodes[8].description',
  'Entregar: avaliação da versão real com entrada incompleta, erro recuperável e retomada. Aceite: orientação suficiente sem conhecimento de IA, respostas preservadas quando permitido, próximo botão e botão final alcançáveis no celular e nenhuma promessa de resultado inventado. Medir: recuperação, abandono por etapa e esforço observado; separar hipótese de causa, defeito reproduzido e preferência humana.',
  '$.commercialCriteriaVersion',
  'PDE_COMMERCIAL_PRINCIPLES_V1'),UTC_TIMESTAMP(),UTC_TIMESTAMP()
FROM business_process_definition source
LEFT JOIN business_process_definition existing ON existing.process_code=source.process_code AND existing.version_number=9
WHERE source.process_code='pde-construction-approval' AND source.version_number=8 AND existing.id IS NULL;

-- pde-communication-sales-journey: v7 -> v8; somente objetivos, preservando contratos, responsáveis e fluxos.
INSERT INTO business_process_definition
 (process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,
  technical_reference,process_type,parent_process_code,execution_scope,diagram_json,created_at,published_at)
SELECT source.process_code,source.name,source.purpose,source.owner_name,source.trigger_description,
 source.outcome_description,8,'PUBLISHED','docs/canonical/cadeia-produtos-pde-canon.v1.md / cinco criterios comerciais',
 source.process_type,source.parent_process_code,source.execution_scope,
 JSON_SET(source.diagram_json,
  '$.nodes[1].description',
  'Entregar: desejo escolhido por Atena, linguagem do público com fonte/data, mensagem/CTA e prova real da versão aprovada; explicitar benefício inicial e benefício adicional pago, entregáveis, uso, preço total/recorrência, prazo, acesso, suporte e reembolso. Aceite: fidelidade ao tipo, ficha e oferta; nenhuma emoção inferida como fato. Registrar se há degustação no plano: quando houver, referenciar pde-tasting-proof-of-value e exigir suas evidências antes de integrar; caso contrário justificar demonstração/amostra real adequada. Medir: primeira interação e oferta efetivamente vista → CTA → checkout → compra, por variante/janela.',
  '$.nodes[3].description',
  'Entregar: destino aprovado que torna a primeira ação evidente e demonstra valor real antes do compromisso, conforme o contrato do produto. Aceite: delegar landing ao subprocesso canônico somente quando prevista; cumprir promessa inicial, explicar limite gratuito e ganho pago; sem obrigar vídeo, questionário ou cadastro universais. Se o plano escolher degustação, referenciar sua execução e pareceres antes da integração. Medir: visitantes → primeira ação → benefício e exposição efetiva à oferta, sem confundir geração com visualização.',
  '$.nodes[4].description',
  'Entregar: versões aprovadas de canal, destino, checkout e acesso com eventos correlacionados até venda, entrega e primeiro uso. Aceite: quando o plano escolher degustação, conferir referência da execução pde-tasting-proof-of-value, prova útil, limites, custo e pareceres; sem degustação, registrar motivo e demonstração real. Declarar gatilho, unidade, denominador, janela e segregação de cada evento. Exposição à oferta exige visibilidade real e tempo mínimo definidos no plano; resultado pronto ou oferta fora da tela não a comprovam. Medir: benefício → oferta vista → clique → checkout → compra conciliada, além de aplicação, acesso e retorno. Não publicar nem autorizar gasto.',
  '$.commercialCriteriaVersion',
  'PDE_COMMERCIAL_PRINCIPLES_V1'),UTC_TIMESTAMP(),UTC_TIMESTAMP()
FROM business_process_definition source
LEFT JOIN business_process_definition existing ON existing.process_code=source.process_code AND existing.version_number=8
WHERE source.process_code='pde-communication-sales-journey' AND source.version_number=7 AND existing.id IS NULL;

-- pde-tasting-proof-of-value: v2 -> v3; somente objetivos, preservando contratos, responsáveis e fluxos.
INSERT INTO business_process_definition
 (process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,
  technical_reference,process_type,parent_process_code,execution_scope,diagram_json,created_at,published_at)
SELECT source.process_code,source.name,source.purpose,source.owner_name,source.trigger_description,
 source.outcome_description,3,'PUBLISHED','docs/canonical/cadeia-produtos-pde-canon.v1.md / cinco criterios comerciais',
 source.process_type,source.parent_process_code,source.execution_scope,
 JSON_SET(source.diagram_json,
  '$.nodes[1].description',
  'Entregar: hipótese do desejo, público/situação, benefício inicial aplicável e ligação com a continuidade paga, usando fontes com data. Aceite: degustação explicitamente escolhida no plano; respeitar tipo e ficha, definir o que a pessoa faz/recebe e uma explicação concorrente; não impor questionário ou cadastro antes do valor. Medir: primeira interação, benefício recebido e interesse em continuar, com versão, janela e denominadores.',
  '$.nodes[2].description',
  'Entregar: fronteira gratuita/paga, custo máximo por uso, limite total, expiração/antiabuso e economia da continuidade paga. Aceite: custos completos e origem das premissas, cenários conservador/esperado/uso intenso, contribuição e margem vigentes; desconhecido bloqueia, sem virar zero. Medir: custo por benefício útil e por compra líquida; impedir que participação gratuita seja tratada como receita. Parecer não autoriza gasto.',
  '$.nodes[3].description',
  'Entregar: pequena orientação aplicável, amostra ou uso limitado real do produto aprovado, com primeira ação fácil e continuidade clara. Aceite: cumprir integralmente a promessa inicial; próximo botão e botão final acessíveis no celular, entradas mínimas, limites/consentimento/retomada conforme contrato; não esconder o benefício prometido atrás de novo cadastro. Medir: início → conclusão → resultado recebido, aplicação relatada separada de exibição e de teste sintético.',
  '$.nodes[4].description',
  'Entregar: avaliação da compreensão e aplicação do benefício inicial, esforço e motivo para continuar pagando. Aceite: observar percurso e botões no celular; explicar com evidência o que a pessoa recebe agora e o que ganha ao pagar; não inferir satisfação de resultado exibido ou e-mail. Medir: conclusão, utilidade observada/relatada e entendimento da oferta. Identificar simulação de agente; observações humanas consentidas e amostra paga ficam separadas.',
  '$.nodes[5].description',
  'Entregar: conferência de promessa, prova real, fronteira gratuita/paga, preço total/recorrência, entregáveis, uso, prazo, acesso, suporte, reembolso, direitos e privacidade na mesma versão. Aceite: consistência com produto e checkout, nenhum depoimento fictício e nenhuma promessa inicial descumprida para forçar compra. Medir: divergências comprovadas e evidência de correção; aprovação não equivale a venda.',
  '$.nodes[6].description',
  'Entregar: vínculo auditável ao Processo 4 antes de integrar a jornada, com versão, pareceres, custo/limites e eventos de degustação, resultado, oferta, checkout, compra, acesso, retorno, expiração e abuso. Aceite: gatilhos observáveis, unidade/denominador/janela, correlação e testes segregados; oferta vista depende de visibilidade e tempo mínimo definidos, nunca apenas resultado pronto. Medir: valor recebido → exposição efetiva → CTA → checkout → compra líquida; resultado exibido não comprova aplicação.',
  '$.commercialCriteriaVersion',
  'PDE_COMMERCIAL_PRINCIPLES_V1'),UTC_TIMESTAMP(),UTC_TIMESTAMP()
FROM business_process_definition source
LEFT JOIN business_process_definition existing ON existing.process_code=source.process_code AND existing.version_number=3
WHERE source.process_code='pde-tasting-proof-of-value' AND source.version_number=2 AND existing.id IS NULL;

-- operacao-otimizacao-experimento: v5 -> v6; somente objetivos, preservando contratos, responsáveis e fluxos.
INSERT INTO business_process_definition
 (process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,
  technical_reference,process_type,parent_process_code,execution_scope,diagram_json,created_at,published_at)
SELECT source.process_code,source.name,source.purpose,source.owner_name,source.trigger_description,
 source.outcome_description,6,'PUBLISHED','docs/canonical/cadeia-produtos-pde-canon.v1.md / cinco criterios comerciais',
 source.process_type,source.parent_process_code,source.execution_scope,
 JSON_SET(source.diagram_json,
  '$.nodes[1].description',
  'Entregar: conciliação de eventos com fonte, versão, gatilho, unidade, numerador/denominador, atribuição e janela. Aceite: visitantes humanos distintos; testes, bots, duplicidades e reembolsos segregados; não inventar zero para fonte ausente. Verificar exposição real à oferta com limiar declarado; resultado pronto não dispara oferta vista. Medir: separar primeira interação, conclusão, resultado, aplicação/relato, cadastro, login, retorno, checkout e compra.',
  '$.nodes[2].description',
  'Entregar: amostra e janela atingidas ou leitura inconclusiva, usando o planejamento aprovado. Aceite: contar pessoas elegíveis deduplicadas, aguardar janela de compra e reportar incerteza; não copiar 100/500 visitas a todo produto sem justificativa. Parada financeira protege o caixa, mas não comprova rejeição estatística. Medir: amostra observada/meta, compras líquidas e cobertura das fontes, sempre dentro do teto autorizado.',
  '$.nodes[3].description',
  'Entregar: primeira perda mensurável do funil com numerador, denominador, versão, origem e janela; hipótese causal e explicação concorrente. Aceite: comparar início → conclusão → resultado → oferta efetivamente vista → checkout → compra, sem confundir cadastro/login nem resultado/uso; registrar lacunas e amostra insuficiente. Medir: abandono por etapa e tempo até o benefício. Correlação ou clique não confirma causa, satisfação ou lucro.',
  '$.nodes[4].description',
  'Entregar: uma variável principal, referência anterior, hipótese, explicação concorrente, condições mantidas e critério de sucesso/falha predefinido. Aceite: preservar estratégia, preço, público e produto quando não forem a variável autorizada; contradição volta a Atena, produto a Dédalo e comunicação a Íris. Medir: efeito na etapa-alvo junto com compras líquidas, entrega, CAC e margem; participação isolada não autoriza escala.',
  '$.nodes[7].description',
  'Entregar: parecer sobre orçamento, receita líquida, CAC e custo integral da variante, incluindo IA, suporte, taxas, entrega e reembolsos. Aceite: fontes/validade e cenários conservador/esperado/uso intenso; desconhecido não é zero; contribuição positiva e margem conforme política aprovada. Medir: compras líquidas, contribuição e margem após custos e aquisição. Parecer não autoriza gasto ou ampliação de teto.',
  '$.nodes[10].description',
  'Entregar: comparação da variante com a referência, uma mudança principal, condições mantidas, amostra/janela e limitações; preservar histórico. Aceite: evidência suficiente ou conclusão inconclusiva; testes qualitativos não contam na amostra paga. Medir: etapa-alvo, vendas líquidas, receita conciliada, CAC, custo completo, entrega útil e reembolso. Melhor participação sem venda/margem não comprova sucesso comercial.',
  '$.commercialCriteriaVersion',
  'PDE_COMMERCIAL_PRINCIPLES_V1'),UTC_TIMESTAMP(),UTC_TIMESTAMP()
FROM business_process_definition source
LEFT JOIN business_process_definition existing ON existing.process_code=source.process_code AND existing.version_number=6
WHERE source.process_code='operacao-otimizacao-experimento' AND source.version_number=5 AND existing.id IS NULL;

-- value-chain-learning-sales-cycle: v4 -> v5; somente objetivos, preservando contratos, responsáveis e fluxos.
INSERT INTO business_process_definition
 (process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,
  technical_reference,process_type,parent_process_code,execution_scope,diagram_json,created_at,published_at)
SELECT source.process_code,source.name,source.purpose,source.owner_name,source.trigger_description,
 source.outcome_description,5,'PUBLISHED','docs/canonical/cadeia-produtos-pde-canon.v1.md / cinco criterios comerciais',
 source.process_type,source.parent_process_code,source.execution_scope,
 JSON_SET(source.diagram_json,
  '$.nodes[1].description',
  'Entregar: aprendizado com fonte/data, versões, estado/objetivo da etapa, fato observado, hipótese e explicação concorrente; separar sucessos, falhas e limites de validade. Aceite: conciliação do experimento anterior sem transformar participação do experimento de referência em causalidade ou venda; resolver contradições antes de reutilizar memória. Medir: evidência nova, recorrência do gargalo e resultado comercial conhecido, sem dados pessoais no aprendizado.',
  '$.nodes[2].description',
  'Entregar: referência anterior, uma variável principal, hipótese e explicação concorrente; oferta, canal, público e entrega mantidos quando não forem a variável, amostra, janela de compra, atribuição, teto e parada predefinidos. Aceite: metas justificadas por produto e economia, custo desconhecido pendente; mudança material requer aprovação própria. Medir: etapa-alvo e guardas de compras líquidas, utilidade, CAC, custo integral e margem. Não confundir limite financeiro com conclusão estatística.',
  '$.nodes[12].description',
  'O backend concilia fontes do experimento e persiste período, versão e segregação; resultados não são digitados. Entregar: pessoas humanas distintas, início/conclusão/resultado, oferta efetivamente vista, cadastro/login/retorno, checkout, compras líquidas, receita, custo integral, CAC, entrega/uso e reembolso quando houver fonte. Aceite: gatilhos e denominadores válidos; fonte ausente não vira zero; resultado pronto não comprova oferta vista ou satisfação. Medir: cobertura, amostra/incerteza e contribuição/margem conforme política.',
  '$.nodes[13].description',
  'Entregar: proposta de Atena comparando três alternativas com a conciliação oficial, justificativa, evidência, aprendizado, hipótese, explicação concorrente e destino. Aceite: separar sinal de participação de venda; amostra insuficiente é inconclusiva; escala exige compras líquidas, entrega útil, custos essenciais conhecidos, contribuição positiva e margem vigente. Usuário pode editar/aprovar; proposta inválida/desatualizada bloqueia o losango. Medir: vendas líquidas, CAC, margem e entrega. Este parecer não autoriza gasto/publicação.',
  '$.commercialCriteriaVersion',
  'PDE_COMMERCIAL_PRINCIPLES_V1'),UTC_TIMESTAMP(),UTC_TIMESTAMP()
FROM business_process_definition source
LEFT JOIN business_process_definition existing ON existing.process_code=source.process_code AND existing.version_number=5
WHERE source.process_code='value-chain-learning-sales-cycle' AND source.version_number=4 AND existing.id IS NULL;

-- pde-sales-delivery-learning: v8 -> v9; somente objetivos, preservando contratos, responsáveis e fluxos.
INSERT INTO business_process_definition
 (process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,
  technical_reference,process_type,parent_process_code,execution_scope,diagram_json,created_at,published_at)
SELECT source.process_code,source.name,source.purpose,source.owner_name,source.trigger_description,
 source.outcome_description,9,'PUBLISHED','docs/canonical/cadeia-produtos-pde-canon.v1.md / cinco criterios comerciais',
 source.process_type,source.parent_process_code,source.execution_scope,
 JSON_SET(source.diagram_json,
  '$.nodes[6].description',
  'O backend concilia automaticamente fontes do experimento. Entregar: receita conciliada, compras líquidas após reembolsos, CAC, custo integral, contribuição/margem e entrega/uso/satisfação com cobertura, versão e janela. Aceite: humanos deduplicados e testes separados; fonte inválida bloqueia, desconhecido não é zero. Participação e resultado exibido são indicadores intermediários, não venda ou utilidade comprovada. Medir: compras líquidas, contribuição, margem e cobertura dos resultados.',
  '$.nodes[7].description',
  'Entregar: ciclo deste produto/experimento com referência anterior, aprendizado, fato versus hipótese, explicação concorrente, uma variável alterada e condições mantidas. Aceite: registrar amostra/janela/atribuição, limites aprovados, evidência nova e decisão; reutilizar resultados válidos e seguir a atividade orientada pela causa. Medir: compras líquidas, CAC, custo integral, contribuição, margem e entrega útil. Escala depende de resultado sustentável e nova autorização, nunca apenas participação.',
  '$.commercialCriteriaVersion',
  'PDE_COMMERCIAL_PRINCIPLES_V1'),UTC_TIMESTAMP(),UTC_TIMESTAMP()
FROM business_process_definition source
LEFT JOIN business_process_definition existing ON existing.process_code=source.process_code AND existing.version_number=9
WHERE source.process_code='pde-sales-delivery-learning' AND source.version_number=8 AND existing.id IS NULL;

INSERT INTO business_process_activity_definition
 (process_definition_id,activity_id,name,objective,owner_name,execution_resource_code,subprocess_code,definition_json,created_at)
SELECT p.id,JSON_UNQUOTE(JSON_EXTRACT(p.diagram_json,CONCAT('$.nodes[',n.number,'].id'))),JSON_UNQUOTE(JSON_EXTRACT(p.diagram_json,CONCAT('$.nodes[',n.number,'].label'))),JSON_UNQUOTE(JSON_EXTRACT(p.diagram_json,CONCAT('$.nodes[',n.number,'].description'))),
 JSON_UNQUOTE(JSON_EXTRACT(p.diagram_json,CONCAT('$.nodes[',n.number,'].owner'))),JSON_UNQUOTE(JSON_EXTRACT(p.diagram_json,CONCAT('$.nodes[',n.number,'].executionResourceCode'))),JSON_UNQUOTE(JSON_EXTRACT(p.diagram_json,CONCAT('$.nodes[',n.number,'].subprocessCode'))),
 JSON_EXTRACT(p.diagram_json,CONCAT('$.nodes[',n.number,']')),UTC_TIMESTAMP()
FROM business_process_definition p
JOIN (SELECT 0 AS number UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15 UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20 UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24 UNION ALL SELECT 25 UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL SELECT 28 UNION ALL SELECT 29 UNION ALL SELECT 30 UNION ALL SELECT 31) n ON n.number < JSON_LENGTH(JSON_EXTRACT(p.diagram_json,'$.nodes'))
LEFT JOIN business_process_activity_definition existing ON existing.process_definition_id=p.id AND existing.activity_id=JSON_UNQUOTE(JSON_EXTRACT(p.diagram_json,CONCAT('$.nodes[',n.number,'].id')))
WHERE ((p.process_code='pde-commercial-plan-offer' AND p.version_number=7) OR (p.process_code='pde-construction-approval' AND p.version_number=9) OR (p.process_code='pde-communication-sales-journey' AND p.version_number=8) OR (p.process_code='pde-tasting-proof-of-value' AND p.version_number=3) OR (p.process_code='operacao-otimizacao-experimento' AND p.version_number=6) OR (p.process_code='value-chain-learning-sales-cycle' AND p.version_number=5) OR (p.process_code='pde-sales-delivery-learning' AND p.version_number=9)) AND JSON_UNQUOTE(JSON_EXTRACT(p.diagram_json,CONCAT('$.nodes[',n.number,'].type')))='TASK' AND existing.id IS NULL;

INSERT INTO business_process_chain_definition
 (chain_code,name,purpose,outcome_description,primary_metric,version_number,status,created_at,published_at)
SELECT source.chain_code,source.name,CONCAT(source.purpose,' Critérios comerciais explícitos v1.'),source.outcome_description,source.primary_metric,
 18,'PUBLISHED',UTC_TIMESTAMP(),UTC_TIMESTAMP()
FROM business_process_chain_definition source
LEFT JOIN business_process_chain_definition existing ON existing.chain_code=source.chain_code AND existing.version_number=18
WHERE source.chain_code='pde-value-creation-delivery' AND source.version_number=17 AND existing.id IS NULL;

INSERT INTO business_process_chain_item
 (chain_definition_id,process_definition_id,sequence_number,value_contribution,created_at)
SELECT target.id,COALESCE(new_process.id,old_process.id),item.sequence_number,item.value_contribution,UTC_TIMESTAMP()
FROM business_process_chain_definition source
JOIN business_process_chain_item item ON item.chain_definition_id=source.id
JOIN business_process_definition old_process ON old_process.id=item.process_definition_id
LEFT JOIN business_process_definition new_process ON new_process.process_code=old_process.process_code
 AND new_process.version_number=old_process.version_number+1
 AND JSON_UNQUOTE(JSON_EXTRACT(new_process.diagram_json,'$.commercialCriteriaVersion'))='PDE_COMMERCIAL_PRINCIPLES_V1'
JOIN business_process_chain_definition target ON target.chain_code=source.chain_code AND target.version_number=18
LEFT JOIN business_process_chain_item existing ON existing.chain_definition_id=target.id AND existing.sequence_number=item.sequence_number
WHERE source.chain_code='pde-value-creation-delivery' AND source.version_number=17 AND existing.id IS NULL;


UPDATE business_process_definition SET status='PUBLISHED' WHERE process_code='pde-commercial-plan-offer' AND version_number=7;
UPDATE business_process_definition SET status='PUBLISHED' WHERE process_code='pde-construction-approval' AND version_number=9;
UPDATE business_process_definition SET status='PUBLISHED' WHERE process_code='pde-communication-sales-journey' AND version_number=8;
UPDATE business_process_definition SET status='PUBLISHED' WHERE process_code='pde-tasting-proof-of-value' AND version_number=3;
UPDATE business_process_definition SET status='PUBLISHED' WHERE process_code='operacao-otimizacao-experimento' AND version_number=6;
UPDATE business_process_definition SET status='PUBLISHED' WHERE process_code='value-chain-learning-sales-cycle' AND version_number=5;
UPDATE business_process_definition SET status='PUBLISHED' WHERE process_code='pde-sales-delivery-learning' AND version_number=9;
UPDATE business_process_chain_definition SET status='PUBLISHED' WHERE chain_code='pde-value-creation-delivery' AND version_number=18;

-- pde-opportunity-discovery: v8 -> v9; contratos executáveis preservados.
INSERT INTO business_process_definition
 (process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,
  technical_reference,process_type,parent_process_code,execution_scope,diagram_json,created_at,published_at)
SELECT source.process_code,source.name,
 CONCAT(source.purpose,' Satisfação pretendida, prova funcional, identidade e ocasião de retorno são explícitas; emoção e memória permanecem hipóteses.'),
 source.owner_name,source.trigger_description,source.outcome_description,9,'PUBLISHED',
 'docs/canonical/cadeia-produtos-pde-canon.v1.md / SATISFACTION_CONTINUITY_V1',
 source.process_type,source.parent_process_code,source.execution_scope,
 JSON_SET(source.diagram_json,
  '$.nodes[1].description',
  'Entregar duas ou três candidatas com ocasião concreta, resultado desejado, dificuldade e alternativa usada; qualificar ofertas atuais por preço, entrega, público, aderência e data; distinguir linguagem de vendedores, relatos de clientes, anúncios e artigos científicos. Registrar lacunas por candidata, sem escolher mercado, oferta ou produto. Aceite: fontes rastreáveis, atuais quando a decisão exigir e separadas por função; referência antiga ou indireta permanece marcada. Medir cobertura, atualidade, independência, ofertas aderentes e custo conhecido da coleta. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Situação reconhecível, satisfação possível (orgulho, alívio, segurança ou praticidade), linguagem pública, ocasião, alternativas e contrapontos com URL, data e papel da fonte. Aceite: Separar relato espontâneo, publicidade e inferência; não escolher estratégia nem apresentar hipótese emocional como sentimento comprovado. Medir: Cobertura, atualidade, independência e lacunas das fontes.',
  '$.nodes[2].description',
  'Entregar um plano por candidata com pergunta pendente, fonte adequada, evidência necessária, contraponto, consultas executadas e limite de consultas/custo. Reutilizar fatos válidos e investigar automaticamente relatos públicos de uso, compra, desistência e reclamação; preservar URL, data, trecho, papel da fonte e limites de interpretação. Aceite: preservar identidades das candidatas, cobrir todas, registrar o que resolveu ou não cada lacuna e parar sem aprovação quando não houver progresso ou o teto for atingido. Não exige entrevistas, recrutamento nem contato externo. Relatos públicos não comprovam vendas do nosso produto. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Evidência específica que sustente ou contradiga a satisfação proposta e a ocasião em que o benefício volta a ser necessário. Aceite: Reutilizar fontes válidas, aprofundar somente lacunas com consultas e limites aprovados; sem entrevistas, recrutamento ou opinião solicitada. Medir: Lacunas resolvidas ou preservadas, consultas, custo conhecido e limites de validade.',
  '$.satisfactionContinuityVersion',
  'SATISFACTION_CONTINUITY_V1'),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)
FROM business_process_definition source
LEFT JOIN business_process_definition existing ON existing.process_code=source.process_code AND existing.version_number=9
WHERE source.process_code='pde-opportunity-discovery' AND source.version_number=8 AND existing.id IS NULL;

-- pde-commercial-plan-offer: v10 -> v11; contratos executáveis preservados.
INSERT INTO business_process_definition
 (process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,
  technical_reference,process_type,parent_process_code,execution_scope,diagram_json,created_at,published_at)
SELECT source.process_code,source.name,
 CONCAT(source.purpose,' Satisfação pretendida, prova funcional, identidade e ocasião de retorno são explícitas; emoção e memória permanecem hipóteses.'),
 source.owner_name,source.trigger_description,source.outcome_description,11,'PUBLISHED',
 'docs/canonical/cadeia-produtos-pde-canon.v1.md / SATISFACTION_CONTINUITY_V1',
 source.process_type,source.parent_process_code,source.execution_scope,
 JSON_SET(source.diagram_json,
  '$.nodes[1].description',
  'Entregar estratégia MARKET_STRATEGY_V4 com hipótese, ocasião, alternativa, valor pronto, fontes públicas vigentes e plano PDE_AGENT_VALIDATION_V1. Aceite: exatamente três cenários e três dispositivos, nenhum recrutamento, convite, leitura privada, opinião solicitada, pessoa fictícia, pagamento, publicação, campanha ou gasto. Agentes não constituem prova de mercado. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Uma satisfação principal como hipótese, situação, resultado funcional que pode proporcioná-la, fonte/data, explicação concorrente, identidade do produto e ocasião futura de utilidade. Aceite: Comparar orgulho, alívio ou outra hipótese sustentada, escolher uma; preservar tipo, oferta e canal aprovados, sem inferir emoção individual. Medir: Primeira interação por variante/janela, compras e contribuição; linguagem reconhecível não comprova venda.',
  '$.nodes[2].description',
  'Entregar preço e custo como hipóteses, envelope técnico finito e contribuição projetada. Aceite: custo auditável por pacote, nenhuma cobrança real e nenhum orçamento de mídia autorizado. Medir custo real da construção sem declarar venda ou margem comprovada. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Economia da entrega e de eventual amostra: custo integral, tentativas, suporte, usos sem compra, teto por uso/total e continuidade paga. Aceite: Reutilizar parecer válido; custos desconhecidos permanecem pendentes, cenários conservador/esperado/intenso e limites aprovados sem novo gasto. Medir: CAC, compras líquidas, custo por entrega útil e contribuição após aquisição e todos os custos variáveis.',
  '$.nodes[3].description',
  'Entregar jornada, componentes, audiovisual, acesso e instrumentação para homologação ADHERENT, RECOVERY e SAFETY em desktop, iPhone e Pixel. Aceite: resultado pronto em até dez minutos, tráfego interno segregado e efeitos externos nulos. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Relação entre satisfação pretendida, ação, resultado utilizável, vantagem sobre alternativas, identidade e retomada quando contratada. Aceite: Prova adequada ao formato aprovado, entradas necessárias e contexto preservado; demonstração ou degustação somente quando prevista, com limites de Plutus. Medir: Esforço, erros, tempo até benefício e aplicação observável, separados de satisfação e compra.',
  '$.satisfactionContinuityVersion',
  'SATISFACTION_CONTINUITY_V1'),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)
FROM business_process_definition source
LEFT JOIN business_process_definition existing ON existing.process_code=source.process_code AND existing.version_number=11
WHERE source.process_code='pde-commercial-plan-offer' AND source.version_number=10 AND existing.id IS NULL;

-- pde-construction-approval: v9 -> v10; contratos executáveis preservados.
INSERT INTO business_process_definition
 (process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,
  technical_reference,process_type,parent_process_code,execution_scope,diagram_json,created_at,published_at)
SELECT source.process_code,source.name,
 CONCAT(source.purpose,' Satisfação pretendida, prova funcional, identidade e ocasião de retorno são explícitas; emoção e memória permanecem hipóteses.'),
 source.owner_name,source.trigger_description,source.outcome_description,10,'PUBLISHED',
 'docs/canonical/cadeia-produtos-pde-canon.v1.md / SATISFACTION_CONTINUITY_V1',
 source.process_type,source.parent_process_code,source.execution_scope,
 JSON_SET(source.diagram_json,
  '$.nodes[1].description',
  'Entregar: jornada privada do formato aprovado com primeira ação evidente, apenas entradas necessárias e benefício aplicável. Aceite: justificar cada pergunta, manter próximo botão e botão final alcançáveis no celular sem rolagem inesperada ou conteúdo bloqueante; cumprir a promessa inicial e explicar o próximo passo. Medir: início, conclusão das entradas e resultado recebido separadamente, abandono por etapa e tempo até o benefício. A quantidade de escolhas depende do benefício e do formato, sem padrão universal. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Caminho mínimo da situação reconhecível até a ação e o resultado que tornam plausível a satisfação proposta. Aceite: Contexto fiel, entradas justificadas, aplicação compreensível e primeira ação mobile; não exigir cadastro, questionário ou amostra universais. Medir: Conclusão, abandono por etapa e tempo até benefício na mesma versão.',
  '$.nodes[2].description',
  'Produz os componentes funcionais da mesma versão do produto sem reutilizar artefato de outro PDE. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Resultado e referências fiéis às informações da cliente, com identidade coerente e orientação para uso. Aceite: Comprovar personalização e correspondência entre apresentação e saída funcional; demonstração sintética identificada não representa cliente real. Medir: Utilidade funcional, erros de contexto e correspondência entre promessa e entregável.',
  '$.nodes[3].description',
  'Produz somente o audiovisual exigido; a ausência de necessidade deve permanecer explícita. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Audiovisual funcional previsto, com contexto e prova coerentes com a mesma versão real. Aceite: Preservar peças, corpo e contexto da representação quando houver transformação ilustrativa; não inventar resultados, depoimentos ou necessidade de vídeo. Medir: Compreensão, legibilidade mobile, continuidade e integridade técnica; revisão não comprova emoção humana.',
  '$.nodes[4].description',
  'Entregar: acesso de teste, salvamento/retomada quando previstos, tratamento de erro, privacidade e eventos correlacionados à versão. Aceite: cadastro não equivale a login; resultado exibido não equivale a uso; registrar consentimento e excluir testes da amostra comercial. Medir: resultado → interesse em salvar → cadastro → acesso concluído → retorno, sem inferir etapas ausentes. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Identidade identificável, resultado salvo e reencontro simples quando acesso/retomada fizerem parte da oferta. Aceite: Preservar contexto e privacidade, recuperação sem duplicar geração/cobrança; nenhuma recorrência ou recompra obrigatória inventada. Medir: Acesso, retomada e retorno voluntário em eventos próprios; retorno não comprova memória ou satisfação.',
  '$.nodes[5].description',
  'Entregar: evidências da mesma versão em desktop, iPhone e Android, com caminho feliz, falha e retomada. Aceite: primeira ação, próximo botão e botão final visíveis/acessíveis após cada passo, inclusive teclado aberto; resultado legível e compreensível; oferta fora da tela não gera exposição. Registrar URL, versão, capturas e resultado por cenário. Medir: conclusão funcional, erros e tempo até o benefício em tráfego de QA segregado. Correções retornam aqui; teste aprovado não prova vendas. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Manifesto, hashes, capturas e testes da mesma versão para ação, resultado, aplicação e recuperação em desktop/iPhone/Android. Aceite: Correspondência da promessa com o comportamento servido; dados sintéticos segregados e bloqueio técnico antes de revisão paga se a prova não corresponder. Medir: Conclusão funcional, erros, tempo até resultado e custo conhecido por homologação concluída.',
  '$.nodes[6].description',
  'Transforma a rejeição funcional vigente em instruções executáveis, corrige a causa-raiz, cria nova versão e devolve o fluxo à homologação técnica. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Correção na camada da causa comprovada da promessa, personalização, utilidade, acesso ou recuperação. Aceite: Preservar entradas e resultados válidos; nova versão retorna à homologação técnica, sem contornar gate ou repetir consumo por impedimento idêntico. Medir: Recorrência do defeito e conclusão funcional comparadas à versão anterior; nenhuma venda presumida.',
  '$.nodes[7].description',
  'Entregar: avaliação independente de compreensão, desejo, esforço, utilidade e próximo passo na versão real. Aceite: localizar primeira ação sem ajuda, concluir entradas, compreender como aplicar o resultado e explicar o valor adicional da continuidade paga. Registrar evidência observada e lacunas; resultado exibido não comprova aplicação ou satisfação. Medir: conclusão e tempo por cenário. Identificar a leitura como simulação de agente, sem contá-la como cliente ou venda. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Avaliação simulada de clareza, esforço, aplicação do resultado e plausibilidade da satisfação pretendida na experiência real. Aceite: Identificar a simulação; verificar próximo passo e benefício adicional sem afirmar satisfação, preferência humana ou cliente inexistente. Medir: Conclusão, esforço e tempo por cenário; resultado exibido não comprova aplicação.',
  '$.nodes[8].description',
  'Entregar: avaliação da versão real com entrada incompleta, erro recuperável e retomada. Aceite: orientação suficiente sem conhecimento de IA, respostas preservadas quando permitido, próximo botão e botão final alcançáveis no celular e nenhuma promessa de resultado inventado. Medir: recuperação, abandono por etapa e esforço observado; separar hipótese de causa, defeito reproduzido e preferência humana. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Avaliação simulada de contexto preservado e retomada após entrada incompleta ou falha. Aceite: Orientação suficiente no celular, limites honestos e recuperação sem esforço redundante; não recrutar pessoas nem solicitar opinião. Medir: Recuperação, perda de contexto e erros reproduzidos, separados de preferência estética.',
  '$.nodes[9].description',
  'Com raciocínio máximo, avalia pedido fora do escopo e exige bloqueio seguro sem resultado inventado. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Avaliação simulada de limites e segurança da promessa funcional e emocional. Aceite: Bloquear pedidos fora do escopo, diagnóstico psicológico, satisfação garantida e resultado inventado; respeitar autonomia e preferências. Medir: Falhas de segurança e bloqueios corretos no cenário segregado.',
  '$.nodes[10].description',
  'Confirma fidelidade, segurança, verdade, privacidade, segregação interna e ausência de alegação humana ou efeito comercial. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Parecer independente que liga promessa, prova funcional, identidade, versão e limites da transformação representada. Aceite: Personagem não parece depoimento real; nenhuma compra, agendamento, satisfação ou emoção fictícia como prova; fonte/prova ausente retorna à origem. Medir: Divergências confirmadas, correções e evidências; aprovação não comprova vendas.',
  '$.nodes[11].description',
  'Valida somente evidências persistidas, sequenciais e da mesma versão; libera comunicação em STOP sem publicar, cobrar, criar campanha ou autorizar gasto. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Referências persistidas das avaliações sequenciais da mesma versão, com fatos e hipóteses separados. Aceite: Preservar o gate determinístico e os resultados válidos; liberar somente comunicação em STOP, sem afirmar satisfação ou demanda nem autorizar gasto. Medir: Cobertura de evidências e bloqueios técnicos claros, com responsável e ação.',
  '$.satisfactionContinuityVersion',
  'SATISFACTION_CONTINUITY_V1'),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)
FROM business_process_definition source
LEFT JOIN business_process_definition existing ON existing.process_code=source.process_code AND existing.version_number=10
WHERE source.process_code='pde-construction-approval' AND source.version_number=9 AND existing.id IS NULL;

-- pde-communication-sales-journey: v9 -> v10; contratos executáveis preservados.
INSERT INTO business_process_definition
 (process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,
  technical_reference,process_type,parent_process_code,execution_scope,diagram_json,created_at,published_at)
SELECT source.process_code,source.name,
 CONCAT(source.purpose,' Satisfação pretendida, prova funcional, identidade e ocasião de retorno são explícitas; emoção e memória permanecem hipóteses.'),
 source.owner_name,source.trigger_description,source.outcome_description,10,'PUBLISHED',
 'docs/canonical/cadeia-produtos-pde-canon.v1.md / SATISFACTION_CONTINUITY_V1',
 source.process_type,source.parent_process_code,source.execution_scope,
 JSON_SET(source.diagram_json,
  '$.nodes[1].description',
  'Entregar: desejo escolhido por Atena, linguagem do publico com fonte/data, mensagem, CTA e prova real da versao aprovada para Instagram Ads; explicitar beneficio inicial e beneficio adicional pago, entregaveis, uso, preco, prazo, acesso, suporte e reembolso. Aceite: fidelidade ao tipo, ficha e oferta; nenhuma emocao inferida como fato; convite individual, lista propria, contato direto e divulgacao organica nao sao canais substitutos. Medir: primeira interacao e oferta efetivamente vista -> CTA -> checkout -> compra, por campanha, variante e janela. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Situação familiar → participação concreta do produto → resultado utilizável → satisfação possível; satisfação proposta e fontes, identidade contínua, ocasião de retorno e contrapontos nos campos existentes do contrato. Aceite: Preservar estratégia, versão, preço e canal; cada promessa aponta para prova real. Explicar adicional pago e limites. Degustação escolhida referencia pde-tasting-proof-of-value; sem contrato/homologação, comunicar somente demonstração disponível. Medir: Primeira interação, demonstração, oferta efetivamente vista, checkout e compra em eventos distintos.',
  '$.nodes[2].description',
  'Entregar: criativos finais para Instagram Ads no formato aprovado pelo contrato do produto, com identidade publica, promessa, prova, CTA e destino coerentes. Aceite: revisao independente, direitos de uso e correspondencia com oferta, checkout e entrega; nao produzir mensagem de convite individual. Medir: impressoes atribuidas -> primeira interacao e custo por etapa, sem confundir producao com desempenho. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Briefing para Íris/Apolo com identificação cotidiana, produto em uso, transformação possível e nome reconhecível, delegando ao subprocesso canônico. Aceite: Personagem e encenação identificadas, sem agenda cheia ou depoimento inventado; identidade consistente com página, prova e entrega. Medir: Impressão → interação → visita por variante; desempenho depende do mercado.',
  '$.nodes[3].description',
  'Entregar: destino aprovado que torna a primeira acao evidente e demonstra valor real antes do compromisso, conforme o contrato do produto. Aceite: receber trafego pago do Instagram com continuidade, checkout e recuperacao funcionais em celular; cumprir a promessa inicial e explicar o ganho pago. Medir: visitantes atribuidos -> primeira acao -> beneficio e exposicao efetiva a oferta. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Destino que demonstra o resultado real, participação e benefício adicional pago com a mesma identidade. Aceite: Amostra personalizada somente quando prevista/homologada no subprocesso de degustação; alternativa real documentada quando não aplicável. Não prometer funcionalidade futura. Medir: Demonstração, resultado recebido, oferta vista e checkout separados; salvamento não comprova utilidade.',
  '$.nodes[4].description',
  'Entregar: Instagram Ads, destino, checkout e acesso versionados com UTM, campanha, criativo e eventos correlacionados ate venda, entrega e primeiro uso. Aceite: identidade oficial do Instagram e publico salvo; segregar QA, bots, duplicidades e historicos diretos; convite individual, lista propria, contato direto ou organico nao suprem aquisicao. Medir: impressao -> visita -> beneficio -> oferta vista -> clique -> checkout -> compra conciliada. Nao publicar nem autorizar gasto. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Vínculo entre satisfação proposta, prova, identidade e ocasião de retorno nos contratos aprovados de anúncio, destino, checkout e entrega. Aceite: Versões, fontes e eventos correlacionados; referências/pareceres e limites da degustação antes de integrar, QA/bots/duplicidades excluídos. Atribuição ausente retorna à integração. Medir: Visita → primeira ação → demonstração → oferta vista → checkout → compra → entrega → uso/retorno voluntário.',
  '$.satisfactionContinuityVersion',
  'SATISFACTION_CONTINUITY_V1'),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)
FROM business_process_definition source
LEFT JOIN business_process_definition existing ON existing.process_code=source.process_code AND existing.version_number=10
WHERE source.process_code='pde-communication-sales-journey' AND source.version_number=9 AND existing.id IS NULL;

-- pde-commercial-homologation-activation: v10 -> v11; contratos executáveis preservados.
INSERT INTO business_process_definition
 (process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,
  technical_reference,process_type,parent_process_code,execution_scope,diagram_json,created_at,published_at)
SELECT source.process_code,source.name,
 CONCAT(source.purpose,' Satisfação pretendida, prova funcional, identidade e ocasião de retorno são explícitas; emoção e memória permanecem hipóteses.'),
 source.owner_name,source.trigger_description,source.outcome_description,11,'PUBLISHED',
 'docs/canonical/cadeia-produtos-pde-canon.v1.md / SATISFACTION_CONTINUITY_V1',
 source.process_type,source.parent_process_code,source.execution_scope,
 JSON_SET(source.diagram_json,
  '$.nodes[1].description',
  'Seleciona pelo tipo cadastrado e pela ficha o subprocesso comercial aplicavel. A candidata deve usar Instagram Ads (Meta Ads); convite individual, lista propria, contato direto e divulgacao organica bloqueiam a preparacao, sem inferencia pelo nome ou formato do produto. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Preparação pelo tipo/ficha com promessa funcional, satisfação como hipótese, identidade, demonstração e eventual amostra compatíveis. Aceite: Mesmos produto, versão e oferta; amostra exige limites e provas, custos essenciais conhecidos; preservar Instagram Ads e decisões próprias. Medir: Divergências de promessa/entrega, cobertura financeira e preparação concluída.',
  '$.nodes[2].description',
  'Reutiliza o parecer de Psique da mesma preparacao e confere a experiencia real recebida pelo trafego pago do Instagram; mudanca material ou canal divergente exige renovacao. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Parecer simulado independente sobre identificação, clareza, aplicação, esforço e transformação plausível da versão real. Aceite: Reutilizar provas vigentes; ausência/divergência de build bloqueia tecnicamente antes de revisão paga. Não solicitar opiniões ou declarar emoção humana comprovada. Medir: Conclusão por cenário, esforço, retrabalho e custo conhecido por homologação.',
  '$.nodes[3].description',
  'Reutiliza o parecer independente de Temis e confere promessa, criativo, Instagram, publico, preco, checkout e entrega da mesma versao; divergencia bloqueia e retorna a origem. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Parecer independente sobre identidade, promessa, prova real, fronteira gratuita/paga e condições da oferta. Aceite: Anúncio, demonstração, compra e entrega coerentes; sem agenda cheia, depoimento sintético ou satisfação garantida. Retornar lacuna à origem. Medir: Divergências confirmadas e prova de correção; aprovação não autoriza campanha.',
  '$.nodes[4].description',
  'Chama a homologacao tecnica para comprovar desktop, celular, compra simulada, acesso, entrega, falhas, eventos, segregacao e limites do experimento de Instagram Ads, reutilizando evidencias vigentes. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Retorno do subprocesso técnico com versões, hashes, mobile, transação segregada, entrega, eventos e limites financeiros. Aceite: Reutilizar somente evidência vigente; comprovar oferta vista separada de resultado pronto e atribuição na mesma versão, sem operar mídia. Medir: Conclusão técnica, falhas, tempo de preparação e cobertura dos custos.',
  '$.nodes[6].description',
  'Aprova explicitamente versao, Instagram Ads, teto financeiro, periodo e condicoes de parada. Nenhum gate tecnico concede gasto; convite individual, lista propria, contato direto ou divulgacao organica nao podem ser autorizados. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Decisão explícita sobre versão, Instagram Ads, teto, janela e paradas, com demonstração e custos comprovados. Aceite: Parecer de agente e melhoria de missão não autorizam gasto; preservar decisão humana do orçamento sem pesquisa com participantes. Medir: Limites persistidos e vigência; não apresentar autorização como compra ou receita.',
  '$.satisfactionContinuityVersion',
  'SATISFACTION_CONTINUITY_V1'),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)
FROM business_process_definition source
LEFT JOIN business_process_definition existing ON existing.process_code=source.process_code AND existing.version_number=11
WHERE source.process_code='pde-commercial-homologation-activation' AND source.version_number=10 AND existing.id IS NULL;

-- pde-sales-delivery-learning: v10 -> v11; contratos executáveis preservados.
INSERT INTO business_process_definition
 (process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,
  technical_reference,process_type,parent_process_code,execution_scope,diagram_json,created_at,published_at)
SELECT source.process_code,source.name,
 CONCAT(source.purpose,' Satisfação pretendida, prova funcional, identidade e ocasião de retorno são explícitas; emoção e memória permanecem hipóteses.'),
 source.owner_name,source.trigger_description,source.outcome_description,11,'PUBLISHED',
 'docs/canonical/cadeia-produtos-pde-canon.v1.md / SATISFACTION_CONTINUITY_V1',
 source.process_type,source.parent_process_code,source.execution_scope,
 JSON_SET(source.diagram_json,
  '$.nodes[4].description',
  'Delega aquisicao paga no Instagram, mensuracao atribuida e teste de uma variavel ao subprocesso canonico, sempre dentro do teto, janela e condicoes de parada autorizados. Nao criar ou importar contatos individuais. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Delegação de teste com uma mudança principal entre foco nos entregáveis e satisfação vinculada à prova real, quando essa for a hipótese aprovada. Aceite: Preservar produto, preço, público, página e condições; testar amostra em comparação posterior separada, respeitando teto/janela e atribuição. Medir: Compras líquidas, custo por compra e contribuição; amostra insuficiente pode ser inconclusiva.',
  '$.nodes[5].description',
  'Delega conciliação, entrega, suporte, satisfação e reembolso ao subprocesso canônico. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Delegação da entrega fiel, uso, suporte espontâneo, reembolso e reencontro do produto quando necessário. Aceite: Preservar identidade e resultado contratado, sem reentrega ou pesquisa de satisfação duplicada. Medir: Entrega, uso observável e retorno voluntário atribuível; retorno não comprova satisfação.',
  '$.nodes[6].description',
  'O backend concilia fontes do experimento de Instagram Ads por campanha, criativo, UTM e versao. Fonte invalida bloqueia; QA, bots, trafego direto, organico ou convite individual nao comprovam aquisicao, compra ou receita. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Conciliação de compra, entrega, uso, retorno, reembolso e contribuição com fontes, versões e denominadores. Aceite: Separar fato observado, satisfação/memória como hipóteses e explicações concorrentes; relato espontâneo complementa, não substitui venda. Fonte ausente permanece desconhecida. Medir: Compras líquidas, CAC, custo integral e contribuição; recompra/indicação somente quando pertinentes e atribuídas.',
  '$.nodes[7].description',
  'Abra ou retome o ciclo deste produto e experimento pago. Concilie vendas liquidas, CAC, custo integral, entrega e margem; registre decisao e siga a atividade orientada. Mudanca comercial exige sucessor e nova homologacao; correcao tecnica preserva a iteracao. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Aprendizado sobre satisfação proposta, prova, identidade e ocasião de retorno, com fatos favoráveis/contrários e limites. Aceite: Preservar referência anterior e uma mudança principal; promessa sem suporte volta à estratégia/comunicação, resultado à construção, atribuição à integração e margem à economia. Medir: Compra conciliada, entrega útil e contribuição positiva antes de repetir ou solicitar escala.',
  '$.satisfactionContinuityVersion',
  'SATISFACTION_CONTINUITY_V1'),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)
FROM business_process_definition source
LEFT JOIN business_process_definition existing ON existing.process_code=source.process_code AND existing.version_number=11
WHERE source.process_code='pde-sales-delivery-learning' AND source.version_number=10 AND existing.id IS NULL;

-- pde-tasting-proof-of-value: v3 -> v4; contratos executáveis preservados.
INSERT INTO business_process_definition
 (process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,
  technical_reference,process_type,parent_process_code,execution_scope,diagram_json,created_at,published_at)
SELECT source.process_code,source.name,
 CONCAT(source.purpose,' Satisfação pretendida, prova funcional, identidade e ocasião de retorno são explícitas; emoção e memória permanecem hipóteses.'),
 source.owner_name,source.trigger_description,source.outcome_description,4,'PUBLISHED',
 'docs/canonical/cadeia-produtos-pde-canon.v1.md / SATISFACTION_CONTINUITY_V1',
 source.process_type,source.parent_process_code,source.execution_scope,
 JSON_SET(source.diagram_json,
  '$.nodes[1].description',
  'Entregar: hipótese do desejo, público/situação, benefício inicial aplicável e ligação com a continuidade paga, usando fontes com data. Aceite: degustação explicitamente escolhida no plano; respeitar tipo e ficha, definir o que a pessoa faz/recebe e uma explicação concorrente; não impor questionário ou cadastro antes do valor. Medir: primeira interação, benefício recebido e interesse em continuar, com versão, janela e denominadores. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Benefício inicial que torna plausível a satisfação proposta, com fonte/data, contraponto e motivo para continuidade paga. Aceite: Degustação explicitamente aprovada pelo plano/tipo; não impor gratuidade, cadastro ou questionário; aquisição segue o canal autorizado. Medir: Primeira ação, benefício, oferta vista e compra separados.',
  '$.nodes[2].description',
  'Entregar: fronteira gratuita/paga, custo máximo por uso, limite total, expiração/antiabuso e economia da continuidade paga. Aceite: custos completos e origem das premissas, cenários conservador/esperado/uso intenso, contribuição e margem vigentes; desconhecido bloqueia, sem virar zero. Medir: custo por benefício útil e por compra líquida; impedir que participação gratuita seja tratada como receita. Parecer não autoriza gasto. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Custos de todas as amostras, inclusive sem compra, tentativas, suporte, limite por uso, teto total e prevenção de abuso. Aceite: Custos com fonte, cenários e margem da continuidade; desconhecido é pendência, parecer não autoriza consumo. Medir: Custo por amostra útil, CAC e contribuição por compra líquida.',
  '$.nodes[3].description',
  'Entregar: pequena orientação aplicável, amostra ou uso limitado real do produto aprovado, com primeira ação fácil e continuidade clara. Aceite: cumprir integralmente a promessa inicial; próximo botão e botão final acessíveis no celular, entradas mínimas, limites/consentimento/retomada conforme contrato; não esconder o benefício prometido atrás de novo cadastro. Medir: início → conclusão → resultado recebido, aplicação relatada separada de exibição e de teste sintético. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Amostra real fiel ao contexto, aplicável, identificada pelo produto e coerente com a entrega paga. Aceite: Cumprir integralmente promessa gratuita e limites; recuperação e retomada quando contratadas, sem gerar/cobrar em duplicidade. Medir: Conclusão, resultado recebido e uso observável; resultado exibido não comprova satisfação.',
  '$.nodes[4].description',
  'Entregar: avaliação da compreensão e aplicação do benefício inicial, esforço e motivo para continuar pagando. Aceite: observar percurso e botões no celular; explicar com evidência o que a pessoa recebe agora e o que ganha ao pagar; não inferir satisfação de resultado exibido ou e-mail. Medir: conclusão, utilidade observada/relatada e entendimento da oferta. Identificar simulação de agente; evidências voluntárias do mercado e simulações ficam separadas. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Avaliação simulada da identificação, esforço, aplicação da amostra e compreensão do benefício adicional pago. Aceite: Agente identificado; sem entrevistas, recrutamento ou opiniões solicitadas. Não transformar aceitação gratuita em disposição de pagar. Medir: Conclusão e compreensão por cenário, separadas de comportamento voluntário do mercado.',
  '$.nodes[5].description',
  'Entregar: conferência de promessa, prova real, fronteira gratuita/paga, preço total/recorrência, entregáveis, uso, prazo, acesso, suporte, reembolso, direitos e privacidade na mesma versão. Aceite: consistência com produto e checkout, nenhum depoimento fictício e nenhuma promessa inicial descumprida para forçar compra. Medir: divergências comprovadas e evidência de correção; aprovação não equivale a venda. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Revisão independente da amostra, produto identificável, prova real e promessa gratuita/paga. Aceite: Oferta, direitos, preço e limites coerentes; nenhuma emoção, depoimento ou venda fictícia. Medir: Divergências e prova corrigida; aprovação técnica não comprova demanda.',
  '$.nodes[6].description',
  'Entregar: vínculo auditável ao Processo 4 antes de integrar a jornada, com versão, pareceres, custo/limites e eventos de degustação, resultado, oferta, checkout, compra, acesso, retorno, expiração e abuso. Aceite: gatilhos observáveis, unidade/denominador/janela, correlação e testes segregados; oferta vista depende de visibilidade e tempo mínimo definidos, nunca apenas resultado pronto. Medir: valor recebido → exposição efetiva → CTA → checkout → compra líquida; resultado exibido não comprova aplicação. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Retorno auditável ao Processo 4 com versão, pareceres, prova, identidade, limites, custos e eventos. Aceite: Backend coordena avanço; amostra sem contrato/prova não é promessa publicável; manter a ocasião de retorno sem inventar recorrência. Medir: Amostra → oferta vista → checkout → compra → entrega/uso; custo inclui quem não compra.',
  '$.satisfactionContinuityVersion',
  'SATISFACTION_CONTINUITY_V1'),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)
FROM business_process_definition source
LEFT JOIN business_process_definition existing ON existing.process_code=source.process_code AND existing.version_number=4
WHERE source.process_code='pde-tasting-proof-of-value' AND source.version_number=3 AND existing.id IS NULL;

-- creative-production-approval: v8 -> v9; contratos executáveis preservados.
INSERT INTO business_process_definition
 (process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,
  technical_reference,process_type,parent_process_code,execution_scope,diagram_json,created_at,published_at)
SELECT source.process_code,source.name,
 CONCAT(source.purpose,' Satisfação pretendida, prova funcional, identidade e ocasião de retorno são explícitas; emoção e memória permanecem hipóteses.'),
 source.owner_name,source.trigger_description,source.outcome_description,9,'PUBLISHED',
 'docs/canonical/cadeia-produtos-pde-canon.v1.md / SATISFACTION_CONTINUITY_V1',
 source.process_type,source.parent_process_code,source.execution_scope,
 JSON_SET(source.diagram_json,
  '$.nodes[2].description',
  'Produz copy, composição, peças estáticas e e-mails sob o contrato aprovado. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Peças com situação familiar, participação do produto, resultado verificável, identidade e satisfação possível coerentes com o contrato de Íris. Aceite: Preservar pixels/provas aprovados e identificar exemplos; não trocar a oferta nem usar emoção como fato. Medir: Legibilidade mobile, clareza e correspondência com destino/entrega; desempenho comercial pendente.',
  '$.nodes[3].description',
  'Produz roteiro técnico, cenas, áudio, montagem e legendas sob o briefing de Íris. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Vídeo com identificação cotidiana, participação concreta do produto e persona transformada por uma ação funcional demonstrável. Aceite: Mesma personagem/contexto quando pertinente; distinguir encenação de prova, preservar produto/peças e mostrar identidade/CTA fiel. Sem depoimento, compra ou agenda fictícios. Medir: Continuidade, voz, legendas, prova da versão real e qualidade final em desktop/mobile.',
  '$.nodes[4].description',
  'Inspeciona cada formato real e registra reação, clareza, desejo, confiança e esforço. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Avaliação simulada da compreensão, esforço, desejo hipotético e capacidade de aplicar o resultado mostrado. Aceite: Agente identificado; personagem não conta como compradora nem satisfação comprovada. Resultado não utilizável volta à construção, mensagem sem suporte à comunicação. Medir: Falhas observadas, legibilidade e conclusão; nenhuma inferência de emoção individual.',
  '$.nodes[5].description',
  'Valida verdade, prova, fidelidade, direitos, compliance e segurança sem criar correção. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Parecer independente ligando promessa, identidade, prova real e limites da oferta. Aceite: Revisar transformação/encenação e fronteira gratuita/paga, fontes/direitos e versão; não fabricar sucesso comercial. Medir: Divergências e evidências de correção, sem autorizar gasto/publicação.',
  '$.satisfactionContinuityVersion',
  'SATISFACTION_CONTINUITY_V1'),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)
FROM business_process_definition source
LEFT JOIN business_process_definition existing ON existing.process_code=source.process_code AND existing.version_number=9
WHERE source.process_code='creative-production-approval' AND source.version_number=8 AND existing.id IS NULL;

-- experiment-homologation-activation: v5 -> v6; contratos executáveis preservados.
INSERT INTO business_process_definition
 (process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,
  technical_reference,process_type,parent_process_code,execution_scope,diagram_json,created_at,published_at)
SELECT source.process_code,source.name,
 CONCAT(source.purpose,' Satisfação pretendida, prova funcional, identidade e ocasião de retorno são explícitas; emoção e memória permanecem hipóteses.'),
 source.owner_name,source.trigger_description,source.outcome_description,6,'PUBLISHED',
 'docs/canonical/cadeia-produtos-pde-canon.v1.md / SATISFACTION_CONTINUITY_V1',
 source.process_type,source.parent_process_code,source.execution_scope,
 JSON_SET(source.diagram_json,
  '$.nodes[1].description',
  'Comprova versão, mobile, desempenho, links, ativos e correspondência. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Provas da mesma versão de anúncio, demonstração e destino com identidade consistente e ação/resultado funcionais. Aceite: Manifesto/hashes e pixels corretos, mobile, links e limites; divergência é bloqueio técnico anterior à revisão paga. Medir: Falhas de versão, acessibilidade e desempenho; health isolado não comprova a experiência.',
  '$.nodes[2].description',
  'Executa transação segregada sem registrar venda real. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Compra simulada, acesso e entrega coerentes com o benefício anunciado e a identidade do produto. Aceite: Dados de teste segregados, sem venda real, cobrança ou geração duplicada; preservar preço e condições aprovados. Medir: Conclusão transacional, entrega e recuperação por cenário.',
  '$.nodes[3].description',
  'Confere contratos operacionais de Hermes sem operar tráfego. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Eventos observáveis de interação, demonstração, oferta vista, checkout, compra, entrega, uso e retorno quando pertinentes. Aceite: Atribuição, versão, janela, denominadores e deduplicação; resultado pronto não é oferta vista, retorno não comprova memória/satisfação; ausente é desconhecido. Medir: Cobertura e segregação de QA/bots/duplicidades, sem operar tráfego.',
  '$.nodes[4].description',
  'Confere teto e regras aprovados por Plutus sem recalcular ou autorizar gasto. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Limites persistidos de aquisição, entrega e amostra quando prevista, com validade do plano e parecer. Aceite: Incluir usos gratuitos sem compra, tentativas/suporte; verificar teto/janela/parada sem recalcular nem autorizar gasto. Medir: Cobertura dos custos e travas; contribuição projetada não é lucro realizado.',
  '$.satisfactionContinuityVersion',
  'SATISFACTION_CONTINUITY_V1'),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)
FROM business_process_definition source
LEFT JOIN business_process_definition existing ON existing.process_code=source.process_code AND existing.version_number=6
WHERE source.process_code='experiment-homologation-activation' AND source.version_number=5 AND existing.id IS NULL;

-- operacao-otimizacao-experimento: v6 -> v7; contratos executáveis preservados.
INSERT INTO business_process_definition
 (process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,
  technical_reference,process_type,parent_process_code,execution_scope,diagram_json,created_at,published_at)
SELECT source.process_code,source.name,
 CONCAT(source.purpose,' Satisfação pretendida, prova funcional, identidade e ocasião de retorno são explícitas; emoção e memória permanecem hipóteses.'),
 source.owner_name,source.trigger_description,source.outcome_description,7,'PUBLISHED',
 'docs/canonical/cadeia-produtos-pde-canon.v1.md / SATISFACTION_CONTINUITY_V1',
 source.process_type,source.parent_process_code,source.execution_scope,
 JSON_SET(source.diagram_json,
  '$.nodes[1].description',
  'Entregar: conciliação de eventos com fonte, versão, gatilho, unidade, numerador/denominador, atribuição e janela. Aceite: visitantes humanos distintos; testes, bots, duplicidades e reembolsos segregados; não inventar zero para fonte ausente. Verificar exposição real à oferta com limiar declarado; resultado pronto não dispara oferta vista. Medir: separar primeira interação, conclusão, resultado, aplicação/relato, cadastro, login, retorno, checkout e compra. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Funil da situação reconhecida até compra, entrega, uso e retorno, com fonte e versão. Aceite: Somente comportamentos observáveis e atribuídos, sem solicitar lembrança/opinião nem inferir emoção; fonte ausente não é zero. Medir: Conversão por etapa, compras líquidas e cobertura.',
  '$.nodes[3].description',
  'Entregar: primeira perda mensurável do funil com numerador, denominador, versão, origem e janela; hipótese causal e explicação concorrente. Aceite: comparar início → conclusão → resultado → oferta efetivamente vista → checkout → compra, sem confundir cadastro/login nem resultado/uso; registrar lacunas e amostra insuficiente. Medir: abandono por etapa e tempo até o benefício. Correlação ou clique não confirma causa, satisfação ou lucro. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Primeira perda mensurável e hipótese/explicação concorrente sobre mensagem, prova, esforço ou continuidade. Aceite: Retorno não prova memória/satisfação; localizar causa com históricos comparáveis antes de alterar estratégia, preço ou produto. Medir: Abandono, tempo até benefício e custo por compra, com denominadores.',
  '$.nodes[4].description',
  'Entregar: uma variável principal, referência anterior, hipótese, explicação concorrente, condições mantidas e critério de sucesso/falha predefinido. Aceite: preservar estratégia, preço, público e produto quando não forem a variável autorizada; contradição volta a Atena, produto a Dédalo e comunicação a Íris. Medir: efeito na etapa-alvo junto com compras líquidas, entrega, CAC e margem; participação isolada não autoriza escala. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Teste de abordagem dos entregáveis versus satisfação ligada à prova, se autorizado, com referência e uma mudança principal. Aceite: Preservar produto/preço/público/página; testar amostra separadamente e devolver problema à atividade responsável. Medir: Compras, contribuição e guardas de entrega/reembolso, admitindo resultado inconclusivo.',
  '$.nodes[5].description',
  'Avalia a variante real sem substituir dados do funil ou materializar correção. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Revisão simulada da variante e da ligação entre situação, ação, resultado e satisfação possível. Aceite: Sem preferência humana fabricada; manter identidade e produto reais, usar evidência do artefato. Medir: Clareza, esforço e utilidade funcional; não substituir dados de mercado.',
  '$.nodes[6].description',
  'Avalia prova, fidelidade, direitos e compliance sem criar a variante. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Parecer independente sobre promessa, prova, identidade e transformação mostradas. Aceite: Sem satisfação garantida, depoimento ou agendamento fictícios; amostra precisa existir e cumprir o contrato. Medir: Divergências confirmadas e retornos à origem.',
  '$.nodes[7].description',
  'Entregar: parecer sobre orçamento, receita líquida, CAC e custo integral da variante, incluindo IA, suporte, taxas, entrega e reembolsos. Aceite: fontes/validade e cenários conservador/esperado/uso intenso; desconhecido não é zero; contribuição positiva e margem conforme política aprovada. Medir: compras líquidas, contribuição e margem após custos e aquisição. Parecer não autoriza gasto ou ampliação de teto. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Economia medida de entrega, aquisição e amostras, inclusive sem compra, tentativas, suporte e reembolsos. Aceite: Custos essenciais com fonte; preservar pareceres válidos, desconhecido pendente; não autorizar novo teto. Medir: CAC, contribuição, margem e cobertura após todos os custos variáveis.',
  '$.nodes[10].description',
  'Entregar: comparação da variante com a referência, uma mudança principal, condições mantidas, amostra/janela e limitações; preservar histórico. Aceite: evidência suficiente ou conclusão inconclusiva; testes qualitativos não contam na amostra paga. Medir: etapa-alvo, vendas líquidas, receita conciliada, CAC, custo completo, entrega útil e reembolso. Melhor participação sem venda/margem não comprova sucesso comercial. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Comparação com fatos favoráveis/contrários à satisfação proposta, à prova e à utilidade de retorno. Aceite: Uma mudança principal, amostra/janela, versões e explicações concorrentes; retorno/recompra não prova emoção ou mecanismo de memória. Medir: Compras líquidas, entrega/uso, reembolso e contribuição para repetir ou encerrar.',
  '$.satisfactionContinuityVersion',
  'SATISFACTION_CONTINUITY_V1'),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)
FROM business_process_definition source
LEFT JOIN business_process_definition existing ON existing.process_code=source.process_code AND existing.version_number=7
WHERE source.process_code='operacao-otimizacao-experimento' AND source.version_number=6 AND existing.id IS NULL;

-- venda-entrega-satisfacao-cliente: v4 -> v5; contratos executáveis preservados.
INSERT INTO business_process_definition
 (process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,
  technical_reference,process_type,parent_process_code,execution_scope,diagram_json,created_at,published_at)
SELECT source.process_code,source.name,
 CONCAT(source.purpose,' Satisfação pretendida, prova funcional, identidade e ocasião de retorno são explícitas; emoção e memória permanecem hipóteses.'),
 source.owner_name,source.trigger_description,source.outcome_description,5,'PUBLISHED',
 'docs/canonical/cadeia-produtos-pde-canon.v1.md / SATISFACTION_CONTINUITY_V1',
 source.process_type,source.parent_process_code,source.execution_scope,
 JSON_SET(source.diagram_json,
  '$.nodes[2].description',
  'Produz somente a personalização não audiovisual prevista sem mudar oferta ou preço. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Entrega personalizada coerente com a situação, identidade e resultado funcional comprados. Aceite: Preservar contexto e limites da oferta, sem modificar imagem do trabalho ou inventar capacidade. Medir: Correspondência, custo integral e conclusão funcional.',
  '$.nodes[4].description',
  'Comprova canal, recebimento, links, arquivos e prazo. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Entrega identificável, acessível e reencontrável quando previsto, com orientação de aplicação. Aceite: Cumprir prazo/canal, permitir recuperar resultado sem reentrega ou cobrança duplicada. Medir: Recebimento, links válidos, primeiro uso e retorno voluntário atribuível.',
  '$.nodes[5].description',
  'Registra uso, valor percebido, dificuldade e satisfação sem inventar comportamento. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Leitura por Psique de uso observável, dificuldade, suporte e relatos espontâneos com fonte/data e limites. Aceite: Sem solicitar opinião, entrevistar ou recrutar; separar simulação, comportamento e relato. Retorno/recompra não comprova satisfação ou memória. Medir: Uso, retorno, cancelamento e reembolso; emoção sem evidência permanece hipótese.',
  '$.nodes[6].description',
  'Relaciona pagamento, custo, reembolso e margem sem movimentar recursos. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Conciliação de receita líquida, aquisição, taxas, entrega, IA, tentativas, suporte e amostras, inclusive sem compra. Aceite: Custos essenciais conhecidos e reembolsos conciliados; sem movimentar recursos ou confundir projeção com lucro. Medir: Contribuição por venda, margem e cobertura; custos fixos/preparação avaliados separadamente.',
  '$.nodes[7].description',
  'Preserva procedência; prova só é reutilizada com consentimento e aprovação. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Resultado da hipótese de satisfação com contrapontos, versão, fonte, identidade e ocasião legítima de retorno. Aceite: Reutilização governada e consentimento quando necessário; indicação/recompra atribuídas e pertinentes, sem fabricar depoimento. Medir: Vendas líquidas, entrega útil, reembolso e contribuição; retorno não comprova mecanismo psicológico.',
  '$.satisfactionContinuityVersion',
  'SATISFACTION_CONTINUITY_V1'),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)
FROM business_process_definition source
LEFT JOIN business_process_definition existing ON existing.process_code=source.process_code AND existing.version_number=5
WHERE source.process_code='venda-entrega-satisfacao-cliente' AND source.version_number=4 AND existing.id IS NULL;

-- value-chain-learning-sales-cycle: v5 -> v6; contratos executáveis preservados.
INSERT INTO business_process_definition
 (process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,
  technical_reference,process_type,parent_process_code,execution_scope,diagram_json,created_at,published_at)
SELECT source.process_code,source.name,
 CONCAT(source.purpose,' Satisfação pretendida, prova funcional, identidade e ocasião de retorno são explícitas; emoção e memória permanecem hipóteses.'),
 source.owner_name,source.trigger_description,source.outcome_description,6,'PUBLISHED',
 'docs/canonical/cadeia-produtos-pde-canon.v1.md / SATISFACTION_CONTINUITY_V1',
 source.process_type,source.parent_process_code,source.execution_scope,
 JSON_SET(source.diagram_json,
  '$.nodes[1].description',
  'Entregar: aprendizado com fonte/data, versões, estado/objetivo da etapa, fato observado, hipótese e explicação concorrente; separar sucessos, falhas e limites de validade. Aceite: conciliação do experimento anterior sem transformar participação do experimento de referência em causalidade ou venda; resolver contradições antes de reutilizar memória. Medir: evidência nova, recorrência do gargalo e resultado comercial conhecido, sem dados pessoais no aprendizado. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Fatos favoráveis/contrários à satisfação proposta, prova, identidade e ocasião de retorno com fontes e versões. Aceite: Separar observação, relato espontâneo e hipótese; memória/emoção não são inferidas de clique/retorno. Medir: Gargalo, recorrência e resultado comercial conhecido.',
  '$.nodes[2].description',
  'Entregar: referência anterior, uma variável principal, hipótese e explicação concorrente; oferta, canal, público e entrega mantidos quando não forem a variável, amostra, janela de compra, atribuição, teto e parada predefinidos. Aceite: metas justificadas por produto e economia, custo desconhecido pendente; mudança material requer aprovação própria. Medir: etapa-alvo e guardas de compras líquidas, utilidade, CAC, custo integral e margem. Não confundir limite financeiro com conclusão estatística. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Uma mudança principal na abordagem com referência, alternativa concorrente e condições comparáveis. Aceite: Preservar produto, preço, público e página quando a variável for a narrativa; amostra é teste separado com custos/limites aprovados. Medir: Compras líquidas, CAC e contribuição, com janela, amostra e atribuição definidas.',
  '$.nodes[4].description',
  'Defina objetivo, CTA e métrica próprios do vídeo AD e da entrada LANDING_HERO. Demonstre o produto real e registre a hipótese principal, as variáveis mantidas e a referência do teto de produção. Consumo exige autorização e preflight no Estúdio. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Briefing de Íris para Apolo com situação reconhecível, participação concreta do produto, resultado aplicável, identidade e satisfação possível. Aceite: Versão/prova reais e persona transformada identificada como encenação, CTA/limites coerentes; não prometer satisfação garantida. Medir: Métrica do papel no funil, qualidade final e custo conhecido por vídeo aceito.',
  '$.nodes[8].description',
  'Execute homologação técnica e avaliações independentes da mesma versão. Informe a instância real do gate multiagente aprovado, a prova da jornada preservando testes internos e dados comerciais separados. Reprovação volta ao ajuste. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Homologação técnica e revisões simuladas independentes da mesma versão e da fidelidade anúncio → demonstração → compra → entrega. Aceite: Sem entrevistas, recrutamento ou opinião solicitada; simulação identificada, eventos/tráfego segregados e limites comprovados. Medir: Conclusão por cenário, falhas, custo e tempo de homologação; testes não comprovam vendas.',
  '$.nodes[12].description',
  'O backend concilia fontes do experimento e persiste período, versão e segregação; resultados não são digitados. Entregar: pessoas humanas distintas, início/conclusão/resultado, oferta efetivamente vista, cadastro/login/retorno, checkout, compras líquidas, receita, custo integral, CAC, entrega/uso e reembolso quando houver fonte. Aceite: gatilhos e denominadores válidos; fonte ausente não vira zero; resultado pronto não comprova oferta vista ou satisfação. Medir: cobertura, amostra/incerteza e contribuição/margem conforme política. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Conciliação de compra, entrega, uso, retorno, reembolso e todos os custos, inclusive amostras sem compra. Aceite: Fonte/versão/janela e deduplicação; oferta vista é evento próprio, desconhecido não vira zero; retorno/recompra não prova emoção ou lembrança. Medir: Compras líquidas, CAC, contribuição e cobertura, com incerteza explícita.',
  '$.nodes[13].description',
  'Entregar: proposta de Atena comparando três alternativas com a conciliação oficial, justificativa, evidência, aprendizado, hipótese, explicação concorrente e destino. Aceite: separar sinal de participação de venda; amostra insuficiente é inconclusiva; escala exige compras líquidas, entrega útil, custos essenciais conhecidos, contribuição positiva e margem vigente. Usuário pode editar/aprovar; proposta inválida/desatualizada bloqueia o losango. Medir: vendas líquidas, CAC, margem e entrega. Este parecer não autoriza gasto/publicação. Contrato SATISFACTION_CONTINUITY_V1 — Entregar: Decisão com três alternativas, fatos favoráveis/contrários, explicação concorrente e retorno à camada da causa. Aceite: Promessa sem prova à estratégia/comunicação; utilidade à construção; atribuição à integração; margem à economia. Amostra insuficiente admite inconclusivo. Medir: Repetir/solicitar escala somente com compra conciliada, entrega útil e contribuição positiva, sem novo gasto implícito.',
  '$.satisfactionContinuityVersion',
  'SATISFACTION_CONTINUITY_V1'),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)
FROM business_process_definition source
LEFT JOIN business_process_definition existing ON existing.process_code=source.process_code AND existing.version_number=6
WHERE source.process_code='value-chain-learning-sales-cycle' AND source.version_number=5 AND existing.id IS NULL;

-- A definição relacional usa exatamente o nó versionado consumido pela tela e pela missão BPM.
INSERT INTO business_process_activity_definition
 (process_definition_id,activity_id,name,objective,owner_name,execution_resource_code,subprocess_code,definition_json,created_at)
SELECT process.id,
 JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',node.number,'].id'))),
 JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',node.number,'].label'))),
 JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',node.number,'].description'))),
 NULLIF(JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',node.number,'].owner'))),'null'),
 NULLIF(JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',node.number,'].executionResourceCode'))),'null'),
 NULLIF(JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',node.number,'].subprocessCode'))),'null'),
 JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',node.number,']')),UTC_TIMESTAMP(6)
FROM business_process_definition process
CROSS JOIN (SELECT 0 AS number UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15 UNION ALL SELECT 16) node
LEFT JOIN business_process_activity_definition existing
 ON existing.process_definition_id=process.id
 AND existing.activity_id=JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',node.number,'].id')))
WHERE JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,'$.satisfactionContinuityVersion'))='SATISFACTION_CONTINUITY_V1'
 AND ((process_code='pde-opportunity-discovery' AND version_number=9) OR (process_code='pde-commercial-plan-offer' AND version_number=11) OR (process_code='pde-construction-approval' AND version_number=10) OR (process_code='pde-communication-sales-journey' AND version_number=10) OR (process_code='pde-commercial-homologation-activation' AND version_number=11) OR (process_code='pde-sales-delivery-learning' AND version_number=11) OR (process_code='pde-tasting-proof-of-value' AND version_number=4) OR (process_code='creative-production-approval' AND version_number=9) OR (process_code='experiment-homologation-activation' AND version_number=6) OR (process_code='operacao-otimizacao-experimento' AND version_number=7) OR (process_code='venda-entrega-satisfacao-cliente' AND version_number=5) OR (process_code='value-chain-learning-sales-cycle' AND version_number=6))
 AND JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',node.number,'].type')))='TASK'
 AND existing.id IS NULL;

INSERT INTO business_process_chain_definition
 (chain_code,name,purpose,outcome_description,primary_metric,version_number,status,created_at,published_at)
SELECT source.chain_code,source.name,
 CONCAT(source.purpose,' SATISFACTION_CONTINUITY_V1: situação reconhecível → benefício desejado → experiência real → produto identificável → motivo legítimo para voltar; sem entrevistas ou opinião solicitada.'),
 source.outcome_description,'Compras líquidas, entrega útil, contribuição e retorno voluntário atribuível',25,'PUBLISHED',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)
FROM business_process_chain_definition source
LEFT JOIN business_process_chain_definition existing ON existing.chain_code=source.chain_code AND existing.version_number=25
WHERE source.chain_code='pde-value-creation-delivery' AND source.version_number=24 AND existing.id IS NULL;

INSERT INTO business_process_chain_item
 (chain_definition_id,process_definition_id,sequence_number,value_contribution,created_at)
SELECT chain.id,process.id,1,'Argos reúne situações, linguagem pública, satisfação possível, fontes e contrapontos, sem escolher estratégia nem provar demanda.',UTC_TIMESTAMP(6)
FROM business_process_chain_definition chain
JOIN business_process_definition process ON process.process_code='pde-opportunity-discovery' AND process.version_number=9
LEFT JOIN business_process_chain_item existing ON existing.chain_definition_id=chain.id AND existing.sequence_number=1
WHERE chain.chain_code='pde-value-creation-delivery' AND chain.version_number=25 AND existing.id IS NULL;

INSERT INTO business_process_chain_item
 (chain_definition_id,process_definition_id,sequence_number,value_contribution,created_at)
SELECT chain.id,process.id,2,'Atena escolhe uma satisfação como hipótese; Plutus limita custos, inclusive amostras sem compra; Dédalo liga ação, resultado, identidade e ocasião de retorno.',UTC_TIMESTAMP(6)
FROM business_process_chain_definition chain
JOIN business_process_definition process ON process.process_code='pde-commercial-plan-offer' AND process.version_number=11
LEFT JOIN business_process_chain_item existing ON existing.chain_definition_id=chain.id AND existing.sequence_number=2
WHERE chain.chain_code='pde-value-creation-delivery' AND chain.version_number=25 AND existing.id IS NULL;

INSERT INTO business_process_chain_item
 (chain_definition_id,process_definition_id,sequence_number,value_contribution,created_at)
SELECT chain.id,process.id,3,'Dédalo constrói resultado fiel, aplicável e recuperável; Psique e Têmis avaliam a mesma versão sem alegar satisfação humana.',UTC_TIMESTAMP(6)
FROM business_process_chain_definition chain
JOIN business_process_definition process ON process.process_code='pde-construction-approval' AND process.version_number=10
LEFT JOIN business_process_chain_item existing ON existing.chain_definition_id=chain.id AND existing.sequence_number=3
WHERE chain.chain_code='pde-value-creation-delivery' AND chain.version_number=25 AND existing.id IS NULL;

INSERT INTO business_process_chain_item
 (chain_definition_id,process_definition_id,sequence_number,value_contribution,created_at)
SELECT chain.id,process.id,4,'Íris e Apolo mostram situação, participação do produto e satisfação possível com identidade contínua; o backend integra prova e continuidade paga.',UTC_TIMESTAMP(6)
FROM business_process_chain_definition chain
JOIN business_process_definition process ON process.process_code='pde-communication-sales-journey' AND process.version_number=10
LEFT JOIN business_process_chain_item existing ON existing.chain_definition_id=chain.id AND existing.sequence_number=4
WHERE chain.chain_code='pde-value-creation-delivery' AND chain.version_number=25 AND existing.id IS NULL;

INSERT INTO business_process_chain_item
 (chain_definition_id,process_definition_id,sequence_number,value_contribution,created_at)
SELECT chain.id,process.id,5,'Preparação, revisões e preflight comprovam promessa, versão, compra/entrega, eventos e limites; autorização humana permanece separada.',UTC_TIMESTAMP(6)
FROM business_process_chain_definition chain
JOIN business_process_definition process ON process.process_code='pde-commercial-homologation-activation' AND process.version_number=11
LEFT JOIN business_process_chain_item existing ON existing.chain_definition_id=chain.id AND existing.sequence_number=5
WHERE chain.chain_code='pde-value-creation-delivery' AND chain.version_number=25 AND existing.id IS NULL;

INSERT INTO business_process_chain_item
 (chain_definition_id,process_definition_id,sequence_number,value_contribution,created_at)
SELECT chain.id,process.id,6,'Hermes, entrega e Plutus conciliam compras, uso, reembolso e contribuição; o ciclo preserva hipóteses, contrapontos e retorno à atividade da causa.',UTC_TIMESTAMP(6)
FROM business_process_chain_definition chain
JOIN business_process_definition process ON process.process_code='pde-sales-delivery-learning' AND process.version_number=11
LEFT JOIN business_process_chain_item existing ON existing.chain_definition_id=chain.id AND existing.sequence_number=6
WHERE chain.chain_code='pde-value-creation-delivery' AND chain.version_number=25 AND existing.id IS NULL;

-- Reaplicação após rollback preserva linhas, custos, aprovações e disponibilidade das versões anteriores.
UPDATE business_process_definition SET status='PUBLISHED',published_at=COALESCE(published_at,UTC_TIMESTAMP(6))
WHERE ((process_code='pde-opportunity-discovery' AND version_number=9) OR (process_code='pde-commercial-plan-offer' AND version_number=11) OR (process_code='pde-construction-approval' AND version_number=10) OR (process_code='pde-communication-sales-journey' AND version_number=10) OR (process_code='pde-commercial-homologation-activation' AND version_number=11) OR (process_code='pde-sales-delivery-learning' AND version_number=11) OR (process_code='pde-tasting-proof-of-value' AND version_number=4) OR (process_code='creative-production-approval' AND version_number=9) OR (process_code='experiment-homologation-activation' AND version_number=6) OR (process_code='operacao-otimizacao-experimento' AND version_number=7) OR (process_code='venda-entrega-satisfacao-cliente' AND version_number=5) OR (process_code='value-chain-learning-sales-cycle' AND version_number=6)) AND JSON_UNQUOTE(JSON_EXTRACT(diagram_json,'$.satisfactionContinuityVersion'))='SATISFACTION_CONTINUITY_V1';
UPDATE business_process_chain_definition SET status='PUBLISHED',published_at=COALESCE(published_at,UTC_TIMESTAMP(6))
WHERE chain_code='pde-value-creation-delivery' AND version_number=25 AND purpose LIKE '%SATISFACTION_CONTINUITY_V1%';

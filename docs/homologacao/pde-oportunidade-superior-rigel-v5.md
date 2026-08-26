# Matriz de homologação local — Descoberta PDE v5

## Objetivo e decisão

Executar localmente o rascunho `pde-opportunity-discovery` v5, comparar exatamente três dores e
decidir se alguma oportunidade alcança de forma repetível o benchmark interno de Rigel, 82/100. O
score continua sendo priorização; somente pagamento reconciliado poderá contar como venda.

- **Gargalo:** não existe nova oportunidade comprovada igual ou superior a Rigel, e a fonte Hotmart
  dirigida falha no contrato oficial.
- **Métrica esperada:** consenso `APPROVE` de Argos, Hermes, Dédalo e Psique, score mínimo 82,
  valor percebido mínimo 75 e nenhuma quebra dos gates determinísticos.
- **Continuar:** resultado mínimo 82 em execuções consecutivas, evidências independentes e teste
  futuro possível sem gasto ou publicação.
- **Ajustar:** lacuna resolvível de evidência, mecanismo, canal, economia ou esforço.
- **Parar:** resultado menor que 82, dependência de inspiração como prova, risco incontrolável,
  sobreposição material com o portfólio ou promessa sem base.

## Matriz ponta a ponta

| Área | Cenário | Evidência esperada | Critério |
| --- | --- | --- | --- |
| Caminho feliz | Artigos vivos, snapshot Hotmart, fontes independentes e quatro agentes | relatório estruturado com decisão, score, fontes, inspirações e custo | todos aprovam e a vencedora alcança 82 |
| Coleções vivas | Novos Markdown em `pesquisas/gartner` e `pesquisas/ia-aplicada` | inventário, conteúdo e SHA-256 consultados no início de cada execução | nenhum arquivo atual fica congelado fora do ciclo |
| Hotmart | Contrato oficial entrega produtos classificados pelo Hub | referência, coleta, score/temperatura, limitação e origem | score, ranking e temperatura nunca viram venda |
| Falha Hotmart | Endpoint oficial retorna erro | causa, requestId e fixture local segregada | execução não fabrica fonte e preserva a limitação |
| Inspiração | Artigo ou produto orienta uma hipótese original | origem, data, padrão, confirmação e limite de cópia | duas vias independentes confirmam ou descartam cada uso |
| Evidência | Três oportunidades distintas | ao menos seis fontes por candidata, dez ofertas no ciclo e três por candidata | inspiração não entra na contagem de evidência |
| Agentes | Argos, Hermes, Dédalo e Psique recebem o mesmo dossiê | prompt/schema v5, request, resposta bruta e resultado funcional | nenhuma etapa supera decisão anterior ou altera fatos |
| Falha de agente | Schema, nomes, score ou contagem divergem | erro auditável e execução encerrada | não há correção silenciosa nem avanço parcial |
| Observabilidade | Cada chamada usa Flex e correlação local | endpoint, modelo, status, tokens, custo e erro | consumo ausente fica desconhecido, nunca zero inventado |
| Métricas | Score é separado de resultado comercial | contatos, compras, vendas, receita, mídia e publicação em zero | nenhuma solicitação ou parecer é contado como venda |
| Dados de teste | Artefatos usam `LOCAL_QA` e diretório por ciclo | nenhum callback produtivo | métricas e cadastros do Hub não são alterados |
| Desktop/mobile | Nesta etapa existe somente dossiê local | não há jornada pública | Chromium desktop, iPhone 15 Pro e Pixel 7 ficam obrigatórios após materialização |

## Política de rodadas

A primeira execução é exploratória. Como a rodada revelou defeito no contrato Hotmart e exigiu
correção, a homologação só termina após duas execuções locais completas e consecutivas sem falha
depois da última correção. Qualquer novo defeito reinicia a contagem.

## Resultado executado em 2026-08-26

| Rodada | Recorte vencedor | Score | Valor percebido | Decisão |
| --- | --- | ---: | ---: | --- |
| Exploratória | Preflight de Projeto IA | 73 | 72 | pesquisar mais |
| Homologação 1 | Preflight IA Comparável | 74 | 71 | pesquisar mais |
| Homologação 2 | Matriz IA Antes do Contrato | 74 | 72 | pesquisar mais |
| Benchmark Rigel | Kit WhatsApp Pronto | 82 | — | não alcançado |

As duas rodadas finais foram completas, consecutivas e executadas com o mesmo dossiê refinado. A
última rodada obteve aprovação de Argos e Hermes, mas Dédalo e Psique mantiveram o bloqueio porque o
produto ficou oito pontos abaixo de Rigel e o valor percebido não atingiu o mínimo de 75. A
estabilidade em 74 impede selecionar uma amostragem favorável por acaso.

A melhor hipótese ficou delimitada como uma matriz independente para pequenas e médias empresas que
já comparam propostas ou fornecedores de IA. Em uma sessão, ela organizaria resultado, dados,
limites, responsabilidades, lacunas, perguntas e critérios de aceite, sem indicar fornecedor,
executar IA ou receber credenciais. A pesquisa confirmou compra de serviços de IA e a existência de
diagnósticos pagos, mas não confirmou compra do comparador proposto por decisores brasileiros.

- 38 evidências ativas e 18 ofertas pagas deduplicadas na comparação refinada.
- Cinco artigos Markdown atuais carregados no início de cada execução: dois em `pesquisas/gartner`
  e três em `pesquisas/ia-aplicada`.
- Oito referências Hotmart preservadas como inspiração, com score e temperatura proibidos de virar
  venda.
- Três execuções completas dos quatro agentes; custo Flex estimado total de US$ 1,01730980.
- Duas rodadas finais sem falha depois da última alteração do dossiê.
- Contatos, compras, vendas, receita, mídia e publicações permaneceram em zero.

Conclusão: a homologação técnica local foi concluída, mas nenhuma oportunidade foi aprovada como
produto igual ou superior a Rigel. Não foi criado cadastro, experimento, preço, checkout, campanha
ou ativo público.

# Tipos de Produto Canonicos v1

## Objetivo

Definir os tipos de produto do Marketing Hub para orientar decisoes futuras de nicho, hipotese, oferta, pagina, campanha, custo, preco, tracking e escala.

## Regra financeira comum

Todo produto precisa ser financeiramente viavel para o negocio. Antes de escalar, a decisao deve considerar:

- custo de midia;
- custo de IA;
- custo de producao e operacao;
- preco possivel;
- margem esperada;
- capacidade de entrega sem esforco manual relevante;
- lucro esperado em relacao ao risco do experimento.

Produto sem caminho plausivel de lucro nao deve receber escala paga, mesmo quando gerar curiosidade ou cliques.

## Produto low-ticket

Produto low-ticket e um pacote de infoprodutos de baixo custo, produzido majoritariamente com IA e entregue de forma digital.

Caracteristicas obrigatorias:

- preco baixo o suficiente para compra de baixo atrito;
- entrega composta por ativos digitais, como PDF, checklist, roteiro, template, mensagens prontas, planilha simples, diagnostico, prompts, mini-kit ou plano de acao;
- producao automatizavel por IA, sem consultoria, call, acompanhamento humano ou promessa de gestao manual;
- promessa especifica, aplicavel rapidamente e ligada a uma dor real;
- pagina de venda direta quando o objetivo for venda, com checkout, tracking e prova/preview do que sera recebido;
- margem positiva apos considerar midia, IA, taxas e custo operacional.

O low-ticket deve reduzir dor e esforco de forma simples. Ele nao deve prometer resultado absoluto, renda garantida, agenda cheia garantida, cura, automacao total ou qualquer entrega que dependa de trabalho humano recorrente.

## Produto IA

Produto IA e um infoproduto/ferramenta com integracao a API de IA da OpenAI, embalado para o usuario como uma ferramenta simples, facil e quase magica de usar no dia a dia.

Para o usuario, o valor percebido nao deve ser "usar IA". O valor deve ser a ferramenta resolvendo uma tarefa real com pouco esforco, como organizar informacoes, transformar entradas brutas em saidas uteis, gerar materiais aplicaveis, diagnosticar uma rotina, adaptar mensagens, montar planos ou simplificar decisoes.

Caracteristicas obrigatorias:

- usa OpenAI por tras para gerar, transformar, revisar, classificar ou personalizar a entrega;
- experiencia simples para o usuario, sem exigir que ele entenda prompts, modelos ou configuracoes de IA;
- resolve uma tarefa frequente ou dolorosa da rotina da persona;
- entrega saida pratica, editavel ou acionavel;
- possui tracking de uso, custo de IA, resultado e sinal de valor percebido;
- tem modelo economico com margem positiva considerando chamadas de IA, infraestrutura, suporte, midia e taxas;
- antes de campanha paga dedicada, precisa demonstrar dor unica, promessa clara, entregavel demonstravel e sinal de interesse qualificado.

Produto IA pode nascer como teste controlado dentro do planejamento, mas so deve virar campanha ou produto principal quando o ganho percebido for claro e o custo por uso permitir lucro.

### Subtipos canonicos de Produto IA

Produto IA nao deve ser tratado como uma categoria unica. O sistema deve classificar o subtipo porque cada um possui promessa, custo, experiencia, metrica e risco operacional diferentes.

Subtipos iniciais:

| Subtipo | Nome operacional | Objetivo comercial | Uso recomendado |
|---|---|---|---|
| `AI_VISUAL_PREVIEW` | AI Visual Preview | Mostrar uma previa visual do resultado, do depois ou da entrega. | Quando a imagem ajuda o lead a entender rapidamente o beneficio prometido. |
| `AI_PERSONALIZED_SAMPLE` | AI Personalized Sample | Gerar uma amostra visual ou textual exclusiva para o lead antes da compra. | Primeiro MVP recomendado para testar impacto visual e personalizacao. |
| `AI_TRANSFORMATION_SIMULATOR` | AI Transformation Simulator | Simular visualmente uma transformacao desejada. | Quando a promessa depende de comparar estado atual e estado desejado. |
| `AI_VISUAL_ASSET_PACK` | AI Visual Asset Pack | Entregar pacote de imagens, criativos, mockups ou materiais visuais personalizados. | Quando o cliente compra ativos prontos para usar. |
| `AI_IDENTITY_AVATAR_PRODUCT` | AI Identity / Avatar Product | Criar representacao visual de pessoa, marca, persona, avatar ou estilo. | Quando identidade, pertencimento ou expressao visual forem parte central do valor. |
| `AI_REPORT_VISUAL_EVIDENCE` | AI Report + Visual Evidence | Combinar diagnostico textual com imagens, graficos ou quadros visuais. | Quando o visual aumenta clareza, prova ou urgencia da decisao. |

O subtipo nao substitui o tipo principal `Produto IA`. Ele especializa a experiencia para que o sistema consiga comparar resultados, custos e aprendizados entre produtos parecidos.

### Persistencia operacional do subtipo

O subtipo de Produto IA deve ser persistido como `product_ai_subtype` na hipotese e no experimento.

Regra operacional:

- a hipotese pode declarar o subtipo quando o fluxo sistemico identificar que a oferta e Produto IA;
- o experimento herda o subtipo da hipotese quando nasce pelo fluxo normal;
- se o experimento declarar um subtipo explicitamente, esse valor deve ficar registrado para rastrear a variacao testada;
- o primeiro MVP visual/personalizado usa `AI_PERSONALIZED_SAMPLE` como subtipo inicial;
- relatórios, custos, prompts, schemas e aprendizados devem sempre conseguir voltar ao tipo/subtipo do produto testado.

### Regra mandatória — Produto IA nasce pelo sistema

Nenhum Produto IA, low-ticket, oferta, pagina, checkout, amostra, promessa ou variacao deve ser criado manualmente como atalho fora dos recursos do Marketing Hub.

Todo Produto IA deve nascer por uma execucao rastreavel do sistema, preservando no minimo:

- nicho ou contexto de origem;
- hipotese;
- dor principal;
- resultado desejado;
- mecanismo plausivel;
- prova ou evidencia usada;
- oferta;
- tipo e subtipo de produto;
- prompts e schemas usados;
- custos estimados e realizados de IA;
- experimento de validacao;
- versao ou variavel primaria quando for uma variacao.

Antes de criar o experimento, o backend deve validar o preparo da hipotese Produto IA por endpoint sistemico. Para o MVP `AI_PERSONALIZED_SAMPLE`, a hipotese so pode virar experimento quando possuir nicho/contexto, dor principal, persona, promessa, mecanismo, preco, pacote de oferta, entregaveis do pacote e descricao da amostra personalizada. A tela pode exibir bloqueios e aplicar um rascunho canonico, mas a trava definitiva fica no backend.

Quando uma hipotese criada pelo sistema ja possuir nicho/contexto, dor principal, persona, promessa e mecanismo, mas ainda nao possuir os campos operacionais do MVP, o backend pode executar o comando sistemico `POST /api/product-ai/hypotheses/{hypothesisId}/personalized-sample-preparation`. Esse comando apenas completa a hipotese existente com `AI_PERSONALIZED_SAMPLE`, preco inicial, pacote de oferta e entregavel minimo de amostra personalizada. Ele nao cria hipotese nova, nao cria experimento e nao autoriza atalho manual fora do fluxo.

Depois que o experimento `AI_PERSONALIZED_SAMPLE` existir, a publicacao de campanha deve continuar bloqueada ate o backend criar ou reaproveitar o funil canonico de coleta pelo endpoint `POST /api/product-ai/experiments/{experimentId}/personalized-sample-funnel`. Esse funil deve ser um `LeadPortalFlow` aprovado e vinculado ao experimento, coletando no minimo nome, e-mail, WhatsApp, negocio/projeto, contexto atual, objetivo visual e dados de personalizacao. Sem esses dados, o produto nao tem insumo suficiente para prometer amostra exclusiva.

A pagina de venda aprovada pelo GeraSalesPage para `AI_PERSONALIZED_SAMPLE` deve ser publicada dentro desse mesmo `LeadPortalFlow` de coleta, com formulario gerenciado pelo Lead Portal. O link de campanha deve apontar para esse funil-pagina unico. E proibido publicar uma pagina separada que trate o funil como checkout direto ou que pule a coleta de dados de personalizacao.

Os prompts ativos do GeraSalesPage devem receber contexto suficiente para diferenciar venda direta de funil de personalização. Para `AI_PERSONALIZED_SAMPLE`, o destino comercial da página deve ser o funil-pagina do Lead Portal, deixando claro para o worker que a primeira ação pública é coleta de dados, não checkout direto. O quality review deve aceitar formulário somente nesse subtipo e deve bloquear qualquer ambiguidade entre amostra gratuita, produto pago, preço e entrega paga.

Para `AI_PERSONALIZED_SAMPLE`, a pagina publicada dentro do Lead Portal nunca deve embutir o proprio Lead Portal, flow ou funil em `iframe`. O backend deve remover iframe autorreferente antes de publicar e os prompts/schemas ativos devem instruir o worker a usar bloco de formulario gerenciado, nao iframe, para evitar recursao visual e perda de experiencia do lead.

Depois da compra aprovada, a entrega paga do `AI_PERSONALIZED_SAMPLE` deve ser executada pelo módulo externo `product-ai-worker`, no pipeline versionado `personalizedsample.v1` e etapa `paid-delivery`. O backend deve enfileirar a entrega, entregar prompt/schema versionados pelo `pending`, receber `recebeRequest` antes da chamada OpenAI, receber `recebeResponse` com saída funcional, tokens, service tier e artefato, calcular o custo autoritativo e marcar a compra como entregue. E proibido vender Produto IA dependendo de entrega manual ou de worker que acesse banco diretamente.

Criar o mesmo conceito para outro nicho exige nova execucao do fluxo com novo contexto de nicho. Variar um produto para melhora exige nova versao ou novo experimento com variavel primaria declarada.

Padroes externos, como os dossies da Biblioteca de Paginas de Vendas, podem ser usados como insumo de briefing e aprendizado. Eles nao podem substituir a geracao rastreavel de hipotese, dor, mecanismo, prova, oferta e experimento pelo sistema.

## Decisao entre tipos

Use low-ticket quando o problema pode ser resolvido por um pacote digital estatico ou semi-estatico, barato, rapido de produzir e vendavel por pagina curta.

Use Produto IA quando o valor principal depende de transformar uma entrada especifica do usuario em uma saida personalizada, com reducao forte de esforco e experiencia simples.

Quando houver duvida, comece por low-ticket para validar dor/oferta com menor custo. Evolua para Produto IA quando a personalizacao aumentar claramente a percepcao de valor e a margem continuar viavel.

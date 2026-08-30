# Tipos de Produto Canonicos v1

## Objetivo

Definir os tipos de produto do Marketing Hub para orientar decisoes futuras de nicho, hipotese, oferta, pagina, campanha, custo, preco, tracking e escala.

Este documento e a descricao canonica unica dos tipos de produto do sistema. Qualquer novo tipo, subtipo ou mudanca de regra comercial sobre tipos de produto deve ser registrado aqui antes de orientar implementacao, campanhas, experimentos ou relatorios.

## Catálogo extensível e apelidos

Os tipos não são um `enum` fechado. O Marketing Hub deve manter um cadastro operacional com:

- `code`: identidade estável usada por integrações e agentes;
- `name`: nome canônico legível;
- `description`: fronteira do mecanismo de valor e quando usar;
- `aliases`: nomes alternativos usados somente para pesquisa e resolução interna;
- `status`: `PROPOSED`, `ACTIVE` ou `RETIRED`;
- `blueprint_version` e `primary_channel`: versão da base reutilizável e canal principal;
- trabalho do cliente, mecanismo de valor, jornada, entradas, saídas, memória, integrações,
  proteções e métricas de sucesso;
- módulos iniciais de SDK Java e React, quando aplicáveis;
- quantidade de produtos vinculados, calculada pelo backend.

Uma ideia nova pode ser cadastrada como `PROPOSED` sem alterar a classificação de produtos nem
limitar a exploração. Ela só pode receber produtos quando estiver `ACTIVE`. A ativação de tipo novo
exige nome, fronteira e base de construção completos, além do registro correspondente neste cânone.
O backend calcula e expõe se essa base está pronta e quais campos faltam; o frontend não pode
simular essa conclusão. Um tipo aposentado permanece ligado aos produtos históricos, mas não pode
receber novos vínculos.

Apelido não cria outro tipo e nunca substitui o código estável. Pessoas e agentes podem localizar o
mesmo tipo por código, nome ou apelido, mas devem persistir o identificador canônico. Apelidos de
tipo são internos e não entram automaticamente em oferta, landing, checkout ou entrega.

O universo canônico dos nomes internos de tipos é **Minerais**. Cada tipo recebe um mineral único no
campo próprio `internal_name`. O mineral não muda quando a descrição do tipo evoluir, nunca
substitui `code`, `name` ou os apelidos e não pode ser reutilizado em outro tipo. Pesquisa e telas
administrativas devem considerar o codinome; superfícies públicas não devem recebê-lo
automaticamente.

Mapa inicial aprovado em 2026-08-23:

- `PDE`: Opala;
- `LOW_TICKET_DIGITAL_PRODUCT`: Quartzo;
- `AI_PRODUCT`: Safira;
- `AI_SANDBOX_CONVERSATIONAL_PRODUCT`: Fluorita;
- `AI_PWA_CONSULTANT_PRODUCT`: Turmalina;
- `EXTERNAL_MEMBERS_AREA_PRODUCT`: Ágata;
- `AI_INTERACTIVE_EDUCATIONAL_EXPERIENCE`: Labradorita.

Formato de entrega, canal, ativo comercial, mecanismo técnico e nome de trabalho do produto não
devem virar novos tipos quando puderem ser representados pelo tipo vigente, modo comercial,
subtipo, formato ou apelido. A exceção é quando a interface com o cliente muda conjuntamente a
jornada vendida, aquisição, continuidade, integrações, evidências, proteções e SDK mínimo. Essa é a
fronteira que separa o consultor PWA do consultor WhatsApp. Antes de ativar qualquer categoria nova,
comparar esses contratos com as categorias existentes para evitar fragmentar métricas e
aprendizados.

## Fontes consolidadas

Este canon consolida, em uma unica descricao operacional, os tipos e modos de produto ja descritos nos documentos do Marketing Hub, principalmente:

- `docs/registros/experimentos.md`;
- `docs/marketing/plano-primeira-venda-funis-produtos-2026-06-29.md`;
- `docs/implementacao/experimentos/plano-mestre-evolucao-funis-produtos-personalizacao.md`;
- `docs/canonical/product-ai-worker-canon.v1.md`;
- `docs/canonical/trafego-frio-compra-direta-canon.v1.md`;
- `docs/canonical/avatar-sales-video-canonical-rules.md`;
- `docs/plano-produtos/documento.md`;
- `docs/plano-produtos/plano_futuro_experiencia_virtual_com_ia.md`.

Quando houver diferenca entre nomes usados em documentos antigos e este canon, este documento prevalece como fonte unica para classificacao comercial dos tipos de produto.

## Mapa canonico dos tipos e modos

Tipos canonicos de produto:

| Tipo canonico | Nome operacional | Venda principal | Quando usar |
|---|---|---|---|
| `PDE` | PDE - Produto Digital Experiencial | Experiência digital orientada a uma transformação observável. | Quando o mecanismo principal é uma jornada interativa de valor, ainda que use outros modos técnicos. |
| `LOW_TICKET_DIGITAL_PRODUCT` | Produto low-ticket | Compra direta de pacote digital simples. | Para validar dor/oferta com baixo atrito e buscar primeira venda. |
| `AI_PRODUCT` | Produto IA | Ferramenta, amostra, simulador, asset pack ou entrega personalizada com IA. | Quando a personalizacao ou transformacao por IA aumenta valor percebido. |
| `AI_SANDBOX_CONVERSATIONAL_PRODUCT` | Consultor WhatsApp com IA | Consultoria contextual no WhatsApp, com texto, imagem e memória individual. | Quando menor atrito, conversa recorrente e resposta no canal já usado aumentam conversão, retenção ou ticket. |
| `AI_PWA_CONSULTANT_PRODUCT` | Consultor PWA com IA | Consultoria mobile-first instalável, com experiência visual própria, fotos e memória individual. | Quando a entrega precisa de interface rica, histórico, comparação visual e autonomia maiores que o WhatsApp oferece. |
| `EXTERNAL_MEMBERS_AREA_PRODUCT` | Area de membros externa | Acesso a conteudo, curso, comunidade ou produto protegido apos compra. | Quando a entrega exige login, progresso, acesso recorrente ou controle de assinatura. |
| `AI_INTERACTIVE_EDUCATIONAL_EXPERIENCE` | Experiencia educacional interativa com IA | Produto educacional em missoes/cenarios/personagens com feedback de IA. | Quando o valor vem de praticar situacoes, tomar decisoes e receber avaliacao. |

Modos comerciais transversais:

| Modo | Definicao | Aplica-se a |
|---|---|---|
| `GENERIC` | Entrega igual ou quase igual para todos os compradores. | Principalmente low-ticket e area de membros. |
| `PERSONALIZED` | Entrega adaptada ao lead/cliente com dados coletados. | Produto IA, produto personalizado por lead e atendimento por sandbox. |
| `STATIC_ASSET` | Arquivo pronto: PDF, planilha, template, checklist, roteiro, pack. | Low-ticket e area de membros. |
| `GENERATED_PER_LEAD` | Saida gerada individualmente para cada lead/cliente. | Produto IA e atendimento por sandbox. |
| `HYBRID` | Estrutura-base padronizada com partes personalizadas. | Produto IA personalizado e alguns low-tickets evoluidos. |

Ativos comerciais como video de vendas, avatar, pagina, criativo, amostra, lead magnet, checkout e email nao sao automaticamente tipos de produto. Eles podem virar produto apenas quando forem a entrega comprada pelo cliente. Caso contrario, sao ativos de aquisicao, prova, conversao ou entrega.

Os nomes legados `PDE - Consultor Especialista por WhatsApp` e `Produto IA de atendimento
personalizado por sandbox` são apelidos internos de `AI_SANDBOX_CONVERSATIONAL_PRODUCT`.
`Produto low-ticket de posts personalizados` é apelido de `LOW_TICKET_DIGITAL_PRODUCT`. Um produto
web só usa `AI_PWA_CONSULTANT_PRODUCT` quando implementar a jornada rica e a base técnica deste
cânone; uma landing, formulário ou chat web isolado não cria esse tipo.

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

## Funil canonico Clube MUSA/PDE com paywall interno

Produtos PDE ou areas de membros com assinatura devem usar o tipo operacional de experimento
`PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL` quando a jornada comercial depender de entrada na area,
login, visualizacao inicial, compra do acesso e ativacao pos-compra.

Funil obrigatório:

```text
Visualizacao do anuncio
→ clique no anuncio para a tela de login do PDE/MUSA
→ login ou criacao de conta
→ entrada no sistema
→ visualizacao da parte inicial gratuita
→ bloqueio das partes mais importantes
→ visualizacao da oferta de compra do acesso
→ clique no plano/checkout
→ compra ou assinatura aprovada
→ acesso completo liberado
→ primeiro uso/ativacao
```

Eventos operacionais mínimos para esse funil:

- `PDE_LOGIN_ENTRY`: entrada na tela de login do PDE/MUSA vinda do anuncio;
- `LOGIN_STARTED`: início de login ou criação de conta;
- `LOGIN_COMPLETED`: login concluído por Google, magic link ou e-mail;
- `PDE_INITIAL_ACCESS_VIEWED`: visualização da parte inicial gratuita;
- `PDE_IMPORTANT_PART_BLOCKED`: tentativa de continuar em parte importante bloqueada;
- `PAYWALL_VIEWED`: visualização da oferta de compra do acesso;
- `SUBSCRIPTION_CLICKED`: clique no plano ou checkout;
- `SUBSCRIPTION_APPROVED`: assinatura ou compra aprovada;
- `ACCESS_RELEASED`: acesso completo liberado após compra ou assinatura aprovada;
- `FIRST_USE`: primeiro consumo real da experiência, como abrir missão/material ou concluir a primeira missão.

Quando o produto depende de uma entrega operacional anterior ao uso, a regra específica prevalece:
`DELIVERY_COMPLETED` marca a persistência da entrega material e `FIRST_USE` só pode ocorrer após a
primeira aplicação comprovada. Briefing inicial não representa primeiro uso nesse tipo de jornada.

O login nao libera acesso completo. Ele libera somente entrada no sistema e consumo da parte inicial gratuita. As partes mais importantes do PDE/MUSA devem exigir compra aprovada. E proibido criar fluxo canonico alternativo em que anuncio leve direto para checkout ou em que qualquer e-mail valido libere toda a experiencia sem pagamento.

A compra aprovada nao deve ser tratada como fim do funil. Para produto recorrente, o sistema
deve medir ativacao pos-compra porque ela antecipa retencao, renovacao, upgrade, cancelamento
e risco de churn.

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

### Variações canonicas de low-ticket

#### Produto generico ou semi-generico

Produto generico ou semi-generico e o modo preferencial de low-ticket para primeira venda. A entrega e igual ou quase igual para todos os compradores, com adaptacao minima feita pelo proprio cliente.

Exemplos:

- kit de mensagens prontas;
- checklist operacional;
- planilha simples;
- roteiro de atendimento;
- mini-diagnostico preenchivel;
- pacote de templates;
- prompts de IA aplicaveis pelo comprador;
- plano de acao de 7 dias.

Uso recomendado:

- quando a dor e clara e repetida no nicho;
- quando o comprador consegue perceber valor por preview;
- quando a primeira meta e medir compra aprovada, nao apenas lead;
- quando os dados do usuario nao sao obrigatorios para entregar a primeira versao.

Funil recomendado para primeira venda:

```text
Anuncio
→ pagina propria de venda curta
→ checkout
→ entrega simples
```

#### Produto personalizado por lead

Produto personalizado por lead e uma entrega digital em que parte relevante do produto depende de dados coletados antes ou depois da compra.

Ele pode ser low-ticket evoluido, Produto IA ou hibrido. A classificacao final depende do mecanismo principal:

- se a personalizacao for leve e a entrega for majoritariamente estatica, classificar como low-ticket `HYBRID`;
- se a personalizacao depender de geracao, revisao, transformacao ou diagnostico por IA, classificar como `AI_PRODUCT`;
- se a personalizacao acontecer dentro de conversa com memoria individual e sandbox por cliente, classificar como `AI_SANDBOX_CONVERSATIONAL_PRODUCT`.

Uso recomendado:

- quando os dados do lead aumentam claramente a utilidade da entrega;
- quando a amostra personalizada melhora desejo e prova;
- quando o custo de coleta, IA e follow-up cabe na margem;
- quando o funil consegue explicar por que o usuario deve informar seus dados.

Risco principal: transformar formulario em barreira antes do valor percebido. Para primeira venda, produto generico low-ticket costuma ser mais simples e direto.

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

Quando uma hipotese criada pelo sistema ja possuir nicho/contexto, dor principal, persona, promessa e mecanismo, mas ainda nao possuir os campos operacionais do MVP, o backend pode executar o comando sistemico `POST /api/product-ai/hypotheses/{hypothesisId}/personalized-sample-preparation`. Esse comando apenas completa a hipotese existente com `AI_PERSONALIZED_SAMPLE`, preco inicial, pacote de oferta e entregavel minimo de amostra personalizada. Ele nao cria experimento e nao autoriza atalho manual fora do fluxo.

Quando o aprendizado comercial indicar que o funil gratuito nao esta gerando compra, o backend pode executar `POST /api/product-ai/hypotheses/{hypothesisId}/experiment-preparation` com `productAiSubtype=AI_VISUAL_PREVIEW` para criar ou reaproveitar uma variante paralela da mesma base comercial. Essa variante representa o Funil B de venda de entrada: pagina curta, CTA de checkout, compra da previa visual por preco baixo e envio dos dados/foto depois do pagamento. A hipotese original `AI_PERSONALIZED_SAMPLE` deve permanecer intacta para preservar comparacao e aprendizado entre funil de amostra gratuita e funil de previa paga.

Um experimento Produto IA nao pode ser criado com subtipo diferente do subtipo preparado na hipotese vinculada. Se a tela selecionar `AI_VISUAL_PREVIEW`, deve primeiro usar a variante retornada pelo preparo sistemico antes de salvar o experimento. Essa trava evita misturar metricas de lead/amostra com metricas de venda de entrada.

Depois que o experimento `AI_PERSONALIZED_SAMPLE` existir, a publicacao de campanha deve continuar bloqueada ate existir um funil canonico de coleta no Lead Portal. A criacao, manutencao, formulario, upload, acompanhamento e experiencia publica desse funil pertencem ao `lead-portal`; o backend principal do Marketing Hub nao deve expor endpoint publico ou de atendimento de lead/cliente para esse caso. A prontidao deve aceitar somente um contrato de template canonico completo, sem exigir campos de outro template. O briefing generico coleta e-mail, negocio/projeto, contexto atual, objetivo visual e dados de personalizacao. A microamostra de redes sociais coleta e-mail, nome profissional, servico divulgado e escolha de estilo visual; foto de referencia permanece opcional. Nome e WhatsApp podem ser solicitados dentro dos dados de personalizacao no template generico quando forem necessarios para a peca, mas nao devem criar dois campos obrigatorios adicionais antes da amostra. Um formulario que misture campos parciais de templates diferentes deve continuar bloqueado.

Quando o contexto comercial do experimento indicar o piloto `DecoraIA Express`, decoracao ou melhoria de ambiente por foto, o fluxo do Lead Portal deve especializar o funil de `AI_PERSONALIZED_SAMPLE` para coletar foto do ambiente por `IMAGE_UPLOAD`, ambiente a transformar, incomodo principal, objetivo visual, orcamento aproximado, dados de personalizacao e preferencias visuais. Essa especializacao nao cria novo tipo de produto; ela e a primeira variacao concreta do Produto IA visual personalizado.

A pagina de venda aprovada pelo GeraSalesPage para `AI_PERSONALIZED_SAMPLE` deve ser publicada dentro desse mesmo `LeadPortalFlow` de coleta, com formulario gerenciado pelo Lead Portal. O link de campanha deve apontar para esse funil-pagina unico. E proibido publicar uma pagina separada que trate o funil como checkout direto ou que pule a coleta de dados de personalizacao.

Para `AI_VISUAL_PREVIEW` usado como Funil B de venda de entrada, a primeira acao publica deve ser o checkout da previa paga, nao um formulario gratuito. A coleta de foto, objetivo visual e preferencias ocorre depois da compra aprovada, para medir disposicao real de pagamento antes de acionar a personalizacao completa.

O valor central de `AI_PERSONALIZED_SAMPLE` e vender uma solucao aplicada a realidade do lead, nao vender "IA", template generico ou PDF estatico. A amostra gratuita deve provar a personalizacao com um recorte pequeno e util; o produto pago deve entregar a versao completa tambem personalizada, usando os dados capturados no Lead Portal. Se a amostra for personalizada e a entrega paga for generica, o funil quebra a promessa, reduz valor percebido e aumenta risco de frustracao.

Funil canonico recomendado para o primeiro teste:

```text
Anuncio
→ Lead Portal com promessa e formulario curto
→ Amostra personalizada gratuita
→ E-mail 1: entrega da amostra e oferta leve
→ E-mail 2: follow-up de conversao
→ Checkout
→ Entrega paga personalizada
```

Para o primeiro experimento, a sequencia deve ser simples: dois e-mails sao suficientes para validar interesse, clique e compra sem aumentar complexidade operacional. Sequencias maiores so devem ser usadas depois, quando houver evidencia de abertura, clique, resposta ou interesse sem compra.

O produto pago deve ser uma entrega hibrida personalizada: estrutura-base padronizada para escalar, com conteudo final adaptado aos dados do lead. A entrega deve conter diagnostico, plano ou materiais aplicaveis ao contexto declarado, separando claramente o que foi recebido como amostra gratuita e o que foi liberado apos pagamento.

Exemplo canonico de aplicacao: para manicures em domicilio, a amostra gratuita pode ser um post personalizado acompanhado de uma legenda pronta, suficiente para demonstrar qualidade e aplicacao sem substituir o produto completo. A compra deve entregar o kit completo do experimento, com materiais adicionais coerentes com a promessa, adaptados ao tom escolhido e aos dados informados. O experimento de amostra deve gerar somente uma imagem por pacote; aumentar a quantidade de pecas exige nova versao ou decisao comercial explicita.

Os prompts ativos do GeraSalesPage devem receber contexto suficiente para diferenciar venda direta de funil de personalização. Para `AI_PERSONALIZED_SAMPLE`, o destino comercial da página deve ser o funil-pagina do Lead Portal, deixando claro para o worker que a primeira ação pública é coleta de dados, não checkout direto. O quality review deve aceitar formulário somente nesse subtipo e deve bloquear qualquer ambiguidade entre amostra gratuita, produto pago, preço e entrega paga.

Para `AI_PERSONALIZED_SAMPLE`, a pagina publicada dentro do Lead Portal nunca deve embutir o proprio Lead Portal, flow ou funil em `iframe`. O backend deve remover iframe autorreferente antes de publicar e os prompts/schemas ativos devem instruir o worker a usar bloco de formulario gerenciado, nao iframe, para evitar recursao visual e perda de experiencia do lead.

Depois da compra aprovada, a entrega paga do `AI_PERSONALIZED_SAMPLE` deve ser executada pelo módulo externo `product-ai-worker`, no pipeline versionado `personalizedsample.v1` e etapa `paid-delivery`. O backend deve enfileirar a entrega, entregar prompt/schema versionados pelo `pending`, receber `recebeRequest` antes da chamada OpenAI, receber `recebeResponse` com saída funcional, tokens, service tier e artefato, calcular o custo autoritativo e marcar a compra como entregue. E proibido vender Produto IA dependendo de entrega manual ou de worker que acesse banco diretamente.

Criar o mesmo conceito para outro nicho exige nova execucao do fluxo com novo contexto de nicho. Variar um produto para melhora exige nova versao ou novo experimento com variavel primaria declarada.

Padroes externos, como os dossies da Biblioteca de Paginas de Vendas, podem ser usados como insumo de briefing e aprendizado. Eles nao podem substituir a geracao rastreavel de hipotese, dor, mecanismo, prova, oferta e experimento pelo sistema.

### Produto IA com video, avatar ou imagem

Video, avatar e imagem podem assumir dois papeis diferentes:

- ativo comercial: criativo, prova visual, video de vendas, avatar explicando a oferta ou bloco de pagina;
- produto comprado: video personalizado, avatar do cliente, pack visual, simulacao, identidade ou asset visual entregue ao comprador.

Quando forem apenas ativos comerciais, devem seguir os canones dos respectivos modulos, mas nao criam novo tipo de produto.

Quando forem a entrega comprada, devem ser classificados como `AI_PRODUCT` no subtipo mais proximo:

- `AI_VISUAL_PREVIEW` para previa visual;
- `AI_VISUAL_ASSET_PACK` para pacote de imagens/criativos/mockups;
- `AI_IDENTITY_AVATAR_PRODUCT` para avatar, identidade ou personagem;
- `AI_TRANSFORMATION_SIMULATOR` para antes/depois ou simulacao visual;
- `AI_REPORT_VISUAL_EVIDENCE` para diagnostico com evidencia visual.

O modulo Avatar Sales Video deve ser tratado como capacidade de ativo/produto audiovisual com regras proprias de evidencia, etica, personagem e publicacao. Ele nao substitui o tipo principal do produto vendido.

## Princípios comuns dos consultores com IA

Os comentários, reclamações e artigos consolidados em `/pesquisas` reforçam que o consultor não
deve ser definido pelo chat nem pelo modelo. Seu produto é um **workflow vertical de decisão e
acompanhamento**. Os sinais são insumos de desenho, não prova de venda; receita só existe após
pagamento reconciliado do próprio produto.

Todo Consultor PWA ou WhatsApp deve seguir estes princípios:

- **momento concreto antes da persona:** declarar qual situação faz a pessoa procurar ajuda agora,
  o custo do erro ou do esforço e qual decisão precisa ser tomada;
- **microvalor observável:** entregar rapidamente uma interpretação, comparação, organização ou
  próxima ação que a pessoa consiga conferir; apenas explicar o tema ou repetir conteúdo gratuito
  não sustenta um produto pago;
- **ciclo de relacionamento:** operar `contexto → interpretação → recomendação → ação → feedback →
  memória revisada`, permitindo que um novo contato comece melhor que o anterior;
- **personalização autorizada:** perguntar o necessário e distinguir dado declarado, observado,
  confirmado e inferido. Atributo íntimo ou sensível não pode ser adivinhado a partir de foto,
  comportamento ou histórico;
- **controle proporcional ao risco:** pesquisar, organizar e recomendar podem ser automáticos;
  compra, pagamento, publicação, comunicação externa e outra ação de alto impacto exigem
  confirmação e caminho de interrupção, correção ou atendimento humano;
- **certeza do próximo passo:** a orientação deve dizer o que foi entendido, o que recomenda, em
  qual evidência/confiança se apoia, qual limitação existe e o que acontecerá se a pessoa avançar;
- **empatia sem personificação enganosa:** reconhecer de forma breve a intenção, o esforço ou a
  ansiedade da situação, sem afirmar sentimentos humanos, intimidade inexistente ou certeza que a
  evidência não oferece;
- **vantagem contra o gratuito:** cada produto concreto precisa declarar qual trabalho executa que
  ChatGPT, busca, planilha ou comparação manual não resolvem com esforço semelhante;
- **retenção por utilidade:** retorno, instalação e notificação só são solicitados depois de valor
  percebido e quando ajudarem a próxima missão, nunca apenas para aumentar presença ou disparos.

Base interna desta decisão:

- `pesquisas/ia-aplicada/2026-08-24-produtos-digitais-tendencias-consumo.md`: usuários pagam pela
  interpretação e pelo próximo passo, não pelo acesso ao chatbot;
- `pesquisas/ia-aplicada/2026-08-27-produtos-digitais-tendencias-consumo.md`: memória contextual,
  recorrência e orientação por objetivo são o ativo central do especialista vertical;
- `pesquisas/momentos-de-compra-b2c/2026-08-28-momentos-de-compra-b2c.md`: microvalor, estado
  operacional persistente, evidência auditável e vantagem sobre a alternativa gratuita;
- `pesquisas/neuromarketing/2026-08-26-0137-neuromarketing.md` e
  `pesquisas/neuromarketing/2026-08-27-0125-neuromarketing.md`: empatia, reversibilidade,
  permissão e personalização baseada no que a pessoa escolheu declarar;
- `pesquisas/neuromarketing/2026-08-29-0139-neuromarketing.md`: contexto de confiança antes de
  interagir no WhatsApp;
- `docs/pesquisa-profunda/pesquisa-pde-mobile-entrada-na-vida-do-cliente-brasileiro.md`: URL e
  microvalor antes de instalação ou cadastro pesado.

## Consultor PWA com IA — Turmalina

O `AI_PWA_CONSULTANT_PRODUCT` é um consultor pessoal mobile-first acessado por URL HTTPS e
instalável como PWA. Seu valor não é oferecer um chat genérico: é tornar uma capacidade sofisticada
de IA simples, visual, contínua e orientada ao trabalho concreto do cliente. A interface própria
permite organizar fotos, comparações, histórico, cartões de orientação e direitos de dados com mais
clareza que um mensageiro.

Fluxo mínimo:

```text
Link profundo abre a missão prometida
→ demonstração ou microvalor efêmero com a menor entrada possível
→ login ou vínculo seguro quando for necessário salvar, retomar ou acessar dado privado
→ consentimento contextual para uso de dados, memória e mídia
→ objetivo, contexto declarado e foto opcional
→ estado de processamento compreensível
→ microresultado, evidência, confiança e uma próxima ação
→ refinamento ou comparação
→ avaliação de utilidade
→ memória autorizada para contato futuro
→ convite de instalação somente após valor e com benefício explícito
→ retorno na etapa correta da missão
```

Base obrigatória de construção:

- PWA em React 18, responsiva e acessível, com manifest, service worker, HTTPS e fallback quando
  instalação, câmera ou recurso do dispositivo não estiver disponível;
- frontend consumindo somente o backend PDE; navegador nunca acessa Codex App Server, banco ou
  endpoint de outro módulo;
- backend PDE como fonte de identidade, acesso, consentimento, memória, pendência, auditoria,
  métricas, gate e resultado;
- worker Java usando o perfil de consultores do PDE Harness SDK e Codex App Server local por
  `stdio`, sem OpenAI API direta;
- imagens copiadas para workspace efêmero segregado após validação de tipo, tamanho e SHA-256;
  caminhos originais e conteúdo privado não podem aparecer em prompt, log ou outro cliente;
- memória durável no escopo `tenant + produto + cliente`, com fatos relevantes filtrados antes do
  ranking, procedência, confiança, validade, correção e esquecimento; thread apenas para contexto
  recente da conversa;
- prompt separado em parte do agente, parte da atividade e mensagem atual, todos versionados e
  auditáveis junto ao schema e ao prompt completo efetivamente executado;
- envelope do consultor exigindo microvalor operacional, personalização autorizada, incerteza
  explícita, uma próxima ação prioritária e controle proporcional ao risco;
- resposta funcional estruturada em mensagem, recomendação, justificativa, próxima pergunta,
  candidatos de memória e bloqueio com orientação e links seguros;
- eventos segregados por navegador, sistema operacional e modo `browser` ou `standalone/PWA`, com
  convite de instalação registrado somente depois de microvalor comprovado.

Entradas mínimas: cliente autenticado, objetivo atual, contexto necessário, consentimento e mídia
autorizada quando útil. Saídas mínimas: recomendação principal, motivo simples, próximo passo,
estado de bloqueio e oportunidade clara de refinamento. Um produto específico, como a Amora, deve
especializar identidade, método, limites e schema no worker, sem alterar o núcleo do SDK.

Métricas mínimas: entrada, microvalor, tempo até valor, abandono antes do valor e da identificação,
login, primeira mensagem, foto enviada, orientação entregue, utilidade, refinamento, próxima ação
aceita, convite de instalação após valor, instalação e uso posterior, retorno D1/D7/D30, compra ou
renovação reconciliada, custo, margem, erro, ajuda, reclamação, correção e exclusão. Eventos de QA
devem ser segregados e nunca contam como venda.

Use Turmalina quando uma experiência visual própria, histórico navegável, comparação ou interação
mais rica for parte do valor vendido. Não use para uma landing, formulário, chat web isolado ou
simples espelho do WhatsApp.

## Consultor WhatsApp com IA — Fluorita

O `AI_SANDBOX_CONVERSATIONAL_PRODUCT`, de nome operacional **Consultor WhatsApp com IA**, é um
produto conversacional em que o cliente entra pelo WhatsApp, faz uma solicitação ou continua uma
conversa anterior e recebe resposta personalizada com texto, imagem ou ambos. O valor central não é
"conversar com IA"; é receber atendimento útil, contextual e personalizado, com memória do
relacionamento e capacidade de transformar dados do próprio cliente em orientação, diagnóstico,
proposta, criativo, plano ou entrega aplicável. A evolução do nome preserva o código Fluorita e todo
produto histórico já vinculado.

Este tipo usa o conceito operacional do exemplo `/exemplos/aih6`: uma solicitacao entra pelo front/backend, o backend cria uma execucao isolada, o Codex App Server disponibiliza uma sandbox para o modelo trabalhar, o modelo usa ferramentas e contexto dentro dessa sandbox, devolve um resultado auditavel e a execucao e encerrada. A diferenca canonica e que, neste produto, a sandbox nao baixa um repositorio para gerar codigo; ela baixa ou recebe os dados de relacionamento daquele cliente, materiais autorizados e fontes complementares necessarias para produzir uma resposta comercial ou operacional personalizada.

O runtime canônico deste tipo é o PDE Harness SDK sobre o Codex App Server, conforme
`docs/canonical/pde-platform-canon.v1.md`. PDE novo baseado em agentes não pode chamar diretamente
a OpenAI API nem usar o OpenAI Agents SDK como runtime; deve acessar o App Server local ao worker
por `stdio` usando o PDE Harness SDK em Java 21, com sessão ChatGPT gerenciada pelo Codex, contratos
tipados e isolamento por cliente.
Indisponibilidade do App Server bloqueia a execução e nunca autoriza fallback silencioso para API.

Fluxo canonico inicial:

```text
Cliente envia mensagem no WhatsApp
→ provedor WhatsApp entrega evento ao backend
→ backend identifica cliente, conversa, produto e contexto autorizado
→ primeira resposta reafirma marca, origem, motivo, escopo e opções de parar ou falar com pessoa
→ cliente confirma o objetivo e as permissões desta interação
→ backend cria uma sandbox exclusiva para aquele cliente/interacao via Codex App Server
→ sandbox recebe historico de interacoes, dados permitidos e materiais auxiliares
→ modelo analisa, pesquisa quando permitido e produz microvalor operacional
→ resposta apresenta evidência/confiança, uma próxima ação e o que acontecerá depois
→ backend envia texto e/ou imagem pelo WhatsApp
→ backend registra entrada, contexto, permissões, resposta, feedback, custos, status e resultado
→ memória é revisada somente com fatos úteis e procedência explícita
→ sandbox e descartada
```

Caracteristicas obrigatorias:

- uma sandbox isolada por cliente/interacao relevante, sem reutilizar workspace entre clientes;
- historico do cliente carregado a partir de dados persistidos e autorizados, nunca por memoria solta do modelo;
- memoria de relacionamento persistida pelo backend no escopo `tenant + produto + cliente`, com
  revisao, procedencia, validade, correcao e exclusao auditaveis; a thread do Codex serve apenas como
  continuidade recente e nunca como unica memoria do cliente;
- vinculo de thread restrito ao escopo `tenant + produto + versao + cliente + conversa`; receber um
  `threadId` sem esse vinculo, com memoria de outro escopo ou com revisao regressiva deve bloquear a
  execucao antes de carregar o historico;
- recuperacao seletiva: cada contato recebe resumo e fatos relevantes para a missao atual, dentro de
  limite de contexto, em vez de copiar indiscriminadamente todo o historico do relacionamento;
- filtragem antes da relevancia: banco e indice de busca devem restringir primeiro o escopo exato
  `tenant + produto + cliente`; busca global seguida de filtro posterior e proibida porque pode
  colocar dados de outro relacionamento no conjunto candidato;
- toda interacao deve permanecer em trilha imutavel e idempotente antes de alimentar uma nova
  revisao de memoria. Correcao explicita atual do cliente prevalece sobre fato antigo ou inferido;
  inferencias permanecem rotuladas com procedencia, confianca e validade;
- a primeira resposta deve incluir contexto de confiança: marca, origem da conversa, motivo,
  capacidade do consultor, próximo passo e opções de encerrar ou pedir atendimento humano;
- personalização sensível usa somente dado declarado e autorizado. Quando faltar contexto, o
  consultor pergunta em vez de inferir corpo, saúde, finanças, identidade ou atributo íntimo;
- a resposta deve executar microtrabalho verificável, explicitar evidência e confiança, priorizar
  uma próxima ação e informar o que acontece depois; resposta genérica não conclui a missão;
- ações de alto impacto carregam envelope de permissão e confirmação; o consultor não compra, paga,
  publica, envia comunicação externa ou ultrapassa o escopo por conta própria;
- separacao clara entre dados do cliente, materiais de apoio, fontes externas e resposta final;
- registro auditavel da mensagem recebida, contexto entregue a sandbox, prompts/schemas quando houver, resposta enviada, midia gerada, custo, erro e status;
- resposta enviada pelo canal original do cliente, inicialmente WhatsApp, com suporte a texto e imagem quando o caso de uso exigir;
- encerramento e descarte da sandbox apos a execucao, preservando apenas os registros necessarios no banco;
- protecao de privacidade: dados de um cliente nunca podem aparecer na sandbox, resposta ou artefatos de outro cliente;
- identificadores de thread e vinculos de memoria nunca podem ser recebidos do WhatsApp, frontend
  ou outro canal publico; somente o backend os resolve pela conversa autenticada;
- direito de correcao e esquecimento: o backend deve invalidar memoria, vinculos e threads associados
  quando a politica ou solicitacao autorizada exigir, sem reaproveitar o dado removido em contato
  futuro;
- modelo economico com margem positiva considerando WhatsApp, IA, imagem, infraestrutura, armazenamento, suporte e recuperacao de falhas.

Usos recomendados:

- consultoria automatizada de entrada, com diagnostico personalizado e proximo passo pago;
- atendimento pos-lead para aumentar conversao de produtos digitais;
- entrega personalizada sob demanda, quando o valor depende do historico e do contexto do cliente;
- geracao de criativos, mensagens, planos, roteiros, imagens ou recomendacoes a partir de dados do proprio cliente;
- reativacao de leads e clientes com abordagem contextual, em vez de disparo generico.

Este tipo deve ser tratado como produto de alto potencial comercial porque combina canal de resposta direta, personalizacao real e reducao forte de esforco para o cliente. O risco principal e operacional: se a memoria estiver incompleta, se a sandbox misturar clientes, se a resposta nao for auditavel ou se o custo por atendimento superar a margem, o produto perde escalabilidade. Por isso, a primeira versao deve priorizar poucos casos de uso de alto valor percebido, resposta curta, custo controlado e metricas claras.

Metricas minimas:

- taxa de resposta do cliente no WhatsApp;
- contexto de confiança exibido e objetivo confirmado;
- microvalor alcançado e continuidade por três ou mais mensagens;
- tempo ate primeira resposta util;
- custo medio por atendimento;
- taxa de aceite da próxima ação, clique, agendamento, compra ou proximo passo;
- taxa de reabertura de conversa;
- satisfacao, reclamacao ou sinal negativo;
- correção, esquecimento, opt-out e intervenção humana;
- margem por conversa, por cliente e por oferta;
- recorrencia de uso quando o produto for vendido como assinatura ou acompanhamento.

Gatilhos de bloqueio:

- ausencia de identificacao segura do cliente;
- historico insuficiente para personalizacao prometida;
- risco de expor dado de outro cliente;
- solicitacao fora do escopo do produto vendido;
- necessidade de decisao humana, juridica, medica, financeira ou sensivel sem protocolo especifico;
- custo estimado acima da margem esperada;
- falha no registro auditavel da interacao.

Este tipo nao substitui low-ticket nem Produto IA tradicional. Ele deve ser escolhido quando o canal conversacional e a memoria do relacionamento aumentam a conversao ou o valor percebido mais do que uma pagina, formulario, entrega estatica ou ferramenta self-service.

## Area de membros externa

Area de membros externa e um produto digital em que a venda, pagamento ou assinatura podem acontecer em plataforma externa, como Kiwify, enquanto o Marketing Hub ou sistema associado controla acesso, login, conteudo, progresso, eventos de compra e status de assinatura.

Este tipo serve para produtos que nao cabem bem como simples arquivo entregue apos checkout.

Caracteristicas obrigatorias:

- compra aprovada libera acesso, nunca promessa sem validacao de pagamento;
- eventos de venda, reembolso, chargeback, cancelamento e renovacao devem ser recebidos por webhook ou reconciliados por API;
- acesso precisa ser idempotente, auditavel e reversivel;
- usuario, produto, matricula/acesso e eventos comerciais devem ficar rastreados;
- se houver assinatura, o produto deve tratar atraso, cancelamento, renovacao e expiracao;
- o conteudo protegido deve ser coerente com a oferta vendida.

Usos recomendados:

- curso, comunidade, biblioteca, assinatura, clube, area de alunos ou produto com atualizacoes;
- produto com multiplos modulos, aulas, downloads ou progresso;
- entrega que precisa de controle de acesso depois da compra;
- produto vendido por checkout externo com area propria de consumo.

Este tipo pode conter low-tickets, Produto IA ou experiencias educacionais, mas sua caracteristica principal e o controle de acesso e consumo pos-compra.

Metricas minimas:

- compra aprovada;
- liberacao de acesso;
- primeiro acesso;
- consumo de conteudo;
- renovacao, cancelamento, reembolso e chargeback;
- tickets de suporte;
- margem por aluno/assinante.

## Experiencia educacional interativa com IA

Experiencia educacional interativa com IA e um produto em que o usuario aprende praticando em cenarios, missoes, personagens, decisoes, consequencias e feedback. O valor central nao e assistir aulas; e viver uma situacao simulada, tomar decisoes e melhorar com avaliacao.

Exemplos:

- treinamento de vendas com cliente simulado;
- atendimento a cliente insatisfeito;
- lideranca e resolucao de conflitos;
- marketing com campanhas ficticias e analise de resultado;
- programacao ou operacao em ambiente simulado;
- empreendedorismo com decisoes e consequencias.

Caracteristicas obrigatorias:

- possui experiencia, missoes, cenarios, personagens, decisoes, feedback e progresso;
- IA pode atuar como personagem, tutor, avaliador ou narrador;
- compra/acesso deve ser controlado por backend ou area de membros;
- historico de interacoes, progresso, notas e feedbacks deve ser persistido;
- promessa deve ser ligada a pratica e evolucao de habilidade, nao a resultado garantido;
- criterios de avaliacao devem ser claros, auditaveis e coerentes com o conteudo vendido.

Usos recomendados:

- quando o cliente precisa treinar comportamento, julgamento, comunicacao ou tomada de decisao;
- quando a pratica simulada aumenta mais valor percebido do que aula passiva;
- quando a recorrencia ou assinatura pode ser sustentada por novas missoes e cenarios.

Este tipo pode usar `AI_PRODUCT` internamente, mas deve ser classificado separadamente quando a experiencia educacional completa for o produto vendido.

## Decisao entre tipos

Use low-ticket quando o problema pode ser resolvido por um pacote digital estatico ou semi-estatico, barato, rapido de produzir e vendavel por pagina curta.

Use Produto IA quando o valor principal depende de transformar uma entrada especifica do usuario em uma saida personalizada, com reducao forte de esforco e experiencia simples.

Use Consultor WhatsApp com IA quando o valor principal depende de conversa, memória individual,
contexto acumulado e resposta personalizada no canal que o cliente já usa, com o menor atrito de
entrada possível.

Use Consultor PWA com IA quando o valor depende também de interface visual própria, comparação,
histórico navegável, instalação opcional, controle de dados e uma jornada self-service mais rica.

Use area de membros externa quando a entrega precisa de acesso protegido, progresso, recorrencia, assinatura, biblioteca, comunidade ou controle pos-compra.

Use experiencia educacional interativa com IA quando o produto vendido for pratica simulada com missoes, personagens, decisoes, consequencias e feedback.

Quando houver dúvida, comece por low-ticket genérico para validar dor/oferta com menor custo.
Evolua para Produto IA quando a personalização aumentar claramente a percepção de valor e a margem
continuar viável. Entre os consultores, prefira WhatsApp para provar conversa e recorrência com baixo
atrito; adote PWA quando a experiência visual própria for parte mensurável da entrega. Evolua para
área de membros ou experiência educacional quando a venda exigir consumo recorrente, progresso ou
treinamento mais profundo.

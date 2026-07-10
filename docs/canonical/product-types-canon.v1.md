# Tipos de Produto Canonicos v1

## Objetivo

Definir os tipos de produto do Marketing Hub para orientar decisoes futuras de nicho, hipotese, oferta, pagina, campanha, custo, preco, tracking e escala.

Este documento e a descricao canonica unica dos tipos de produto do sistema. Qualquer novo tipo, subtipo ou mudanca de regra comercial sobre tipos de produto deve ser registrado aqui antes de orientar implementacao, campanhas, experimentos ou relatorios.

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
| `LOW_TICKET_DIGITAL_PRODUCT` | Produto low-ticket | Compra direta de pacote digital simples. | Para validar dor/oferta com baixo atrito e buscar primeira venda. |
| `AI_PRODUCT` | Produto IA | Ferramenta, amostra, simulador, asset pack ou entrega personalizada com IA. | Quando a personalizacao ou transformacao por IA aumenta valor percebido. |
| `AI_SANDBOX_CONVERSATIONAL_PRODUCT` | Produto IA de atendimento personalizado por sandbox | Atendimento ou entrega conversacional personalizada por WhatsApp/canal equivalente. | Quando memoria individual e resposta contextual aumentam conversao, retencao ou ticket. |
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

Depois que o experimento `AI_PERSONALIZED_SAMPLE` existir, a publicacao de campanha deve continuar bloqueada ate existir um funil canonico de coleta no Lead Portal. A criacao, manutencao, formulario, upload, acompanhamento e experiencia publica desse funil pertencem ao `lead-portal`; o backend principal do Marketing Hub nao deve expor endpoint publico ou de atendimento de lead/cliente para esse caso. O funil deve coletar no minimo nome, e-mail, WhatsApp, negocio/projeto, contexto atual, objetivo visual e dados de personalizacao. Sem esses dados, o produto nao tem insumo suficiente para prometer amostra exclusiva.

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

Exemplo canonico de aplicacao: para manicures em domicilio, a amostra gratuita pode ser uma `Agenda Blindada 7D` parcial com diagnostico da agenda e 1 ou 2 mensagens personalizadas. A compra deve entregar o `Kit Personalizado Agenda Blindada 7D`, com diagnostico completo, plano dos proximos 7 dias, mensagens de confirmacao, cobranca de sinal, reagendamento, atraso e cliente que some, adaptadas ao tom escolhido e aos dados de agenda informados.

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

## Produto IA de atendimento personalizado por sandbox

Produto IA de atendimento personalizado por sandbox e um produto conversacional em que o cliente entra pelo WhatsApp, faz uma solicitacao ou continua uma conversa anterior, e recebe uma resposta personalizada com texto, imagem ou ambos. O valor central nao e "conversar com IA"; e receber um atendimento util, contextual e personalizado, com memoria do relacionamento e capacidade de transformar dados do proprio cliente em orientacao, diagnostico, proposta, criativo, plano ou entrega aplicavel.

Este tipo usa o conceito operacional do exemplo `/exemplos/aih6`: uma solicitacao entra pelo front/backend, o backend cria uma execucao isolada, o Codex App Server disponibiliza uma sandbox para o modelo trabalhar, o modelo usa ferramentas e contexto dentro dessa sandbox, devolve um resultado auditavel e a execucao e encerrada. A diferenca canonica e que, neste produto, a sandbox nao baixa um repositorio para gerar codigo; ela baixa ou recebe os dados de relacionamento daquele cliente, materiais autorizados e fontes complementares necessarias para produzir uma resposta comercial ou operacional personalizada.

Fluxo canonico inicial:

```text
Cliente envia mensagem no WhatsApp
→ provedor WhatsApp entrega evento ao backend
→ backend identifica cliente, conversa, produto e contexto autorizado
→ backend cria uma sandbox exclusiva para aquele cliente/interacao via Codex App Server
→ sandbox recebe historico de interacoes, dados permitidos e materiais auxiliares
→ modelo analisa, pesquisa quando permitido e produz resposta personalizada
→ backend envia texto e/ou imagem pelo WhatsApp
→ backend registra entrada, contexto usado, resposta, custos, status e resultado
→ sandbox e descartada
```

Caracteristicas obrigatorias:

- uma sandbox isolada por cliente/interacao relevante, sem reutilizar workspace entre clientes;
- historico do cliente carregado a partir de dados persistidos e autorizados, nunca por memoria solta do modelo;
- separacao clara entre dados do cliente, materiais de apoio, fontes externas e resposta final;
- registro auditavel da mensagem recebida, contexto entregue a sandbox, prompts/schemas quando houver, resposta enviada, midia gerada, custo, erro e status;
- resposta enviada pelo canal original do cliente, inicialmente WhatsApp, com suporte a texto e imagem quando o caso de uso exigir;
- encerramento e descarte da sandbox apos a execucao, preservando apenas os registros necessarios no banco;
- protecao de privacidade: dados de um cliente nunca podem aparecer na sandbox, resposta ou artefatos de outro cliente;
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
- tempo ate primeira resposta util;
- custo medio por atendimento;
- taxa de clique, agendamento, compra ou proximo passo;
- taxa de reabertura de conversa;
- satisfacao, reclamacao ou sinal negativo;
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

Use Produto IA de atendimento personalizado por sandbox quando o valor principal depende de conversa, memoria individual do cliente, contexto acumulado e resposta personalizada enviada por WhatsApp ou canal conversacional equivalente.

Use area de membros externa quando a entrega precisa de acesso protegido, progresso, recorrencia, assinatura, biblioteca, comunidade ou controle pos-compra.

Use experiencia educacional interativa com IA quando o produto vendido for pratica simulada com missoes, personagens, decisoes, consequencias e feedback.

Quando houver duvida, comece por low-ticket generico para validar dor/oferta com menor custo. Evolua para Produto IA quando a personalizacao aumentar claramente a percepcao de valor e a margem continuar viavel. Evolua para atendimento personalizado por sandbox quando o relacionamento individual e a resposta contextual tiverem potencial de aumentar conversao, retencao ou ticket medio de forma mensuravel. Evolua para area de membros ou experiencia educacional quando a venda exigir consumo recorrente, progresso ou treinamento mais profundo.

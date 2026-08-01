# Estudio de Audio e Video Canon v1

## Decisao de produto

O Marketing Hub tera um item de menu separado chamado **Estudio de Audio e Video** para producoes audiovisuais sofisticadas.

Esse estudio nasce para videos com:

- varias cenas;
- roteiro longo;
- narracao;
- trilha sonora;
- desenho de ritmo;
- continuidade visual;
- pos-producao;
- revisao editorial;
- potencial de uso em campanhas, conteudo organico premium, treinamento, apresentacoes comerciais e PDEs de maior valor percebido.

## Separacao dos fluxos atuais

Os fluxos existentes de video devem continuar nos seus lugares atuais:

- criativos de experimentos;
- videos curtos organicos;
- videos especificos de PDEs;
- aprovacao de videos;
- geracao e tratamento de videos usados dentro de funis ja existentes;
- provedores e tratamentos operacionais atuais.

O novo estudio nao substitui esses fluxos. Ele cria uma frente gradual para producoes mais complexas, com mais etapas criativas e maior exigencia de acabamento.

## Direcao comercial

O objetivo do estudio e aumentar o valor percebido dos produtos digitais e campanhas do Marketing Hub, permitindo criar pecas audiovisuais mais sedutoras, narrativas e memoraveis.

Cada evolucao do estudio deve preservar a regra principal do sistema: gerar vendas de produtos digitais que entregam valor real com IA aplicada ao dia a dia.

## Brief Cinematico de Video Comercial PDE

Todo video comercial de PDE deve nascer de um **Brief Cinematico de Video Comercial PDE** antes da geracao de roteiro, storyboard, imagem-base, renderizacao ou publicacao.

Esse brief deve ficar associado ao perfil/playbook comercial do video e conter, no minimo:

- papel no funil: anuncio, landing, amostra, prova, onboarding, retencao ou outro papel comercial explicito;
- promessa que o video precisa tangibilizar;
- dor que precisa aparecer visualmente;
- cena principal;
- sujeito, personagem, produto ou interface principal;
- movimento do sujeito, da interface ou do ambiente;
- camera e enquadramento;
- luz e estetica;
- emocao esperada;
- CTA ou transicao para a proxima etapa;
- restricoes de qualidade, incluindo clareza, audio, aderencia a oferta, uso comercial aprovado e restricoes do provedor;
- prompt cinematografico final quando houver geracao direta por modelo de video.

O brief nao deve ser decorativo. Ele deve deixar claro qual transformacao o consumidor precisa enxergar e como essa cena reduz esforco mental, aumenta desejo, prova o mecanismo ou aproxima o proximo clique.

## Categorias e duracao

O Estudio de Audio e Video deve trabalhar por **categoria de projeto**, para suportar tanto
pecas comerciais curtas quanto producoes longas sem misturar objetivo, roteiro e governanca.

Categorias canonicas:

- **Video Comercial Curto** (`COMMERCIAL_SHORT`): 6 a 60 segundos. Uso: hero de PDE, anuncio,
  Reels, Stories, retargeting, amostra e criativo de aquisicao.
- **Video Longo / VSL** (`LONG_FORM`): 180 segundos ou mais. Uso: pagina de venda, paywall,
  explicacao de oferta, quebra de objecoes e conteudo comercial profundo.
- **Video Institucional / Conteudo** (`INSTITUTIONAL_CONTENT`): duracao definida pelo contexto,
  desde que tenha objetivo comercial explicito, brief, roteiro, uso e criterio de aprovacao.

O Marketing Hub deve bloquear criacao, edicao ou geracao de projeto quando a duracao nao for
compativel com a categoria informada. A tela pode orientar o operador, mas o backend deve ser a
fonte de verdade desse bloqueio para evitar criacao por API, automacao ou dados legados.

Para a MUSA v7, a categoria correta do video hero cinematografico e **Video Comercial Curto**,
com duracao alvo de aproximadamente 30 segundos.

## Separacao backend e providers de video

O backend principal do Marketing Hub deve ser responsavel por:

- cadastro do projeto, brief, blueprint, cenas, biblias visuais e regras de continuidade;
- persistencia de jobs, eventos, status, custos, evidencias, assets, HLS e metricas;
- endpoint de pendencias para o executor;
- callbacks de progresso, sucesso, falha, expiracao e registro de artefatos;
- relatorio operacional e comercial para a tela.

O backend principal **nao deve** implementar integracao direta com providers de video, clientes
HTTP/SDKs de renderizacao, adaptadores Luma, Kling, HeyGen, Runway, Veo ou qualquer executor de
provider. Essas responsabilidades pertencem ao modulo executor de video, atualmente tratado como
`video-management-service`, que deve consumir pendencias oficiais do backend e reportar resultados
pelos callbacks canonicos.

Essa separacao evita que o Estudio vire um conjunto caotico de chamadas externas dentro do backend:
o Hub opera como cockpit no-code e fonte de verdade; o modulo de integracao executa a producao.

## Biblia visual obrigatoria

Antes de qualquer renderizacao, producao, revisao ou aprovacao de video no Estudio, o projeto deve ter uma **biblia visual** persistida no backend.

A biblia visual deve conter, no minimo:

- personagens e imagens mestre aprovadas, com angulos, figurino, acessorios e URLs ou IDs de referencia;
- ambientes e imagens mestre, incluindo plano geral, angulo oposto, lateral, entradas, saidas, objetos fixos e mapa simples;
- objetos, produto e marca, incluindo telas do PDE, interface, logotipo, textos e referencias separadas para composicao;
- direcao visual, incluindo estilo, luz, lente, textura, paleta, enquadramento e nivel de realismo;
- plano de geracao de imagem, priorizando solicitar primeiro ao modelo de imagem OpenAI as imagens mestre e frames-chave antes de gerar video;
- regras de continuidade para preservar rosto, cabelo, figurino, escala, objetos, arquitetura, temperatura de cor e identidade visual entre cenas.

O backend deve bloquear status de construcao ou liberacao de video quando qualquer bloco estiver ausente. A tela deve coletar esses dados como etapa de pre-producao premium, mas nao deve ser a unica barreira de qualidade.

## Primeiro escopo

A primeira versao deve ser um cockpit de construcao no frontend, deixando clara a fronteira entre:

- videos rapidos e tratamentos existentes;
- producoes sofisticadas do Estudio de Audio e Video.

As proximas evolucoes devem ser incrementais e orientadas por etapas auditaveis: briefing, roteiro, cenas, referencias visuais, voz, trilha, montagem, revisao e publicacao.

## Videos de referencia para analise

O subitem **Videos para analise** do Estudio de Audio e Video deve ser uma fila de envio de
videos externos/de sucesso escolhidos pelo usuario para aprendizado do sistema, e nao uma
listagem passiva de videos ja gerados pelos experimentos.

Cada video de referencia deve registrar, no minimo:

- URL publica ou origem acessivel do video;
- titulo operacional;
- plataforma ou fonte;
- nicho, produto ou contexto comercial;
- papel no funil quando conhecido;
- objetivo de aprendizado, como gancho, ritmo, prova, objecao, CTA, edicao, promessa ou retencao;
- evidencia de sucesso percebida ou medida, como views, comentarios, criativo vencedor, vendas,
  retencao, compartilhamentos ou observacao comercial do operador;
- status da analise, preservando fila, analise em andamento, analisado ou rejeitado.

Essa fila deve alimentar aprendizados reutilizaveis para novos roteiros, criativos, ofertas,
storyboards, cortes e criterios de revisao comercial. Videos de referencia nao devem ser
misturados com assets produzidos em experimentos, porque a finalidade e diferente: referencia
externa para aprendizado versus ativo interno de campanha.

## Etapas premium de producao com IA

A tela do Estudio de Audio e Video deve organizar projetos premium pelas seguintes etapas operacionais:

1. **Estrategia / oferta e funil:** objetivo comercial, publico, dor, promessa, mecanismo, canal, duracao e metrica primaria.
2. **Roteiro / narrativa:** gancho, historia, demonstracao, prova, objecoes e CTA falado ou visual.
3. **Biblia visual / pre-producao:** personagem, ambiente, objetos, marca, direcao visual, imagens mestre e regras de continuidade.
4. **Storyboard / plano de cenas:** cenas curtas com enquadramento, movimento, acao, emocao e transicao.
5. **Audio / voz e trilha:** narracao, ritmo, pausas, trilha, efeitos e legibilidade para consumo sem som.
6. **Geracao IA / provider:** escolha de Luma, Kling, HeyGen ou outro motor conforme tipo de cena, custo, duracao e consistencia.
7. **Montagem / pos-producao:** corte de falhas, ritmo, uniao de cenas, legenda, audio, capa, HLS e fallback MP4.
8. **Revisao / gate comercial:** promessa permitida, clareza, continuidade, audio, prova, CTA, HLS e aderencia ao PDE.
9. **Aprendizado / metricas:** play, retencao, clique, diagnostico, paywall, checkout e compra para decidir novos cortes.

Essa ordem evita desperdicio de geracao: nenhum video premium deve avancar para renderizacao antes de existir clareza comercial, roteiro estruturado e referencias visuais suficientes para preservar consistencia e valor percebido.

## HLS para PDEs

Quando um projeto do Estudio de Audio e Video gerar material para PDE, a entrega publicavel deve incluir HLS (`.m3u8`) gerenciado pelo Marketing Hub. O arquivo MP4 pode ser preservado como master/origem, mas a URL usada pelo PDE deve ser a playlist HLS registrada no ativo comercial ou no job correspondente.

A tela do Marketing Hub deve permitir identificar se o video possui HLS pronto para PDE. Video sem HLS nao deve ser tratado como pronto para publicacao em PDE, mesmo que possua MP4 aprovado.

Quando existir uma playlist HLS ja publicada fora do fluxo completo de render, ela deve ser cadastrada ou corrigida no ativo comercial do Marketing Hub antes de ser usada por PDE. O operador deve conseguir salvar a URL `.m3u8` no Hub, associada ao experimento/projeto/job, para preservar governanca de custo, revisao, aprovacao e rastreabilidade.

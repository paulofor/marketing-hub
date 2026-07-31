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

## Duracao minima obrigatoria

O Estudio de Audio e Video deve ser usado somente para videos com **3 minutos ou mais** (`180` segundos ou mais).

Videos abaixo de 3 minutos pertencem aos fluxos rapidos de criativo, anuncio, corte curto, video organico curto ou assets especificos de experimentos. Esses fluxos podem continuar usando provedores e tratamentos existentes, mas nao devem acionar o Estudio.

O Marketing Hub deve bloquear criacao, edicao ou geracao de projeto do Estudio quando `targetDurationSeconds` for menor que `180`. A tela pode orientar o operador, mas o backend deve ser a fonte de verdade desse bloqueio para evitar criacao por API, automacao ou dados legados.

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

## HLS para PDEs

Quando um projeto do Estudio de Audio e Video gerar material para PDE, a entrega publicavel deve incluir HLS (`.m3u8`) gerenciado pelo Marketing Hub. O arquivo MP4 pode ser preservado como master/origem, mas a URL usada pelo PDE deve ser a playlist HLS registrada no ativo comercial ou no job correspondente.

A tela do Marketing Hub deve permitir identificar se o video possui HLS pronto para PDE. Video sem HLS nao deve ser tratado como pronto para publicacao em PDE, mesmo que possua MP4 aprovado.

Quando existir uma playlist HLS ja publicada fora do fluxo completo de render, ela deve ser cadastrada ou corrigida no ativo comercial do Marketing Hub antes de ser usada por PDE. O operador deve conseguir salvar a URL `.m3u8` no Hub, associada ao experimento/projeto/job, para preservar governanca de custo, revisao, aprovacao e rastreabilidade.

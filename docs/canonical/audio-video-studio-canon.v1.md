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

## Primeiro escopo

A primeira versao deve ser um cockpit de construcao no frontend, deixando clara a fronteira entre:

- videos rapidos e tratamentos existentes;
- producoes sofisticadas do Estudio de Audio e Video.

As proximas evolucoes devem ser incrementais e orientadas por etapas auditaveis: briefing, roteiro, cenas, referencias visuais, voz, trilha, montagem, revisao e publicacao.

## HLS para PDEs

Quando um projeto do Estudio de Audio e Video gerar material para PDE, a entrega publicavel deve incluir HLS (`.m3u8`) gerenciado pelo Marketing Hub. O arquivo MP4 pode ser preservado como master/origem, mas a URL usada pelo PDE deve ser a playlist HLS registrada no ativo comercial ou no job correspondente.

A tela do Marketing Hub deve permitir identificar se o video possui HLS pronto para PDE. Video sem HLS nao deve ser tratado como pronto para publicacao em PDE, mesmo que possua MP4 aprovado.

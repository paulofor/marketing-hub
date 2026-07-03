# Processo simples — dossiês da Biblioteca de Páginas de Vendas

Data: 2026-07-03

## Objetivo

A Biblioteca de Páginas de Vendas passa a ter dois dossiês separados, com objetivos diferentes e pipelines independentes:

1. `salespagepatterns.v1` — **Padrões de Página de Venda**
   - foco: design, visual, estrutura da página, copy, oferta, prova, garantia e CTA;
   - uso: melhorar páginas próprias, GeraSalesPage, GeraLanding, wireframes, prompts de imagem e presets de design.

2. `warmupecosystem.v1` — **Aquecimento e Ecossistema**
   - foco: redes sociais, vídeos, reviews, afiliados, comunidades, páginas auxiliares, canais do produtor e outros sinais externos que aquecem leads;
   - uso: entender se produtos vencedores dependem de pré-venda, autoridade externa ou distribuição além da página.

Os dois pipelines usam produtos quentes como fonte inicial, começando por Hotmart com temperatura `>= 80`, mas não compartilham estado operacional. Um dossiê concluído não deve bloquear o outro.

## Pipeline `salespagepatterns.v1`

Este pipeline deve responder:

- quais padrões de design aparecem em páginas de produtos quentes;
- quais padrões de hierarquia visual e densidade ajudam a venda;
- quais fórmulas de copy são recorrentes;
- quais blocos de prova, garantia, oferta e CTA aparecem;
- quais padrões podem virar insumo abstrato para páginas próprias sem copiar conteúdo, marca ou identidade visual.

Etapas planejadas:

1. `intake`;
2. `page-pattern-extraction`;
3. `pattern-synthesis`.

## Pipeline `warmupecosystem.v1`

Este pipeline substitui o nome genérico anterior `dossieproduto.v1` para o objetivo de aquecimento.

O dossiê deve responder:

- quais termos devem ser pesquisados para entender a presença pública do produto;
- quais resultados externos parecem relevantes;
- quais recursos, além da própria página de venda, estão aquecendo o público;
- qual conclusão final deve ficar disponível na tela do item da biblioteca.

## Etapas do processo de aquecimento

### Etapa 1 — Extrair termos de pesquisa

O worker acessa no backend o texto completo da página de venda.

Depois, envia esse texto para a OpenAI com a orientação de extrair termos de pesquisa capazes de investigar o prestígio daquela página, produto, produtor, promessa, marca e sinais públicos relacionados.

### Etapa 2 — Armazenar termos no backend

Ao receber a resposta da OpenAI, o worker envia os termos de pesquisa para o backend.

O backend armazena esses termos no banco de dados vinculados ao item da Biblioteca de Páginas de Vendas.

### Etapa 3 — Executar pesquisas sugeridas

O worker obtém do backend os termos de pesquisa armazenados.

Com esses termos, executa as pesquisas sugeridas e envia os resultados encontrados novamente para o backend.

O backend persiste os resultados vinculados ao item da biblioteca e aos termos que originaram cada pesquisa.

### Etapa 4 — Analisar relação com o produto

O worker obtém do backend os resultados de pesquisa persistidos.

Depois, envia esses resultados para a OpenAI analisar:

- o que tem relação real com a página e o produto;
- quais evidências reforçam prestígio, prova social, autoridade ou distribuição;
- quais outros recursos além da página de venda aquecem o público desse produto, como vídeos, redes sociais, anúncios, reviews, afiliados, comunidades, matérias, páginas auxiliares ou canais do produtor.

### Etapa 5 — Disponibilizar dossiê final

O worker envia as conclusões finais para o backend.

O backend grava o dossiê final no banco de dados, vinculado ao item da Biblioteca de Páginas de Vendas.

Esse dossiê passa a ficar disponível na tela do item da biblioteca para apoiar decisões comerciais e reutilização de padrões vencedores.

## Regras simples

- O worker executa OpenAI e pesquisas externas; o backend controla estado, persistência e exposição para a tela.
- O worker não acessa o banco diretamente.
- Prompts e schemas dos pipelines de dossiê ficam no banco do backend em `ai_prompt_schema_template`.
- O worker recebe prompt/schema pelo endpoint `pending`; não deve carregar prompt/schema local do próprio módulo.
- Cada dossiê gerado deve ficar vinculado ao `prompt_template_key`, `prompt_template_version` e `schema_name` usados na execução.
- Cada etapa deve deixar evidência persistida suficiente para auditoria e exibição ao usuário.
- A tela deve mostrar o dossiê final vindo do backend, sem recomputar conclusões no frontend.
- Chamadas OpenAI devem usar três tentativas: tentativa 1 Flex, tentativa 2 Flex e tentativa 3 Standard/default.
- Todo custo retornado por OpenAI deve ser persistido no registro da etapa, somado ao custo individual da página e somado ao custo total da biblioteca.

# Processo simples — geração do dossiê da Biblioteca de Páginas de Vendas

Data: 2026-06-25

## Objetivo

Gerar um dossiê simples para entender o prestígio e o aquecimento público de um produto a partir da página de venda já capturada na biblioteca.

O dossiê deve responder:

- quais termos devem ser pesquisados para entender a presença pública do produto;
- quais resultados externos parecem relevantes;
- quais recursos, além da própria página de venda, estão aquecendo o público;
- qual conclusão final deve ficar disponível na tela do item da biblioteca.

## Etapas do processo

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
- Cada etapa deve deixar evidência persistida suficiente para auditoria e exibição ao usuário.
- A tela deve mostrar o dossiê final vindo do backend, sem recomputar conclusões no frontend.

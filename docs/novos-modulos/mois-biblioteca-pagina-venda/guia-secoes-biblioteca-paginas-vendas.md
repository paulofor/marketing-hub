# Guia das seções — Biblioteca de Páginas de Vendas (MOIS)

Data: 2026-05-18

## Contexto
A tela **Biblioteca de Páginas de Vendas** (rota `/mois/sales-pages-library`) foi criada para acompanhamento operacional do pipeline de biblioteca de sales pages no MOIS, cobrindo ingestão, processamento e diagnóstico de análise.

## Acesso
- Menu lateral: **Biblioteca Sales Pages**.
- Rota: `/mois/sales-pages-library`.

## Estrutura da página

### 1) Cabeçalho
- Título da página: **Biblioteca de Páginas de Vendas**.
- Subtítulo: acompanhamento de ingestão, fila de análise e resultado por página.
- Ação: botão **Voltar ao workspace** (retorna para `/mois`).

### 2) Entradas ingeridas
Objetivo: mostrar tudo que já entrou na biblioteca.

Colunas exibidas:
- URL canônica
- Origem
- Título
- Ingestões
- Última captura

Comportamentos:
- estado de carregamento;
- estado de erro;
- estado vazio (sem entradas);
- paginação simples (anterior/próxima + total).

### 3) Fila de jobs de análise
Objetivo: monitorar o processamento assíncrono das páginas.

Recursos:
- filtro por status: `PENDING`, `FETCHING`, `DONE`, `FAILED`.

Colunas exibidas:
- Job
- Status
- Tentativas
- Atualizado em
- Erro

Comportamentos:
- estado de carregamento;
- estado de erro;
- estado vazio;
- paginação simples.

### 4) Páginas e status da análise
Objetivo: listar páginas da biblioteca e o resultado agregado da análise.

Colunas exibidas:
- Página
- URL canônica
- Status
- Score
- Ações

Ações da linha:
- **Ver análise**: seleciona a página para abrir o detalhe abaixo.
- **Reanalisar**: dispara novo processamento para a página.

UX assíncrona:
- durante a reanálise, o botão fica desabilitado e exibe spinner.

### 5) Detalhes da análise da página
Objetivo: detalhar a análise da página selecionada.

Exibe:
- badges de contexto (página, job, status, score);
- notas da análise;
- payloads de análise em JSON:
  - Seções;
  - Copy;
  - Visual;
  - Imagem.

Estados:
- sem página selecionada;
- carregando análise;
- análise indisponível;
- análise disponível.

## Resultado prático para operação
A página permite:
1. validar a qualidade da ingestão;
2. identificar gargalos e falhas de job;
3. acompanhar score/status por página;
4. reprocessar rapidamente itens com problema;
5. inspecionar os artefatos JSON gerados na análise.

## Tela de pipeline da biblioteca

### 1) Primeira etapa — Obtenção dos HTML
Objetivo: orientar a operação sobre a primeira camada necessária para qualquer análise confiável da biblioteca: capturar o HTML bruto das URLs ingeridas.

Informações exibidas no card:
- entrada esperada: URLs normalizadas da biblioteca;
- saída esperada: HTML bruto versionado em snapshot;
- critério de qualidade: conteúdo útil para análise, sem marcador técnico interno;
- itens de acompanhamento: URLs pendentes, snapshots salvos, falhas de acesso, hash, tamanho e data da última captura.

Resultado prático: a equipe consegue enxergar que a análise de copy, estrutura, prova, oferta e visual depende primeiro da existência de snapshots brutos rastreáveis.

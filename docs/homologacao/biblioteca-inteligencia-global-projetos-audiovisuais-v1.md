# Matriz de homologacao — Biblioteca de Inteligencia global v1

## Objetivo

Comprovar que a Biblioteca de Inteligencia pertence ao Marketing Hub e atende projetos
audiovisuais existentes e futuros, mantendo um catalogo global unico e selecoes contextuais
pequenas nos detalhes e jobs. Nenhum artigo pode ser tratado como venda, prova ou autorizacao de
gasto/publicacao.

## Matriz ponta a ponta

| Area | Cenario | Evidencia esperada |
| --- | --- | --- |
| Caminho feliz | Abrir a biblioteca sem ID de projeto | Catalogo, contagens, politicas e cartoes retornam por `/api/research-intelligence/v1/catalog`. |
| Projeto existente | Abrir detalhe de projeto antigo | Quatro rotas contextuais aparecem sem migracao ou copia do catalogo. |
| Projeto futuro | Criar projeto sem IDs do Vega | Backend devolve selecao com fingerprint propria e os jobs recebem a rota do agente. |
| Contexto | Comparar dois briefings diferentes | Fingerprints diferentes; limite de quatro cartoes por agente permanece. |
| Validade | Consultar fonte vencida | Fonte continua auditavel no catalogo, mas nao aparece como elegivel nem entra na selecao. |
| Validacao | Agente cita cartao inexistente ou omite colecao recebida | Gate bloqueia antes do provider pago, preservando auditoria. |
| Falha de integracao | Endpoint global indisponivel | Tela informa falha; nenhum cartao ausente e apresentado como pesquisa valida. |
| Observabilidade | Executar job audiovisual | Metadados persistem versao, IDs, caminhos e hashes da selecao usada. |
| Desempenho | Abrir Estudio ou lista de projetos | Catalogo global nao e requisitado; somente sua pagina o carrega, e a tela exibe 12 cartoes inicialmente. |
| Metricas | Avaliar resultado do ativo | Retrabalho, retencao, CTA, checkout, pagamento e custo continuam separados da evidencia editorial. |
| Segregacao | Executar testes locais | Fixtures e IDs de teste nao alteram produto, experimento, campanha, gasto ou evento comercial real. |
| Desktop | Chromium 1440x900 | Navegacao, filtros, expansao e carregamento incremental sem overflow ou erro de console. |
| Mobile | iPhone 15 Pro e Pixel 7 | Cards, filtros e politicas em uma coluna, com interacoes touch utilizaveis. |

## Criterios de decisao

- Continuar: todo projeto elegivel recebe selecao auditavel e o catalogo global permanece
  consultavel sem duplicacao.
- Ajustar: rota sem cobertura, fonte ativa ausente, latencia perceptivel ou layout com overflow.
- Parar: qualquer dependencia de ID do Vega, artigo vencido entregue ao agente, catalogo integral
  injetado no prompt ou perda da linhagem do job.

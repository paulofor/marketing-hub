# Homologação — Vega ciclo 2 / ativação comercial v12

Data: 2026-09-15

## Objetivo

Fazer o ciclo 2 e o experimento 92 chegarem à decisão comercial por um percurso executável na
tela, preservando a versão `musa-pde-entry-v12-primeiro-ajuste-aplicavel`, os vídeos aprovados,
o teto do ciclo e a segregação das métricas. Nenhuma chamada à Meta ou cobrança real integra a
homologação técnica.

## Causa-raiz confirmada antes da alteração

- A tentativa real pelo formulário de autorização retornou HTTP 409 porque o ciclo possui teto de
  R$ 100,00 e o experimento 92 não possui teto operacional.
- O experimento também não possui slot próprio, criativo publicável nem público selecionado; a
  tela do ciclo, porém, anuncia o comando de autorização como disponível.
- A oferta pública do MUSA escolhe hoje o slot alterado mais recentemente, sem seletor de versão.
  O slot v5 passou a ser escolhido para o slug de Vega e o endpoint público retornou 412 por
  misturar produto e experimento. Assim, copiar apenas o orçamento não criaria uma jornada
  vendável.
- A integração concluída durante o ciclo é privada e declara explicitamente que publicação e gasto
  não foram autorizados. Ela não pode ser reutilizada como evidência comercial.

## Alternativas comparadas

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Corrigir somente o 409 | Mudança pequena | Autoriza um experimento sem destino, criativo ou medição; não gera uma venda segura | Rejeitada |
| Reutilizar integralmente a v7 e os ativos do experimento 91 | Mais rápida | Mistura versões e hipóteses e contamina a leitura causal do ciclo 2 | Rejeitada |
| Criar slot v8 próprio para a v12 sobre o motor comercial comprovado | Preserva checkout, preço, entrega e baixo custo, mas testa a nova promessa e os vídeos no experimento 92 | Esforço intermediário de versionamento, integração e homologação | Escolhida |

## Matriz ponta a ponta

| Área | Cenário | Resultado obrigatório |
| --- | --- | --- |
| Caminho feliz | Host v8 carrega o contrato v12, vídeo aprovado, primeiro ajuste e oferta de R$ 67 | Identidade de slot/versão/experimento 92 íntegra; CTA leva ao checkout canônico |
| Validações | Oferta e contrato de jornada são consultados pelo slot do host | Nenhuma versão seleciona o slot apenas por data de atualização |
| Falhas | Slot inexistente, versão divergente, contrato privado ou correlação com token bruto | Falha fechada e causa clara, sem marcar prontidão |
| Integração BPM | Integração privada já concluída e posterior integração comercial | A evidência privada não satisfaz o gate comercial; a comercial valida o slot exato |
| Prontidão Meta | Jornada integrada sem criativo versus jornada com criativo aprovado | Facebook continua exigindo criativo publicável próprio do experimento 92 |
| Financeiro | Ciclo R$ 100 versus plano legado R$ 200 e experimento sem teto | Um único aceite persiste teto exato, período restante e valor diário seguro; falha reverte tudo |
| Continuidade BPM | Aceite do teto muda o ciclo para publicação | A próxima atividade continua no Processo 5 canônico, sem desviar para o detalhe genérico do experimento |
| Observabilidade | Eventos de visita, valor, CTA, checkout, compra, acesso, primeira utilização e reembolso | Versão/experimento/sessão correlacionados; QA segregado; nenhum token bruto persistido |
| Métricas | Sessões e eventos de homologação | `mh_test`/`INTERNAL_QA`; não contam como tráfego humano, venda ou receita |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados | Copy, vídeo, questionário, CTA, políticas e retomada utilizáveis |
| Mídia | HLS e MP4 aprovados | HTTP 200, reprodução, áudio/legendas e fallback sem erro |
| Segurança comercial | Rodada local e produção antes de autorização | Zero cobrança, zero liberação Meta, zero gasto e nenhum estado RUNNING inferido |
| Liberação Meta | Aceite final de experimento Facebook sem campanha registrada | Pedido entra na fila do worker ainda em PLANNED; somente a campanha confirmada muda para RUNNING |
| Regressão | Hosts v5, v6 e v7 | Cada host mantém seu slot e contrato histórico próprios |

## Regra das rodadas

A primeira rodada é diagnóstica. Como a reprodução já revelou defeitos, após a última correção serão
executadas duas rodadas completas e consecutivas sem falha. Qualquer novo defeito reinicia a
contagem. A aplicação remota somente poderá usar imagens construídas pelos arquivos versionados e
depois de concluída a validação local.

## Limite de decisão humana

A correção pode preparar slot, criativo, segmentação, orçamento e preflight. O operador não repete
o teto na configuração do experimento: a confirmação do ciclo aplica os limites de forma atômica.
A liberação final para o Facebook Ads Worker continua sendo uma decisão humana; ela não antecipa
`RUNNING` antes da campanha existir. A homologação não cria campanha, cobrança ou gasto externo
para descobrir defeitos.

## Resultado da homologação local

- Duas rodadas completas e consecutivas foram aprovadas após a última correção. Cada rodada
  executou 586 testes Java da cadeia de aprendizado, sem falhas ou erros, e validou os 20 cenários
  REST sobre MySQL 5.7 sem chamada externa.
- Em cada rodada, os percursos da cadeia foram exercitados em Chromium desktop, iPhone 15 Pro e
  Pixel 7 emulados, incluindo a atividade 6.4, autorização atômica, publicação simulada, medição,
  venda sem entrega, entrega comprovada e decisão do ciclo.
- A integração isolada do PDE executou 30 testes Playwright por rodada nos mesmos três perfis. O
  backend do PDE aprovou 179 testes unitários, e o frontend aprovou testes, verificação TypeScript
  e build de produção.
- A suíte integral do backend aprovou 3.045 testes, sem falhas ou erros. A validação incluiu
  migração, rollback, reaplicação e idempotência no MySQL 5.7, contratos do workflow e limpeza da
  topologia Docker temporária.
- Nenhuma campanha, cobrança, venda ou métrica produtiva foi criada durante a homologação.

## Estado esperado após integração e publicação pelo fluxo oficial

O ciclo 2 permanece em autorização até a versão comercial isolada ser publicada e os vínculos do
experimento 92 serem materializados pela tela. A confirmação do teto de mídia aplica orçamento e
janela de forma atômica; a liberação coloca o trabalho na fila do Facebook Ads Worker em estado
`PLANNED`, e somente a confirmação da campanha pode torná-lo `RUNNING`. A atividade 6.4 continua
aberta durante tráfego, vendas, entregas e coleta de evidências, sendo concluída apenas pela decisão
baseada nos resultados reais do ciclo.

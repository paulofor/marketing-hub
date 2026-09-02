# Matriz de homologação — sucessor Facebook do Vega com teto de mídia v1

Data: 2026-09-01

## Objetivo comercial

Criar, pelo Marketing Hub, um novo experimento Facebook para o Vega sem alterar o histórico do experimento direto #90. O sucessor deve nascer `PLANNED`, com orçamento diário de R$ 20,00, teto total de mídia de R$ 100,00 e duração máxima coerente com esse teto. A publicação continua sujeita aos gates de criativo, público, destino, checkout e autorização explícita.

## Métrica e critérios

- Métrica esperada: um sucessor Facebook vinculado ao mesmo produto, hipótese e plano comercial, sem métricas, custos, campanhas ou aprovações herdadas.
- Continuar: sucessor criado com contrato íntegro; campanha liberada somente após todos os gates; gasto sincronizado sempre abaixo ou igual ao teto, salvo atraso inevitável da Meta entre leituras.
- Ajustar: tela ou backend aceitam combinação de datas e orçamento que ultrapassa o teto, ou o worker não solicita pausa no primeiro sync que alcança o limite.
- Parar: qualquer publicação sem autorização, vínculo com conta/página incompatível, perda do checkout/destino aprovado, mistura de métricas do #90 ou gasto sem teto persistido.

## Cenários ponta a ponta

| Área | Cenário | Resultado esperado |
| --- | --- | --- |
| Caminho feliz | Abrir #90, escolher sucessor Facebook, informar R$ 20/dia, R$ 100 total, 5 dias, página e Instagram compatíveis | Backend cria novo identificador sequencial, plataforma `FACEBOOK`, status `PLANNED` e preserva produto, hipótese, proposta, preço, destino e checkout |
| Segregação | Comparar #90 e sucessor | #90 permanece `RUNNING` e `DIRECT_ONE_TO_ONE`; sucessor começa sem eventos, métricas, campanha, custo, liberação, criativos aprovados ou seleção operacional herdada |
| Plano comercial | Criar sucessor a partir de experimento associado ao plano | Novo experimento entra no portfólio do mesmo plano, sem ampliar o teto do plano |
| Validação | Teto ausente, zero ou negativo | Requisição bloqueada com mensagem de negócio |
| Validação | Orçamento diário ausente, zero, negativo ou maior que o teto | Requisição bloqueada |
| Validação | Período cujo orçamento máximo (`dias inclusivos × orçamento diário`) supera o teto | Requisição bloqueada |
| Validação | Página não está conectada ao ator Instagram selecionado | Worker comprova a conexão na Meta antes de criar o criativo e bloqueia a publicação sem criar campanha utilizável |
| Segurança | Tentar liberar experimento Facebook sem teto total | Readiness permanece bloqueado e o worker não recebe a publicação |
| Integração | Publicador recebe sucessor pronto | Orçamento diário continua no ad set; campanha recebe `spend_cap=10000` (centavos), sem orçamento diário ou vitalício concorrente |
| Métricas | Gasto sincronizado abaixo de R$ 100 | Campanha continua sujeita aos demais gates de parada |
| Métricas | Gasto sincronizado alcança R$ 100 | Worker solicita pausa diretamente na Meta e backend invalida o experimento, registra motivo auditável e solicita parada das campanhas vinculadas |
| Falha de backend | Callback de métricas falha após leitura do teto | Proteção local do worker ainda solicita pausa na Meta e registra erro completo |
| Observabilidade | Criar sucessor e atingir teto | Logs e dados persistidos identificam experimento, campanha, gasto observado, teto e motivo da parada |
| Concorrência | Dois cliques rápidos ou duas requisições paralelas | Botão fica desabilitado durante a chamada e a restrição única `(origem, canal)` impede dois sucessores |
| Desktop | Chromium desktop | Formulário, validações, confirmação e navegação funcionam sem overflow |
| Mobile | Chromium emulado como iPhone 15 Pro | Campos, selects e ação permanecem legíveis e acionáveis por toque |
| Mobile | Chromium emulado como Pixel 7 | Campos, selects e ação permanecem legíveis e acionáveis por toque |

## Dados de teste

- Usar registros locais dedicados e nomes de experimento não produtivos.
- Não chamar a Graph API real nem criar campanha externa durante a homologação; usar servidor HTTP de teste para Meta e backend.
- Não registrar visitas, leads, checkouts, pagamentos ou vendas como consequência da homologação.
- Não usar token Meta real em logs, fixtures ou screenshots.

## Evidências mínimas

- Testes unitários do backend para criação, validação, segregação e vínculo ao plano.
- Testes de readiness e parada automática por teto.
- Testes do Facebook Ads Worker para pausa direta na Meta e payload do callback.
- Teste do publicador para `spend_cap` nativo de R$ 100,00 e ausência de orçamento diário/vitalício na campanha.
- Testes do frontend para estado de carregamento, validação e navegação.
- Liquibase validado em MySQL 5.7, incluindo reaplicação idempotente.
- Build das imagens alteradas e jornada visual nos três perfis de navegador.

## Extensão da matriz — ativação e publicação do sucessor

Data: 2026-09-02

| Área | Cenário | Resultado esperado |
| --- | --- | --- |
| Conteúdo | Solicitar ângulo, copy e briefing visual do sucessor | As três etapas avançam em ordem, persistem request/response/custo e não ficam sem início por disputa com integrações lentas |
| Isolamento | Saturar o agendador compartilhado do AI Worker | O pipeline comercial continua sendo iniciado pelo agendador exclusivo, sem duplicar job |
| Empacotamento | Construir a imagem do AI Worker após a suíte | O workflow preserva o JAR aprovado e a imagem apenas o incorpora, sem recompilar o backend inteiro nem depender de artefato anterior |
| Prova do produto | Plano possui `PRODUCT_PROOF` ou `DELIVERY` em imagem e estado `APPROVED` | Tela exibe a prova, backend libera a materialização e o worker recebe somente as referências do mesmo plano |
| Falha segura | Plano possui apenas `ADS`, `SOCIAL`, vídeo, rascunho ou URL vazia | Botão e backend bloqueiam antes de consumir geração |
| Compatibilidade | Experimento legado possui imagem concluída do GeraLanding | Referência continua aceita sem regressão |
| Superfície PDE | Sucessor aponta para o mesmo produto, URL MUSA versionada e checkout do experimento direto homologado | Apenas landing/destino podem reutilizar a evidência do antecessor |
| Segregação | Sucessor muda produto, URL ou checkout | Reuso é recusado; criativo, público, campanha, gasto e métricas jamais são herdados |
| Criativo | Materializar as três variações | Criativos nascem `DRAFT`, vinculados ao #91 e usam a prova aprovada, sem criar venda ou aprovação |
| Revisão | Enviar criativos a Têmis | Parecer multimodal independente decide `APPROVED`, `ADJUST` ou `REJECTED`; somente `READY` entra na fila Meta |
| Publicação | Liberar #91 após todos os gates | Facebook Ads Worker cria uma única campanha, ad set e anúncio com R$ 20/dia e `spend_cap=10000` |
| Observabilidade | Consultar Hub, logs e Meta após publicação | IDs externos, passos, payloads, respostas, status e teto ficam correlacionados ao #91 |
| Métricas | Campanha recém-publicada sem eventos humanos | visitas, CTA, checkout, pagamento e receita permanecem zero; publicação não conta como venda |
| Dispositivos | Abrir experimento em desktop, iPhone 15 Pro e Pixel 7 | Prova, geração, revisão, gates e liberação permanecem legíveis e acionáveis |

Se qualquer rodada revelar defeito, a contagem reinicia e duas rodadas locais completas e
consecutivas devem passar depois da última correção.

## Resultado da homologação local

Em 2026-09-01, duas rodadas completas e consecutivas terminaram sem falhas funcionais:

- 2.209 testes do backend, 116 do Facebook Ads Worker e 457 do frontend por rodada;
- contrato HTTP validado do `GET` de prontidão ao `POST` de criação, preservando o #90 e devolvendo o sucessor existente em nova consulta;
- Spotless, TypeScript e build de produção aprovados;
- imagens do backend, frontend e Facebook Ads Worker construídas pelo fluxo versionado;
- cinco changesets aplicados e reaplicados em MySQL 5.7, com tipos, chave estrangeira e índice único verificados fisicamente;
- tentativa de segundo sucessor `(origem, FACEBOOK)` recusada especificamente pelo índice único;
- criação simulada pela tela em Chromium desktop, iPhone 15 Pro e Pixel 7, sem overflow e sem carregar identidades Meta antes da abertura do formulário;
- payload confirmado com R$ 20,00/dia, R$ 100,00 de teto, cinco dias, página e Instagram selecionados;
- Graph API substituída por test double: nenhuma campanha, visita, checkout, pagamento, venda ou gasto real foi criado.

Em 2026-09-02, após a preparação operacional do experimento #91 e a correção dos bloqueios de
conteúdo e prova visual, duas novas rodadas completas e consecutivas terminaram sem falhas:

- 2.217 testes do backend, 240 do AI Worker, 116 do Facebook Ads Worker e 459 do frontend por rodada;
- Spotless, Prettier, build de produção, Actionlint, ShellCheck e contratos de imagem aprovados;
- imagens do backend, frontend, AI Worker e Facebook Ads Worker reconstruídas em cada rodada;
- scheduler comercial exclusivo confirmado sem duplicar job e imagem do AI Worker construída a
  partir do JAR aprovado, sem recompilar o backend;
- publicação Meta simulada com campanha, ad set, criativo, anúncio, callback `RUNNING`, orçamento de
  R$ 20,00/dia, `spend_cap=10000` e pausa ao atingir R$ 100,00;
- jornada de geração repetida em Chromium desktop, iPhone 15 Pro e Pixel 7, com prova MUSA visível,
  ação habilitada, solicitação concluída, zero overflow e zero erro no console;
- Meta e backend substituídos por test doubles: nenhum gasto, evento humano, pagamento ou venda foi
  criado pela homologação.

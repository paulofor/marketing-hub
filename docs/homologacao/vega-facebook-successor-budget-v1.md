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

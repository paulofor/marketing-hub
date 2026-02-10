# Facebook Ads Worker

Worker responsável por criar campanhas no Facebook Ads, incluindo os
posicionamentos no Facebook e no Instagram, e coletar métricas usando a API de
Marketing do Facebook. O serviço reutiliza o modelo de dados definido no
projeto `backend`, evitando duplicação de entidades.

Para sugerir interesses relacionados a um seed, o worker consulta a Graph API
via `/search` com `type=adinterestsuggestion` e envia a lista de seeds no
parâmetro `interest_list`, conforme a documentação oficial de Targeting Search.

## Fila de resolução de targeting

O backend grava os candidatos que precisam ser validados na tabela `targeting_resolution_job`.
O worker não depende mais de chamadas HTTP para receber esses itens: a cada ciclo o componente
`TargetingResolutionQueueProcessor` consulta diretamente o MySQL, recupera até `targeting.queue.batch-size`
registros com status `PENDING`, processa cada seed na Graph API e atualiza o status da linha para
`SUCCEEDED` ou `FAILED`. Jobs travados por reinicializações são liberados automaticamente após o TTL
configurado em `targeting.queue.lock-ttl`.

Para que o worker acesse a fila é obrigatório configurar as variáveis abaixo (todas com os mesmos valores
utilizados pelo backend):

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
- Pool JDBC (opcional): `DB_MAX_POOL_SIZE`, `DB_MIN_IDLE`, `DB_CONNECTION_TIMEOUT`,
  `DB_VALIDATION_TIMEOUT`, `DB_IDLE_TIMEOUT`, `DB_MAX_LIFETIME`, `DB_KEEPALIVE_TIME`
- `TARGETING_QUEUE_ENABLED` (default `true`)
- `TARGETING_QUEUE_BATCH_SIZE` (default `5`)
- `TARGETING_QUEUE_POLL_INTERVAL` (ISO8601, default `PT30S`)
- `TARGETING_QUEUE_LOCK_TTL` (default `PT2M`)

Sem essas credenciais o worker não consegue resolver novos candidatos.


### Padrão de banco alinhado com o backend

Para reduzir divergência operacional entre serviços, o `facebook-ads-worker` usa o
mesmo padrão de configuração do `backend/ads-service` para host, usuário e parâmetros
do pool Hikari. Em ambientes containerizados, evite `localhost` para `DB_HOST`, pois
o valor aponta para o próprio container do worker e pode causar `Connection refused`.

## Pixels e eventos

A sincronização de pixels e o envio de eventos foram desativados por padrão
para evitar erros de permissão ao lidar com contas que não liberaram o uso de
pixels. O agendador `FacebookPixelScheduler` só é registrado quando a
propriedade `facebookpixel.enabled` está definida como `true`. Mantendo o valor
padrão (`false`), o worker limita-se a publicar campanha, conjunto e anúncio.

O fluxo automatizado cria toda a hierarquia necessária para veiculação:

1. **Campanha** (`POST /campaigns`) com objetivo `OUTCOME_TRAFFIC` quando o
   destino é um site e `OUTCOME_LEADS` quando o criativo direciona para um
   formulário de leads, sempre com status inicial `PAUSED` e
   `special_ad_categories = []`, conforme documentado na
   [Marketing API](https://developers.facebook.com/docs/marketing-api/reference/ad-campaign-group#Creating) para contas que
   não se enquadram em categorias especiais.
2. **Conjunto de anúncios** (`POST /adsets`) atrelado à campanha, também em
   `PAUSED`, com destino herdado da conta (por exemplo, `WEBSITE`). Antes de
   chamar a Graph API o worker consulta o backend (`GET
   /api/adsets?experimentId={id}`) para montar a segmentação e, quando
   necessário, criar uma Saved Audience reutilizável. A segmentação enviada
   para a Meta é sempre o Brasil inteiro (`geo_locations.countries = ["BR"]`)
   e com Advantage+ Audience habilitado (`targeting_automation.advantage_audience = 1`),
   garantindo o alcance nacional independente do público retornado pelo backend.
   Interesses e custom audiences são normalizados quando presentes no JSON de
   segmentação, e campos auxiliares não reconhecidos pela Graph API são
   removidos (por exemplo, `detailed_targeting_description`) para evitar erros
   `(#100) Invalid parameter`. Valores enviados em `languages` são convertidos
   para `locales` numéricos aceitos pela Meta (por exemplo, `pt_BR` -> `16`) antes do POST e o campo
   original é descartado para manter compatibilidade com a Graph API. Entradas
   de `geo_locations.regions` são aceitas apenas quando o `key` é numérico;
   códigos não numéricos (por exemplo, `"SP"`) são descartados com aviso em log
   para evitar a rejeição `(#100) Invalid parameter` devolvida pela Meta. Chaves
   não textuais em `geo_locations` também são ignoradas antes do envio para a
   Graph API, garantindo que o payload use apenas campos com nomes válidos.
   Quando o criativo aponta para um formulário de leads,
   o worker ajusta automaticamente `destination_type = ON_AD` e força
   `optimization_goal = LEAD_GENERATION` para satisfazer as regras da Graph API
   para Lead Ads. O orçamento diário do conjunto é calculado a partir do campo
   `dailyBudget` do experimento (em reais), convertido para centavos antes do
   POST; somente na ausência desse valor o worker recorre ao
   `adSetDailyBudget` configurado na conta da Meta.
4. **Imagem do criativo**: em vez de enviar `POST /adimages`, o worker
   referencia diretamente a URL pública retornada pelo backend no campo
   `object_story_spec.link_data.picture`. Quando o caminho é relativo (por
   exemplo, `/uploads/arquivo.jpg`), o worker o normaliza para o domínio
   configurado em `backend.base-url` antes de encaminhá-lo ao Facebook. Essa
   abordagem evita depender da biblioteca de imagens da conta e é suportada pela
   Graph API desde que a URL seja acessível pelo crawler da Meta.
5. **Criativo** (`POST /adcreatives`) baseado em um `object_story_spec`
   contendo o `page_id` definido na conta selecionada no backend. Quando a conta
   não possui `defaultPageId`, o worker utiliza a página vinculada ao
   experimento no backend (exposta como `facebookPage`, `associatedFacebookPage`
   ou `facebookPageAssociation`) e ignora o experimento caso nenhuma associação
   exista. A mesma regra vale para a identidade do Instagram: o worker consome o
   campo `instagramAccount` retornado pelo backend ou o `defaultInstagramActorId`
   configurado na conta e popula `instagram_user_id` quando disponível. Caso
   nenhum identificador esteja disponível, o worker registra o aviso e segue
   sem `instagram_user_id`, permitindo veiculação apenas no Facebook.
   Opcionalmente o fluxo inclui mensagem e call-to-action vindos do próprio
   criativo. A imagem é sempre veiculada via `link_data.picture` — não há hash
   salvo na biblioteca —, garantindo que o anúncio utilize exatamente o ativo
   hospedado pelo backend.
6. **Anúncio** (`POST /ads`) que referencia o conjunto e o criativo recém
   criados, mantido pausado até que o time operacional revise os detalhes no
   Gerenciador de Anúncios.

As chamadas ao backend utilizam o prefixo `/api`. O worker consome
`/api/facebook-campaigns/experiments-ready`, tratando respostas `404` como
"nenhum experimento disponível" para evitar falhas no agendamento. Falhas de
conexão ao recuperar os experimentos são registradas em log e ignoradas para
que o agendamento continue saudável. Após criar campanha, conjunto, criativo e
anúncio, o worker envia um `CreateCampaignRequest` para o backend via
`POST /api/facebook-campaigns`, preenchendo os identificadores de cada nível da
hierarquia para manter rastreabilidade completa (`facebook_ads_campaign`,
`facebook_ads_ad_set`, `facebook_ads_ad_creative` e `facebook_ads_ad`). O
`CreateCampaignRequest` também leva `experimentAdSetId` para relacionar o
conjunto criado na Meta ao público configurado no experimento e registrar os
códigos retornados pelo Facebook no banco de dados do backend. O
`targetingJson` fornecido pelo backend é preservado no request para manter a
segmentação aplicada diretamente no conjunto. Assim que o backend confirma o registro, o worker marca o experimento
de origem como `RUNNING` com
`PATCH /api/experiments/{id}/status?status=RUNNING`, evitando que o mesmo
experimento reapareça em consultas futuras a `/facebook-campaigns/experiments-ready`
e preservando o identificador gerado pela Meta para coleta de resultados. Todas as
chamadas HTTP ao backend devem registrar a URL completa, parâmetros, payload e
resposta recebida para acelerar o diagnóstico de incidentes em produção. Os logs
seguem o padrão visual `==>` para requisições (por exemplo, `url==>https://...`)
e `<==` para respostas, inclusive em cenários de erro, permitindo identificar
rapidamente a direção do tráfego durante uma análise.

Quando a jornada do experimento exige um Instant Form aprovado, o worker assume
que o formulário já foi criado manualmente diretamente na Meta. O
`FacebookCampaignService` apenas publica rascunhos existentes (`publishInstantForm`)
e reutiliza o identificador resolvido ao montar criativos com
`call_to_action.value.lead_gen_form_id`, mantendo o destino `ON_AD` e o objetivo
`OUTCOME_LEADS` conforme as regras mais recentes da Graph API. Identificadores
temporários no formato `ai_form_*` são normalizados para o padrão `form_*`
antes da publicação, e o share link é reconstruído com o identificador final
quando disponível.

Todas as chamadas à Graph API são logadas detalhadamente para facilitar
investigações de erros (por exemplo, respostas `400 Bad Request`). Os logs
registram caminho da requisição, payload enviado (com `access_token`
anonimizado), status HTTP retornado, cabeçalhos de resposta (também com
valores sensíveis mascarados), corpo devolvido pelo Facebook e, quando
presente, os campos estruturados de erro (`type`, `code`, `error_subcode`,
`error_user_title`, `error_user_msg`, `fbtrace_id` e `error_data`). Isso
permite cruzar rapidamente o incidente com a documentação oficial.

## Coleta de métricas de campanha

O agendador de métricas consulta o endpoint de Insights (`/{campaignId}/insights`)
com `date_preset = maximum` para obter o período completo disponível, evitando o
erro `(#100) lifetime is not a valid date_preset` quando a Graph API rejeita o
valor `lifetime`. O retorno é consolidado no backend via
`POST /api/facebook-campaigns/{campaignId}/metrics`, mantendo a janela de
datas (`date_start`/`date_stop`) fornecida pela própria Meta.
Quando a Graph API devolve `data=[]` no Insights (campanhas sem entrega ainda),
o worker registra a ausência de dados e envia métricas zeradas ao backend para
atualizar o `metrics_last_synced_at` sem marcar erro.

Para preservar o formato JSON mesmo nos logs, utilize o utilitário
`JsonLogFormatter.wrap(...)` ao registrar payloads, parâmetros e respostas.
Ele serializa as estruturas para JSON antes da interpolação na mensagem,
garantindo que campos de texto apareçam entre aspas (por exemplo,
`{"nome":"Paulo"}`) em vez da forma padrão de `Map.toString()` (`{nome=Paulo}`).
Esse padrão facilita buscas em ferramentas de observabilidade e reduz ambiguidades
durante a análise de incidentes.

## Testes

Os testes de integração usam `FailFastMockWebServer` (wrapper do `MockWebServer`) para
garantir que toda requisição do worker tenha stub explícito. Caso uma chamada HTTP não
tenha resposta enfileirada, o teste falha imediatamente, evitando travamentos
silenciosos durante a execução da suíte.
O wrapper também expõe `getRequestCount()` para facilitar asserções sobre o número de
chamadas disparadas pelos serviços em cada cenário de teste.
Os cenários de validação de interesses devem considerar o fallback de locale na Graph API:
quando não há correspondência em `pt_BR`, o worker tenta novamente em `en_US` e, se ainda
assim não houver resultado, realiza uma terceira consulta sem locale. Testes que simulam
esse comportamento precisam enfileirar três respostas para evitar falhas por requisições
sem stub.

## Renovação automática de tokens

Além da criação de campanhas, o worker monitora tokens configurados nas
contas do backend e renova automaticamente aqueles que estão prestes a expirar.
O fluxo funciona da seguinte forma:

1. A cada execução do agendador (`FacebookTokenRenewalScheduler`) o worker
   consulta o endpoint `/api/accounts/facebook/renewal/eligible`, que devolve
   apenas as contas com `tokenRenewalEnabled = true`, token prestes a expirar e
   credenciais completas (`appId`, `appSecret`).
2. Para cada conta elegível, o serviço `FacebookTokenRenewalService` usa o
   método `FacebookAdsService.renewLongLivedToken` para chamar a Graph API
   diretamente (`/{version}/oauth/access_token` com `grant_type=fb_exchange_token`).
   O worker calcula a data de expiração a partir de `expires_in` (ou aplica o
   fallback de 60 dias quando o Facebook não informa o valor) e registra o
   resultado no backend via `POST /api/accounts/facebook/{id}/token/renewal`.
   Assim o token recém-gerado é persistido e fica disponível para o frontend e
   para futuras execuções do worker.
3. Quando a Graph API devolve `(#190) OAuthException` o `FacebookCampaignService`
   intercepta a exceção, pausa o processamento de experimentos e delega para o
   `FacebookAccessTokenManager` repetir o mesmo fluxo de geração direta. Se o
   aplicativo (`facebook.app-id`/`facebook.app-secret`) estiver configurado, o
   novo token é aplicado em memória imediatamente e a fila de experimentos volta
   a ser processada na próxima execução agendada. Caso a geração falhe, o worker
   registra o erro no backend através do endpoint de `token/renewal`, mantendo o
   token anterior até que uma nova tentativa (automática ou manual) seja
   bem-sucedida.
4. Esses dados são exibidos na tela de contas do frontend, permitindo acompanhar
   a última tentativa, o último sucesso e eventuais mensagens de erro.

Caso a geração do token dispare um erro da Graph API, o backend recebe o
status `FAILED` com a mensagem retornada em `tokenRenewalLastError`, mantendo o
token anterior até que uma nova tentativa (manual ou automática) seja
bem-sucedida.

### Falha momentânea ao consultar a configuração do backend

Em ambientes instáveis pode ocorrer do backend encerrar a conexão HTTP antes de
enviar a resposta do endpoint `GET /api/accounts/facebook/worker-config`. Nessa
situação o worker registra apenas um aviso indicando a falha de comunicação e
prossegue normalmente com o ciclo agendado, tentando novamente na próxima
execução sem interromper o processamento de campanhas. Para evitar poluição dos
logs, o aviso é emitido apenas uma vez enquanto a indisponibilidade persiste e é
reabilitado automaticamente assim que o backend voltar a responder.

Quando o backend ainda não possui uma conta marcada para o worker e responde
`404 Not Found`, o serviço segue a mesma abordagem: registra o primeiro aviso e
silencia tentativas subsequentes até que a configuração esteja disponível
novamente.

## Configuração via backend

O worker não lê mais variáveis de ambiente para parametrizar o Facebook. Todas
as credenciais e defaults operacionais são preenchidos pela equipe através da
tela **Contas do Facebook** no frontend. A conta marcada com "Utilizar esta
conta no Facebook Ads Worker" é exposta pelo endpoint
`GET /api/accounts/facebook/worker-config` e contém:

- **Token de acesso de longa duração**, **App ID** e **App Secret** usados para
  autenticação e renovação automática;
- **ID da conta de anúncios**, **Página padrão**, **Instagram Actor ID**,
  **URL padrão** e **call-to-action** utilizados como fallback quando o
  experimento não define esses valores;
- Parâmetros padrão do conjunto de anúncios (orçamento diário, billing event,
  optimization goal, destination type, estratégia de lance, bid amount e país
  alvo);
- Template de mensagem do criativo (com suporte a `%s` para incluir o nome do
  experimento).

Ao carregar uma execução o `FacebookCampaignService` consulta essa configuração,
sincroniza o token em memória (`FacebookAdsService`) e aplica todos os valores
nas chamadas à Graph API. Dessa forma basta atualizar os campos na interface web
para trocar de conta, ajustar orçamento ou modificar o destino padrão sem
reiniciar o worker. As únicas propriedades externas mantidas em arquivo de
configuração são `backend.base-url`, `backend.api-prefix` e
`facebook.graph-api.version`.

Ainda não existem testes unitários neste módulo. Ao introduzir cobertura
automatizada, priorize cenários que validem o fluxo completo de configuração —
incluindo a atualização do token retornado pelo backend antes de cada ciclo —
para manter a lógica de sincronização alinhada com o comportamento em produção.

## Validação automática de interesses

O backend mantém uma lista de interesses do Facebook que ainda não possuem o
`id` oficial retornado pela Graph API. O **Facebook Ads Worker** executa um
processo periódico (`FacebookInterestValidationScheduler`) que consulta o
endpoint `/api/facebook-interests/pending` e valida apenas os interesses nunca
pesquisados. Para cada entrada encontrada o worker pesquisa o interesse na
Graph API (`/act_{adAccountId}/targetingsearch?type=adinterest&q={nome}`):

1. Quando a Meta devolve um resultado, o worker associa o `facebookInterestId`
   e, se necessário, atualiza o nome para o rótulo retornado pela API
   (por exemplo, substituir "Pilates" por "Pilates Training" quando esse for o
   código disponível).
2. Quando nenhum resultado é encontrado, o interesse é marcado como **INVALID**
   no backend, permitindo correções manuais.

O resultado é reportado ao backend via `PATCH /api/facebook-interests/{id}` com
o `status` (VALID ou INVALID), o código resolvido e o nome final, garantindo que
novas criações de segmentação utilizem apenas interesses aceitos pela Meta.

## Data Model

As tabelas prefixadas com `facebook_ads_` descritas em
[docs/data-model.md](../docs/data-model.md) são utilizadas para persistir
informações de campanhas, conjuntos de anúncios, criativos e parâmetros de
rastreamento.

## Documentation

Um diagrama de classes simplificado pode ser encontrado em
[docs/facebook-ads-worker/class-diagram.md](../docs/facebook-ads-worker/class-diagram.md).
Consulte também a documentação oficial da Graph API sempre que precisar
interagir com a plataforma: https://developers.facebook.com/docs/graph-api e
https://developers.facebook.com/docs/graph-api/reference.

### Troubleshooting de campanhas com erro `(#200) Permissions error`

Ao criar campanhas o Facebook pode devolver `400 Bad Request` com
`error.code = 200` e mensagem `Permissions error`. Esse retorno indica que o
token utilizado não possui o escopo `ads_management` aprovado ou que a conta
de anúncios não tem acesso padrão liberado para produção. Consulte a tabela
de [códigos de erro da Marketing API](https://developers.facebook.com/docs/marketing-api/error-reference/#ErrorCodes)
para confirmar os requisitos de permissão. Quando o erro vem acompanhado do
`error_subcode = 1815199` ("A conta de anúncios não tem acesso a esta conta do
Instagram"), o worker tenta novamente criar o criativo sem enviar o
`instagram_user_id`, permitindo que a campanha prossiga apenas com o Facebook
quando a conta não possui acesso ao Instagram informado pelo backend. Caso o
Facebook ainda rejeite a requisição — seja por outras permissões ausentes ou
por continuar exigindo o Instagram — o worker marca automaticamente o
experimento como `FAILED` via
`PATCH /api/experiments/{id}/status?status=FAILED` para evitar novas tentativas
sem intervenção humana. Caso o backend retorne erro e não consiga atualizar o
status, o worker mantém o identificador do experimento em uma lista de bloqueio
em memória e ignora execuções seguintes até que o serviço seja reiniciado. Após
ajustar as permissões, atualize manualmente o status do experimento para que ele
volte a ser elegível e reinicie o worker para que o novo token seja utilizado.

### Erro `(#100) Invalid parameter` ao criar o criativo

O endpoint [`POST /{ad_account_id}/adcreatives`](https://developers.facebook.com/docs/marketing-api/reference/ad-creative#Creating)
exige um `page_id` válido no `object_story_spec` quando o criativo representa
uma Página do Facebook. O worker resolve esse identificador na seguinte ordem:
1) página padrão configurada na conta do backend; 2) página associada ao
experimento. Experimentos sem associação e sem fallback configurado na conta
são ignorados e permanecem elegíveis para a próxima execução, registrando o
aviso correspondente nos logs. Cadastre a página na conta ou associe uma página
ao experimento antes da próxima execução para que a criação seja bem-sucedida.

### Erro `(#100) Invalid parameter` ao criar o conjunto de anúncios

Algumas contas de anúncios exigem que cada conjunto informe limites explícitos
de lance (`bid_cap`) ou metas de retorno (`ROAS`). Nesses casos, a Graph API
responde com `error_subcode = 2490487` e a mensagem "Valor ou restrições de
lance obrigatórios". Para manter o fluxo padrão funcional, o worker agora envia
`bid_strategy = LOWEST_COST_WITHOUT_CAP` por default, estratégia que não exige
valores adicionais. Caso sua conta utilize políticas diferentes, atualize os
campos **Valor do lance (centavos, opcional)** e **Estratégia de lance** na tela
**Contas do Facebook** do frontend para refletir a configuração real do
Gerenciador de Anúncios. O backend persiste essas alterações e o worker passa a
utilizar os novos valores automaticamente no ciclo seguinte, sem necessidade de
reiniciar o serviço.

Quando o backend retornar interesses apenas pelo nome, o worker consulta a
[Targeting Search](https://developers.facebook.com/docs/marketing-api/audiences/reference/targeting-search)
da Graph API para resolver automaticamente os identificadores aceitos pelo
Facebook antes de criar o conjunto de anúncios. Caso o nome
informado não possua correspondência, o interesse é descartado e o log aponta o
termo ignorado. Para evitar quedas de desempenho em execuções subsequentes, os
resultados são armazenados em cache em memória durante o ciclo atual do worker.

Da mesma forma, o worker valida `custom_audiences` e
`excluded_custom_audiences` antes de enviá-los para a Graph API. Entradas que
já chegam com um `id` numérico são preservadas; quando o backend envia apenas o
nome e nenhum identificador pode ser deduzido, o worker registra um aviso e
remove o item da segmentação para evitar erros `(#100) Param
targeting[custom_audiences][X] must be a valid custom audience id` durante a
criação do conjunto de anúncios.

### Erro `(#100) bid_amount is not allowed when bid strategy is lowest cost`

A documentação oficial de estratégias de lance da Meta
(https://developers.facebook.com/docs/marketing-api/bidding/#bid-strategy)
explica que a estratégia **LOWEST_COST_WITHOUT_CAP** não aceita lance manual
(`bid_amount`). Quando o valor é enviado junto a essa estratégia (ou quando o
`bid_strategy` fica em branco e a API assume o lowest cost como padrão), a
Graph API responde com `(#100) Invalid parameter` informando que o
`bid_amount` não é permitido. Para evitar a rejeição, o worker agora remove
automaticamente o `bid_amount` quando a estratégia configurada é lowest cost ou
não foi informada. Quem precisar controlar o lance manualmente deve alterar a
conta no backend para uma estratégia compatível (por exemplo, COST_CAP) antes
do próximo ciclo do worker.

### Erro `(#190) OAuthException` indicando token expirado

Quando o Facebook devolve `code = 190` com `error_subcode = 463/467` ou
mensagem "Session has expired", o worker interpreta que o token fornecido pelo
backend não é mais válido. O `FacebookCampaignService` interrompe
temporariamente a transformação de novos experimentos, registra o aviso apenas
uma vez e delega a renovação para o `FacebookAccessTokenManager`. O serviço tenta
renovar o token automaticamente via Graph API quando `appId` e `appSecret` estão
preenchidos na conta selecionada. Em caso de sucesso, o novo token é aplicado sem
reiniciar o worker e os experimentos voltam a ser processados na próxima
execução. Caso a primeira tentativa falhe, o worker continua tentando a
renovação em cada ciclo agendado e retoma o processamento assim que obtiver um
token válido novamente. Se a renovação automática estiver desabilitada ou falhar
repetidamente, o log registra uma mensagem de erro com os detalhes retornados
pela Graph API, orientando a atualizar o token manualmente na interface e
reiniciar o serviço após a substituição.

### Erro de compilação `cannot find symbol: variable Objects`

Ao adicionar novos métodos no `FacebookAdsService`, certifique-se de importar
`java.util.Objects` quando utilizar `Objects.requireNonNull`. A ausência do
import impede a compilação do módulo e exibe a mensagem acima. Após sincronizar
o import, execute `mvn -s settings.xml compile` para validar o projeto antes de
publicar o pacote.

## Build
```
mvn -s settings.xml package
```

## Test
```
mvn -s settings.xml test
```
Os testes com `MockWebServer` devem utilizar `takeRequest` com timeout e validar
o retorno para evitar travamentos silenciosos no pipeline. Garanta também que
exista uma resposta enfileirada para cada chamada esperada, evitando bloqueios
na leitura do `WebClient`.

## Docker e publicação

Para executar o worker como container:

```sh
docker compose build
docker compose up -d
```

Principais variáveis de ambiente (todas já possuem defaults no `docker-compose.yml`):

- `BACKEND_BASE_URL` e `BACKEND_API_PREFIX` para apontar para o backend;
- `FACEBOOK_GRAPH_API_BASE_URL` e `FACEBOOK_GRAPH_API_VERSION` para controlar a versão da Graph API;
- `FACEBOOKCAMPAIGN_SCHEDULER_DELAY`, `FACEBOOKPIXEL_SCHEDULER_DELAY`, `FACEBOOK_INTEREST_VALIDATION_SCHEDULER_DELAY` e `FACEBOOK_TOKEN_RENEWAL_SCHEDULER_DELAY` para ajustar as janelas dos agendadores;
- `FACEBOOK_ADS_WORKER_PORT` caso queira expor a porta HTTP (padrão mapeia `8082:8080`).

O Dockerfile usa Java 21 e gera um jar único (`app.jar`) a partir do Maven. Tokens do GitHub (`GITHUB_TOKEN`/`GITHUB_ACTOR`) são aceitos como build args para quem precisar baixar artefatos privados.

### Pipeline de deploy automático

O workflow `.github/workflows/facebook-ads-worker.yml` replica o fluxo do AI Worker: roda testes Maven, builda a imagem e publica no GitHub Container Registry em
`ghcr.io/<owner>/facebook-ads-worker:latest` (e com tag do commit). Em pushes para `main`, o action sobe a stack via SSH no VPS `191.252.120.96`, sincronizando os arquivos do diretório `facebook-ads-worker/` e usando `docker-compose.deploy.yml` para apontar para a imagem publicada. Configure os segredos `VPS_SSH_KEY`, `FACEBOOK_ADS_WORKER_REMOTE_PATH` (opcional) e `GHCR_TOKEN`/`GHCR_USERNAME` se for usar credenciais próprias do registry.

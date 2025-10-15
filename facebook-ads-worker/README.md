# Facebook Ads Worker

Worker responsável por criar campanhas no Facebook Ads, incluindo os
posicionamentos no Facebook e no Instagram, e coletar métricas usando a API de
Marketing do Facebook. O serviço reutiliza o modelo de dados definido no
projeto `backend`, evitando duplicação de entidades.

O fluxo automatizado cria toda a hierarquia necessária para veiculação:

1. **Campanha** (`POST /campaigns`) com objetivo `OUTCOME_TRAFFIC`, status
   inicial `PAUSED` e `special_ad_categories = []`, conforme documentado na
   [Marketing API](https://developers.facebook.com/docs/marketing-api/reference/ad-campaign-group#Creating) para contas que
   não se enquadram em categorias especiais.
2. **Conjunto de anúncios** (`POST /adsets`) atrelado à campanha, também em
   `PAUSED`, com segmentação geográfica simples e destino `WEBSITE`.
3. **Imagem do criativo**: em vez de enviar `POST /adimages`, o worker
   referencia diretamente a URL pública retornada pelo backend no campo
   `object_story_spec.link_data.picture`. Quando o caminho é relativo (por
   exemplo, `/uploads/arquivo.jpg`), o worker o normaliza para o domínio
   configurado em `backend.base-url` antes de encaminhá-lo ao Facebook. Essa
   abordagem evita depender da biblioteca de imagens da conta e é suportada pela
   Graph API desde que a URL seja acessível pelo crawler da Meta.
4. **Criativo** (`POST /adcreatives`) baseado em um `object_story_spec`
   contendo o `page_id` definido na conta selecionada no backend. Quando a conta
   não possui `defaultPageId`, o worker utiliza a página vinculada ao
   experimento no backend (exposta como `facebookPage`, `associatedFacebookPage`
   ou `facebookPageAssociation`) e ignora o experimento caso nenhuma associação
   exista. A mesma regra vale para a identidade do Instagram: o worker consome o
   campo `instagramAccount` retornado pelo backend e popula `instagram_user_id`
   com o código cadastrado na conta. Caso o experimento não esteja relacionado a
   uma conta do Instagram, o worker registra o aviso e pula a publicação.
   Opcionalmente o fluxo inclui mensagem e call-to-action vindos do próprio
   criativo. A imagem é sempre veiculada via `link_data.picture` — não há hash
   salvo na biblioteca —, garantindo que o anúncio utilize exatamente o ativo
   hospedado pelo backend.
5. **Anúncio** (`POST /ads`) que referencia o conjunto e o criativo recém
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
`facebook_ads_ad_set`, `facebook_ads_ad_creative` e `facebook_ads_ad`). Todas as
chamadas HTTP ao backend devem registrar a URL completa, parâmetros, payload e
resposta recebida para acelerar o diagnóstico de incidentes em produção. Os logs
seguem o padrão visual `==>` para requisições (por exemplo, `url==>https://...`)
e `<==` para respostas, inclusive em cenários de erro, permitindo identificar
rapidamente a direção do tráfego durante uma análise.

Quando a jornada do experimento define um novo passo de captura com instant
form aprovado, o worker publica o formulário antes de criar a campanha. O fluxo
é dividido em duas etapas complementares:

1. **Publicação proativa** – O `FacebookInstantFormPublicationScheduler`
   consulta `/api/instant-forms/ready-to-publish` e delega para o
   `FacebookInstantFormPublicationService`, que publica cada formulário aprovado
   via `FacebookAdsService.publishInstantForm`. Na sequência o serviço lê os
   detalhes com `FacebookAdsService.fetchInstantForm`, normaliza o identificador
   devolvido pela Meta e calcula o `shareLink` (usando o padrão
   `https://www.facebook.com/ads/leadgen/?id=<id>` quando necessário). Por fim o
   backend é atualizado com `PATCH /api/instant-forms/{id}/publication`,
   preenchendo `published = true`, `publishedAt`, link compartilhável e o
   `facebookFormId` definitivo. Quando o backend ainda não tiver persistido o
   identificador de rascunho (`facebookFormId` nulo ou vazio), o serviço consulta
   a Graph API (`/{pageId}/leadgen_forms`) e busca pelo formulário com o mesmo
   nome na página informada. Encontrando um rascunho correspondente, o worker
   reutiliza o `draft_id` (ou `id`, quando o Facebook não devolve o campo) para
   concluir a publicação e reporta o valor resolvido ao backend.
   > ⚠️ O backend deve persistir o identificador do rascunho retornado na criação
   > do formulário. A Meta só atribui o ID definitivo após a publicação, portanto
   > o worker não consegue prosseguir se esse valor estiver ausente.
2. **Validação na criação da campanha** – O `FacebookCampaignService` reaproveita
   os dados persistidos. Se, por algum motivo, o formulário ainda não estiver
   publicado ou a Meta não tiver retornado o ID final, o serviço tenta novamente
   publicar antes de criar o criativo. Enquanto o ID definitivo não estiver
   disponível, o CTA permanece apontando para o link de compartilhamento e o
   worker evita enviar `lead_gen_form_id`, impedindo erros do tipo `(#100) Param
   call_to_action[value][lead_gen_form_id] must be a valid Lead Gen Data id`.
   Assim que o identificador for confirmado, o worker passa a anexá-lo ao CTA e
   define `destination_type = FACEBOOK`, direcionando os usuários diretamente ao
   instant form selecionado.

Todas as chamadas à Graph API são logadas detalhadamente para facilitar
investigações de erros (por exemplo, respostas `400 Bad Request`). Os logs
registram caminho da requisição, payload enviado (com `access_token`
anonimizado), status HTTP retornado, cabeçalhos de resposta (também com
valores sensíveis mascarados), corpo devolvido pelo Facebook e, quando
presente, os campos estruturados de erro (`type`, `code`, `error_subcode`,
`error_user_title`, `error_user_msg`, `fbtrace_id` e `error_data`). Isso
permite cruzar rapidamente o incidente com a documentação oficial.

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

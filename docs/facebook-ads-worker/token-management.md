# Gerenciamento de tokens de acesso do Facebook

Este documento descreve como gerar tokens de longa duração para o Facebook Ads,
como o **Facebook Ads Worker** monitora essas credenciais e de que forma o
backend persiste o resultado das renovações. Utilize este guia sempre que
precisar atualizar o token utilizado em produção ou investigar problemas de
expiração automática.

## 1. Geração inicial do token

1. Gere um token de usuário curto através do
   [Graph API Explorer](https://developers.facebook.com/tools/explorer/) ou da
   tela **Contas do Facebook** no backend.
2. Converta esse token curto em um token de longa duração chamando a rota
   `/{version}/oauth/access_token` da Graph API com os seguintes parâmetros:
   - `client_id`: App ID do aplicativo aprovado para anunciar;
   - `client_secret`: App Secret do mesmo aplicativo;
   - `grant_type=fb_exchange_token`;
   - `fb_exchange_token`: o token curto obtido no passo anterior.
3. Copie o `access_token` retornado e preencha os campos **Token de acesso**,
   **App ID** e **App Secret** na conta desejada dentro de **Contas do Facebook**
   no frontend. O backend persiste esses valores em `facebook_account` e inicia o
   controle de expiração (`tokenExpiresAt`, `tokenLastRefreshedAt`).

> O worker também executa essa mesma chamada quando precisa transformar um token
> recém renovado em memória, garantindo que a lógica seja idêntica ao processo
> manual descrito acima.

## 2. Como o Facebook Ads Worker monitora o token

1. A cada ciclo do `FacebookTokenRenewalScheduler`, o worker consulta o endpoint
   `GET /api/accounts/facebook/renewal/eligible` para descobrir quais contas
   possuem `tokenRenewalEnabled = true`, credenciais completas e expiração
   iminente.
2. Para cada conta elegível, o `FacebookTokenRenewalService` chama a Graph API na
   rota `/{version}/oauth/access_token` com `grant_type=fb_exchange_token`,
   reaproveitando `appId`, `appSecret` e o token atual para solicitar um novo
   token de longa duração.
3. Se a resposta indicar sucesso, o worker atualiza seu `FacebookAdsService` em
   memória quando o token renovado pertence à mesma conta configurada e, em
   seguida, informa o backend via `POST /api/accounts/facebook/{id}/token/renewal`
   com o novo token, data de expiração calculada e o instante de renovação.
4. Em caso de erro (por exemplo, `(#190) OAuthException`), o worker envia o mesmo
   endpoint com `status=FAILED` e a mensagem retornada pela Graph API. O token
   antigo permanece em uso até uma intervenção manual ou uma nova tentativa bem
   sucedida.

## 3. Comportamento do backend

- **Filtragem de elegíveis** – O endpoint `GET /api/accounts/facebook/renewal/eligible`
  percorre todas as contas cadastradas, restringindo apenas às que possuem token,
  `appId`, `appSecret`, renovação automática ativada e que estão dentro da janela
  configurada para expiração. O backend devolve a lista de candidatos contendo
  identificador, nome, credenciais e a data atual de expiração.
- **Registro da tentativa** – Ao receber `POST /api/accounts/facebook/{id}/token/renewal`
  o backend atualiza `tokenRenewalStatus` e `tokenRenewalLastAttemptAt` com o
  horário informado (ou o horário da requisição quando ausente).
- **Sucesso** – Quando `status=SUCCESS`, o backend exige `accessToken`
  preenchido, atualiza `tokenExpiresAt`, `tokenLastRefreshedAt` e `tokenRenewedAt`
  e limpa `tokenRenewalLastError`. O novo token passa a ser utilizado pelo
  frontend e por futuras execuções do worker.
- **Falha** – Quando `status=FAILED`, o backend mantém o token anterior e grava a
  mensagem recebida em `tokenRenewalLastError`, permitindo que o frontend
  exiba o motivo da falha e que operadores decidam por uma nova tentativa ou
  substituição manual do token.

## 4. Boas práticas operacionais

- Sempre valide que o App ID e o App Secret pertencem ao mesmo aplicativo usado
  para gerar o token; inconsistências resultam em erro `OAuthException`.
- Quando precisar substituir o token manualmente, atualize-o na interface do
  backend. O worker detecta a mudança e passa a utilizar o novo valor sem
  reiniciar o serviço.
- Monitore periodicamente os campos **Última tentativa de renovação** e **Último
  erro** na tela de contas para confirmar que o agendador está funcionando e que
  nenhuma credencial está próxima do vencimento.

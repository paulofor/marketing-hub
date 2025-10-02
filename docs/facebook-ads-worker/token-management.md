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
2. Para cada conta elegível, o `FacebookTokenRenewalService` aciona o endpoint
   `POST /api/accounts/facebook/{id}/token/revalidation`. O backend reutiliza o
   `appId`, o `appSecret` e o token atual armazenados na conta para trocar o
   token diretamente na Graph API.
3. Ao receber `status=SUCCESS`, o worker atualiza seu `FacebookAdsService` em
   memória quando o token renovado pertence à mesma conta configurada. O backend
   já registra o novo token, a data de expiração e o horário da tentativa.
4. Em caso de falha (por exemplo, `(#190) OAuthException`), o backend devolve
   `status=FAILED` com a mensagem da Graph API, mantendo o token anterior até que
   uma intervenção manual ou nova tentativa automática seja bem-sucedida.

## 3. Comportamento do backend

- **Filtragem de elegíveis** – O endpoint `GET /api/accounts/facebook/renewal/eligible`
  percorre todas as contas cadastradas, restringindo apenas às que possuem token,
  `appId`, `appSecret`, renovação automática ativada e que estão dentro da janela
  configurada para expiração. O backend devolve a lista de candidatos contendo
  identificador, nome, credenciais e a data atual de expiração.
- **Registro da tentativa** – O endpoint `POST /api/accounts/facebook/{id}/token/revalidation`
  atualiza `tokenRenewalStatus`, `tokenRenewalLastAttemptAt`, `tokenRenewedAt` e
  `tokenExpiresAt` automaticamente após consultar a Graph API, registrando o
  token recebido ou a mensagem de erro.
- **Sucesso** – Quando `status=SUCCESS`, o backend salva o novo `accessToken`,
  atualiza `tokenExpiresAt`, `tokenLastRefreshedAt` e `tokenRenewedAt` e limpa
  `tokenRenewalLastError`. O novo token passa a ser utilizado pelo frontend e por
  futuras execuções do worker.
- **Falha** – Quando `status=FAILED`, o backend mantém o token anterior e grava a
  mensagem retornada em `tokenRenewalLastError`, permitindo que o frontend
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
- Revise também os novos campos opcionais (como `defaultLeadGenFormId`) ao
  atualizar tokens: eles não impactam a autenticação, mas garantem que campanhas
  de leads continuem direcionando corretamente os formulários.

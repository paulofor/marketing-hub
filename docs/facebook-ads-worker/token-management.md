# Gerenciamento de tokens de acesso do Facebook

Este documento descreve como gerar tokens de longa duração para o Facebook Ads,
como o **Facebook Ads Worker** monitora essas credenciais e de que forma o
backend persiste o resultado das renovações. Utilize este guia sempre que
precisar atualizar o token utilizado em produção ou investigar problemas de
expiração automática.

> Atualização 2025-05-20: experimento sem `defaultPageId` requer uma página
> associada (`fb_page`). Certifique-se de que o token gerado tenha acesso à
> página vinculada ao experimento, caso contrário o worker ignorará a criação da
> campanha.
>
> Atualização 2025-08-22: vincule uma `instagramAccount` a cada experimento.
> Sem esse relacionamento o worker não consegue definir `instagram_user_id` e
> deixa o experimento fora da fila até que a conta esteja configurada.

## 1. Geração inicial do token

1. Obtenha uma credencial de usuário válida pelo fluxo oficial de login do
   aplicativo ou pelo [Graph API Explorer](https://developers.facebook.com/tools/explorer/),
   usando o aplicativo e a conta autorizados. O painel administrativo atual exibe
   o estado e permite revalidar, mas **não implementa login interativo da Meta nem
   um campo de substituição do token principal**.
2. A troca é feita no servidor pelo endpoint `/{version}/oauth/access_token`, com
   `grant_type=fb_exchange_token`, `client_id`, `client_secret` e `fb_exchange_token`.
   O segredo nunca deve ser colocado no navegador ou na conversa.
3. Na recuperação operacional, disponibilize a credencial em arquivo protegido
   ao responsável pela integração. Ela deve ser aplicada pelo contrato oficial
   de atualização da conta, que persiste em `fb_account`; não editar diretamente
   o banco. Acompanhe o resultado em **Contas do Facebook**. App ID e App Secret
   já cadastrados são preservados quando não houver necessidade de alterá-los.

> O worker também executa essa mesma chamada quando precisa gerar um novo token
> automaticamente, garantindo que a lógica seja idêntica ao processo manual
> descrito acima.

## 2. Como o Facebook Ads Worker monitora o token

1. A cada ciclo do `FacebookTokenRenewalScheduler`, o worker consulta o endpoint
   `GET /api/accounts/facebook/renewal/eligible` para descobrir quais contas
   possuem `tokenRenewalEnabled = true`, credenciais completas e expiração
   iminente.
2. Para cada conta elegível, o `FacebookTokenRenewalService` chama diretamente a
   Graph API através de `FacebookAdsService.renewLongLivedToken`. O worker envia
   o `appId`, o `appSecret` e o token atual (`fb_exchange_token`) para gerar um
   novo token quando a Meta permitir a troca. A validade vem de `expires_in`; não é presumida.
3. Ao receber a resposta (`access_token` + `expires_in` positivo), o worker
   reporta o resultado ao backend via `POST
   /api/accounts/facebook/{id}/token/renewal`. O backend persiste o novo token,
   a data de expiração calculada em UTC e os horários da tentativa. Os consumidores
   carregam o token no próximo ciclo; a renovação não troca o contexto de campanhas em curso.
4. Em caso de falha (por exemplo, `(#190) OAuthException`), o worker registra o
   erro com status `FAILED` no mesmo endpoint `token/renewal`, mantendo o token
   anterior até que uma intervenção manual ou nova tentativa automática seja
   bem-sucedida.

## 3. Comportamento do backend

  - **Filtragem de elegíveis** – O endpoint `GET /api/accounts/facebook/renewal/eligible`
    percorre todas as contas cadastradas, restringindo apenas às que possuem token,
    `appId`, `appSecret`, renovação automática ativada e que estão dentro da janela
    configurada para expiração. O backend devolve a lista de candidatos contendo
    identificador, nome, credenciais e a data atual de expiração.
  - **Registro da tentativa** – O endpoint `POST /api/accounts/facebook/{id}/token/renewal`
    recebe o resultado enviado pelo worker (sucesso ou falha) e atualiza
    `tokenRenewalStatus`, `tokenRenewalLastAttemptAt`, `tokenRenewedAt` e
    `tokenExpiresAt`, preservando o token mais recente ou a mensagem de erro.
- **Sucesso** – Quando `status=SUCCESS`, o backend salva o novo `accessToken`,
  atualiza `tokenExpiresAt` conforme o prazo positivo informado pela Meta, bem como
  `tokenLastRefreshedAt` e `tokenRenewedAt`, e limpa `tokenRenewalLastError`. O
  novo token passa a ser utilizado pelo frontend e por futuras execuções do
  worker.
- **Falha** – Quando `status=FAILED`, o backend mantém o token anterior e grava a
  mensagem retornada em `tokenRenewalLastError`, permitindo que o frontend
  exiba o motivo da falha e que operadores decidam por uma nova tentativa ou
  substituição manual do token.

## 4. Boas práticas operacionais

- Sempre valide que o App ID e o App Secret pertencem ao mesmo aplicativo usado
  para gerar o token; inconsistências resultam em erro `OAuthException`.
- Quando a Meta exigir recuperação da autenticação, use o fluxo oficial e o
  contrato de atualização descritos acima. O worker detecta a credencial persistida
  no próximo ciclo, sem reiniciar o serviço.
- Monitore periodicamente os campos **Última tentativa de renovação** e **Último
  erro** na tela de contas para confirmar que o agendador está funcionando e que
  nenhuma credencial está próxima do vencimento.
- Revise também os novos campos opcionais (como `defaultLeadGenFormId`) ao
  atualizar tokens: eles não impactam a autenticação, mas garantem que campanhas
  de leads continuem direcionando corretamente os formulários.

## Recuperação automática e validade real — 20/10/2026

A renovação usa um agendador exclusivo e mantém o intervalo operacional existente.
As outras rotinas continuam seriais. Todos os clientes WebClient gerenciados pelo
Spring recebem timeout de conexão de 10 segundos e de inatividade de resposta de
60 segundos, incluindo leitura do corpo. Uma nova passagem pode recuperar a consulta,
sem manter o worker preso indefinidamente. Esses limites são propriedades do executor.

Falha de persistência do callback é propagada: troca aceita pela Meta sem confirmação
no backend não é anunciada como concluída. Resposta sem validade positiva não substitui
a credencial anterior. O backend administrativo usa a mesma regra de `expires_in`.

A [Meta](https://developers.facebook.com/docs/facebook-login/guides/access-tokens/get-long-lived/)
exige token válido na troca. Renovação automática não recria consentimento de uma
sessão já vencida ou revogada; nesse caso, há reconexão excepcional do titular. A flag
de automação permanece ligada. Não usar token de outro aplicativo nem fabricar prazo.

A investigação comprovou a fila bloqueada em `MetaAudienceBackendClient.listPending`
desde 10/10, antes do vencimento da conta #1 em 19/10. A matriz local está em
[meta-renovacao-automatica-v1.md](../homologacao/meta-renovacao-automatica-v1.md).

A documentação oficial descreve extensão de token curto e renovação por SDK
condicionada ao uso do aplicativo. Uma chamada de servidor bem-sucedida não deve
ser interpretada como garantia de renovação perpétua: conferir sempre a validade
retornada. A automação conserva esse limite do provedor.

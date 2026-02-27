# AGENTS.md — Facebook Ads Worker

- 🚨 **Muito importante:** qualquer alteração neste módulo deve ser refletida em todos os arquivos `.md` deste diretório. Mantenha a documentação atualizada.
- Este projeto utiliza o modelo de dados definido no **backend**.
- Não duplique ou mantenha modelo de dados aqui; importe-o do backend.
- Em produção utilizamos **MySql 5**.
- Tipos de dados permitidos (MySql 5): `INT`, `BIGINT`, `DECIMAL`, `DOUBLE`, `FLOAT`, `CHAR`, `VARCHAR`, `TEXT`, `LONGTEXT`, `BINARY(16)` para `UUID`, `DATE`, `DATETIME`, `TIMESTAMP`, `BOOLEAN`.
- Utilize o `facebook-ads-worker` para todas as chamadas à API do Facebook.
- Consulte sempre a documentação oficial da Graph API ao trabalhar neste módulo: https://developers.facebook.com/docs/graph-api e https://developers.facebook.com/docs/graph-api/reference.
- A versão da Graph API é configurável via propriedade `facebook.graph-api.version` (default `v23.0`) e deve estar alinhada com a recomendação oficial.
- Nas chamadas de validação de público (`/targetingvalidation`) e estimativa de alcance (`/reachestimate`), serializar `targeting_spec` com URL encoding completo para evitar erro de expansão (`Not enough variable values available to expand`) quando o JSON inclui chaves entre `{}`.
- Ao serializar `targeting_spec`, use codificação estrita (UTF-8) e preserve espaços como `%20` (não `+`) para manter compatibilidade com a Graph API.
- Na fila de resolução de targeting (`targeting_resolution_job`), sempre priorize o `ad_account_id` da conta de Facebook associada ao experimento (`ad_set -> experiment -> fb_page -> fb_account`) antes de qualquer fallback global.
- Sugestões de interesses baseadas em seed devem usar o endpoint `/act_<AD_ACCOUNT_ID>/targetingsuggestions` com o parâmetro `targeting_list=[{"type":"interests","id":"..."}]` guardando os seeds escolhidos.
- No discovery de público do playbook (`FACEBOOK_SEED_LOOKUP`), execute chamadas separadas em `/act_<AD_ACCOUNT_ID>/targetingsearch` para `adinterest`, `adbehavior` e `adworkposition`; não use payload misto sem `type` nem fallback por `/search` para essa etapa.
- Remova campos de segmentação não suportados pela Graph API antes do envio (por exemplo, `detailed_targeting_description`) para evitar erros `(#100) Invalid parameter`.
- Em chamadas de `/targetingvalidation` e `/reachestimate`, se a Graph API indicar `interest/interesse ... id <número> inválido`, remova esse ID do `targeting_spec` (inclusive em `flexible_spec`) e tente novamente para evitar bloqueio por interesses descontinuados.
- Na criação de ad sets (`POST /adsets`), se a Meta responder com erro no formato `targeting[<campo>][<índice>][id]`, remova o item inválido pelo índice indicado e reenvie a requisição para evitar falha definitiva por IDs descontinuados.
- Na etapa `FACEBOOK_VALIDATE_SPEC`, trate respostas do `/targetingvalidation` com `data=[]` como `VALID` quando não houver `is_valid` explícito, preservando compatibilidade com variações da Graph API.
- Em `geo_locations` descarte chaves que não sejam texto e remova `regions` cujos `key` não sejam numéricos para manter a compatibilidade com a Graph API.
- Quando o destino do experimento for um formulário de leads, ajuste o conjunto de anúncios para `destination_type = ON_AD`, force `optimization_goal = LEAD_GENERATION` e não envie `link` externo no criativo; utilize apenas `call_to_action.value.lead_gen_form_id`.
- Na criação de ad sets de campanha, manter `targeting_automation.advantage_audience = 0` para garantir o público Advantage+ desabilitado por padrão.
- Quando não houver `instagramAccount` no experimento, utilize o `defaultInstagramActorId` configurado ou o identificador vindo do criativo, seguindo sem `instagram_user_id` se nenhum valor estiver disponível.
- Identificadores de instant form no formato `ai_form_*` devem ser normalizados para `form_*` antes de chamar a Graph API.
- Não mantenha segredos no repositório; use variáveis de ambiente ou GitHub Secrets.
- Endpoints do backend devem ser acessados com o prefixo configurado em `backend.api-prefix` (default `/api`).
- Na sincronização de segmentações salvas manualmente no nicho, o worker deve consultar `GET /{graphVersion}/search` com `type` específico (`adinterest`, `adworkposition`, `adbehavior`) para obter `id`, `audience_size_lower_bound` e `audience_size_upper_bound`, reportando os valores ao backend em `/api/internal/targeting/elements/{id}/metaads`.
- Na sincronização de segmentações manuais via `GET /{graphVersion}/search`, não envie o parâmetro `fields`; mantenha apenas os parâmetros obrigatórios (`type`, `q`, `limit`, `access_token` e opcionais de locale/país).
- Alinhe a configuração de banco deste módulo com o padrão do `backend/ads-service` (host/usuário/pool Hikari) para evitar divergência entre ambientes.
- Sempre que chamar o backend registre logs com **URL completa**, parâmetros, payload enviado (quando existir) e a resposta recebida
  para facilitar troubleshooting.
- Ao registrar payloads ou respostas estruturadas em logs utilize `JsonLogFormatter.wrap(...)` para serializar objetos como JSON
  (incluindo aspas em strings) e manter tokens mascarados.
- Prefixe os valores de URL nos logs de integração com endpoints usando `==>` para requisições e `<==` para respostas (incluindo
  erros), garantindo um padrão visual consistente em todo o módulo.
- O fluxo de campanhas consulta o backend (`/api/adsets?experimentId={id}`) antes de criar campanhas na Meta para preparar a
  segmentação e Saved Audiences; mantenha esse detalhe documentado ao ajustar o fluxo.
- Na criação de campanhas, priorize sempre a segmentação validada pelo pipeline de público (`targetingRequestId` com
  `GET /api/targeting/requests/{id}?includeCandidates=true`); use `targetingJson`/campos legados apenas como fallback de compatibilidade.
- Em caso de erro de permissão do Facebook, o worker bloqueia o experimento em memória até que o serviço seja reiniciado.
- Ao publicar instant forms aprove os rascunhos com `facebookFormId` nulo e reporte o identificador definitivo recebido da Meta
  através de `PATCH /api/instant-forms/{id}/publication`. A criação automática foi descontinuada; os formulários devem ser
  cadastrados manualmente diretamente na Meta.
- Perguntas padrão do Instant Form (ex.: `FULL_NAME`, `EMAIL`, `PHONE`) não aceitam rótulos personalizados; ignore ou remova o
  `label` nessas situações para evitar o erro `(#100) Invalid parameter` com `error_subcode = 1892063`.
- Valores de opções em perguntas personalizadas devem ser normalizados (remoção de acentos,
  substituição de espaços por `_` e descarte de caracteres fora de `[A-Za-z0-9_-]`) antes do envio
  para garantir que cada alternativa possua `value` explícito e evitar o erro `(#100) Invalid parameter`
  com `error_subcode = 1892091`.
- Ao reportar a criação da campanha no backend inclua `experimentAdSetId` no payload para
  relacionar o conjunto de anúncios criado na Meta ao público do experimento e registrar os códigos
  retornados pelo Facebook.
- Em testes com `MockWebServer`, utilize `takeRequest` com timeout e valide o retorno para evitar
  travamentos silenciosos no pipeline. Enfileire respostas para cada chamada esperada para
  não bloquear o `WebClient`.
- Em testes com `MockWebServer`, prefira `FailFastMockWebServer` (wrapper do `MockWebServer`) e valide se não houve requisições
  sem stub (falha imediata) para evitar respostas pendentes mascararem novas regressões.
- O `FailFastMockWebServer` expõe `getRequestCount()` para checar quantas requisições o cenário
  disparou, facilitando asserções nos testes.
- Ao testar validação de interesses, lembre que o worker consulta a Graph API em sequência
  (`pt_BR`, `en_US` e sem locale). Enfileire respostas para cada tentativa para evitar falhas
  por requisições não stubadas.
- A coleta de métricas via Insights deve usar `date_preset = maximum` para obter o histórico
  completo; o valor `lifetime` não é aceito pela Graph API e gera erro `(#100)`.
- Quando a Graph API retorna `data=[]` no Insights, reporte métricas zeradas ao backend
  em vez de tratar como erro para evitar itens presos como pendentes.
- Chamadas de Insights (`/{campaignId}/insights`) devem evitar logs de sucesso em `INFO`
  para reduzir poluição; mantenha logs de erro/aviso para troubleshooting.

- Perguntas personalizadas geradas pelo ChatGPT agora são persistidas no backend
  e devolvidas em JSON para o worker; mantenha compatível qualquer mudança que
  altere a estrutura das perguntas serializadas.

## Serviços existentes
- **Campanhas de Facebook Ads** (`campaign`): cria campanhas para Facebook e Instagram utilizando o `facebook-ads-worker` com criativos gerados pelo **AI Worker** e aprovados pelo usuário no frontend.

## Orientação para novos serviços
- Siga o mesmo padrão do serviço de **campanhas de Facebook Ads**:
  - criar um pacote com o nome do domínio (ex: `campaign`);
  - implementar uma classe `*Service` com a lógica de integração com a API do Facebook;
  - criar um `*Scheduler` com `@Scheduled` para executar o serviço periodicamente;
  - encapsular qualquer cliente do Facebook dentro do mesmo pacote.

- Configuração padrão de execução em Docker grava logs em arquivo (`LOGGING_FILE_NAME`) com volume dedicado em `/var/log/facebook-ads-worker`; preserve esse comportamento ao ajustar compose/deploy.

- Os testes de campanha devem considerar a sequência de backend com `/api/experiments/{id}/adset-playbook` antes do fallback em `/api/adsets?experimentId={id}` e possíveis POSTs em `/api/experiments/{id}/facebook-api-logs` entre as etapas de criação.

- Em testes com `FailFastMockWebServer`, mantenha stubs explícitos para o fluxo principal e use respostas condicionais de fallback apenas para chamadas auxiliares (logs/status/publicação), para evitar flakiness por ordem de requisição.

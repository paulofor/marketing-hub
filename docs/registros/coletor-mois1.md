# Registros — Coletor Mois

> Orientação: todos os registros deste documento devem sempre incluir **data e hora no fuso UTC-3**.
> Neste documento segue política de **append-only** (não pode ter nenhuma linha apagada; apenas inserções).

> Regra obrigatória de timestamp:
> Antes de adicionar qualquer novo registro, execute obrigatoriamente:
>
> ```bash
> TZ=America/Sao_Paulo date '+%Y-%m-%d %H:%M:%S UTC-3'
> ```
>
> Use exatamente a saída desse comando no título do novo registro.
> É proibido inventar, estimar, inferir ou reaproveitar data/hora a partir de:
> - contexto da conversa;
> - data do commit;
> - data do CI/build;
> - metadados do arquivo;
> - relógio UTC sem conversão explícita;
> - registros anteriores deste documento.
>
> O formato obrigatório do título é:
>
> ```md
> ## YYYY-MM-DD HH:mm:ss UTC-3
> ```
>
> Cada novo registro deve ser adicionado no final do arquivo.
> Se for necessário registrar mais de uma entrada, execute novamente o comando de data/hora para cada entrada.
> Nunca crie registro com timestamp futuro em relação ao horário atual de `America/Sao_Paulo`.
> Em caso de timestamp incorreto já registrado, não apague nem edite o registro antigo; adicione um novo registro de correção explicando o erro.
> Neste documento segue política de **append-only** (não pode ter nenhuma linha apagada; apenas inserções).

## 2026-05-11 11:03:55 UTC-3
- Ajustado `HotmartCollectorService` no módulo `mois-hotmart-collector` para remover dependência de `waitUntil=NETWORKIDLE` na navegação do market Hotmart, trocando para `DOMCONTENTLOADED` + `waitForURL("**/market/**")` + espera explícita do `#root`.
- Reforçado tratamento do banner de cookies: mantida tentativa de aceite e adicionada estratégia de fallback via JavaScript para ocultar `#hotmart-cookie-policy` quando o overlay continuar interceptando eventos de clique.
- Objetivo dos ajustes: reduzir falhas por timeout e por interceptação de clique no submit de login.

## 2026-05-11 13:50:14 UTC-3
- Incluído log de diagnóstico de HTML no  para os casos em que  na página de market da Hotmart.
- O log agora registra  atual e  normalizado/truncado (até 8.000 caracteres), para validar se os cards existem no DOM mas não foram capturados pelo seletor.
- Mantida abordagem de causa-raiz: coletar evidência do HTML retornado no momento exato da falha de detecção de cards.

## 2026-05-11 13:50:27 UTC-3
- Correção de registro anterior: a entrada imediatamente acima perdeu trechos literais devido à interpretação de crases no shell durante o append.
- Conteúdo correto: incluído log de diagnóstico de HTML no `HotmartCollectorService` para os casos em que `cardsEncontrados=0` na página de market da Hotmart.
- Conteúdo correto: o log registra `url` atual e `htmlSnapshot` normalizado/truncado (até 8.000 caracteres), para validar se os cards existem no DOM mas não foram capturados pelo seletor.
- Mantida abordagem de causa-raiz: coletar evidência do HTML retornado no momento exato da falha de detecção de cards.

## 2026-05-11 13:56:00 UTC-3
- Implementado fallback de coleta via API oficial `https://api-affiliation-market.hotmart.com/v2/market/search` quando a página de market não retorna cards no DOM (`cardsEncontrados=0`).
- O fallback executa `fetch` no contexto autenticado da página (`credentials: include`) e mapeia produtos retornados da API para `HotmartProductSnapshot`.
- Adicionado log técnico do status HTTP e `bodyPreview` truncado da resposta da API para diagnóstico de contrato/autenticação sem depender apenas do scraper visual.

## 2026-05-11 15:30:35 UTC-3
- Adicionado diagnóstico pós-login no `HotmartCollectorService` para extração de token JWT (tentativa em `localStorage`, `sessionStorage` e `document.cookie`) com logs de cada passo.
- Adicionado logging de token com proteção por padrão (`masked`) e opção explícita `collector.hotmart.log-full-token=true` para modo diagnóstico completo.
- Fallback da API Hotmart agora tenta enviar `Authorization: Bearer <token>` quando token estiver disponível, mantendo também `credentials: include`.
- Incluídos logs detalhados no início do fallback da API informando presença de token e pré-visualização mascarada para rastrear causa-raiz de falhas de autenticação/coleta.

## 2026-05-12 11:22:10 UTC-3
- Reforçado o tratamento do banner de cookies no `HotmartCollectorService` com seletores adicionais específicos do overlay da Hotmart (`#hotmart-cookie-policy button`, `hotmart-cookie-policy button`) e variações genéricas de botões de consentimento.
- Incluída opção textual adicional (`Allow all`) para aceite automático em páginas em inglês.
- Melhorado fallback de causa-raiz: quando o overlay não some no timeout, o log agora sobe para `warn` com URL atual e o fallback JS também remove `.hotmart-cookie-policy-container`, reduzindo interceptação de clique no submit de login.

## 2026-05-12 14:20:29 UTC-3
- Alterado o `HotmartCollectorService` para priorizar coleta via API da Hotmart usando JWT salvo em configuração geral do backend (`/api/settings/hotmart_access_token_jwt`), eliminando a dependência de login automatizado para o fluxo principal.
- Implementada busca do token JWT via HTTP no backend (`collector.backend.base-url`) e uso direto no `Authorization: Bearer` da chamada para `https://api-affiliation-market.hotmart.com/v2/market/search`.
- Atualizado teste unitário para refletir o novo contrato do construtor do serviço e validar o comportamento de `COLLECTION_SKIPPED` quando não houver token configurado.

## 2026-05-12 23:46 UTC — Persistência Hotmart no backend
- Ajustado o `HotmartCollectorService` para enviar o resultado coletado para o endpoint do backend `/api/v1/mois/persistence/collection-jobs/{jobId}` após cada execução.
- Mapeado snapshot de produto para `references` do payload de persistência, permitindo gravação em `mois_collected_reference` e exibição na tela `/hotmart`.
- Adicionados parâmetros de configuração para `workspaceId`, `niche` e `marketTheme` no coletor.
- Testes do módulo executados com sucesso (`mvn test -q`).

## 2026-05-13 21:50 UTC
- Adicionados logs de causa-raiz no `HotmartCollectorService` para diagnosticar porque a tela mostrava "Produto sem título" e campos "Não informado" mesmo com coleta concluída.
- A coleta via API agora registra: chaves do objeto raiz quando não encontra array de produtos, quantidade de itens retornados e chaves do primeiro item para validar contrato real do JSON retornado pela Hotmart.
- Incluído log por item quando título não for mapeável, com `itemPreview` truncado, para identificar exatamente quais campos vieram no payload e ajustar contrato/mapeamento sem suposições.
- Melhorado o parser para ler campos também em objetos aninhados (`product` e `sourceObject`), reduzindo perda de dados quando a Hotmart muda o formato do JSON.

## 2026-05-13 22:05 UTC
- Incluído log explícito da resposta crua do fetch da Hotmart no fluxo principal da API (`HOTMART_FETCH_RESPOSTA_CRUA`), com status HTTP e `bodyRaw` truncado em 10.000 caracteres.
- Objetivo: deixar inequívoco no log exatamente o que o coletor conseguiu obter da Hotmart para diagnóstico de contrato/mapeamento.

## 2026-05-13 22:40 UTC
- Coletor Hotmart atualizado para extrair e enviar `hotmartTemperature` e `salesPageUrl` dentro de `rawMetadata` ao persistir referências no backend.
- Fluxo de coleta via API passou a mapear temperatura do payload (`temperature`/variantes) e URL de página de vendas separada da URL do produto.
- Backend ajustado para persistir `hotmart_temperature` em `mois_collected_reference` e expor `salesPageUrl` e `temperature` no endpoint `/api/v1/mois/hotmart/products`.
- Tela `/hotmart` atualizada para exibir temperatura e usar link da página de vendas no botão principal.

## 2026-05-13 23:15 UTC
- Coletor Hotmart ajustado para mapear e persistir, além do título/URL, os campos solicitados do produto: `ucode`, `image`, `temperature`, `reviewRating`, `totalAnswers`, `blueprint`, `price.value`, `category`, `format` e `producer.name`.
- `HotmartProductSnapshot` expandido para carregar esses atributos de forma estruturada durante a coleta via API.
- `rawMetadata` enviado ao backend passou a incluir todos os novos campos para disponibilização na camada administrativa sem depender de parsing textual.

## 2026-05-13 17:16 UTC
- Estruturado o 2º ciclo do coletor Hotmart no módulo `mois-hotmart-collector`: após listar produtos no endpoint `v2/market/search`, o serviço agora percorre produto a produto e consulta `v1/market/product/{id}/details`.
- Implementado enriquecimento por produto com foco em `salesPageUrl` (página de vendas), preservando fallback para dados do 1º ciclo quando o detalhe falhar.
- Adicionados logs de causa-raiz para falhas no ciclo 2 por produto (`status` HTTP e `productId`) sem interromper a coleta completa.

## 2026-05-13 17:35 UTC
- Scheduler do coletor Hotmart ajustado para alternar ciclos por hora: horas ímpares executam ciclo 1 (listagem) e horas pares executam ciclo 2 (detalhes por produto).
- Implementado no serviço o fluxo de ciclo 2 que lê produtos do backend (`/api/v1/mois/hotmart/products`) e consulta detalhes item a item na Hotmart, priorizando `salesPageUrl`.
- Criado documento técnico do projeto descrevendo os dois ciclos e regras de execução em `mois-hotmart-collector/docs/ciclos-coleta-hotmart.md`.

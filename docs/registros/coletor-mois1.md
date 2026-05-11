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

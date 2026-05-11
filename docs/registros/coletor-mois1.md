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

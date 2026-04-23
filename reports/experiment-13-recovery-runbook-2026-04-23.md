# Runbook — Recuperação do experimento 13 (retry não resolveu)

Data: 2026-04-23 (UTC)

## Situação

O experimento 13 falha na criação de criativo (`POST /adcreatives`) com erro da Meta `code=100` e `error_subcode=3858258` (“A imagem não foi baixada”).

Quando isso acontece, repetir o mesmo retry com `picture` por URL normalmente não resolve.

## Causa provável

A Meta não consegue baixar a imagem pela URL externa no momento da publicação (problema intermitente de fetch da rede da Meta para o host da imagem).

## Plano de recuperação imediata (operacional)

1. **Subir nova imagem** no criativo (arquivo novo, nome novo).
2. **Aguardar 2–5 minutos** para propagação no storage/CDN.
3. Validar URL com:
   - `curl -I <url>`
   - `curl -A "facebookexternalhit/1.1" -I <url>`
4. **Reprocessar o experimento 13 uma única vez**.
5. Se falhar novamente com `3858258`, parar retry por URL e aplicar fallback por hash:
   - fazer upload em `/{ad_account}/adimages`
   - criar criativo com `image_hash` (sem `picture`)

## Checklist de diagnóstico mínimo (antes de novo retry)

- Endpoint de falha: `POST /adcreatives`.
- `error_subcode` confirmado como `3858258`.
- URL acessível em `GET/HEAD` com status `200`.
- `content-type` válido (`image/jpeg` ou `image/png`).
- Sem redirecionamento quebrado/assinado expirado.

## Correção definitiva sugerida no produto

1. Tornar `adimages + image_hash` o caminho principal de publicação.
2. Deixar `picture` por URL apenas como fallback.
3. No primeiro `3858258`, alternar automaticamente para hash no mesmo ciclo.
4. Persistir no backend metadados da falha:
   - `error_subcode`
   - `fbtrace_id`
   - URL da imagem usada

## Mensagem curta para operação

> “No experimento 13, retry simples não resolve porque o erro é de download da imagem pela Meta (`3858258`). Vamos reenviar usando `image_hash` após upload em `/adimages`.”

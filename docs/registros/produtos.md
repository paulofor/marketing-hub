# Registro de evolução do catálogo de produtos

## 2026-08-23 — Identidade interna separada do nome comercial

- Evidência: o cadastro persistia somente `product.name`; produtos como “PDE Anti-Invisibilidade
  Profissional” e nomes de trabalho versionados disputavam o mesmo campo usado nas ofertas públicas.
- Causa-raiz: identidade de trabalho, nome comercial e nomes históricos não possuíam contratos
  separados, criando risco de duplicação do produto ou exposição de rótulo técnico ao cliente.
- Decisão: manter `id` e `slug` como identidade estável, separar nome interno e nome comercial e
  permitir até 20 apelidos internos únicos e pesquisáveis.
- Proteção comercial: apelidos não entram em definição pública, landing, checkout ou entrega; a
  resolução interna retorna o produto canônico que continua vinculado por `id` e `slug`.
- Critério: uma busca por qualquer nome deve localizar o mesmo cadastro, enquanto a comunicação
  pública usa exclusivamente o nome comercial.

## 2026-08-23 — Catálogo extensível de tipos de produto

- Evidência produtiva: nove registros usavam três rótulos distintos de tipo e três ainda estavam
  sem classificação; `product_type` era texto livre, sem cadastro ou prevenção de duplicidade.
- Alternativas comparadas: manter texto livre, fechar os tipos em enum ou criar catálogo extensível.
- Decisão: catálogo com código estável, nome, descrição, apelidos, estado e contagem de produtos.
- Liberdade de exploração: tipos novos podem nascer como `PROPOSED`; somente `ACTIVE` recebe novos
  produtos, e `RETIRED` preserva histórico.
- Compatibilidade: os três rótulos produtivos atuais são migrados de forma determinística; os dois
  rótulos que descreviam canal/formato viram apelidos dos tipos canônicos e o texto legado permanece
  disponível durante a transição.
- Prevenção: nome, código e apelidos são únicos entre tipos e a tela deixa de aceitar classificação
  livre no cadastro de produtos.

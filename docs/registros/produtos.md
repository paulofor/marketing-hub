# Registro de evolução do catálogo de produtos

## 2026-08-24 — Descoberta encerrada sem novo produto aprovado

- Processo executado: `pde-opportunity-discovery` v4, com Rigel 82/100 como benchmark fixo.
- Resultado: `PESQUISAR MAIS`; Pedido no Azul liderou duas rodadas finais com 73 e 70, sem alcançar
  o benchmark e com valor percebido de 72 nas duas execuções.
- Decisão de portfólio: nenhum produto, tipo, estrela, oferta ou experimento foi cadastrado. Pedido
  no Azul permanece somente como sinal prioritário, evitando transformar nota ou parecer em venda.
- Lacunas para reabrir a pesquisa: preferência observada frente à calculadora gratuita, intenção de
  pagar, conclusão sem assistência e rota orgânica atribuível.
- Evidência completa: `docs/marketing/descoberta-oportunidade-pde-2026-08-24.md`.

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

## 2026-08-23 — Universos dos nomes internos

- Decisão: produtos usam estrelas e tipos usam minerais, sem reutilização de codinomes.
- Produtos cadastrados em produção: Vega/Método MUSA, Sirius/Anti-Invisibilidade,
  Capella/Agenda Cheia, Altair/Especialista no WhatsApp e Rigel/Kit WhatsApp Pronto.
- Codinomes reservados para cadastro após publicar a atualização isolada: Polaris/Nexo,
  Antares/rascunho Personal Trainer, Spica/rascunho Manicure e Regulus/rascunho Autoridade e
  negociação.
- Minerais preservados na tela publicada como apelidos pesquisáveis: Opala/PDE, Quartzo/low-ticket,
  Safira/Produto IA, Fluorita/atendimento por sandbox, Ágata/área de membros e
  Labradorita/educação interativa. O cadastro no campo próprio aguarda a publicação da identidade
  interna de tipos e será concluído pela tela.
- Causa-raiz adicional: o formulário integral bloqueava produtos legados sem tipo e regravava todo o
  contrato mesmo quando somente o nome interno mudava; a atualização isolada passa a proteger os
  campos comerciais e permitir governar rascunhos sem inventar classificação.
- Prevenção adicional: o tipo passa a ter `internal_name` próprio; minerais deixam de depender de
  apelidos, aparecem como família interna nos produtos e continuam vedados nas superfícies públicas.

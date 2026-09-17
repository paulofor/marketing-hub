# Vega #92 — governança sintética e retomada comercial

## Escopo confirmado

- produto Vega, ID 4, tipo cadastrado PDE/Opala;
- cadeia histórica 14, ciclo 2, experimento 92;
- versão `musa-pde-entry-v12-primeiro-ajuste-aplicavel`;
- subprocesso Opala v1, definição 77, execução automática 8;
- estado inicial observado: 5/8 atividades concluídas, com Psique aguardando versão comercial,
  entrada aprovada e criativo aprovado;
- criativo corrente: #529, mídia aprovada #41, parecer de Têmis `REJECTED`.

## Causa-raiz e alternativas

O asset bruto #2794 registra Runway `gen4.5`, dois jobs externos, SHA-256 e prompts sem entrada
visual. O asset final #2804 registra SHA-256 e disclosure inseparável de apresentadora e voz
sintéticas. Mesmo assim, o contrato de governança exigia imagem de apresentadora, consentimento e
direitos de referência, como se essa produção tivesse usado Product UGC.

| Alternativa | Benefício | Risco/esforço | Decisão |
| --- | --- | --- | --- |
| Preencher campos ausentes no caso #529 | Liberação rápida | Fabricaria uma referência que não participou da geração e não corrigiria recorrência | Rejeitada |
| Dispensar referência em todo vídeo sintético | Mudança pequena | Poderia liberar Product UGC ou performance externa sem consentimento | Rejeitada |
| Distinguir geração por texto de geração com referência | Preserva a prova real e generaliza | Exige contrato v2, testes do backend e instrução de Têmis | Adotada |

O contrato v2 mantém `EXPLICIT_REFERENCE` com os gates anteriores. O novo modo
`PROMPT_ONLY_SYNTHETIC` é aceito somente com lista fechada de campos do request, personagem
sintética, disclosure no arquivo final, hashes, tarefas e modelo exato `ACTIVE` com adapter,
licença e quality gate verificados. Campo desconhecido resulta em `UNRESOLVED`.

## Matriz definida antes das rodadas finais

| Dimensão | Critério |
| --- | --- |
| Caminho feliz | Snapshot equivalente ao Vega #529 produz `VERIFIED` sem inventar referência ou consentimento |
| Referência explícita | Product UGC continua exigindo asset, origem, prompt, consentimento e direitos |
| Falhas | Disclosure ausente, modelo divergente e campo de referência inesperado mantêm `INCOMPLETE` |
| Integração | Backend serializa o contrato v2 e Têmis reconhece os dois modos sem aprovar por texto isolado |
| Observabilidade | Erros de payload preservam contexto e stack trace; tarefa e hashes seguem auditáveis |
| Segregação | Produto 4, experimento 92, projeto 4 e mídia #41 não aceitam evidência de outro escopo |
| Jornada comercial | Oferta v12, checkout Pepper, desktop, iPhone e Android permanecem verificáveis sem compra |
| Métricas e custo | Nenhuma chamada paga ou mídia nova; custo histórico permanece preservado |
| Processo | Revisão só pode ser repetida após backend e Têmis publicados; aprovação humana continua obrigatória |

## Resultados

### Recuperação segura da candidata

- o runtime oficial de `v8.clubemusa.com.br` passou a responder a imagem v8 e a versão
  `musa-pde-entry-v12-primeiro-ajuste-aplicavel`;
- contrato público, oferta do experimento 92, preço único de R$ 67 e checkout Pepper `owm6x`
  responderam HTTP 200;
- pelo frontend administrativo, o comando **Testar URL** comprovou health, contrato, oferta,
  integração, entrada, copy e HLS; em seguida, **Preparar para publicação** promoveu somente o
  slot v8 de `CANDIDATE` para `READY`;
- `publishedExperienceJson` e `publishedAt` permaneceram nulos: nenhuma publicação comercial,
  campanha, cobrança ou gasto foi antecipado;
- navegação pública somente de leitura foi confirmada sem erros de console em desktop, iPhone 15
  Pro e Pixel 7, com primeiro ajuste e preço de R$ 67 visíveis.

### Correções sistêmicas locais

- a governança de mídia v2 separa referência explícita de geração sintética somente por texto e
  continua bloqueando referência inesperada, disclosure ausente, modelo divergente, tarefa ausente
  ou licença não verificada;
- Têmis passa a avaliar o modo real da produção sem exigir consentimento de uma apresentadora que
  nunca existiu e sem dispensar consentimento quando uma pessoa ou imagem externa participou;
- o gate comercial passa a reconhecer uma única candidata exata, validada e com contrato de revisão,
  sem exigir `publishedAt` antes de Psique e Têmis;
- a seleção exige o mesmo produto, experimento e versão e rejeita URL divergente, duplicidade,
  contrato ausente ou validação incompleta;
- os aprendizados foram registrados nos dois cânones relacionados e em `docs/registros/loops.md`.

### Homologação final

Foram concluídas duas rodadas completas consecutivas depois da última alteração. Em cada rodada:

- backend principal: 3.221 testes, zero falhas e 17 ignorados previstos;
- Têmis: 100 testes, zero falhas e 1 ignorado previsto;
- Psique: 113 testes, zero falhas e 1 ignorado previsto;
- backend PDE: 182 testes, zero falhas;
- frontend administrativo: 703 testes, typecheck e build aprovados;
- frontend PDE: typecheck, validação de ativos e build aprovados;
- topologia local com MySQL 5.7: 30 cenários Playwright aprovados em Chromium, iPhone 15 Pro e
  Pixel 7, incluindo v12, checkout, acesso, eventos, compra simulada e reembolso sem duplicidade;
- proxy versionado: Vega v7, Vega v12 e Mira permaneceram isolados, inclusive após recriação de
  container;
- `git diff --check` aprovado e topologia temporária removida ao final.

### Estado real após a validação

O subprocesso Opala histórico continua em produção com 5/8 atividades concluídas. Psique, Têmis e
a consolidação ainda não foram executados porque backend e worker publicados continuam com os
contratos anteriores. O criativo #529 permanece `DRAFT` e `REJECTED`; o parecer e seu custo foram
preservados e não houve nova chamada paga. O experimento 92 permanece `PLANNED`, como exigido até a
campanha ser confirmada externamente.

## Limite operacional

As correções de governança e preflight permanecem locais até passarem por Pull Request. Portanto, a
tarefa de Têmis não deve ser repetida em produção antes dessa publicação: o contrato antigo
reprovaria novamente e consumiria outra inferência sem chance de sucesso. Depois do deploy, o #529
deve receber uma única nova revisão; aprovação do parecer não substitui a aprovação humana do
criativo, nem autoriza orçamento, publicação ou mídia.

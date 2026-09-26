# Matriz de homologação — identidade do produto no Processo 2

## Objetivo

Comprovar que um produto novo recebe nome interno e tipo decididos por Atena antes da economia e da
construção, sem alterar identidade de produtos existentes nem tratar cadastro como resultado
comercial.

## Alternativas avaliadas

| Alternativa | Benefício | Risco/custo | Decisão |
|---|---|---|---|
| Corrigir somente o produto #11 | Menor esforço imediato | Mira já comprovou que o defeito reaparece | Rejeitada |
| Fixar nomes e tipos em enum no worker | Validação simples | Engessa descoberta e duplica o catálogo | Rejeitada |
| Contrato versionado + catálogo ativo + validação backend | Fecha a origem e preserva flexibilidade | Esforço moderado em backend, worker e tela | Escolhida |

## Cenários locais obrigatórios

| Área | Cenário | Aceite |
|---|---|---|
| Caminho feliz | Ciclo autônomo v9 com Atena `APPROVE` | `PRODUCT_IDENTITY_V1/CREATE` chega ao produto, ao contrato e ao relatório |
| Validação | Nome provisório, nome ocupado, tipo ausente/inativo ou mineral divergente | Materialização bloqueada antes de criar produto |
| Compatibilidade | Tarefa v8 ainda pendente | Usa prompt/schema legados e preserva o comportamento histórico |
| Produto existente | Planejamento inicial ou sucessor v9 | `PRESERVE` coincide exatamente com nome e tipo atuais |
| Integração | Atena → Plutus → Dédalo → materialização | Tipo escolhido orienta os sucessores sem permitir redefinição |
| Observabilidade | Falha de contrato ou persistência | Log contém tarefa e referência; relatório mostra identidade final persistida |
| Migração | Processo v8/cadeia v22/produto #11 no MySQL 5.7 | Publica v9/v23 e corrige Alcyone/Safira sem erro 1093 |
| Métrica | Produto materializado | Continua `PLANNED`, `STOP`, sem campanha, compra, receita ou margem inventada |
| Segregação | Dados de teste | IDs sintéticos não tocam produtos reais; reparo produtivo é limitado ao produto #11 da execução #32 |
| Desktop | Chromium 1440×900 | Execução mostra nome comercial, Alcyone, Safira e link do produto |
| Celular | iPhone 15 Pro e Pixel 7 | Identidade permanece legível, sem overflow e com link acionável |

## Evidências esperadas

- testes unitários completos do backend e do `experiment-strategist-worker`;
- testes e build do frontend;
- validação estática e fixture física dedicada do changelog no MySQL 5.7, incluindo idempotência,
  rollback e reaplicação;
- captura local desktop, iPhone 15 Pro e Pixel 7 do relatório com resposta simulada;
- após o merge, processo v9/cadeia v23 publicados e execução #32 exibindo Alcyone/Safira.

## Resultado local — 26/09/2026

- Backend: 3.616 testes registrados em 594 classes, sem falha ou erro no conjunto final de
  relatórios. A suíte integral encontrou inicialmente a ausência dos dois artefatos v9 no catálogo
  de harness; o manifesto foi corrigido e `AgentHarnessCatalogTest` passou na repetição isolada.
- Worker de Atena: 42 testes em sete classes, sem falha ou erro; o consumer cobre criação,
  preservação, nome ocupado, rótulo provisório e tipo divergente.
- Frontend: suíte completa com 180 arquivos e 784 testes aprovada; após o ajuste final do contrato
  de relatório v3, os 23 testes da tela foram repetidos, seguidos de `typecheck` e build aprovados.
- MySQL 5.7: fixture física aplicou os dois changesets, confirmou Alcyone/Safira, reaplicou sem
  duplicar, executou os dois rollbacks e reaplicou com sucesso. A topologia temporária foi removida.
- Liquibase e automação: validação estática, `bash -n`, ShellCheck, Actionlint e `git diff --check`
  aprovados.
- Empacotamento: JAR executável aprovado com 4.169 classes idênticas às testadas, 664 recursos
  externos íntegros e 438 cartões do catálogo inicializados.
- Visual: Chromium desktop 1440×900, iPhone 15 Pro e Pixel 7 exibiram nome comercial, nome interno,
  tipo e link do produto sem overflow horizontal ou erro de console.
- Segregação: banco e respostas usados na homologação eram sintéticos; nenhum produto, campanha,
  venda, receita ou gasto real foi criado localmente.

# Homologação local — expectativas e surpresa positiva v1

Matriz definida antes de executar testes. Sem produto selecionado, sem tarefas pagas ou campanhas.

| Área | Critério de aceite | Verificação |
|---|---|---|
| Pesquisa | Sete referências iniciais e quatro complementares, desenho e limites separados de hipótese | Catálogo real carregado e fontes HTTP/DOI documentadas |
| Planejamento | Quatro superfícies, três variantes contextualizadas, prova ausente explícita | Testes HTTP controller + serviço real |
| Entradas | Campos vazios, enum inválido, texto excessivo recusados | Bean Validation / HTTP local real |
| Harness | Contexto relevante recebe protocolo; contexto não relacionado preservado; curadoria e limite respeitados | Seleção real de catálogo, dois contextos e fluxo legado |
| Medição | Produto/ciclo segregados; ausência não vira zero; bloqueio recente prevalece | Repositórios como doubles, serviço real e contrato de consulta |
| Aprendizado | Compras sem contribuição/uso não confirmam; resultado positivo não implica causalidade; versão antiga rejeitada | Casos de regressão determinísticos |
| Interface | Prévia, erros, carregamento, fontes, aprendizado e navegação | Vitest e Chromium desktop/iPhone 15 Pro/Pixel 7 |
| Integração | Contrato HTTP real da capacidade no backend e interface consumindo as respostas | Homologação local com fontes de medição sintéticas isoladas |
| Observabilidade | Fingerprint inclui entrada/protocolo; fonte/evento/ciclo visíveis; erro de parsing correlacionado | Testes de resposta e auditoria |
| Comercial | Nenhum teste é evidência de mercado; sem publicação, orçamento, decisão de ciclo ou inferência paga | Repositórios sem writes, commercialAuthorization=false e causalConclusion=false |

A prévia é consultiva e não persistida. Planos aprovados continuam nos artefatos das tarefas e no brief dos ciclos existentes; não há uma segunda memória ou fila. A medição é somente leitura de recibos existentes, sem coletar novamente nem acionar conciliação.

Comando de aceite reproduzível com TAP: `node --test --test-reporter=tap scripts/test-consumer-expectations.mjs`.
Executa suíte unitária completa do backend, HTTP real da capacidade, consulta de persistência H2,
Chromium desktop/iPhone/Pixel, testes da interface, TypeScript e build. O navegador usa dados de
ciclos sintéticos e encaminha apenas `/api/consumer-expectations/v1` ao backend efêmero; não usa
endpoints publicados. O teste browser é opt-in no Maven comum (`-Dexpectation.browser=true`) e
obrigatório nesse comando de aceite local. WebKit/Safari nativo não são cobertos pela emulação.

## Execução em ambiente limitado

A sandbox disponibilizou 3,5 GiB durante a rodada. Manter o compilador, a JVM padrão de 3 GiB,
contextos Spring acumulados e o navegador juntos causou pressão de memória e timeout na compilação
auxiliar de Dédalo. O comando de aceite separa compilação, suíte geral, arquitetura e navegador;
limita heap somente nos processos locais, reduz o cache de contextos e reinicia a JVM a cada
30 classes. Não altera POM, asserts, contratos nem testes de arquitetura. O build frontend usa
heap de 2304 MiB somente no processo local: o padrão de cerca de 1,8 GiB esgotou a memória na
geração dos chunks. O build de produção completo passou com o ajuste, preservando minificação
e sourcemaps. A compilação auxiliar de Dédalo também passou após isolamento.

Os três testes TAP são independentes: prova funcional; tipagem/build; regressão/arquitetura.
É possível selecionar a etapa com `--test-name-pattern` e repetir apenas a validação afetada.
Nesta execução, TypeScript passou pelo comando `npm --prefix frontend run typecheck`, e o build
por `NODE_OPTIONS=--max-old-space-size=2304 npm --prefix frontend run build`. A prova funcional e
regressão são registradas por `node --test --test-reporter=tap --test-name-pattern='ciencia|regressao' scripts/test-consumer-expectations.mjs`.

O runner remove `NODE_TEST_CONTEXT` dos subprocessos para que testes Node internos não sejam
ignorados como recursivos. Exige TAP positivo do navegador e preserva logs enquanto cada processo
executa. Uma etapa ignorada, interrompida ou sem resultado não conta como prova funcional.

## Resultados observados

- Capacidade e biblioteca: 40 testes Java, incluindo encaminhamento das seis dimensões; a prova
  de navegador é executada em JVM própria. A repetição focal cobre o ajuste lexical posterior à
  regressão geral, sem repetir módulos não afetados.
- Persistência: consulta JPA real em H2 confirma ordenação, bloqueio recente e segregação de ciclos.
- Interface: sete testes Vitest, TypeScript e build de produção aprovados.
- Navegador: HTTP real e Chromium desktop/iPhone 15 Pro/Pixel 7 aprovados; imagens inspecionadas,
  sem rolagem horizontal ou erros JavaScript. iPhone/Pixel são emulações Chromium.
- Catálogo existente: 394 cartões validados, Bash/ShellCheck e contrato do workflow frontend aprovados.
- Atestações existentes: 865 arquivos e 228 manifestos comerciais conferidos sem modificar controles.
- Regressão geral: 6.123 casos processados, zero falhas e zero erros. Inclui 92 verificações da
  arquitetura central aprovadas. Os 85 casos condicionais ignorados na rodada padrão dependem de
  ambientes/flags próprios; o navegador desta capacidade é executado separadamente e passou.
  Integrações MySQL opt-in de outros módulos não são apresentadas como executadas localmente.
- Revisão final da medição: referência nula, numérica ou vazia não pode identificar uma fonte válida;
  esses cenários integram a prova funcional repetida após o ajuste restritivo.

As imagens e logs locais estão em `.codex/expectation-evidence`, declarados no manifesto de entrega.
Nenhuma medição sintética foi enviada ao ambiente publicado; nenhum resultado financeiro foi inferido.

## Pendência global anterior à entrega

A [execução 45608014436](https://github.com/paulofor/marketing-hub/actions/runs/45608014436),
da revisão `890d140afe918d93fa1a4aa8f602e8cd4b587961`, aprovou os contratos dos cartões e
falhou na auditoria global por 41 branches de pesquisa pendentes. A comparação posterior com
`47aa3545cab7176102592861e06da29222700396` confirmou as mesmas 41 pontas e 119 arquivos
com conteúdo diferente da main. Isso antecede esta capacidade; não é evidência de regressão dela.

O acervo inclui relatórios e cartões de outras pesquisas e exige revisão própria antes de
incorporação. Os controles da auditoria e as branches históricas foram preservados. Esse estado
deve permanecer explícito ao acompanhar os workflows da entrega, sem declarar a auditoria verde
nem confundir saúde da aplicação com integração de todo o acervo.

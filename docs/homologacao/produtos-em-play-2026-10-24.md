# Produtos em play — homologação

## Escolha e evidência

Alternativas: combinar várias consultas no navegador (baixo esforço, risco de divergência);
mostrar a última tarefa (leve, ignora decisões e subprocessos); consolidar contratos oficiais
no backend (esforço moderado, mantém contexto e estados). Adotada a terceira.

O banco em 24/10 confirma quatro produtos em PLAY: Alcyone, Capella, Mira e Vega. Pais em
WAITING_SUBPROCESS têm filhos em WAITING_INPUT. Os ciclos usam cadeia 26 e o catálogo já
está na 27; a leitura deve preservar o contexto original. PLAY não significa execução.

## Matriz de homologação definida antes dos testes

| Área | Aceite local |
| --- | --- |
| Principal | Somente PLAY, nome, atividade, responsável, estado, contexto e link oficiais. |
| Estados | Execução, fila, bloqueio, espera humana, pausa, conclusão e indisponibilidade distintos. |
| Integração | Query JPA em H2, serviço e endpoint HTTP; frontend consome o contrato serializado. |
| Isolamento | Caso de Vega, outra identidade, caminho válido, referência/ciclo/cadeia preservados. |
| Falhas | Filhos cruzados/circulares, erro parcial, erro total, atualização falha, vazio e carregamento. |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; menu, motivos, links, sem overflow ou erro de console. |
| Acessibilidade | Texto e ícones além de cores, foco, teclado, contraste e movimento reduzido. |
| Observabilidade | Horário da consulta/conciliação; log contextual sem expor erro interno na tela. |
| Segurança/métricas | Apenas GET; nenhuma inferência, campanha, cobrança ou mutação; fixtures sintéticas. |

## Melhoria reutilizável

O contrato `product.operations.v1` distingue automação permitida e execução real, segue o
filho persistido e usa a atividade atual para não repetir bloqueios antigos. Hipótese:
reduzir consultas manuais e diagnósticos incorretos sobre os agentes. Adoção condicionada
a regressões de contexto/estado aprovadas; rever se divergir das fontes. Sem alegação de
melhora comercial medida.

## Resultado

Validação local aprovada: serviço/HTTP, consulta JPA em H2, navegação de ciclos, arquitetura,
componentes React, typecheck e build. Chromium desktop, iPhone 15 Pro e Pixel 7 passaram com
o JSON produzido pelo teste HTTP do backend, verificando estados, detalhes, links, falha de
atualização, recuperação, vazio, ausência de mutações e de overflow. São testes sintéticos.

A rodada identificou a necessidade de explicitar ISO-8601 nos horários do novo contrato;
o ajuste e a regressão foram aplicados antes de publicar. O teste visual também passou a
interceptar somente caminhos `/api/`, preservando módulos JavaScript de `/src/api/` no Vite.
Spotless, Prettier, actionlint e `git diff --check` conferidos. Evidências locais em
`artifacts/products-in-play`, sem versionar dados reais. Evidências da publicação, versão e
conferência da tela ficam registradas no PR desta entrega.

# Performance do painel — 28/10/2026

## Escopo e evidência inicial

Solicitação: investigar dificuldade de uso no frontend/backend e corrigir consultas/índices
quando comprovado. Nenhum produto, orçamento, tarefa de agente ou campanha foi alterado.
Base analisada/publicada: `04d6f7007698e55c7fc709fc01c838496bea5177`.

Medições de navegação em Chromium e chamadas HTTP do mesmo ambiente:

| Consulta | Antes da correção |
| --- | --- |
| HTML administrativo | 0,36 s |
| Resumo global de vídeos | 8,39–13,14 s; chamada isolada 11,31 s / 162 bytes |
| Posição dos produtos em PLAY | 6,14–8,09 s |
| Produtos em play | 5,23 s isolado; carregando além de 15 s junto da navegação |
| Monitor de agentes | não concluiu nos 15 s da navegação; chamada isolada expirou após 90 s |

São amostras de produção sob carga variável, não percentis nem benchmark controlado.
MCP confirmou MySQL acessível e ausência de espera por locks nas amostras. `performance_schema`
mostrou consultas integrais de auditoria e varreduras frequentes de GeraLanding. Os contadores
acumulados não foram tratados como taxa atual. Host com 1,96 GiB de RAM, 164 MiB disponíveis,
520 MiB de swap ocupados e backend em aproximadamente 1,32 GiB; frontend Nginx em cerca de 2 MiB.

## Decisão técnica

Alternativas avaliadas: aumentar capacidade (alívio, custo permanente e causa preservada),
cachear respostas no backend (menor leitura, risco de esconder gates/decisões atuais) ou
reduzir dados consultados e indexar a fila (escolhida: mantém verdade atual com menos trabalho).
Não foi necessário trocar hosts nem modificar a frequência dos executores.

## Correção reutilizável e critérios de aceite

- Monitor usa a última tarefa, inclusive concluída, com limite no SQL e sem prompts/resultados.
- Contadores de vídeo preservam classificação, linhagem, escopo e revisão independente atual.
  Não carregam DTOs/auditorias de produções históricas para contar estados.
- Processo consultado por referência usa as ocorrências exatas e evita histórico de experimentos
  no resumo; navegação histórica sem referência permanece disponível.
- Índice MySQL 5.7 por `stage_code,status,execution_requested_at`, online, reversível e sem
  reescrever changesets antigos. Não há alteração de dados de negócio.
- Resumo no navegador respeita invalidação, escopo e atualização de 30 s; consultas abandonadas
  são canceladas. Não há inferência de estado pelo frontend.

## Homologação local

Matriz: estados atuais/terminais, histórico e linhagem, isolamento de produto e experimento,
consulta vazia, gate bloqueado, decisão opcional, atualizações e invalidação no navegador;
auditorias sintéticas extensas e limite SQL; MySQL 5.7 com reaplicação e EXPLAIN;
navegação em computador, iPhone e Pixel emulados. Apenas dados sintéticos, sem inferência,
pagamento ou tráfego comercial. Resultados confirmados: 92 testes Java de regras/persistência (monitor, resumo e política de vídeos,
execução BPM, repositórios e produtos em play), 14 testes de frontend e uma homologação HTTP
integrada em desktop/iPhone/Pixel, sem chamadas externas. As projeções mantiveram zero entidades
JPA hidratadas mesmo com grandes campos sintéticos. A revisão atual continua chamando o gate
independente uma vez por peça. Os 92 testes de arquitetura passaram. A fixture MySQL 5.7 aplicou e reaplicou o índice;
o EXPLAIN selecionou o índice e não usou filesort nos 10 mil itens sintéticos. A topologia
foi removida com volumes ao final. Total Java relevante: 185 testes, sem falha final.

## Limites

Testes demonstram comportamento e redução do trabalho consultado; não medem aumento de vendas.
Medições após publicação serão comparadas sem prometer latência constante. Memória do host
continua merecendo acompanhamento após retirar as leituras evitáveis.

Limitação local observada: a sandbox tem 3 GiB efetivos. Compilar com source maps enquanto
outras ferramentas ocupam memória excedeu esse limite; o build funcional de homologação usa a
mesma configuração, sem mapas de depuração nem cálculo do tamanho comprimido. O build padrão
versionado permanece inalterado e será conferido também pelo CI. As validações pesadas foram
serializadas. Como o builder Liquibase também esgotou memória isoladamente, a compilação da
fixture passou a ter heap limitado e configurável (768 MiB por padrão), sem alterar a JVM
produtiva.

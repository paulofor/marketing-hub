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

## Conferência publicada e passagem indireta do ciclo

O PR #5735 foi integrado em `1c63bd9137df60d85be198e395dafe8fdca981b8`.
Backend e frontend confirmaram esse SHA e saúde normal; os 17 workflows da main
e os encadeados pela publicação correspondente terminaram com sucesso.
O índice foi conferido fisicamente no MySQL, com as três colunas na ordem prevista.

Em duas amostras após a instalação, o monitor respondeu em 0,71–0,77 s e o resumo
de vídeos em 1,01–1,02 s. As posições dos produtos ficaram em 3,42–3,66 s e produtos
em play em 3,34–4,53 s. Não são percentis nem promessa de latência constante.
Na consulta recorrente da fila, o intervalo anterior tinha 153 execuções, média de
1.086,83 linhas examinadas e 481,34 ms. No intervalo posterior, dez execuções
examinaram 12 linhas em média e levaram 52,51 ms, devolvendo quatro resultados.
São contadores diferenciais de produção, não taxa inferida de acumulados históricos.

A navegação mostrou uma passagem adicional: o contexto de Vega no card levava
9,14 s; a chamada isolada confirmou 12,82 s. O resolvedor de próximo trabalho
usava a sobrecarga com auditorias. Além disso, o resumo só adotava projeções
compactas quando recebia `sourceReference` explícita, mesmo havendo `learningCycleId`
com identidade canônica suficiente. A integração local reproduziu essa leitura
integral mesmo depois de desligar a auditoria no resolvedor.

Alternativas: cachear o contexto (risco de esconder decisão recente), criar outro
endpoint (duplicaria o contrato) ou completar o uso da projeção existente. A terceira
foi escolhida: o resolvedor pede estado sem auditoria e o serviço resolve a referência
oficial do ciclo antes de consultar tarefas e ocorrências. Referência divergente,
consulta histórica e auditoria solicitada continuam com seus contratos anteriores.

O ajuste passou em 85 testes locais: continuidade, contexto, cadeia publicada,
revalidação e execução HTTP com service/gates reais e dependências simuladas.
O teste integrado exige a projeção leve e proíbe consultar tarefas completas ou
todo o histórico de experimentos na resolução do próximo trabalho. Os cenários
cobrem Vega, Capella e identificadores sintéticos distintos; bloqueios e seleção
humana permanecem preservados. A melhora desta segunda passagem deve ser medida
após a publicação correspondente.

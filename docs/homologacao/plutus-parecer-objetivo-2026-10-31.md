# Parecer financeiro curto no ciclo

## Evidência e escolha

Capella #112 possui Plutus #883 concluído, mas a demonstração consultava somente o plano
comercial, ainda ligado ao experimento #105. A tela omitia a avaliação do planejamento.
A estratégia #886 é posterior: os números de #883 só podem ser referência a reavaliar.

Alternativas: apenas encurtar o texto (baixo esforço, mantém o vazio); gerar novo parecer
a cada consulta (gasto e repetição desnecessários); reutilizar fontes persistidas com resumo
numérico e validade explícita (escolhida, resolve a lacuna sem nova inferência).

## Matriz local, definida antes dos testes

| Caso | Aceite |
| --- | --- |
| Parecer de plano vigente | Recomendação, preço/custo/sobra e fonte; detalhes recolhidos |
| Capella: planejamento anterior à estratégia | Números de #883 visíveis como referência; nenhuma aprovação vigente |
| Outra identidade e experiência válida | Mesmo comportamento sem exceção por nome ou ID |
| Tentativa atual bloqueada ou em andamento | Não recuperar aprovação antiga como atual |
| Dados ausentes, inválidos, obsoletos | Ausência explícita, sem zeros inventados ou promessa de retorno |
| Escopo e moedas | Preparação em USD separada da venda em BRL; hipótese interna não autoriza mídia |
| Desktop, iPhone e Pixel | Resumo legível, expansão dos detalhes, sem overflow |
| Recuperação e orçamento | Consulta não grava, não inicia IA; aprovação existente mantém idempotência |

Dados locais são sintéticos e não entram em métricas comerciais. A melhoria é reutilizável
por Plutus e pelos produtos. Sucesso técnico: parecer localizável, legível e fiel à fonte.
Impacto em vendas depende de medição posterior; nenhuma nova inferência é necessária.

## Resultado local

- Backend: 49 testes relacionados aprovados (19 do resumo, 14 do endpoint/recibo,
  13 de consultas JPA e três de composição), além de 92 regras de arquitetura.
- Plutus: os 90 testes do módulo passaram, incluindo o prompt realmente composto.
- Frontend: 27 testes relacionados e TypeScript passaram; navegação validada em Chromium
  desktop, iPhone 15 Pro e Pixel 7 emulados, com parecer comercial e referência anterior sem plano.
- A consulta lê uma projeção e não carrega entidades de auditoria. O banco publicado já possui
  `idx_agent_task_source_reference`; nenhuma migração é necessária.
- A prévia mantém o texto integral recuperável, não converte dados ausentes em zero e distingue
  pendência em execução, análise não solicitada, interrupção e parecer negativo vigente.
- Aprovação de orçamento: formulário e replay existentes preservados, somente com dados locais.
  Não houve nova inferência, autorização ou campanha nos registros produtivos.
- Build do frontend: minificação e TypeScript aprovados em container isolado, sem source maps.
  A geração dos mapas completos excedeu a memória disponível (Node e container, saída 137);
  essa limitação local é explícita e o CI continua gerando os mapas completos.
- Spotless, diff e montagem de evidências comerciais passaram. A concorrência inicial entre
  compilação e testes excedeu a memória local; a validação Java foi concluída em etapas sequenciais.

Critério de adoção: fonte exata e números corretos com resumo legível, preservando os controles
financeiros e as pendências. Reverter a apresentação se ela ocultar uma falha atual, misturar
produtos/versões ou sugerir que uma hipótese autoriza gasto. A instrução de brevidade orienta
as próximas respostas; não rejeita nem repete pareceres históricos apenas pelo tamanho.

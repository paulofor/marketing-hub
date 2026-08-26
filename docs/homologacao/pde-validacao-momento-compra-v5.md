# Matriz de homologação — Validação do Momento de Compra PDE v5

## Objetivo e decisão de arquitetura

Inserir uma etapa obrigatória entre a definição da rota de distribuição e a priorização final da
oportunidade. Nenhuma candidata B2C/Instagram pode ser comparada com Rigel apenas por artigos,
ofertas, anúncios, intenção declarada ou score de modelo. A candidata também deve transformar afeto
e pertencimento, reconhecimento ou alívio de esforço em um resultado pronto, sem exigir que o
cliente aprenda ou monte a solução com IA.

Foram comparadas três alternativas:

| Alternativa | Benefício | Risco | Esforço | Decisão |
| --- | --- | --- | --- | --- |
| Reforçar somente prompts | rápida | o modelo ainda pode transformar narrativa em validação | baixo | rejeitada |
| Gate determinístico sobre evidências e eventos persistíveis | impede score sem comportamento observado e preserva a arquitetura atual | exige protótipo privado e duas leituras | médio | escolhida |
| Criar outro pipeline completo | máxima separação | complexidade e custo antes de provar necessidade operacional | alto | adiada |

O backend continua sendo a autoridade do avanço. Os agentes podem pesquisar, desenhar a experiência,
avaliar percepção e revisar risco, mas não podem fabricar métricas nem liberar a priorização.

## Gargalo e critérios

- **Gargalo:** oportunidades chegam ao score final sem prova observada de microvalor, uso do resultado
  pronto, preferência sobre a alternativa gratuita e avanço ao checkout.
- **Evidência:** Entrevista sem Branco ficou em 75–78/100 por intenção de compra, diferenciação,
  distribuição e esforço percebido ainda não comprovados.
- **Métrica esperada:** 100% das candidatas comparadas com Rigel possuem fontes atuais e nominais,
  critérios declarados antes do teste e duas leituras privadas consecutivas que os atendem.
- **Continuar:** as duas leituras alcançam microvalor, uso do resultado pronto sem montagem,
  preferência e avanço ao checkout, sem bloqueio de Psique ou Têmis.
- **Ajustar:** há uso, mas uma das taxas pré-declaradas não é atingida.
- **Parar:** a alternativa gratuita vence, as fontes são inválidas ou surge risco não controlável.

## Matriz ponta a ponta

| Área | Cenário | Evidência esperada | Critério |
| --- | --- | --- | --- |
| Caminho feliz | fontes válidas e duas leituras do protótipo | gate `PASS` antes de Dédalo comparar com Rigel | todas as taxas pré-declaradas atendidas nas duas leituras |
| Qualidade das fontes | artigos e ofertas atuais, nominais e aderentes | inventário por coleção, data, status e itens inválidos | placeholder, identidade incompleta, ausência de preço e tração, vazio, indisponível ou vencido bloqueia |
| Coleção dinâmica | novo Markdown em `pesquisas/momentos-de-compra-b2c` | arquivo descoberto na execução seguinte, com caminho, hash e data | nenhuma lista de artigos fica congelada |
| Cena de compra | prazo, consequência, orçamento, tentativa e gasto atual | contrato estruturado por candidata | nenhum campo essencial vazio |
| Território humano | afeto e pertencimento, reconhecimento ou alívio de esforço | território e duas fontes independentes da candidata | popularidade presumida ou desejo sem fonte bloqueia |
| Entrega pronta | experiência devolve resultado final utilizável | artefato, entrada mínima, até cinco passos e limite de automação | prompting, conhecimento de IA ou montagem manual bloqueia |
| Alternativa gratuita | Google, ChatGPT, planilha, amigo ou conteúdo | alternativa e vantagem específica do protótipo | preferência é observada, não inferida |
| Protótipo privado | experiência limitada e consentida | identificador, modo privado, critérios anteriores ao teste e eventos segregados | não publica, não gasta e não usa métricas humanas de produção |
| Leitura 1 | participantes elegíveis usam o protótipo | início, microvalor, `READY_RESULT_USED`, preferência e checkout com denominadores | critérios atingidos sem bloqueio |
| Leitura 2 | nova leitura independente | mesmos eventos e critérios | segunda leitura também aprovada |
| Inconsistência | apenas uma leitura aprova | status `ADJUST` | Dédalo não compara com Rigel |
| Gratuito vence | preferência fica abaixo do critério | status `STOP` | candidata não avança |
| Resultado não usado | saída é gerada, mas exige montagem, novo prompt ou ferramenta externa | taxa de uso pronto abaixo do mínimo | status `ADJUST`; Dédalo não pontua |
| Critério nulo | taxa mínima de `READY_RESULT_USED` é zero | contrato inválido | backend bloqueia; zero uso nunca aprova |
| Psique/Têmis | revisão detecta esforço, manipulação ou promessa indevida | decisões persistidas por leitura | qualquer bloqueio impede avanço |
| Score | modelo tenta aprovar candidata sem gate | erro de contrato | score não contorna fatos observados |
| Autoridade | callback tenta aprovar sem duas leituras | backend recusa a conclusão | worker não controla o avanço |
| Observabilidade | execução local e modelo | request, response, gate, taxas, motivos e custo | ausência de dado permanece ausência |
| Métricas | teste abre checkout sem pagamento | evento marcado como protótipo privado | nunca conta como venda ou receita |
| Segregação | dados locais ou consentidos | marcador explícito e correlação por ciclo | não contamina métricas humanas/produtivas |
| Desktop/mobile | rascunho v5 e nova etapa na tela de processos | Chromium desktop, iPhone 15 Pro e Pixel 7 | leitura e edição funcionam nos três perfis |

## Política de rodadas

A primeira rodada local completa encerra a homologação se não revelar defeitos. Se revelar qualquer
defeito e houver correção, a contagem reinicia e serão exigidas duas rodadas locais completas e
consecutivas sem falhas depois da última alteração.

## Execução da matriz

Um teste de contrato detectou inicialmente uma expressão regular frágil na leitura dos prompts. Na
revisão posterior do diff, foi detectada também uma divergência entre os nomes do gate local e os
campos esperados pelo callback do backend. A correção unificou o contrato e fez o backend recalcular
as leituras, em vez de confiar somente nos booleanos enviados pelo worker. Na ampliação para valor
humano e entrega pronta, a checagem inicial encontrou uma diferença de formatação na classe Java e
a revisão de causa-raiz identificou que um limiar zero ainda poderia dispensar o uso observado do
resultado e manter o nome da candidata na lista interna de elegíveis, apesar do bloqueio global. O
backend e o executor passaram a exigir limiar positivo, e a lista só é preenchida quando o gate
completo está válido; depois dessa última correção, a contagem definitiva foi novamente reiniciada
conforme a política acima.

- **Rodada definitiva 1:** 50 testes do executor local, 59 testes do
  `product-discovery-worker` e 1.877 testes do backend aprovados; dois testes preexistentes do backend
  permaneceram explicitamente ignorados. O rascunho v5 com a etapa de entrega pronta foi validado em
  Chromium desktop, iPhone 15 Pro e Pixel 7, sem erro de página ou resposta HTTP 5xx.
- **Rodada definitiva 2 consecutiva:** os mesmos 50 testes do executor, 59 testes do worker e 1.877
  testes do backend foram aprovados, novamente com apenas os dois testes preexistentes explicitamente
  ignorados. Os três perfis de navegador repetiram o caminho sem falhas.
- **Efeito comercial da homologação:** zero pagamentos, zero vendas, zero mídia e zero publicação. Os
  eventos do protótipo permanecem segregados das métricas humanas e comerciais.

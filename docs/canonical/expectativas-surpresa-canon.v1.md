# Expectativas e surpresa positiva — v1

Capacidade transversal `CONSUMER_EXPECTATION_V1`, incorporada em 25/11/2026 a pedido do usuário.
Não altera responsabilidades, cadeia histórica, autorização de gasto ou gates de publicação.

## Ciência e limites

O catálogo versionado fica em `backend/ads-service/src/main/resources/consumer-expectation/v1/protocol.json`;
as onze fichas da biblioteca estão em `pesquisas/expectativas-surpresa`. Cada fonte declara desenho,
achado, limite e nível de acesso obtido. Os sete estudos iniciais foram confrontados com pesquisas
sobre delight, curiosidade, música e repetição. Parte da literatura só disponibilizou resumo nesta
consulta: não foram fabricados tamanhos de efeito, amostras ou conclusões de textos não acessados.

Erro de previsão de recompensa não é um medidor de dopamina do cliente. Interesse, prazer,
satisfação, memória, intenção e compra são desfechos diferentes. Incongruência moderada é uma
hipótese condicionada por familiaridade e possibilidade de compreender; não é um ótimo universal.
Originalidade deve estar ligada a adequação e utilidade. Na cobrança, no acesso e na recuperação
é desejável preservar previsibilidade. Não maximizar estranheza nem esconder condições para
produzir surpresa. Não reduzir artificialmente expectativas nem criar recompensas compulsivas.

## Integração operacional

- Argos: evidência pública datada, contexto, linguagem, comportamento observável e contrapontos.
  Expectativa declarada pelo operador ou inferida por agente permanece hipótese, não fato do público.
- Atena: compara controle familiar, descoberta guiada e quebra de expectativa explicada; escolhe
  hipótese contextual e variável principal. Plutus verifica custo por entrega/uso e contribuição.
- Dédalo: utilidade e demonstração reais no produto. Íris: comunicação e oferta fiéis. Apolo: ritmo,
  revelação e audiovisual acessível, com legendas e controles; não é inferida uma frequência ótima.
- Psique: revisão sintética de compreensão, relevância, esforço e possível estranheza; Têmis:
  integridade independente, promessa/prova e limites. Nenhum parecer equivale a consumidor real.
- Hermes: exposição, atribuição e sinais voluntários segregados; coleta e conciliação nos ciclos
  existentes. Atena interpreta resultado e explicações concorrentes no próximo ciclo autorizado.

A biblioteca injeta o protocolo somente quando a tarefa pede expectativa, surpresa, novidade,
familiaridade ou equivalentes suportados. Curadoria explícita é preservada; quatro cartões no máximo.
O fingerprint incorpora a orientação contextual do protocolo e as fontes selecionadas.
O contexto não duplica as onze referências do catálogo; conserva até quatro cartões por rota. O contexto bruto continua
na auditoria do worker/tarefa. Esta versão não obriga campos adicionais nos schemas legados,
não modifica prompts de execuções em andamento e não gera novo trabalho no polling.
Rotas da ferramenta administrativa são omitidas do contexto dos executores: a orientação chega
pelo contrato existente da tarefa, sem anunciar ferramentas inexistentes nem permitir controllers de outro módulo.

A tela **Biblioteca do Harness → Expectativas e surpresa positiva** consome o backend:
`/api/consumer-expectations/v1/protocol`, `/plans/preview` e
`/products/{productId}/cycles/{cycleId}/learning`.
A prévia determinística não é um anúncio, produto construído, especificação aprovada ou plano
persistido. Ela prepara alternativas com a entrada literal e referências declaradas, não valida
remotamente essas referências. O desenho escolhido deve ser registrado no artefato da tarefa e
no brief do ciclo pelos fluxos já existentes. Não foi criada segunda fila, memória ou pipeline.

## Aprendizado e prova comercial

A leitura usa o último recibo `MEASURE` **ou** `MEASUREMENT_BLOCKED`. Somente o contrato automático
vigente, mesmo experimento, dados válidos, exclusão de testes e data posterior à versão permitem
expor métricas. Produto cruzado responde 404; recibo ausente/inválido devolve métricas vazias, não
zeros. Uma falha recente não pode ser escondida por uma medição antiga positiva.

- `MEASUREMENT_REQUIRED`: fonte ausente/inválida; Hermes corrige medição pelo fluxo oficial.
- `INCONCLUSIVE`: sinais insuficientes, ausência de compras ou uso/entrega não comprovados.
- `REVIEW_ECONOMICS`: compras com contribuição não positiva; Plutus revê economia.
- `OBSERVED_NOT_CAUSAL`: compra, contribuição positiva e uso conciliados; ainda não isola surpresa.

São orientações consultivas, nunca decisão automática do ciclo. Não somar resultados de versões,
produtos, canais ou janelas distintos. Contribuição não é lucro líquido. Desconhecido não vira zero.
A capacidade não dispara coleta, paga IA, publica ou aumenta orçamento.

Antes de teste comercial, registrar público/contexto, hipótese, variável, controle, unidade de
exposição, janela, amostra, compra líquida, contribuição e limites de dano. Analisar maturação de
reembolsos, frequência, repetição e coortes, além de tempo até resultado e custo de entrega. Se houver
randomização autorizada, verificar alocação, incerteza e comparações múltiplas; sem ela, comparação
histórica é observacional. Não interromper cedo por CTR favorável. Toda mudança de condições usa
novo ciclo/experimento com predecessor conforme o cânone de ciclos. Recrutamento e opiniões
solicitadas não são gates. Um ciclo sem tráfego suficiente não reprova o produto.

## Hipóteses próprias para investigar

1. **Utilidade descoberta antes da compra:** mostrar um resultado editável pode superar um gancho
   misterioso; comparar compra/contribuição mantendo produto e preço, proteger esforço e reembolso.
2. **Surpresa como redução de trabalho:** remover uma etapa esperada pode valer mais que um bônus;
   medir primeiro resultado útil, compra e custo por entrega, não quantidade de funcionalidades.
3. **Novidade só na superfície adequada:** narrativa nova com checkout familiar pode preservar
   compreensão melhor que renovar toda a jornada; verificar abandono e falhas de pagamento.
4. **Benefício condicionado à familiaridade:** público recorrente pode responder à quebra de padrão
   de modo diferente do novo; segmentar observação por exposição, sem inferir psicologia individual.
5. **Valor que sobrevive à habituação:** ganho de atenção que some na repetição pode coexistir com
   utilidade estável; separar coortes e retenção voluntária antes de investir em mais novidade.
6. **Revelação audiovisual com controle:** demonstrar mecanismo antes do CTA pode ajudar compreensão;
   testar com e sem áudio, legendas e movimento reduzido; prazer musical não é proxy de conversão.

Todas são hipóteses novas do Marketing Hub, sem impacto financeiro comprovado nesta entrega.
Critério de adoção: regressões técnicas aprovadas e ausência de regressão de contexto/isolamento.
Critério comercial futuro: compras líquidas, entrega útil e contribuição com fonte e condições
comparáveis. Reverter uma aplicação que aumente confusão, custo ou quebra de promessa; preservar
os resultados negativos no ciclo, sem regeneração paga automática.

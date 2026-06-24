# Análise consolidada dos experimentos sem sucesso

## 1. Metadados

- **Tema:** Experimentos comerciais
- **Status:** Diagnóstico de entrada obrigatório para o plano de evolução
- **Data:** 24/06/2026
- **Experimentos analisados:** 37, 38, 39 e 40
- **Objetivo:** impedir que falhas técnicas, de dados ou de execução sejam interpretadas como rejeição do mercado e transformar o histórico em requisitos de arquitetura, frontend e apoio à decisão por IA.

## 2. Fontes analisadas

Foram considerados os relatórios e registros mantidos no repositório e em seu histórico:

- `docs/relatorios/experimentos/experimento-37-relatorio-completo.md`;
- `docs/relatorios/experimentos/conclusao-do-experimento-37.md`;
- `docs/relatorios/experimentos/plano-de-melhoria-marketing-hub-experimento-37.md`;
- `docs/relatorios/experimentos/experimento-38-relatorio-completo.md`;
- `relatorios/experimento-39-relatorio-completo.md`;
- `relatorios/experimento-40-relatorio-completo.md`;
- PRs de correção operacional vinculados aos experimentos 38 e 39;
- `docs/registros/experimentos.md`.

Parte desses relatórios completos foi removida posteriormente do `main`, mas permanece rastreável no histórico Git. Esta análise não restaura os arquivos grandes; ela consolida somente os sinais necessários para orientar a evolução.

## 3. Conclusão executiva

Os resultados não permitem concluir simplesmente que “os produtos não funcionaram”. O histórico mostra uma mistura de quatro classes de problema:

1. **execução tecnicamente inválida**, quando publicação, formulário, integração ou medição não estavam comprovadamente funcionais;
2. **entrada estratégica incompleta**, quando persona, variável primária, métrica, público ou promessa não estavam definidos com qualidade suficiente;
3. **dados de descoberta contaminados**, quando fontes frágeis, duplicadas, adjacentes ou sem correspondência direta foram promovidas para nicho, hipótese e campanha;
4. **sinal comercial fraco ou inexistente**, que só pode ser interpretado depois que as três classes anteriores forem descartadas.

A consequência arquitetural central é:

> O Marketing Hub precisa separar o conceito de **experimento**, o conceito de **execução do experimento** e o conceito de **evidência comercial válida**.

Um registro `INVALIDATED` não pode, sozinho, reprovar uma hipótese. Primeiro o sistema deve provar que a execução teve exposição real, ativos coerentes, captura funcional, mensuração íntegra e dados suficientes.

---

## 4. Experimento 37 — atenção sem captura válida

### 4.1 Sinais observados

- 2.524 impressões;
- 114 cliques;
- R$ 25,11 de gasto;
- CPC aproximado de R$ 0,22;
- 100 visualizações do formulário;
- 0 envios;
- encerramento por `FORM_ZERO_CONVERSION_RULE_OF_THREE`.

O topo do funil gerou atenção e clique barato. A perda ocorreu entre visualização e envio do formulário.

### 4.2 Problemas que contaminam a leitura comercial

1. O botão do formulário apareceu como `type="button"`, sem submit funcional evidente.
2. A landing recebeu score 76 e recomendação de regeneração antes da publicação.
3. A primeira dobra não deixava o público específico suficientemente claro.
4. A prova visual tinha baixa legibilidade.
5. O público era amplo.
6. A persona estava preenchida como `teste`.
7. Os criativos eram semelhantes e alguns textos estavam truncados.
8. A isca era ampla e distante do momento de dor mais imediato.

### 4.3 Leitura correta

O experimento 37 não prova que personal trainers rejeitam a hipótese. Ele prova que houve interesse inicial e que a captura publicada não possuía qualidade suficiente para gerar evidência comercial confiável.

### 4.4 Requisitos derivados

- teste ponta a ponta obrigatório antes de liberar mídia;
- bloqueio de publicação quando a revisão da landing recomendar regeneração;
- persona mínima vendável;
- CTA, promessa, formulário e entrega coerentes;
- distinção entre falha técnica e falha de mercado;
- visualização clara do maior gargalo no frontend;
- criação de experimento derivado sem repetir a mesma causa-raiz.

### 4.5 Correção importante do método experimental

O plano histórico propôs mudar simultaneamente dor, isca, landing, segmentação e métrica. Isso é adequado para uma reconstrução comercial, mas não para descobrir qual variável causou melhora.

A evolução deve separar:

- **execução de recuperação:** corrige defeitos técnicos sem mudar a hipótese comercial;
- **novo experimento:** muda uma variável comercial primária e mantém as demais controladas.

Sem essa separação, qualquer resultado positivo continuará sem explicação causal.

---

## 5. Experimento 38 — configuração incompleta e falhas de publicação

### 5.1 Sinais do relatório

- status final `INVALIDATED`;
- mesma hipótese de personal trainer do experimento anterior;
- persona ainda registrada como `teste`;
- interesses e comportamentos vazios no contexto inicial;
- `primaryVariable` ausente;
- `primaryMetric` ausente;
- `kpiTargetCpl` igual a zero;
- amostra planejada sem contrato comercial suficientemente explícito.

### 5.2 Falhas operacionais registradas durante a publicação

O histórico de correções vinculadas ao experimento mostra:

1. rejeição/instabilidade no uso de URLs externas de imagem pela Meta, corrigida com download local, upload multipart por bytes e deduplicação por hash;
2. `DataBufferLimitException` ao carregar um pacote de targeting excessivamente grande, corrigido posteriormente com endpoint filtrado e contrato enxuto;
3. necessidade de melhorar logs e rastreabilidade do caminho de publicação.

### 5.3 Leitura correta

Mesmo que tenha existido exposição posterior, o ciclo foi iniciado sem especificação experimental completa e passou por falhas de infraestrutura/publicação. O resultado final não deve entrar no aprendizado do mercado com o mesmo peso de uma execução estável.

### 5.4 Requisitos derivados

- nenhuma execução inicia sem variável e métrica primárias;
- KPI zero ou ausente gera bloqueio ou alerta explícito;
- publicação só é considerada concluída após confirmação de campanha, conjunto, anúncio e criativo ativos;
- o sistema registra tentativas de publicação separadamente da janela comercial;
- erros de integração não mudam a hipótese para `INVALIDATED` comercialmente;
- payloads operacionais para workers devem ser mínimos e específicos ao experimento;
- cada passo de publicação precisa ficar disponível na tela, não apenas em logs.

---

## 6. Experimento 39 — problemas upstream, reset, alcance e targeting

### 6.1 Sinais do relatório

O nicho de manicure foi materializado com fontes que incluíam:

- evidências duplicadas;
- rótulos genéricos repetidos;
- conteúdo adjacente ao trabalho da profissional;
- fontes de baixa utilidade comercial;
- referências violentas e irrelevantes para a decisão de oferta;
- ausência de contexto operacional e linguagem pública em formato compatível.

Apesar de o nicho possuir uma dor plausível — faltas, deslocamento, agenda e recorrência — a base que alimentou hipótese e ativos estava parcialmente contaminada.

### 6.2 Falhas operacionais registradas

1. a liberação para Facebook Ads falhava ao tentar limpar eventos brutos antes de eventos normalizados vinculados;
2. a estimativa de alcance era enviada sem localização e a Meta recusava a requisição;
3. interesses, cargos e comportamentos formavam uma audiência estreita por composição inadequada;
4. foi necessário transformar o targeting em especificação OR e registrar passos de falha do job de publicação.

### 6.3 Leitura correta

O experimento misturou qualidade insuficiente do insumo de descoberta com falhas de liberação e alcance. Antes de interpretar mercado, o sistema precisa validar se o nicho, a hipótese, o público e a execução foram materializados a partir de evidências aceitáveis.

### 6.4 Requisitos derivados

- gate de qualidade dos insumos de nicho/hipótese;
- lineage até fontes e claims aprovados;
- bloqueio de fontes inseguras, sensacionalistas ou semanticamente adjacentes;
- diagnóstico de alcance antes da publicação;
- diferença entre audiência inexistente, audiência estreita e erro técnico de payload;
- reset idempotente e transacional;
- passos de publicação persistidos por `jobId`;
- frontend deve mostrar por que a campanha não chegou ao mercado.

---

## 7. Experimento 40 — baixa correspondência semântica e contaminação por solução

### 7.1 Sinais do relatório

O nicho de alongamento de unhas apresentava:

- claims genéricos promovidos como dores específicas;
- fontes sobre consumidor ou procedimentos usadas como se provassem a rotina comercial da profissional;
- fontes duplicadas;
- documento de baixa qualidade usado como evidência;
- ausência de linguagem operacional consolidada;
- alertas explícitos de contaminação por solução;
- mistura entre biossegurança, técnica de alongamento, agenda, orçamento e fidelização sem um eixo comercial único.

### 7.2 Leitura correta

A hipótese comercial foi construída sobre um pacote de conhecimento ainda instável. Uma campanha pode até gerar dados, mas o sistema não deveria escalar a materialização antes de resolver a qualidade do contexto de entrada.

### 7.3 Requisitos derivados

- nenhum experimento nasce de artefato upstream reprovado ou incompleto;
- o frontend deve mostrar qualidade/confiança do nicho e da hipótese usados;
- modelos de geração devem receber somente evidências aprovadas;
- `solution contamination` deve impedir que uma solução apareça como se fosse prova da dor;
- dados insuficientes devem gerar revisão ou novo ciclo de pesquisa, não preenchimento especulativo.

---

## 8. Padrões comuns encontrados

| Padrão | Efeito observado | Correção sistêmica |
|---|---|---|
| Publicação não comprovada | Experimento marcado como falho sem chegar corretamente ao mercado | `ExperimentRun` + confirmação operacional da exposição |
| Formulário sem teste E2E | Zero lead interpretado como baixa demanda | gate funcional com submissão de teste excluída das métricas |
| Persona e estratégia incompletas | Ativos genéricos e público pouco confiável | readiness estratégico bloqueante |
| Variável/métrica ausentes | Resultado sem pergunta causal | contrato obrigatório de desenho experimental |
| Landing abaixo do padrão | Hipótese contaminada por UX/captura | gate de qualidade e aprovação comercial |
| Público amplo ou estreito por erro | Clique sem qualidade ou campanha sem alcance | diagnóstico de audiência e targeting versionado |
| Fontes upstream frágeis | Dor, promessa e oferta construídas sobre sinais ruins | gate de evidência e lineage |
| Status único `INVALIDATED` | Mistura falha técnica, comercial e estatística | taxonomia de conclusão e validade da evidência |
| Mudança de várias variáveis | Impossibilidade de saber o que funcionou | experimento atômico e comparação controlada |
| Logs como única explicação | Usuário não consegue decidir na tela | read model persistido para frontend |

---

## 9. Decisão arquitetural adicional — `ExperimentRun`

O plano mestre deve incluir uma entidade de execução separada do experimento.

```text
Experiment
  ├── ExperimentRun 1 — teste técnico, inválido para mercado
  ├── ExperimentRun 2 — publicação interrompida, inválido para mercado
  └── ExperimentRun 3 — execução comercial válida
```

### 9.1 Modelo proposto

```text
experiment_run
- id
- experiment_id
- run_number
- mode
- strategy_version
- asset_bundle_version
- audience_version
- status
- evidence_validity
- started_at
- first_verified_impression_at
- ended_at
- stop_policy
- stop_reason
- failure_classification
- failure_detail
- data_quality_status
- created_by
- created_at
- updated_at
```

Enums iniciais:

```text
mode:
- TEST
- PRODUCTION

status:
- DRAFT
- PREFLIGHT
- READY
- PUBLISHING
- RUNNING
- PAUSED
- COMPLETED
- FAILED
- CANCELLED

evidenceValidity:
- NOT_EVALUATED
- TECHNICALLY_INVALID
- MEASUREMENT_INVALID
- STRATEGICALLY_INVALID
- INSUFFICIENT_DATA
- COMMERCIALLY_VALID
```

### 9.2 Regra de uso

- correção técnica sem mudar a variável comercial cria novo `ExperimentRun` do mesmo experimento;
- mudança de dor, promessa, isca, rota, preço, público primário ou oferta cria novo `Experiment`;
- somente runs `COMMERCIALLY_VALID` entram em comparação, aprendizado de mercado e recomendação de vencedor;
- runs inválidos continuam visíveis para aprendizado operacional e prevenção de recorrência.

---

## 10. Taxonomia obrigatória de causa-raiz

```text
- INTEGRATION_FAILURE
- PUBLICATION_FAILURE
- MEASUREMENT_FAILURE
- FORM_FUNCTIONAL_FAILURE
- LANDING_QUALITY_FAILURE
- UPSTREAM_DATA_QUALITY_FAILURE
- STRATEGY_CONFIGURATION_FAILURE
- AUDIENCE_FAILURE
- CREATIVE_FAILURE
- PROMISE_OR_LEAD_MAGNET_FAILURE
- OFFER_FAILURE
- PRICE_OR_CHECKOUT_FAILURE
- INSUFFICIENT_DATA
- COMMERCIAL_HYPOTHESIS_FAILURE
- USER_STOPPED
```

A classificação deve registrar:

- categoria principal;
- categorias contribuintes;
- evidências;
- confiança determinística ou humana;
- impacto sobre validade comercial;
- correção necessária;
- possibilidade de repetir o mesmo experimento;
- necessidade de criar experimento derivado.

O modelo pode sugerir a classificação, mas o backend valida as condições objetivas e o usuário confirma conclusões com impacto comercial.

---

## 11. Gates obrigatórios antes de mídia

### 11.1 Gate de qualidade upstream

- nicho e hipótese com artefatos aprovados;
- ausência de bloqueios de fonte/claim;
- persona operacional mínima;
- dor, resultado, mecanismo, prova e oferta disponíveis;
- histórico de experimentos anteriores carregado.

### 11.2 Gate de desenho experimental

- pergunta de negócio;
- variável primária;
- métrica primária;
- baseline e alvo, quando aplicável;
- oferta e preço;
- política de parada;
- janela de decisão;
- custos esperados;
- definição do que permanecerá controlado.

### 11.3 Gate de ativos

- criativo aprovado;
- ausência de truncamento crítico;
- mensagem coerente entre anúncio, formulário, landing e entrega;
- landing aprovada para publicação;
- links e assets acessíveis;
- política de privacidade e consentimentos configurados.

### 11.4 Gate funcional ponta a ponta

Executar em modo `TEST`:

1. abrir destino;
2. preencher/enviar formulário ou gerar lead de teste da Meta;
3. confirmar criação/identificação do lead;
4. confirmar evento no funil;
5. confirmar e-mail/magic link;
6. confirmar acesso ao Portal;
7. confirmar geração/entrega da amostra, quando existir;
8. confirmar acesso ao checkout em ambiente apropriado;
9. excluir eventos `TEST` das métricas comerciais.

### 11.5 Gate de publicação Meta

- campanha criada;
- ad set criado;
- anúncio e criativo criados;
- imagem resolvida por hash válido;
- targeting aceito;
- alcance validado;
- estado efetivo consultado na Meta;
- primeira impressão verificada antes de iniciar a janela comercial.

### 11.6 Gate de mensuração

- UTMs/IDs de atribuição presentes;
- eventos deduplicados;
- gasto sincronizado;
- eventos de captura recebidos;
- checkout/pagamento reconciliáveis;
- freshness dentro do limite;
- nenhuma divergência crítica entre fontes.

---

## 12. Impactos obrigatórios no frontend

O usuário deve conseguir distinguir, sem consultar logs:

- experimento ainda não executado;
- teste técnico em andamento;
- publicação falhou;
- campanha publicada sem impressão confirmada;
- execução comercial válida;
- dados insuficientes;
- falha técnica;
- falha de marketing;
- hipótese comercial realmente reprovada.

A tela deve mostrar:

- checklist de gates;
- execução/run atual;
- linha do tempo de publicação;
- última evidência de exposição real;
- integridade e atualização dos dados;
- maior gargalo;
- causa-raiz sugerida;
- evidências e limitações;
- próximo comando recomendado;
- histórico de runs e decisões.

---

## 13. Impactos obrigatórios na decisão por IA

O modelo nunca deve receber somente `status=INVALIDATED` e métricas agregadas.

O snapshot precisa incluir:

- validade de cada run;
- gates que passaram/falharam;
- falhas técnicas e passos de publicação;
- qualidade de nicho/hipótese;
- versões de estratégia, ativos e público;
- métricas somente da janela comercial válida;
- eventos de teste separados;
- custos e receitas;
- histórico de tentativas anteriores;
- evidências positivas e negativas.

Regras duras:

- run tecnicamente inválido não pode reprovar hipótese;
- ausência de impressão confirmada não pode gerar conclusão de mercado;
- formulário não testado impede interpretar zero envio;
- mudança de múltiplas variáveis deve ser rejeitada como desenho inconclusivo;
- recomendação deve citar IDs de evidência persistidos;
- confiança do modelo não substitui nível de evidência calculado pelo backend.

---

## 14. Priorização atualizada

### P0 — impedir nova evidência inválida

1. `ExperimentRun`;
2. gates de preflight;
3. modo `TEST` separado de `PRODUCTION`;
4. taxonomia de falha;
5. confirmação de exposição real;
6. variável e métrica obrigatórias;
7. read model de diagnóstico no frontend.

### P1 — tornar o aprendizado causal

1. `ExperimentStrategy`;
2. comparação de experimentos atômicos;
3. qualidade upstream;
4. ledger econômico;
5. maior gargalo e causa-raiz;
6. experimento derivado com uma variável primária.

### P2 — apoio por IA e automação segura

1. pipeline `experimentdecision.v1`;
2. crítica da recomendação;
3. geração de desenho do próximo teste;
4. feedback humano;
5. automações de baixo risco aprovadas.

---

## 15. Critérios de aceite derivados do histórico

- [ ] nenhum experimento começa produção sem passar por preflight;
- [ ] eventos de teste não entram no funil comercial;
- [ ] publicação sem impressão confirmada não inicia janela de avaliação;
- [ ] uma falha técnica cria run inválido, não reprovação comercial;
- [ ] variável e métrica primárias são obrigatórias;
- [ ] landing reprovada não pode receber mídia;
- [ ] formulário possui prova E2E antes da campanha;
- [ ] qualidade upstream aparece no readiness;
- [ ] fonte insegura ou adjacente bloqueia materialização automática;
- [ ] frontend explica a causa da parada;
- [ ] modelo não declara vencedor usando run inválido;
- [ ] novo experimento muda uma variável comercial principal;
- [ ] correção puramente técnica preserva o mesmo experimento e cria novo run;
- [ ] relatório final separa aprendizado operacional de aprendizado de mercado.

## 16. Decisão final

Os experimentos anteriores não devem ser tratados como um conjunto homogêneo de produtos rejeitados. Eles formam principalmente um baseline de falhas de execução, qualidade de entrada e desenho experimental.

A evolução deve começar pela confiabilidade da evidência. Somente depois faz sentido comparar Instant Form, landing, amostra personalizada e venda direta com apoio dos modelos.

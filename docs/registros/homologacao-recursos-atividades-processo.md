# Matriz de homologação — recursos especializados por atividade

## 04/10/2026 — título de processo na cadeia histórica

Vega/produto 4, ciclo 2, cadeia 14, definição 75 v6: banco e composição da cadeia
confirmam posição 6, mas a leitura das atividades retorna `chainPosition: null`.
A posição corrente pertence à cadeia 26/definição 119 e não identifica a versão histórica.
O resolvedor consultava apenas chamadas de subprocessos e omitia os membros diretos.

Alternativas: correção somente visual deixaria o contrato incompleto; uma consulta adicional
de cadeia na interface duplicaria a resolução; completar o resolvedor existente preserva
a definição exata e serve à tela e ao contexto do AIHUB. Adotada a terceira alternativa.

Matriz definida antes da validação:

| Área | Cenários | Aceite |
| --- | --- | --- |
| Caminho feliz | membro direto publicado e retirado, números distintos | posição persistida da definição exata, sem pai inventado |
| Validações/falhas | outra definição com mesmo código, cadeia ausente/desconhecida | nenhuma posição emprestada; nenhuma mutação |
| Integração | resolvedor → contrato de atividades → título e contexto AIHUB | mesma posição histórica em ambos, sem consulta paga |
| Regressão | subprocessos 5.1/5.2/5.4 e ordem causal | posições e adesões anteriores preservadas |
| Observabilidade/segregação | IDs sintéticos locais; cadeia corrente diferente | referência, ciclo e versões preservados; QA não cria venda |
| Interfaces | Chromium desktop, iPhone 15 Pro e Pixel 7 | título numerado legível e sem overflow horizontal |

Validação local: suíte do backend com 3.890 testes reportados (3.866 executados e 24
condicionais não executados), sem falhas; 848 testes do frontend, typecheck e build aprovados.
Nove cenários visuais em desktop, iPhone 15 Pro e Pixel 7 passaram sem overflow, erro de
JavaScript ou mutação. Os contratos usados na tela foram serializados pelo resolvedor Java real
com repositórios simulados; nenhuma API produtiva foi chamada durante esses cenários.
As regressões reproduziram o defeito antes da correção e preservaram subprocessos aprovados.
A primeira suíte em paralelo ao build excedeu a memória da sandbox; execução sequencial com
memória limitada concluiu a suíte. Evidências locais em `.codex/validation/process-title-number/`.

Não há execução de processo, campanha, inferência ou autorização financeira neste ajuste.

## 02/10/2026 — número do processo na ficha do produto

Mira/produto 10, cadeia 24, definição 58: o título deve mostrar **Processo 5.4 —
Homologação técnica de experimento**. O resolvedor agora entrega a posição da
cadeia também sem ciclo comercial, usa a topologia causal existente e mantém a
adesão Opala histórica isolada. A interface já apresentava esse contrato.

Na mesma tela, a falta de plano governante era convertida em pendência, mas a
avaliação transacional marcava rollback e derrubava a consulta com HTTP 500.
A leitura preserva o bloqueio sem invalidar a transação; execução e gasto
continuam recusados. A regressão usa proxy Spring e transação H2 reais.

Passaram 64 testes relacionados do backend, 19 da ficha, typecheck, build e
verificação visual em desktop e emulação de iPhone/Pixel. Os testes usaram
fixtures segregadas, sem campanha ou comando produtivo. Causas, alternativas e
matriz: [título e posição da cadeia](../homologacao/titulo-processo-posicao-cadeia.md).

| Área | Cenários obrigatórios | Critério de aceite |
| --- | --- | --- |
| Caminho feliz | atividade comum; atividade com `themis-image-studio` | versão salva e recurso oficial visível no diagrama |
| Validações | código inexistente; recurso em START/GATEWAY/END; agente divergente | backend rejeita antes da execução |
| Falhas | recurso inativo; executor comum tenta reservar atividade especializada | fila permanece protegida e sem consumo |
| Integrações | tela → API → MySQL → tarefa → `pending` | contrato entrega código, nome, executor e instruções |
| Observabilidade | resultado e erro continuam correlacionados pela tarefa | nenhum avanço ou custo é inferido pela tela |
| Segregação | dados locais; recurso exato por agente/container | outro agente ou recurso não recebe a atividade |
| Interfaces | desktop Chromium, iPhone 15 Pro e Pixel 7 | seleção e detalhe sem overflow e com rótulos legíveis |

Se a primeira rodada completa revelar defeito, a correção reinicia a validação e exige duas rodadas
completas consecutivas sem falhas após o último ajuste.

## Resultado de 2026-08-20

- suíte completa do backend: 1.659 testes sem falhas;
- suíte completa do frontend: 117 arquivos e 343 testes sem falhas;
- duas rodadas finais consecutivas com volume MySQL 5.7 novo, migração física, API, tarefa e
  executores em containers;
- nas duas rodadas, o executor genérico não reservou a atividade e o executor
  `themis-image-studio` recebeu código, nome, executor e instruções oficiais;
- desktop Chromium, iPhone 15 Pro e Pixel 7 exibiram o recurso no diagrama e no editor sem
  overflow horizontal;
- os dados e containers temporários foram removidos ao final.

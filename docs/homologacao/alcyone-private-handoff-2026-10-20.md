# Alcyone — aceite e continuidade privada do primeiro experimento

## Evidência e decisão

O usuário aprovou a peça #770. O aceite #723 conserva arquivo #1060 e pareceres
#776/#791; o subprocesso #68 concluiu. O pai #44 abriu a landing #75, que ficou
sem provas comerciais, embora o contexto atual seja `INITIAL_EXPERIMENT_PRIVATE`
e declare `requiresLandingGeneration=false`. A seleção já reconhecia esse modo;
a jornada reconhecia somente produto sem experimento e ciclo sucessor.
O replay também confirmou que o cadastro de Alcyone tem URL vazia, enquanto a
URL da versão homologada está no destino aprovado e no gate. A jornada inicial
consome essa prova canônica, sem alterar o contrato já aceito de Íris.

Alternativas: converter a seleção em comercial violaria seu escopo; fabricar um
ciclo alteraria o histórico; consumir o contexto privado vigente no executor já
existente conserva as provas, os custos e a autoridade. Adotada a terceira.

## Matriz de homologação definida antes da execução

| Caso | Aceite |
| --- | --- |
| Primeiro experimento e segunda identidade | Aceite privado → destino → integração, sem landing ou inferência nova |
| Origem da prova | Fonte do produto, alvo do experimento, produto, versão, URL e gate conferidos sem reescrever a prova |
| Negativos | Planejamento bloqueado, produto/origem/gate divergente, flags comerciais e prova técnica ausente não concluem |
| Histórico válido | Produto privado, ciclo sucessor, contrato comercial e parecer independente preservados |
| Recuperação | Repetição não duplica artefatos; filho desnecessário encerra sem cancelar tarefa em curso |
| Observabilidade | Evidência registra origem técnica, seleção, hashes, custo incremental zero e flags privadas |
| Dados e interfaces | Fixtures sintéticas sem cobrança; nenhuma alteração de layout, mesma navegação administrativa |

Hipótese reutilizável: reconhecer o mesmo regime nos dois lados da passagem reduz
espera e geração desnecessária. O aceite local é a sequência comprovada, sem
transferência de aprovação comercial. Impacto em vendas não foi medido.

## Resultados

- Regressão vermelha reproduzida em duas identidades: o executor buscava a landing.
- 47 testes da jornada, 21 do produtor de prova e 3 do retorno de subprocessos passaram.
- Replay offline dos contratos e registros de Alcyone passou até destino e integração,
  com peça #770, aprovação #723 e bytes técnicos originais; zero modelo e zero escrita produtiva.
- A revisão final reproduziu uma falsa invalidação ao permutar campos equivalentes
  da autorização. A evidência agora usa ordem canônica; a regressão exige replay
  idempotente e preservação de ambos os aceites após essa permutação.
- 52 testes JPA do conciliador passaram, incluindo o novo caso de encerramento da
  landing dispensada após aguardar uma tarefa real; custo e histórico preservados.
- A extensão do replay exige atestação de compatibilidade v36 de Mira: somente o
  hash do teste compartilhado muda; experiência e publicação permanecem inalteradas.
- Suíte integral local do backend: 5.057 casos, 5.001 executados com sucesso,
  zero falhas/erros e 56 testes condicionais desabilitados (fixtures/ambientes específicos).
  O replay de Alcyone foi habilitado e executado separadamente, sem consulta produtiva.
- 16 testes do pacote de evidências e 22 do seletor de publicação passaram; pacote
  atualizado gerado com 555 arquivos/128 manifestos. A compatibilidade não autoriza
  publicação de frontend.
- Publicação e conferência operacional serão vinculadas ao PR; não se usa deploy como teste.

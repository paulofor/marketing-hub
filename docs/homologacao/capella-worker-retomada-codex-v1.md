# Retomada de Capella pelo Worker Codex — 19/10/2026

## Evidência e escopo

Capella, produto 7, cadeia 26, ciclo 12, experimento 105, `capella-private-v3`.
A conexão #3 foi confirmada pelo executor. O primeiro job após a retomada,
`9811844f-879f-4c9d-996a-50e0b0f1a3cf`, parou antes de `turn/start`: os sete
templates ativos ainda pediam `gpt-5.5`, ausente de `model/list`. A sessão própria
disponibiliza `gpt-6.1-sol`, já utilizado pelos agentes e presente no catálogo de
custos do backend. Não há saída de modelo nessa tentativa para recuperar.

Foram comparados editar os sete registros pela tela (rápido, mas sem implantação
reprodutível), criar alias automático (ocultaria a troca de modelo) e versionar
os sete contratos preservando seu conteúdo. A terceira opção foi escolhida:
configuração explícita, sem fallback pago, prompts/schemas e histórico preservados.
Outros pipelines e execuções anteriores permanecem fora da migração.

## Matriz definida antes da validação

| Caso | Aceite |
| --- | --- |
| Falha original | Sete templates reais passam a pedir o modelo disponível; prompt/schema idênticos |
| Outro produto | Consumidores distintos selecionam o mesmo contrato compartilhado, sem copiar contexto |
| Caminho válido | Templates de outro pipeline e snapshots históricos preservados |
| Fonte incompleta | Ausência de etapa ou preço do modelo bloqueia antes da alteração |
| Colisão | Contrato conflitante não pode ser sobrescrito ou ativado parcialmente |
| Recuperação | Reaplicação sem duplicar; rollback conserva novos contratos e rastreabilidade |
| Evolução posterior | Rollback não reativa contrato antigo quando outro já o substituiu |
| Executor | Suítes existentes de transporte, schema, callbacks e etapas passam localmente sem provedor |
| Operação | Retomar uma vez pela tela após publicar; conferir saída, custos e próxima passagem |
| Comercial | Preservar US$15 cumulativos de preparação, US$10 audiovisuais e R$100/5 dias após homologação |

Não se altera oferta, preço, experiência, vídeos, meta de 30% para continuidade
ou aceites. A descoberta abaixo da meta permanece exclusivamente no limite já
decidido pelo usuário. Testes sintéticos não contam como tráfego, compras ou lucro.

Fontes: [App Server](https://learn.chatgpt.com/docs/app-server) e
[modelos disponíveis](https://learn.chatgpt.com/docs/models#recommended-models),
consultados em 19/10/2026. A disponibilidade real foi conferida em `model/list`,
sem inferência. Preços no catálogo são uma estimativa conservadora de controle,
não fatura da assinatura ChatGPT.

## Primeira rodada local

Os 58 testes existentes de contratos, fila, recuperação e publicação passaram,
assim como os 27 testes Node de sessão, schema, multimodalidade e replay.
A nova fixture física detectou que `USE` não atualiza o catálogo informado pelo
JDBC ao Liquibase; a fixture passou a usar `Connection.setCatalog`, preservando
o isolamento do schema antes de repetir somente seus três cenários.

## Resultado local

- Backend: 61 casos aprovados, incluindo três cenários físicos MySQL 5.7.44.
- Executor: 39 casos aprovados; um replay visual opcional preexistente não se aplica
  à alteração de configuração. Integração percorre fila, App Server simulado,
  schema, estimativa de custo e callback para três identidades.
- Transporte Node: 27 casos aprovados, sem inferência externa.
- A fixture também passou a confirmar suas alterações antes do rollback Liquibase,
  para que a evolução posterior do cenário realmente permaneça persistida.
- Spotless, Actionlint, includes relativos, contrato temporal, SQL sem subconsulta
  proibida e revisão do diff conferidos. Nenhum changelog histórico foi alterado.

A adoção depende da execução real preservando qualidade e custos; retornar ao
contrato anterior é operação auditável, sem apagar os novos snapshots. Nenhum
resultado local comprova venda, aprovação independente ou saldo da assinatura.

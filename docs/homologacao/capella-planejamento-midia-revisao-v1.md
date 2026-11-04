# Capella — teto de planejamento na revisão comercial — 04/11/2026

O recibo 116 registra R$ 100 para mídia de Capella, produto 7, cadeia 27, ciclo 19,
experimento 112, versão `capella-beleza-v3`, em 02/11/2026 às 23:42 UTC. A planilha
confirma a decisão como vigente. A consulta via MCP confirmou `budget_limit_brl=0`
no ciclo, coerente com a aplicação operacional ainda não autorizada.

A prontidão comercial consultava apenas esse campo operacional e devolvia
`CYCLE_MEDIA_LIMIT`. A decisão salva não chegava ao consumidor seguinte. Os testes
anteriores sempre forneciam um limite positivo no ciclo, deixando essa passagem descoberta.
Os demais insumos comerciais de Capella ainda precisam ser concluídos; esta correção
não os declara aprovados.

| Alternativa | Benefício | Risco ou esforço | Decisão |
| --- | --- | --- | --- |
| Ler o recibo vigente na prontidão existente | Reutiliza a decisão sem nova gravação | Exige conferir contexto e prioridade | Adotada |
| Criar uma projeção financeira compartilhada adicional | Uniformiza outros consumidores | Amplia o contrato sem necessidade comprovada | Adiada |
| Copiar o teto ao ciclo com um novo evento | Mantém o consumidor atual | Duplica estado e exige sincronização e auditoria adicionais | Não adotada |

A prontidão usa primeiro o teto atual de mídia da planilha. Zero continua bloqueando,
mesmo com valor legado positivo. Na ausência de recibo vigente, o caminho anterior
com limite positivo do ciclo permanece compatível. Nenhuma consulta altera orçamento,
janela, etapa, experimento, autorização ou tarefas. As revisões, o preflight e a
confirmação operacional posterior permanecem obrigatórios.

## Homologação local

- Antes da correção, três verificações falharam: recibo aprovado no caso original,
  recibo aprovado em outra identidade e isolamento da pendência de criativo.
- Depois: 13 testes da passagem controller → recibo → prontidão e 22 testes de
  prontidão anterior aprovados. Incluem ausência, zero, decisão posterior de zero,
  valor legado, outra cadeia, ciclo, experimento, versão, recibo anterior à versão,
  grupo incorreto e criativo ausente.
- Também passaram 20 testes dos tetos, 24 da demonstração financeira, quatro da
  autorização comercial e 21 do catálogo operacional. O pacote de provas imutáveis
  permaneceu válido. Spotless e revisão do diff concluídos.
- Dependências externas foram simuladas na sandbox; o controller e os serviços
  financeiros reais foram executados, sem provider pago, campanha ou escrita produtiva.

Critério de adoção: eliminar o bloqueio por ausência de teto quando há recibo positivo
vigente no mesmo contexto, mantendo todos os outros gates. Reverter se a leitura
alterar a verba operacional ou aceitar decisão de outro contexto. Acompanhar ocorrências
de `CYCLE_MEDIA_LIMIT` apesar de recibo válido e solicitações repetidas de decisão.
O teste comprova comportamento; o impacto sobre vendas ainda não foi medido.

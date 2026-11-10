# Continuidade dos tetos financeiros entre ciclos — v1

## Evidência e escolha

Em 10/11/2026, o MCP confirmou Capella: ciclo 20/experimento 113,
predecessor 19/112, e Vega: ciclo 22/115, predecessor 21/114. Os recibos de
preparação continuam nos predecessores (Capella US$ 120, reserva US$ 20;
Vega US$ 200, reserva explícita zero). A consulta agregava custos por produto,
mas procurava recibos apenas no ciclo selecionado. A versão executável também
muda nos sucessores. Painel, agentes, contratos e consulta de logs foram
preservados em `artifacts/cumulative-ceilings` (sem credenciais). O MCP de logs
respondeu normalmente, sem linhas correspondentes ao filtro financeiro.

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Copiar aprovações ao criar ciclos | Mudança pequena | Duplica recibos e aparenta novo orçamento; não recupera sucessores existentes | Rejeitada |
| Novo cadastro global de limites | Centraliza | Migração e mecanismo paralelo sem necessidade comprovada | Rejeitada |
| Resolver recibos pela linhagem persistida | Recupera sucessores, conserva auditoria e um teto único | Validar identidade, ordem e histórico; esforço proporcional | Adotada |

## Matriz local de aceite

| Cenário | Aceite esperado |
| --- | --- |
| Sucessor de Capella e produto com IDs diferentes | Mesmo recibo/teto/reserva; nova versão executável não solicita aprovação monetária |
| Vários predecessores e substituição explícita | Última decisão de cada grupo prevalece; valores não se somam |
| Consumo anterior, reserva e nova tarefa | Saldo é teto menos consumo menos reserva, cada parcela uma vez; guard permanece ativo |
| Limite exato e ultrapassado | Aviso de atenção e revisão na planilha; leitura não inicia gasto |
| Custo desconhecido | Ausência explícita; não inventar zero nem afirmar cobertura |
| Produto/cadeia/linhagem divergentes e corrupção | Recusa segura; nenhuma aprovação alheia |
| Replay e revisão concorrente | Comando repetido idempotente; revisão antiga recusada |
| Consulta de ciclo encerrado | Não lê decisões de descendentes nem oferece retomada |
| HTTP → serviço → recibo → pending/guard | Contratos reais com dependências locais; nenhuma inferência paga |
| Desktop Chromium, iPhone 15 Pro e Pixel 7 emulados | Origem do teto, saldo e alerta legíveis; revisão acessível na própria planilha |
| Regressões existentes | Aprovações, fluxo de autorização e arquitetura passam; mídia/vídeo/STOP preservados |

## Melhoria reutilizável e limites

A lacuna do harness era validar apenas um ciclo isolado. A regressão executa
predecessores e sucessores nos serviços reais, conferindo o recibo nativo que
chega ao executor e o saldo consultado antes da tarefa. Critério de adoção:
nenhuma nova aprovação apenas por troca de ciclo, sem reiniciar saldo ou
perder reservas. Testes locais não demonstram aumento de vendas. Autorizações
pontuais de um vídeo, homologações e publicação mantêm seus gates.

A navegação local revelou ainda um fixture de browser desatualizado: a página atual
consulta o recibo de vídeo único e os projetos do Estúdio, ausentes na simulação.
O teste foi atualizado para esses contratos consultivos e agora registra qualquer
mutação inesperada, preservando cleanup mesmo quando falta uma rota simulada. Não
foi alterada a autorização de vídeo único. Este ajuste valida a página real completa,
sem esconder integrações novas nem usar publicação como diagnóstico.

## Validação local concluída

- 190 testes Java aprovados (98 de contratos/financeiro e 92 de arquitetura), sem falhas ou skips.
- 29 testes de frontend aprovados; TypeScript e build de produção aprovados.
- Browser Chromium: desktop, iPhone 15 Pro e Pixel 7 emulados; navegação real, recibo
  do predecessor, reserva, alerta e link de revisão conferidos. Nenhum POST inesperado.
- Consulta JPA real em H2 confere filtro por linhagem e ordenação por data/ID,
  inclusive empate e revisões locais diferentes. Nenhum changelog foi alterado.
- A fotografia histórica de uma versão invalidada permanece consultável, com recibo
  inelegível para execução. Nenhuma autorização pontual de vídeo foi transferida.
- Arquivos de evidência declarados em `.codex/delivery-artifacts.json`; nenhuma credencial
  faz parte da entrega. Dados fictícios permanecem separados do estado comercial.

O aceite técnico não constitui venda ou contribuição realizada. Custos incompletos e
outros gates permanecem identificados; esta alteração não inicia tarefas nem campanhas.

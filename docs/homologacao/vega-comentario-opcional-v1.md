# Vega — comentário opcional na jornada

## Evidência e decisão

Dédalo #848 bloqueou a atividade 2.3 do ciclo #18/experimento #111 em
28/10/2026: a estratégia de Atena #846 preserva `reflection` opcional, mas
interface e backend exigem texto para concluir o dia. A homologação #844 e
os testes anteriores sempre preenchiam esse campo, ocultando a diferença.

Foram comparadas três alternativas: exigir uma intenção escrita (simples, mas
contraria o contrato aprovado); permitir conclusão explícita sem comentário
(preserva orientação e reduz esforço); retirar a conclusão (perde a distinção
entre orientação salva e avanço intencional). Escolhida a segunda. A v17
conserva sua hipótese e identidade: corrige uma exigência indevida, sem criar
oferta, promessa ou autorização. A v16 mantém o comportamento histórico.

O backend informa se o comentário é obrigatório na conclusão; a interface
consome essa regra. Concluir exige orientação válida e ação explícita. Não
preencher texto automático nem registrar aplicação, preferência ou compra.

## Matriz definida antes dos testes

| Área | Critério de aceite |
| --- | --- |
| Caso original | v17 conclui Dia 1 sem comentário; orientação e entradas permanecem recuperáveis |
| Sete dias | Com direito sintético, alternar dias com e sem comentário; recuperar registros e materiais |
| Falhas | Sem orientação, orientação bloqueada e escolhas alteradas não permitem conclusão |
| Direitos | Sem pagamento, o Dia 2 continua protegido; expiração e isolamento continuam válidos |
| Compatibilidade | v16 preserva comentário obrigatório e caminho antes válido |
| Outras identidades | Repetir os comandos com outro ciclo/produto sintético e sessão distinta |
| Recuperação | Reenvio da conclusão não gera orientação nem duplica progresso; reinício preserva dados |
| Observabilidade | Sessão, operação, resultado e conclusão persistidos; eventos não inferidos |
| Segregação | MySQL 5.7 local, AGENT_VALIDATION, mh_internal_test; sem efeitos externos |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados |
| Passagem | Harness real e validação do relatório; revisões independentes mantidas |

## Resultado

Em 28/10/2026, passaram 25 testes focados do backend, 26 do executor (uma fixture
externa opcional não aplicável), 12 casos de navegador nos três dispositivos,
29 controles do publicador e 16 testes do empacotador de evidências. TypeScript
e build Vite também passaram. O relatório real da matriz foi aceito tanto pelo
runner de Psique quanto pelo validador do backend.

O harness executou nove combinações na v17 e nove na v16. Depois do ajuste dos
materiais, foi repetido somente o caso aderente afetado: sete dias alternando
comentário e omissão, com os três materiais contendo a orientação persistida.
Os materiais não inventam comentário, aplicação ou preferência. Concorrência,
origem estrangeira, isolamento e reinício real foram verificados em MySQL 5.7.
As capturas foram inspecionadas; o botão funciona sem escrita adicional.

O publicador passa a aguardar a capacidade explícita de comentário opcional.
A nova atestação v4 preserva as anteriores e registra as fontes revalidadas.
Resumo: [relatório local](evidencias/vega-comentario-opcional-v1/local-report.json).

O catálogo compartilhado também integra a atestação de Mira. Seu registro foi
comparado semanticamente com a base e permaneceu idêntico; os quatro testes de
contexto conferem os contratos reais de Mira e Vega. A atestação incremental
de Mira atualiza somente essa referência compartilhada, preservando fontes,
provas, limites e aprovações históricas. Não houve nova execução de Mira.

Uma execução Maven concorrente com a aplicação excedeu os 3 GiB da sandbox.
Os testes passaram em série após encerrar somente os processos locais próprios.
Não foi alterada configuração produtiva para acomodar a limitação da sandbox.

### Limite operacional observado

O banco registra US$ 30,8271356 conhecidos estimados na preparação de Vega
(experimentos 100/103/106/109/111 e pesquisa 73), além de US$ 3,50 de reservas
históricas, sem afirmar cobrança conciliada. O teto confirmado de US$ 30 foi
dirigido ao ciclo #16; o ciclo #18 permanece sem autorização própria de mídia.
Nenhuma tarefa nova de modelo foi aberta nesta correção. A retomada independente
e o lançamento dependem de decisão explícita de limite e dos gates restantes.

Testes locais não comprovam vendas nem contribuição. Mídia, produção audiovisual
e tarefas de modelo conservam seus limites; a correção não autoriza despesas.

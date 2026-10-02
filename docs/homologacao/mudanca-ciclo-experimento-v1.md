# Uma mudança, um novo ciclo e experimento — 02/10/2026

## Escopo e decisão

A cadeia v25 explicitava um ciclo por experimento, mas a orientação e o comando REWORK
permitiam trocar versão no mesmo experimento; a expansão também alterava seu envelope.
O usuário decidiu separar cada mudança das condições testadas. O backend já tem identidade
única de experimento por ciclo, predecessor, idempotência e versionamento da cadeia.
O editor da cadeia não expunha os comandos de rascunho/publicação existentes.

Alternativas: editar o experimento histórico (menor esforço, mistura a comparação);
criar apenas outro experimento (separa números, perde a passagem formal e memória);
usar novo ciclo e experimento pelos contratos existentes (esforço moderado, conserva
atribuição, custos e evidências). Aplicada a terceira, com política explícita no BPM.

Novas definições com `experimentChangePolicy=CHANGE_PER_CYCLE_V1` substituem REWORK e
expansão no mesmo registro pela decisão ADJUST, seguida da criação do sucessor pelos
comandos existentes. Preparação ainda sem publicação pode registrar mudança sem inventar
vendas ou conciliação; depois de exposição, a conciliação e os gates são obrigatórios.
Definições históricas não são migradas implicitamente. Após a publicação, a governança vigente
também impede novas mudanças no mesmo experimento de ciclos anteriores ainda abertos.
Não há autorização de gasto.

## Matriz anterior aos testes

| Cenário | Aceite |
| --- | --- |
| Versionar cadeia pela tela | Clonar, editar contribuições e versões de processos, salvar e publicar usando endpoints existentes; histórico legível pelo ID exato. |
| Erro e rascunho | Falha da API aparece; não se publica edição não salva; processos aposentados/duplicados são recusados pelo backend. |
| Mudança em preparação | Novo criativo orienta sucessor com causa, aprendizado, hipótese e retorno; nenhuma métrica fictícia nem alteração no experimento predecessor. |
| Mudança após exposição | Sem conciliação válida ou com campanha em operação, bloquear encerramento comercial; preservar custos e prova original. |
| Não contornar pela API | REWORK e AUTHORIZE_SCALE diretos recusados pelo contrato novo; mesmos comandos governam a tela. |
| Histórico | Definições e eventos anteriores são preservados; a política publicada governa novas mudanças inclusive nos ciclos abertos; novo experimento do mesmo produto e replay sem duplicidade. |
| Harness | Objetivos dos seis processos e subprocessos envolvidos descrevem mudança única, predecessores, evidência, limite e isolamento; comandos chegam ao contexto de Atena. |
| Contratos de publicação | Atividades, recursos, rotas por tipo e grafos preservados; nova política explícita no subprocesso; fontes anteriores e resultados de Capella intactos. |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; edição e navegação legíveis, sem overflow horizontal. |
| Integração local | UI com API simulada e backend com dependências locais; salvar/carregar política, aplicar comando e conferir persistência lógica e histórico. |
| Métricas | Eventos próprios do novo ciclo; QA não chama mídia, modelos, checkout ou banco produtivo; testes não comprovam venda ou lucro. |
| Entrega | Diff revisado, testes do backend e frontend, build, PR/revisão/merge e workflows aplicáveis; depois editar e verificar a cadeia pela UI publicada. |

## Evidências de partida

Tela e API em 02/10/2026: cadeia #25 v25, seis processos, ciclo BPM #112 v6.
MCP: Capella #7 não possui ciclo registrado; experimento #88 consta INVALIDATED,
com janela 25–29/09/2026. Este trabalho não cria uma comparação comercial sem preencher
seu contrato, não altera o teto do #88 e não transfere para ele o novo criativo.


## Lacuna adicional confirmada na validação das candidatas

O validador real rejeitou metadados legados reproduzidos da cadeia vigente. MCP confirmou
Comunicação v7–v10 como `CORE` (v1–v6 eram `VALUE_PROCESS`) e Degustação v2–v4 sem pai
(v1 apontava Comunicação). A migração anterior copiava esses campos sem validá-los pelo
service. As candidatas recuperam `VALUE_PROCESS` e o pai canônico da degustação; somente
as novas versões mudam. O editor e o comando da cadeia recusam tipos fora do contrato.
O teste `CycleChangeDefinitionsTest` submete as 13 candidatas ao validador real antes da
publicação. No subprocesso de ciclos, rótulos antigos misturavam vários agentes em
atividades de coordenação; a nova versão distingue coordenação backend, autoria de Atena
na proposta e autorização humana, preservando os executores especializados nos seus BPMs.

O mesmo teste mostrou incompatibilidade entre o validador genérico do cadastro e os domínios
de construção exigidos pelo runtime (`PdeAgentValidationProcessContract`). A correção aceita
as combinações canônicas exatas e o gate determinístico, sem ampliar permissões dos agentes.
As novas atividades de revisão da homologação recebem as chaves técnicas ausentes; o editor
expõe autoria, domínio, tipo do fluxo e política para que toda publicação ocorra pela tela.
Os testes mantêm rejeição de agentes/contextos incompatíveis e de autoria compartilhada.

## Validação local concluída

- Suíte completa do backend e regressões finais dos contratos afetados: 3.815 casos nos
  relatórios, zero falhas/erros e 24 dispensas preexistentes. As regressões incluem a adoção
  da política em ciclos abertos antigos sem salvar nem migrar sua ocorrência.
- Suíte completa do frontend: 833 testes aprovados; rodada final dos dois editores com
  sete testes aprovados, incluindo um caso adicional de política, autoria e tipo do fluxo.
  TypeScript e build de produção aprovados.
- MySQL 5.7 da engine isolada: alteração direta recusada, fila de publicação protegida,
  predecessor preservado, replay idempotente, sucessor com outro experimento e memória,
  ausência de métricas fictícias; exposição anterior exige conciliação mesmo com status
  posteriormente divergente. Nada foi chamado em provedores ou ambientes comerciais.
- UI local com API/MySQL: criar, editar, publicar e voltar ao ID histórico em desktop,
  iPhone 15 Pro e Pixel 7. UI com catálogo fotografado e APIs locais simuladas: os 13
  payloads foram comparados integralmente às candidatas; editor do ciclo também validado
  nos dois celulares. Sem erros de página ou overflow horizontal.
- Três regressões Python da transformação pura, validação de topologia das 13 definições,
  formatação e análise estática aplicáveis aprovadas. O script local consultado passou em
  `bash -n` e `shellcheck`.
- Pacote do backend conferido: classes testadas idênticas às empacotadas, 722 recursos
  externos íntegros e inicialização do catálogo de 490 cartões no JAR executável.

Comparação: antes havia retorno que alterava a versão do mesmo experimento e expansão no
mesmo registro. Com a política publicada, esses comandos deixam de ser aceitos; uma mudança
encerra sua preparação/decisão com causa e direciona o sucessor. Continuar coleta inalterada
e corrigir conciliação continuam possíveis. Os testes não medem ganho comercial, latência ou
custo de modelos: não houve consumo pago. Publicação e conferência final pela tela serão
registradas no PR e no relatório da entrega, vinculadas ao SHA validado.

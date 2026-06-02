# Análise um a um — alinhamento com objetivo do módulo de vídeo

## Objetivo usado como critério
1. Criar personagem compatível com o nicho.
2. Fazer o personagem falar com o público para vender produto ou ser o próprio conteúdo.
3. Garantir criação orientada por IA com base responsável (ciência + ética), especialmente integrável ao MDS.

## Resultado da triagem (documento por documento)

| Documento (origem) | Alinhamento | Decisão | Justificativa curta |
|---|---|---|---|
| `Comandos-Patterns-Video.txt` | Médio | Manter fora de `melhores` | É roteiro operacional de sprints, não base conceitual do produto. |
| `archive/deep-research-report (2).md` | Baixo | Manter fora | Arquivado/deprecated. |
| `archive/deep-research-report (3).md` | Baixo | Manter fora | Arquivado/deprecated. |
| `archive/deep-research-report (4).md` | Baixo | Manter fora | Arquivado/deprecated. |
| `archive/deep-research-report (5).md` | Baixo | Manter fora | Arquivado/deprecated. |
| `archive/novo.txt` | Baixo | Manter fora | Arquivado/deprecated. |
| `avatar-implementation-plan.md` | Médio | Manter fora | Plano geral de avatar por tenant; pouco focado no avatar de venda em vídeo. |
| `avatar-module-architecture.md` | Alto | **Movido para `melhores`** | Define base técnica e governança para avatar/render/pipeline confiável. |
| `avatar-module-status-atual.md` | Alto | **Movido para `melhores`** | Mostra o estado real do módulo e os gaps para produção. |
| `avatar-sales-video-canonical-artifacts-initial.md` | Alto | **Migrado para `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md`** | Era o principal modelo canônico de artefatos e foi centralizado no arquivo canônico global. |
| `avatar-sales-video-implementation-history-protocol.md` | Médio | Manter fora | Importante para rastreabilidade, mas não é o núcleo do objetivo funcional. |
| `avatar-sales-video-implementation-history.md` | Médio | Manter fora | Histórico de execução, útil mas secundário para definição do produto. |
| `avatar-sales-video-implementation-plan-v2.md` | Médio | Manter fora | Planejamento incremental, não especificação funcional final. |
| `avatar-sales-video-implementation-plan-v3.md` | Médio | Manter fora | Planejamento incremental, não especificação funcional final. |
| `avatar-sales-video-implementation-plan.md` | Médio | Manter fora | Planejamento incremental, não especificação funcional final. |
| `docs/swagger/avatar-sales-video-integration-swagger.yaml` | Médio | Manter fora | Contratos de integração úteis, mas menos direto para personagem/conteúdo. |
| `avatar-sales-video-restart-plan.md` | Médio | Manter fora | Foco em retomada de execução por sprint. |
| `avatar-scene-composition-spec.md` | Alto | **Movido para `melhores`** | Define como o avatar aparece/fala visualmente com qualidade técnica. |
| `ini.md` | Baixo | Manter fora | Sem conteúdo útil. |
| `mapeamento-documentacao-avatar-video.md` | Médio | Manter fora | Inventário documental, não orienta criação do conteúdo/avatar em si. |
| `sales/README.md` | Alto | **Movido para `melhores`** | Delimita o módulo Avatar de Venda com foco em conversão e diálogo. |
| `sales/avatar-sales-character-profiles.md` | Alto | **Movido para `melhores`** | Documento mais alinhado ao objetivo de personagem por nicho. |
| `sales/avatar-sales-conversion-events-spec.md` | Alto | **Movido para `melhores`** | Mede performance real do avatar como conteúdo/oferta. |
| `sales/avatar-sales-dialogue-orchestration.md` | Alto | **Movido para `melhores`** | Define a fala do avatar por estados e intenção comercial. |
| `sales/avatar-sales-objection-playbook.md` | Alto | **Movido para `melhores`** | Cria respostas éticas para objeções, alinhado a persuasão responsável. |
| `sales/avatar-sales-offer-knowledge-schema.md` | Alto | **Movido para `melhores`** | Estrutura conhecimento da oferta para IA responder com precisão. |
| `sales/ini.md` | Baixo | Manter fora | Sem conteúdo útil. |
| `video-management/especificacao-telas-modulo-video.md` | Médio | Manter fora | Útil para operação de jobs, menos direto para estratégia de personagem. |
| `video-module-data-model.md` | Alto | **Movido para `melhores`** | Sustenta persistência canônica do fluxo de avatar/sales video. |

## Conteúdo reunido na pasta `melhores`
- Arquitetura e governança do avatar de vídeo.
- Composição visual/técnica da cena.
- Estado atual para priorização.
- Artefatos canônicos para consistência entre IA e backend (agora centralizados em `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md`).
- Núcleo comercial de personagem, diálogo, objeções e medição de conversão.
- Modelo de dados para sustentação operacional.

## Observação sobre MDS (ciência + ética)
Os documentos movidos criam uma base forte de personagem, roteiro e operação. Como próximo passo, recomenda-se adicionar um documento dedicado de **policy de evidências MDS** (fonte científica mínima, score de credibilidade e critérios éticos de bloqueio de afirmações) para fechar totalmente o seu objetivo.

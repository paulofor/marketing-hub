# Matriz de homologação — Íris, agente de comunicação v1

## Gargalo, evidência e decisão

O gargalo estrutural corrigido é a concentração em Dédalo da construção do PDE pós-compra e da
persuasão pré-compra. O histórico operacional consultado em 2026-08-28 registrava 91 tarefas para
Dédalo, enquanto o catálogo vigente lhe atribuía também landing, copy e peças não audiovisuais.
Isso mistura qualidade da entrega com conversão e impede aprendizado especializado.

A métrica esperada é 100% das novas atividades de comunicação pré-compra atribuídas exclusivamente
a Íris, com Dédalo restrito à experiência usada depois da compra e Têmis restrita à revisão
independente. Nos três próximos PDEs, serão acompanhados aprovação na primeira tentativa, ciclos de
retrabalho, tempo, custo e defeitos de correspondência entre produto e promessa. A divisão continua
se o retrabalho cair sem criar atraso dominante; ajusta se o handoff Íris–Dédalo virar o maior
gargalo; e para se não houver ganho mensurável de qualidade ou conversão.

| Alternativa | Benefício | Risco | Esforço | Aderência ao objetivo |
| --- | --- | --- | --- | --- |
| Manter Dédalo completo | fluxo curto | aprendizagem misturada e autoexpansão de escopo | baixo | baixa |
| Separar tarefas no mesmo agente | melhora o catálogo | a mesma identidade continua decidindo dois domínios | médio | média |
| Criar Íris com executor e contrato próprios | especialização, métricas e revisão independente | novo handoff governado | médio | alta |

Escolha: criar Íris como nono agente. O que a cliente usa depois da compra pertence a Dédalo; o que
a convence antes da compra pertence a Íris. A estratégia continua congelada por Atena, os limites
econômicos por Plutus, a experiência humana é revisada por Psique, a integridade por Têmis, o vídeo
é produzido por Apolo e a operação posterior pertence a Hermes.

## Matriz ponta a ponta local

| Dimensão | Caminho feliz | Validações e falhas | Evidência esperada |
| --- | --- | --- | --- |
| Identidade | Íris existe em `agent`, possui versão, entradas, saídas, funções e harness | chave duplicada, versão ausente ou domínio incompatível bloqueia | cadastro e contrato versionados |
| Fronteira | Atena e Plutus entregam estratégia e limites; Dédalo entrega produto e provas; Íris materializa comunicação | Íris tenta redefinir mercado, oferta, preço ou produto e a saída é rejeitada | referências, versões e hashes preservados |
| Comunicação | Íris produz mensagem, copy, landing, e-mail, peças estáticas e briefings por canal | evidência ausente bloqueia alegação; prova fictícia ou publicação é proibida | pacote funcional separado da auditoria |
| Imagem comercial | `iris-image-studio` produz somente `LANDING`, `ADS` e `SOCIAL` com prova aprovada | `DELIVERY`, `PRODUCT_PROOF`, geração livre ou autoaprovação bloqueiam antes da API externa | job, referência, request, response, custo, binário e gate independente |
| Audiovisual | Íris entrega briefing e Apolo produz vídeo/áudio | Íris retorna vídeo final ou Apolo redefine a mensagem e o contrato falha | briefing e artefato audiovisual distintos |
| Landing | fila `pending` de Íris recebe somente a atividade publicada e devolve HTML completo | HTML ausente, checkout alterado ou source reference cruzada bloqueia | tarefa, request, response, HTML, custo e hash |
| Gates | Psique e Têmis recebem tarefas próprias depois do ativo real | coautoria, autoaprovação ou reescrita pelo revisor bloqueia | pareceres independentes e causas persistidas |
| Orquestração | o backend enfileira a próxima etapa depois do callback | worker dispara etapa seguinte, publica ou gasta e o contrato falha | transições persistidas exclusivamente no backend |
| Observabilidade | request, response bruto, modelo, esforço, tokens, custo, duração, erro e fonte ficam correlacionados | parse, timeout ou callback falho preserva stack trace e mantém gate fechado | execução auditável e logs com `taskId` |
| Tier do modelo | Codex solicita `default` e registra `STANDARD` com exceção explícita porque o catálogo atual não anuncia Flex | configuração Flex omitida pelo runtime, tier falso ou troca para `priority` falha | comando, catálogo local e auditoria coerentes |
| Controle | PLAY permite polling e STOP mantém Íris parada | indisponibilidade do backend é fail-closed | health, versão e controle automático |
| Segregação | plano, produto, experimento e tarefa usam apenas seus artefatos | referência de outra entidade é recusada | IDs e hashes distintos |
| Catálogo | processos novos usam `COMMUNICATION_MATERIALIZATION` somente com Íris | Dédalo em comunicação, Têmis em criação ou dois agentes no mesmo nó falham | teste de matriz e diagramas versionados |
| Compatibilidade | versões históricas de Dédalo permanecem consultáveis; novas execuções usam Íris | migração reescreve histórico ou toma tarefa ativa antiga e falha | versões publicadas/retiradas e auditoria preservada |
| MySQL 5.7 | changelog aplica e reaplica de forma idempotente | include não relativo, temporal inválido, 1093 ou duplicidade falha | validação física no runner MySQL 5.7 |
| Interface | harness exibe Íris e todos os arquivos comportamentais sem esconder agentes existentes | conteúdo truncado, arquivo omitido ou identidade duplicada falha | testes React e contrato do catálogo |
| Navegadores | desktop, iPhone 15 Pro e Pixel 7 mantêm leitura e comandos do harness | overflow, foco inacessível ou ação errada falha | Playwright com dados de teste segregados |
| Segurança comercial | nenhuma rodada publica, envia mensagem, altera preço, campanha ou orçamento | qualquer efeito externo não autorizado interrompe a homologação | ausência de efeitos e callbacks locais |

Uma primeira rodada completa sem defeitos conclui a homologação. Se a rodada revelar um defeito e
houver correção, após a última correção serão executadas duas rodadas locais completas e
consecutivas sem falha; qualquer novo defeito reinicia a contagem.

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

## Rehomologação da distribuição executável — 2026-08-30

O banco operacional já publicava seis atividades para Dédalo e seis para Íris, cada uma com um único
dono e domínio correto. A divergência estava no runtime: uma ponte antiga ainda procurava qualquer
tarefa de Dédalo para abastecer a fila de criativos, o AI Worker se apresentava como Dédalo, o
monitor atribuía a produção visual a ele e seu worker implementava apenas três das seis atividades
atuais de produto. Além disso, o scheduler histórico de landing reservava a próxima tarefa de
Dédalo sem filtrar processo, podendo capturar um contrato de produto e tratá-lo como landing.

| Caso | Caminho feliz | Falha protegida | Evidência local |
| --- | --- | --- | --- |
| Catálogo versus consumidores | todos os 12 pares vigentes possuem um único executor compatível | atividade de Íris em Dédalo ou de Dédalo em Íris é recusada | testes de contratos dos dois workers |
| Arquitetura | Dédalo executa `productArchitecture` com três alternativas e saída funcional | decisão ou arquitetura ausente bloqueia | schema v5 e validador do consumer |
| Construção | jornada, entregáveis e acesso continuam executáveis | contexto de outro produto, pacote parcial ou acesso sem falhas previstas bloqueia | prompts genéricos e testes unitários |
| Degustação | Dédalo entrega microexperiência e artefato real limitado | descrição sem conteúdo, eventos ou segregação de QA bloqueia | prompt/schema `pde-tasting-v1` |
| Venda e entrega | personalização paga é consultada antes das filas de aquisição | venda, versão, conteúdo, qualidade ou handoff ausente bloqueia | prompt/schema `pde-delivery-v1` |
| Comunicação | Íris produz copy/criativo/landing e usa prova real de Dédalo | prompt que se identifique como Dédalo falha | contrato do AI Worker e consumer de Íris |
| Monitor | materialização visual aparece em Íris | Dédalo ou Têmis receber esse trabalho falha | teste do monitor operacional |
| Compatibilidade | endpoints técnicos de experimento continuam funcionando | callback técnico concluir tarefa BPM alheia é impossível | remoção da ponte genérica e testes do controller |
| Auditoria | modelo, prompt, resposta, tokens e custo continuam persistidos pelo registrador de IA ou pela tarefa BPM | execução sem identidade ou contrato registrado falha | testes de prompt, callbacks e auditoria |
| Segregação | referências de produto, venda, processo e atividade vêm do snapshot | contrato homônimo ou contexto cruzado é recusado | resolução pelo par completo e schemas estritos |
| Interface e dispositivos | nenhuma superfície visual foi alterada | comportamento divergente por navegador não se aplica | marcado como não aplicável nesta mudança de backend/worker |
| Segurança comercial | testes locais não publicam, enviam comunicação nem alteram preço, campanha ou orçamento | qualquer efeito externo interrompe a rodada | sandbox read-only e dependências simuladas |

Critério da rodada: todos os testes Java relevantes, schemas JSON, contratos canônicos e busca de
vazamentos de identidade devem passar juntos. Se a primeira rodada revelar defeito, a contagem volta
a zero e serão exigidas duas rodadas completas consecutivas depois da última correção.

## Resultado local final — 2026-08-30

Após a revisão detectar que uma landing histórica podia ocultar no monitor uma atividade vigente de
produto, a prioridade foi corrigida e a contagem reiniciada. Duas rodadas completas e consecutivas
terminaram sem falha. Em cada rodada:

- 551 casos Java foram contabilizados nos seis módulos, com zero falhas, zero erros e três testes
  condicionais ignorados pelo próprio contrato das suítes;
- três testes Node do MCP agregado de Atena passaram;
- os `verify` completos de Dédalo, Íris, AI Worker, Atena e Têmis e os testes relevantes do backend,
  incluindo ArchUnit, passaram;
- harness e schemas JSON foram parseados, o diff não apresentou erro e nenhuma referência proibida
  à ponte genérica, à ativação genérica de landing ou à identidade antiga permaneceu no runtime;
- nenhum changelog foi alterado, porque a consulta operacional confirmou que os 12 pares vigentes já
  possuem um único agente correto no banco;
- interface e navegadores permaneceram não aplicáveis, pois nenhum código visual foi modificado;
- nenhuma publicação, mensagem, preço, campanha, orçamento, venda ou evento comercial foi produzido.

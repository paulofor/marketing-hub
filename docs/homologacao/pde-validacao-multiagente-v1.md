# Homologação local — validação multiagente PDE v1

Data: 2026-09-06

## Objetivo e fronteiras

Comprovar localmente que o processo `pde-construction-approval` v7 substitui a dependência de
convites por uma homologação executável, preserva o histórico v6 e libera somente a preparação de
comunicação em `STOP`. Nenhuma execução desta matriz pode contar pessoa, preferência, checkout,
venda, receita, satisfação, campanha ou gasto.

## Alternativas avaliadas

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Adaptar o v6 no lugar | Menos código | Reescreve a semântica e o histórico das leituras | Rejeitada |
| Registrar agentes como participantes | Avanço aparente rápido | Fabrica evidência humana e contamina conversão | Proibida |
| Publicar v7 paralelo e migrar somente artefatos imutáveis | Histórico íntegro, fluxo auditável e reversível | Mais contratos e testes | Adotada |

## Matriz ponta a ponta

| Dimensão | Cenário | Resultado esperado |
| --- | --- | --- |
| Migração | MySQL 5.7 com Mira no v6 e quatro artefatos concluídos | v7 publicado, v6 preservado/retirado, quatro referências copiadas sem mutação e reaplicação idempotente |
| Compatibilidade | Rollback e nova aplicação | v6 volta a ser publicado, histórico antigo permanece e v7 é reconstruído sem duplicidade |
| Caminho feliz | Entrada aderente | Rotina documental pronta em até dez minutos e cenário concluído |
| Validação | Entrada vazia e evento humano | Botão bloqueado e preferência/checkout recusados para sessão sintética |
| Recuperação | Primeira geração interrompida e reload | Entrada persistida, retomada e conclusão sem nova identidade |
| Segurança | Pedido clínico | Resultado bloqueado, explicação segura e nenhum momento de valor inventado |
| Navegadores/dispositivos | Chromium desktop 1440, iPhone 15 Pro e Pixel 7 | Sem overflow, controles legíveis e screenshots da mesma versão |
| Integrações | `pending`, upload de evidência, callbacks de sucesso/falha e gate | Test doubles comprovam contratos; backend permanece autoridade do avanço |
| Observabilidade | Harness, Psique e Têmis | Entrada, saída bruta, modelo, custo, duração, artefatos, hashes, decisão e causa persistíveis |
| Métricas | Eventos dos cinco percursos do harness | `AGENT_VALIDATION` + `mh_internal_test`, zero sessão humana e zero métrica comercial |
| Efeitos externos | Toda a matriz | pagamento, publicação, campanha e mídia desativados; gasto igual a zero |

## Critério de aceite

- todos os testes unitários e contratos dos quatro módulos passam;
- migração e rollback passam fisicamente no MySQL 5.7;
- o harness real conclui cinco percursos no protótipo local e gera screenshots íntegros;
- frontend compila e a experiência não exibe o codinome interno;
- duas rodadas completas consecutivas passam depois da última correção encontrada.

## Resultado

Homologação concluída em duas rodadas locais completas e consecutivas, sem falhas após a última
correção:

- 2.360 testes do backend, 83 do Customer Agent, 167 do PDE e 89 de Têmis passaram em cada
  rodada; quatro testes do backend permaneceram ignorados pela suíte preexistente;
- build e typecheck do frontend, contratos JavaScript, Actionlint e jornadas em Chromium desktop,
  iPhone 15 Pro e Pixel 7 passaram nas duas rodadas;
- aplicação, reaplicação idempotente, rollback e nova aplicação do changelog passaram fisicamente
  no MySQL 5.7 em cada rodada;
- a imagem real do Customer Agent foi construída pelo Dockerfile versionado e retornou
  `APPROVED`, com cinco cenários, três dispositivos e cinco screenshots íntegros em cada rodada;
- nenhuma execução criou participante, preferência, checkout, venda, receita, campanha, gasto ou
  outro efeito externo; containers, volumes e imagens temporárias foram removidos ao final.

A primeira execução integral identificou que os novos prompts de Psique e Têmis ainda não estavam
no catálogo auditável. A causa foi corrigida no manifesto, coberta por contrato e registrada como
`LOOP-AGENTE-PROMPT-FORA-DO-CATALOGO`; somente então a contagem das duas rodadas foi reiniciada.

# Mira — candidata privada v2

## Escopo e evidência

Preparação autorizada de produto e comunicação, com teto total de US$ 10 de IA, sem mídia ou geração paga de vídeos. As tarefas 600–607 do experimento 99 consumiram aproximadamente US$ 2,0917368. Construção e testes determinísticos locais não consomem esse orçamento.

A arquitetura anterior confundia especificação com implementação. O gate atual corretamente exige experiência executável. A referência comercial inicia com dois produtos obrigatórios; o contrato aceita um. O teste local confirmou quatro campos manuais na referência, dois na entrada reduzida, sem perda dos produtos efetivamente informados. Isso não comprova causa de rejeição comercial: o experimento histórico teve apenas duas visitas humanas.

Alternativas: alterar a experiência comercial histórica; criar uma candidata privada; publicar somente uma demonstração estática. Escolhida a candidata privada: preserva o histórico, valida o funcionamento e não aciona compra, publicação comercial ou integrações pagas.

## Matriz definida antes da implementação

| Dimensão | Critério |
|---|---|
| Entrada | Referência com dois produtos; reduzida com um; inclusão e remoção opcional até 12; validação de todos os produtos incluídos; nenhum campo de idade ou contato |
| Resultado | Organizar exclusivamente texto documental informado; comparar as duas condições com inventário idêntico; texto insuficiente gera bloqueio explicável |
| Percurso | ADHERENT, RECOVERY, SAFETY × DESKTOP_1440, IPHONE_15_PRO, PIXEL_7 × REFERENCE/REDUCED = 18 casos; sem repetição indiscriminada |
| Recuperação | Retomar entrada/resultado após recarga e reinício; resposta perdida após gravação não gera outra organização; erro transitório oferece retomada |
| Limites | Duas organizações úteis por pacote; repetição idêntica não consome outra; concorrência serializada; tentativa técnica simulada fica segregada de custo real |
| Segurança | Credencial opaca, expiração e revogação; sessão de outro ciclo não acessa a prova; objetivos clínicos bloqueados; sem pagamento, campanha ou provedor de IA |
| Dispositivos | Chromium desktop e emulação iPhone 15 Pro/Pixel 7; foco, teclado virtual por viewport reduzida, rótulos, alertas e ausência de rolagem horizontal |
| Observabilidade | Identidade produto/ciclo/experimento/versão/condição/cenário/dispositivo; entradas, saídas, eventos, hashes e horários persistidos; teste não é venda |
| Economia | Custos reais de geração por IA permanecem desconhecidos nesta implementação determinística; não extrapolar contribuição simulada para vendas |
| Publicação | Código, imagem e proxy versionados; PR revisado e integrado; workflows e saúde publicados; revisões independentes de Psique/Têmis antes do avanço comercial |

## Limites

A candidata usa organização determinística, explicitamente identificada. Não comprova geração por IA, diagnóstico, compatibilidade clínica, satisfação humana, demanda ou margem real. Nenhuma credencial real de cliente ou SMTP é usada. A versão comercial anterior permanece disponível para consulta histórica; a candidata tem entrada e contrato próprios. A aprovação do código não equivale ao aceite comercial dos agentes.

## Primeira rodada local

A matriz detectou um erro no nome acessível do objetivo: texto pré-preenchido dentro do próprio `label` impedia o harness de localizar o controle pelo nome exato. Corrigido com associação explícita entre rótulo e campo, aplicável aos demais controles. Reexecutados os 18 percursos afetados pela mudança de marcação: todos passaram; resultado ou bloqueio em até um segundo, conforme os relógios locais do servidor e browser.

A API rejeitou produto opcional incompleto e lista com 13 itens. Dez gerações simultâneas do mesmo pacote consumiram uma única organização. Outra identidade de ciclo produziu relatório próprio sem segredo. Os testes unitários cobrem referência/versão/produto divergentes, objetivo clínico, documento insuficiente, consumo, revogação, prazo e recuperação. Dados sintéticos não entram no funil comercial.

O harness passou a despachar a candidata por identidade e linhagem explícitas e exige as 18 combinações distintas, evitando repetir o caminho histórico ou aceitar somente contagem de capturas. Preserva checkpoints parciais quando uma etapa do próprio teste falha. As experiências históricas de Mira, Vega e Alcyone continuam cobertas pelos testes já existentes.

A construção de produção foi executada pelo runner versionado, com validação JPA do schema MySQL 5.7 e os 18 percursos aprovados. O teste anterior da superfície comercial também passou nos 18 casos existentes, incluindo vídeo, checkout e políticas. Uma rodada ampla sofreu disputa local entre compilações; as cinco classes afetadas passaram na repetição sequencial com o Node válido.

A revisão local identificou que uma segunda entrada podia esconder o resultado útil anterior. A candidata agora preserva um histórico funcional consumível separado da entrada corrente, além da auditoria técnica. Teste específico cobre correção, bloqueio e conclusão da segunda organização sem apagar a primeira.

A validação integrada encontrou uma divergência entre executor e backend: o harness novo produzia 18 provas, mas o gate aceitava somente as matrizes históricas de 5 ou 9. A fonte compartilhada agora reconhece o contrato explícito `PDE_DOCUMENTED_INPUT_COMPARISON_V1`, sem exceções por produto ou ID. Exige sessões distintas, inventário e saída equivalentes, redução de campos, recuperação, bloqueio seguro, prazo e custo externo zero. As matrizes antigas permanecem cobertas. O runner compara o relatório realmente capturado com ambos os contratos Java, prevenindo nova divergência antes do PR.

## Consolidação local em 06/10/2026

A rodada final de construção de produção, MySQL 5.7, browser e contratos backend/executor passou nos 18 percursos. O percurso adicional da segunda organização passou após bloqueio, recarga e correção, preservando o primeiro resultado. As suites locais acumuladas registram 4.165 testes de backend e 153 do executor, sem falhas ou erros; exclusões condicionais históricas permanecem explícitas nos relatórios. O proxy da imagem construída pelo Dockerfile encaminha a candidata ao backend principal, preserva a rota histórica e aplica cache privado e exclusão de indexação. Validadores Liquibase, Bash, ShellCheck, seleção de publicação, contratos de release e pacote de provas também passaram.

O runner é `bash infra/testing/mira-candidate/run-local.sh`, com `MIRA_TEST_COMPOSE_PROJECT` exclusivo. Na sandbox com engine remota, usar `MIRA_TEST_DB_HOST=sandbox-docker`; no GitHub Actions, o padrão é `127.0.0.1`. Evidências são copiadas para `MIRA_TEST_EVIDENCE_DIR`, e a topologia é removida no encerramento. O teste não usa credenciais ou cadastros comerciais.

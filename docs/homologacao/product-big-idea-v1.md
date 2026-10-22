# Homologação da Big Idea do produto

Matriz definida antes da implementação e dos testes. Dados sintéticos isolados, sem chamadas de IA, mídia, pagamentos ou eventos comerciais reais.

| Percurso | Aceite |
| --- | --- |
| Criação/evolução | Primeira revisão, edição com motivo, histórico imutável, vigente correta |
| Validações | Campos vazios/incompletos e revisão concorrente recusados sem perda |
| Isolamento | Produto A não recebe versão/contexto de B |
| Atena | Planejamento aprovado cria; rejeição não cria; existente não é sobrescrita |
| Passagem | Tarefa/experimento fixam versão; evolução não troca contexto anterior |
| Recuperação | Leitura/reenvio não gera revisão ou inferência duplicada |
| Legado | Produtos/tarefas anteriores preservados com ausência explícita |
| Persistência | MySQL 5.7, Liquibase aplicação/reaplicação e colunas JPA alinhadas |
| Cadeia | Nova versão mantém atividades, responsáveis e vínculos históricos |
| Tela | Desktop, iPhone, Pixel: criar, editar, histórico, erro e carregamento |
| Auditoria | Autor, origem, motivo, versão, horários; testes não são vendas |

## Resultado local — 22/10/2026

- API real Spring/JPA e MySQL 5.7: criação, evolução, histórico, repetição idêntica, conflito concorrente, entradas inválidas, isolamento, referência do experimento e versão nova da cadeia aprovados.
- Reinício da API e reaplicação do Liquibase preservaram exatamente o histórico. Hibernate validou as entidades contra as colunas criadas pelos changelogs. A cadeia anterior manteve seis vínculos; a nova também, com Processo 2 atualizado.
- Navegação Chromium desktop, iPhone 15 Pro e Pixel 7 emulados: criação e evolução até v2, histórico e ausência de overflow horizontal aprovados. API e produtos sintéticos exclusivos; nenhuma chamada externa. Erros e conflito mantêm o rascunho nos testes de componente.
- Backend: rodada ampla com 5.203 testes encontrou somente a falta dos dois novos arquivos no catálogo do harness. Corrigido o catálogo, as 268 regressões relevantes passaram, incluindo dois novos testes do callback. Relatórios consolidados: 5.205 testes, zero falhas/erros, 57 integrações opcionais não executadas nessa rodada. A persistência desta entrega foi exercitada separadamente no MySQL real.
- Atena: 64 testes aprovados; contrato completo, preservação da ideia existente, referências distintas e compatibilidade legada. Frontend: 48 testes pertinentes aprovados após corrigir acesso a erro nulo; TypeScript e build aprovados.
- Verificação estática: Liquibase MySQL 5.7, comentários das classes/métodos, Spotless dos Java alterados, Prettier, actionlint, contrato do workflow frontend e diff sem whitespace inválido.
- Nova capacidade incluída no catálogo existente do harness; testes de interface e persistência incorporados aos workflows existentes. Nada de infraestrutura de agentes paralela.

Reprodução: `BIG_IDEA_COMPOSE_PROJECT=aihub-<sessao-exclusiva> python3 infra/testing/product-big-idea/run-local.py`. O runner usa dependências locais, dados sintéticos, encerra a API e remove containers/volumes ao terminar. `BIG_IDEA_BROWSER=1` também executa a navegação quando o preview local está disponível; scripts de preview e browser ficam na mesma pasta. Os logs são gravados em `artifacts/product-big-idea/persistence` (não versionados).

Limites: testes comprovam funcionamento e prevenção de divergência, não qualidade criativa de uma Big Idea específica nem conversão. Produtos históricos não receberam ideias inventadas ou inferências pagas; a criação acontece pela tela ou pelo planejamento autorizado de Atena. Adoção comercial exige medir redução de revisões por incoerência, tempo até entrega aceita e contribuição em experimentos futuros.

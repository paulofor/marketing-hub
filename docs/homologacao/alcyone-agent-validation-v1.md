# Homologação de Alcyone sem piloto humano — v1

Data da matriz: 2026-09-30.

## Escopo e limites

Alcyone #11 deve sair do contrato humano legado, concluir a construção e atravessar a homologação
multiagente antes de qualquer comunicação comercial. O fluxo não recruta, convida, entrevista,
solicita opinião, representa participante fictícia, cobra, publica, cria campanha ou autoriza mídia.
Agentes comprovam prontidão; o mercado voluntário continuará sendo a única prova de demanda.

## Matriz definida antes da rodada local

| Área | Caminho feliz | Validações e falhas | Evidência esperada |
| --- | --- | --- | --- |
| Contrato | `MARKET_STRATEGY_V4` gera `PDE_AGENT_VALIDATION_V1` | V3 incompleto, cenário/dispositivo extra, flag comercial ou humana bloqueiam | Testes de prompt, schema, consumidor e hook |
| Migração | #11, run #26, instância #414 e tarefa #534 convergem para `product:11@agent-validation-v1` | Estado inesperado ou referência concorrente interrompem o changelog | MySQL 5.7 físico, JSON válido e precondições |
| Dédalo | polling reserva #534, carrega contexto e produz jornada | conexão aceita sem resposta termina por timeout e permite novo ciclo | teste HTTP local, logs com correlação e tarefa auditável |
| Construção | jornada, componentes, audiovisual, acesso e instrumentação pertencem à mesma versão | contrato ausente, artefato incompatível ou efeito externo indevido bloqueiam | resultados/artefatos persistidos pelo backend |
| Homologação | técnica + `ADHERENT`, `RECOVERY`, `SAFETY` + Têmis + gate | erro recuperável, pedido fora de escopo, privacidade, custo ou versão divergente bloqueiam | pareceres separados, causa-raiz e próxima correção |
| Observabilidade | request/response, modelo, tokens, custo, horários e status ficam vinculados à execução | falha não pode desaparecer em thread presa ou apenas em log | telemetria e relatório funcional persistido |
| Métricas | resultado em até 600 s e custo técnico de até R$ 24 por pacote | QA interno não entra em visita, conversão, checkout, venda, receita ou satisfação | `trafficClass=AGENT_VALIDATION`, `mh_internal_test`, flags comerciais falsas |
| Segregação | dados sintéticos ficam no produto #11 e na referência v1 | nenhuma evidência de Mira, Vega ou tráfego real pode ser reutilizada | IDs, fonte, versão e hashes coerentes |
| Navegadores | Chromium desktop 1440, iPhone 15 Pro e Pixel 7 | entrada, resultado, retomada, erro, limite e CTA simulado continuam acessíveis | screenshots e assertions Playwright por perfil |

## Alternativas e escolha

1. Encerrar Alcyone: custo baixo, mas elimina a oportunidade sem testar o mecanismo.
2. Migrar apenas o registro #11: desbloqueia agora, porém o próximo produto volta a nascer humano.
3. Corrigir a autoridade de criação, migrar a execução aberta e limitar o polling: maior esforço
   inicial, melhor aderência ao cânone e menor recorrência. Esta é a opção adotada.

## Resultado

A rodada local foi aprovada em 30/09/2026:

- backend: 3.710 testes, sem erro ou falha; 23 cenários explicitamente ignorados pela própria
  suíte;
- Atena: 44 testes, sem erro, falha ou cenário ignorado;
- Dédalo: 79 testes, sem erro, falha ou cenário ignorado, incluindo servidor local que aceita a
  conexão e retém a resposta até o timeout liberar o polling;
- MySQL 5.7 físico: primeira aplicação, idempotência, rollback não regressivo e reaplicação
  aprovados para Processo 2 v10, cadeia v24, produto #11, run #26, instância #414, tarefa #534 e
  três fontes públicas sem duplicidade;
- validações estáticas Liquibase, `bash -n`, ShellCheck, JSON, Spotless e revisão de whitespace
  aprovadas.

A primeira suíte integral detectou que o catálogo central de Atena ainda não declarava os dois
artefatos v10. O manifesto foi corrigido e o teste de cobertura foi repetido com sucesso; publicar
somente o worker deixaria a auditoria incompleta. A reserva produtiva de #534 e os cenários visuais
da versão gerada dependem do deploy e serão comprovados operacionalmente sem transformar QA em
visita, checkout, venda ou satisfação.

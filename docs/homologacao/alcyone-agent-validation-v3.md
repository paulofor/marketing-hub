# Homologação corretiva da continuidade privada de Alcyone — v3

Data da matriz: 2026-09-30.

## Escopo preservado

Esta evolução fecha o bloqueio #570 sem validação humana. Ela não recruta, entrevista ou simula
participantes; não publica Alcyone comercialmente, não cobra, não cria campanha, não autoriza mídia
e não transforma sinais de agentes em evidência de mercado. O produto #11 permanece `PLANNED`, e
somente o comportamento voluntário do mercado poderá produzir evidência comercial.

## Matriz definida antes da rodada corretiva

| Área | Caminho feliz | Validação e falha | Evidência exigida |
| --- | --- | --- | --- |
| Consentimento | agente aceita `ALCYONE_AGENT_INTAKE_CONSENT_V1` antes de visualizar a entrada | aceite ausente, falso ou de outra versão bloqueia `/input` | `consentVersion` e `consentedAt` anteriores a `inputAcceptedAt` |
| Entrada | ocasião, data e referências sintéticas completas são aceitas | entrada incompleta ou referência corporal não produz pacote | `INPUT_INCOMPLETE` e bloqueio seguro sem provider |
| Resultado | três fixtures do mesmo pacote ficam preservadas | pacote inexistente ou pertencente a outra sessão responde 403 | ID, fingerprint e três imagens idênticos |
| Instrumentação | somente `EXPERIENCE_STARTED`, `VALUE_MOMENT`, `READY_RESULT_USED`, `PREFERRED_OVER_FREE` e `CHECKOUT_STARTED` chegam ao funil | apresentar resultado, criar credencial ou autenticar não gera sinal | lista e auditoria contêm somente os cinco sinais canônicos |
| Marcos | resultado pronto/apresentado, interesse, credencial, autenticação, acesso e retorno são persistidos separadamente | marco não observado continua nulo e não é inferido do anterior | campos anuláveis com horários independentes |
| Interesse | agente declara por que pretende retornar | emissão antes de uso ou interesse é recusada | `saveInterestAt` separado da telemetria |
| Política | agente aceita `ALCYONE_AGENT_CONTINUITY_V1` | versão divergente ou aceite falso é recusado | horário persistido, `humanEvidenceClaimed=false` |
| Credencial | segredo opaco é emitido uma vez | bruto não aparece na persistência, evidência, URL ou tela | somente SHA-256 persistido; credencial permanece no navegador de teste |
| Sessão | credencial cria sessão curta e ambas rotacionam | token ou credencial anterior não pode ser reutilizado | 403 para valores antigos |
| Expiração | continuidade funciona após a sessão vencer | credencial vencida é recusada | relógio determinístico e expiração real |
| Recuperação | nova sessão lê exatamente o pacote preservado | `ACCESS_INVALID`, `SESSION_EXPIRED`, `INPUT_INCOMPLETE`, `HARNESS_FAILURE`, `RESULT_UNAVAILABLE` e `RESUME_FAILED` possuem mensagem e ação | seis falhas reproduzidas e recuperadas sem duplicação |
| Retorno | autenticação, autorização do pacote e retorno são operações distintas | indisponibilidade temporária não registra retorno e pode ser repetida | `accessAuthenticatedAt`, `accessCompletedAt` e `returnedAt` |
| Isolamento | proprietário acessa o próprio pacote | segunda sessão recebe 403 | cenário por sessão e pacote |
| Navegadores | desktop 1440, iPhone 15 Pro e Pixel 7 completam os três cenários | contraste AA, teclado, foco, zoom 200%, movimento reduzido, toque, teclado móvel e overflow permanecem funcionais | matriz 3 × 3, checks estruturados e screenshots |
| Efeitos | checkout continua apenas simulado | qualquer provider, cobrança, publicação, campanha ou mídia reprova | custo zero e flags falsas |

## Decisão arquitetural

Foram comparadas três alternativas. Filtrar marcos apenas no relatório seria barato, mas manteria
dez eventos inconsistentes na fonte. Criar outro subsistema de workflow daria isolamento máximo,
com duplicação de estado e migração desproporcionais. A solução adotada mantém o motor v2, exige
consentimento sintético antes da entrada, separa os cinco sinais de funil dos timestamps
operacionais e amplia o gate determinístico. Ela fecha a causa sem trocar a identidade executável
nem ampliar o risco comercial.

O manifesto v3 preserva os manifestos v1 e v2 como prova histórica imutável. A experiência continua
`alcyone-private-v2`; muda apenas sua atestação de prontidão após a correção. A topologia completa
comprovou que Alcyone permanece isolada das superfícies de Mira e Vega.

## Resultado local

A topologia equivalente à publicação foi executada com backend, frontend Alcyone e agente de
cliente na rede Compose exclusiva da sandbox. O resultado foi `APPROVED`: 9 de 9 cenários passaram
em 10 segundos, combinando os fluxos aderente, recuperação e segurança em desktop 1440 px, iPhone
15 Pro e Pixel 7. O relatório bruto tem SHA-256
`0df4d9279f4a539bbfdd7b8867ee7499c70992a60bfb1483f6f49ba65b30de68` e foi acompanhado por nove
screenshots, uma por combinação de cenário e dispositivo.

Os 31 gates ficaram verdadeiros: consentimento anterior à entrada, somente cinco sinais canônicos,
marcos anuláveis independentes, rotação de credenciais, rejeição da sessão expirada, isolamento do
pacote, seis falhas recuperáveis, contraste AA, teclado, foco, zoom de 200%, movimento reduzido,
área útil móvel e ausência de efeitos externos. O segredo bruto não apareceu na evidência, e as
credenciais anteriores receberam 403 depois da rotação.

As validações complementares terminaram assim:

- 204 testes do backend PDE aprovados, sem falha ou teste ignorado;
- 146 testes Java do agente aprovados, com dois testes de integração externa intencionalmente
  ignorados, e 22 testes Node do agente aprovados;
- 106 testes de Têmis aprovados, com um teste externo intencionalmente ignorado;
- 36 testes Node de resolução de deploy e empacotamento, cinco de fingerprint e três do contrato de
  topologia aprovados;
- nove testes Playwright passaram em 12 segundos nos três cenários e três dispositivos;
- build de produção de Alcyone, TypeScript, Prettier, Spotless, OpenAPI e empacotamento de 50
  manifestos aprovados;
- a topologia completa de proxy validou identidade, ativos e recriação isolada de Alcyone, Mira
  privada, Mira comercial e Vega v5–v8.

A primeira execução corretiva bloqueou por contraste de 4,44:1 no marcador vinho sobre o fundo,
abaixo do mínimo AA de 4,5:1. O tom foi ajustado para 4,83:1, e a matriz independente completa foi
repetida com aprovação de todos os checks.

Não houve chamada a provedor, recrutamento, leitura humana, checkout real, cobrança, publicação
comercial, campanha ou gasto de mídia. A evidência comprova apenas prontidão técnica privada por
agentes e não comprova demanda, disposição a pagar ou venda.

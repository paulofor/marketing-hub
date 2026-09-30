# Homologação da continuidade privada de Alcyone — v2

Data da matriz: 2026-09-30.

## Escopo preservado

Esta evolução corrige o gate de acesso encontrado depois da homologação v1. Ela não recruta,
entrevista ou simula participantes humanas; não publica Alcyone, não cobra, não cria campanha, não
autoriza mídia e não transforma sinais de agentes em validação de mercado. Produto #11 permanece
`PLANNED`, e somente o comportamento voluntário do mercado poderá produzir evidência comercial.

## Matriz de homologação definida antes da rodada

| Área | Caminho feliz | Validação e falha | Evidência exigida |
| --- | --- | --- | --- |
| Resultado | Três fixtures do mesmo pacote ficam preservadas | pacote inexistente ou pertencente a outra sessão responde 403 | ID, fingerprint e 3 imagens idênticos |
| Interesse | Agente declara por que pretende retornar | emissão antes de uso ou interesse é recusada | `SAVE_INTEREST_DECLARED` separado |
| Política | Agente aceita `ALCYONE_AGENT_CONTINUITY_V1` | versão divergente ou aceite falso é recusado | evento e horário persistidos, `humanEvidenceClaimed=false` |
| Credencial | segredo opaco é emitido uma vez | bruto não aparece na persistência do backend, log, evidência, URL ou tela | somente SHA-256 persistido no servidor; credencial fica no armazenamento local do navegador de teste |
| Sessão | credencial cria sessão curta e ambas rotacionam | token ou credencial anterior não pode ser reutilizado | 403 para valores antigos |
| Expiração | continuidade funciona após a sessão vencer | credencial com mais de 24 horas é recusada | relógio determinístico e expiração real |
| Retorno | nova sessão lê exatamente o pacote preservado | indisponibilidade temporária não registra retorno e pode ser repetida | `AUTHENTICATED_ACCESS_RESTORED` e `RETURN_COMPLETED` |
| Isolamento | proprietário acessa o próprio pacote | segunda sessão recebe 403 | cenário por sessão e pacote |
| Navegadores | desktop, iPhone 15 Pro e Pixel 7 completam os três cenários | erro, foco, toque e overflow permanecem funcionais | matriz 3 × 3 e screenshots |
| Efeitos | checkout continua apenas simulado | qualquer provider, cobrança, publicação, campanha ou mídia reprova | custo zero e flags falsas |

## Decisão arquitetural

Foram comparadas três alternativas. Marcar etapas apenas no frontend seria barato, mas não provaria
segurança. Reutilizar uma credencial permanente reduziria código, porém permitiria repetição e
ampliaria impacto de vazamento. A solução adotada usa sessão de 30 minutos, credencial de
continuidade de 24 horas, rotação a cada retorno, hash SHA-256 no servidor e autorização explícita
do pacote. O esforço é moderado e determinístico; o risco comercial continua nulo.

O manifesto v2 declara explicitamente que substitui a experiência v1. O empacotador preserva a
prova histórica sem exigir que seus hashes continuem descrevendo o código atual; a sucessora segue
integralmente validada. Atestações separadas comprovam que as mudanças compartilhadas não alteram
as superfícies comerciais de Mira nem de Vega.

## Resultado local

A topologia local equivalente à publicação foi executada com backend, frontend Alcyone e agente de
cliente na rede Compose exclusiva da sandbox. O resultado foi `APPROVED`: 9 de 9 cenários passaram
em 10 segundos, combinando os fluxos aderente, recuperação e segurança em desktop 1440 px, iPhone
15 Pro e Pixel 7. O relatório bruto tem SHA-256
`fb737f73fe9a7041516e235626e7990d3f0728be0141b7efb6924be6c607c45f` e foi acompanhado por nove
screenshots, uma por combinação de cenário e dispositivo.

Todos os gates de continuidade ficaram verdadeiros: aceite versionado da política, rotação da
credencial, rejeição da sessão expirada, negação de pacote entre sessões, recuperação depois de
indisponibilidade e retorno autenticado. O segredo bruto não apareceu em evidência persistida; a
sessão anterior e a credencial anterior receberam 403 após a rotação.

As validações complementares terminaram assim:

- 202 testes do backend PDE aprovados, sem falha ou teste ignorado;
- 146 testes Java do agente aprovados, com dois testes de integração externa intencionalmente
  ignorados, 22 testes Node do agente e 106 testes de Têmis aprovados;
- 11 testes do contrato de release e 51 testes Node de identidade, isolamento, resolução de deploy e
  empacotamento de evidências aprovados;
- topologia completa de proxy validou identidade, ativos e recriação isolada de Alcyone, Mira
  privada, Mira comercial e Vega v5–v8;
- build de produção de Alcyone, TypeScript, Prettier, Spotless, `bash -n` e ShellCheck aprovados;
- Playwright local confirmou separadamente os mesmos 9 cenários em 11 segundos.

A primeira rodada identificou ambiguidade de injeção do relógio no bootstrap do backend e espera
insuficiente do teste para a emissão assíncrona da credencial. As duas causas foram corrigidas; as
validações relacionadas e as suítes completas foram repetidas com sucesso.

A prévia do Pull Request detectou ainda que Têmis tratava a v1 substituída como paralelamente
vigente e que a nova atestação de Vega não carregava o conjunto comercial mínimo exigido por
Psique. O carregador passou a reconhecer a sucessão explícita e as atestações de compatibilidade
passaram a preservar as provas anteriores necessárias; as duas suítes completas foram repetidas.

Não houve chamada a provedor, recrutamento, leitura humana, checkout real, cobrança, publicação
comercial, campanha ou gasto de mídia. A evidência comprova apenas prontidão técnica privada por
agentes e não comprova demanda, disposição a pagar ou venda.

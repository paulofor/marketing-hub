# Vega #92 — reconciliação das evidências da tarefa #442

## Causa confirmada

- As tarefas #441 e #442 capturaram a mesma superfície pública da v8. A imagem completa e todas as
  cinco dobras possuem os mesmos SHA-256 nas duas tentativas.
- Na captura da #442, o primeiro botão começa em 921 px num viewport de 852 px. A ação não aparece
  na primeira dobra, a condição de 90 dias não acompanha a decisão e a privacidade permanece aberta.
- A `main` já contém CTA antes do vídeo, 90 dias sem assinatura ou renovação e privacidade
  recolhível. Entretanto, o PDE publicado ainda informa o commit `ea1cb6d`, anterior à mudança.
- O workflow Customer Agent `35264734328` não publicou Psique: a prova
  `pde-platform/frontend/tests/musa-local-integration.spec.ts` mudou, mas o manifesto v12 v1
  preservou o SHA-256 anterior. O empacotador interrompeu corretamente o build antes dos testes e
  do deploy do worker.

## Alternativas avaliadas

| Alternativa | Benefício | Risco/custo | Decisão |
|---|---|---|---|
| Repetir Psique com a tela atual | Imediata | Repete os mesmos pixels, custo e bloqueio | Rejeitada |
| Atualizar o hash no manifesto v1 | Menor diff | Reescreve uma atestação histórica e não detecta superfície antiga | Rejeitada |
| Criar v2 e validar a tela ao vivo antes do modelo | Preserva histórico e impede revisão paga sobre deploy antigo | Pequena ampliação do contrato visual | Adotada |

## Correção preparada

- O manifesto v12 v1 permanece imutável como histórico; a revisão v2 reatesta a jornada completa.
- A v2 inclui o frontend efetivo, estilo, simulador Pepper, compra, acesso, sete missões, três
  materiais, retomada, conclusão, reembolso e segregação de tráfego `INTERNAL_QA`.
- O manifesto declara a copy mínima da candidata. Antes de chamar o modelo, o capturador de Psique
  verifica no iPhone 15 Pro se **Começar meu ajuste gratuito** está na primeira dobra e se as
  condições **4 escolhas rápidas**, **R$ 67**, **90 dias** e **sem renovação** aparecem na tela.
- Ausência de qualquer sinal vira falha técnica de superfície desatualizada antes da revisão paga;
  não é convertida em parecer comercial `ADJUST`.

## Homologação local

- Build TypeScript/Vite aprovado.
- Topologia Docker isolada com MySQL 5.7, backend PDE, frontend, simulador de contrato e
  sandbox-mail.
- 36/36 jornadas Playwright aprovadas em Chromium desktop, iPhone 15 Pro e Pixel 7.
- Em cada dispositivo, a v12 comprovou CTA e condições antes do vídeo, primeiro ajuste gratuito sem
  fila de IA, pagamento Pepper simulado, identidade da v12, 90 dias, sete missões, três materiais
  protegidos, retomada, conclusão, reembolso idempotente e zero contaminação de métricas humanas.
- A regressão dirigida do contrato visual aprovou 47/47 testes Java, cobrindo rejeição de pixels
  antigos e aceitação da superfície coerente com o manifesto.
- As suítes completas aprovaram 117 testes Java e 17 testes reais de navegador em Psique, além de
  101 testes Java de Têmis. Os dois testes ignorados já eram condicionais previstos dos módulos.

## Limite operacional

O ambiente público continua na revisão anterior e, por isso, não deve receber outra tentativa de
Psique. A nova captura auditável só pode ser produzida depois de PR e deploy oficial da mesma revisão
atestada. Nenhuma publicação, campanha, cobrança real, aprovação humana ou gasto de mídia foi
executado nesta homologação.

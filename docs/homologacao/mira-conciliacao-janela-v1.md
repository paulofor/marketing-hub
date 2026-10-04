# Mira — conciliação histórica e proteção da janela

## Evidência e escopo — 04/10/2026

Mira/produto #10, Safira, experiência `mira-commercial-v1`, experimento #93:
a execução #41 de homologação permanece 3/4, impedida pelo plano #8 `BLOCKED`.
O parecer de Plutus #61/revisão financeira 3 está vigente até 06/10; não é autorização de mídia.
O run produtivo #14 está concluído; a campanha está pausada e o snapshot final da Meta foi
persistido em 03/10. Banco, endpoints oficiais e tela confirmam a mesma identidade.

O ciclo #3 foi adotado como referência histórica pela tela, cadeia #26, subprocesso #125/v7,
com retorno à atividade 6.4 do processo #119. O backend conciliou dois visitantes humanos,
duas sessões, zero compras líquidas, R$ 22,18 de mídia, R$ 27,19 de custo auditável total e
contribuição de -R$ 27,19. Esses valores descrevem o recorte e a cobertura do ledger;
não demonstram inviabilidade do produto nem lucro da empresa. A conciliação preservou
o experimento invalidado, a campanha pausada e os dados originais. A entrada em decisão
acionou Atena automaticamente pelo executor já habilitado, sem aprovação da proposta.

Na tela apareceu **Revalidar janela comercial**, afirmando que o período venceu antes da
ativação, embora a publicação e exposição estivessem comprovadas. A interface usava somente
`status=OPEN` e seu relógio. O comando recusava fases posteriores, mas não aplicava a política
vigente `CHANGE_PER_CYCLE_V1` para ciclos legados ainda em preparação. O problema não decorre
de falta de Plutus nem justifica repetir a homologação histórica.

## Alternativas e correção

| Alternativa | Benefício | Risco / esforço | Decisão |
| --- | --- | --- | --- |
| Ocultar somente referências históricas no React | Pequena mudança | Duplica regra e deixa API divergente | Não |
| Remover toda renovação, inclusive contrato legado anterior à política | Implementação simples | Quebra compatibilidade sem necessidade | Não |
| Uma regra de elegibilidade no backend para leitura e comando | Fecha orientação e escrita indevidas; serve ao harness | Mudança pequena de contrato e regressões | Adotada |

A resposta apresenta `windowRevalidation` com disponibilidade e motivo; a tela usa essa
decisão. A política publicada governa novas mudanças, preservando versões e eventos antigos.
Sem política nova, continuam necessários ciclo aberto, janela encerrada, etapa anterior à
operação, experimento planejado sem liberação/exposição e ausência de referência histórica.
Replays reconhecidos são preservados. Nenhuma mudança de banco ou endpoint novo é necessária.

## Matriz definida antes dos testes

| Área | Cenários | Aceite |
| --- | --- | --- |
| Caminho permitido | Contrato legado, janela encerrada, sem exposição | Mantém teto, identidade e evidência; não publica |
| Política vigente | Ciclo atual e ciclo legado sob CHANGE_PER_CYCLE_V1 | Leitura bloqueia e API recusa antes de escrita |
| Histórico / falhas | Referência histórica, liberação, run publicado com status PLANNED, janela futura | Sem reescrita, sem novo evento, mesma revisão |
| Integração | Serviço real com repositórios simulados → JSON → tela local | Mesmo motivo e disponibilidade, sem relógio do cliente |
| Regressões | Autorização, decisão, conciliação e janela legada elegível | Contratos existentes mantidos |
| Observabilidade | Motivo exposto no contrato, histórico e IDs sintéticos | Nenhuma aprovação, chamada paga ou métrica comercial fabricada |
| Interfaces | Chromium desktop, iPhone 15 Pro e Pixel 7 | Sem ação falsa, erro JavaScript ou overflow |

## Resultado

Validação local concluída antes de commit e PR:

- Backend: 3.895 casos na suíte, 3.871 executados sem falhas; 24 condicionais preexistentes
  não executados (integrações externas/MySQL/browser fora deste recorte e um teste de HTML
  previamente desabilitado). Os 19 testes do contrato alterado passaram, incluindo replay
  depois da adoção da nova política, sem nova escrita ou ampliação de orçamento.
- Frontend: 852 testes em 184 arquivos, typecheck, build e Prettier aprovados.
- Serviço real com repositórios simulados → JSON serializado → build da tela: nove cenários
  em Chromium desktop, iPhone 15 Pro e Pixel 7. Nenhum POST, chamada externa, erro JavaScript
  ou overflow. Fixtures sintéticas; nada foi enviado ao banco produtivo durante os testes.
- JAR: 4.207 classes idênticas à compilação testada, 745 recursos externos íntegros e
  inicialização do catálogo de 511 cartões. Nove testes do verificador aprovados.
- Spotless dos arquivos alterados e revisão do diff aprovados. Não houve alteração de SQL,
  esquema, scripts shell, prompt de IA nem orçamento. A integração afetada foi simulada
  localmente; o workflow existente do PR também executa sua matriz MySQL 5.7.

Antes, o relógio do frontend oferecia uma ação incompatível e o endpoint legado não aplicava
a política publicada. Depois, leitura e comando compartilham a mesma regra e preservam os
replays. A melhoria do harness está no contrato consumível e nas regressões reutilizáveis;
nenhum ganho de vendas ou redução de custo por tarefa foi medido.

A proposta #3 de Atena ficou `READY`, sem aprovação humana, em aproximadamente 149 segundos;
20.506 tokens de entrada e 3.805 de saída, custo ainda não informado. Sua hipótese de reduzir
esforço não é uma causa comprovada e deve ser confrontada com o contrato de pagamento anterior
à personalização. Foi apresentada ao usuário a alternativa de manter produto, público e preço,
mudando somente a comunicação no sucessor, sem autorização de gasto. A atividade de decisão
do ciclo continua pendente dessa escolha. A homologação histórica #41 não foi declarada concluída.

PR, SHA e resultados de publicação serão vinculados na evidência de entrega do próprio PR;
aprovação local não significa publicação nem conclusão comercial.

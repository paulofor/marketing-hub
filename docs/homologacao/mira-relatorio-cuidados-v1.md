# Mira — relatório e criativo de cuidados v1

Data: 2026-10-01. Produto #10, experimento histórico #93, oferta R$ 49.

## Evidência confirmada antes da implementação

A Meta confirma PAUSED e R$ 22,18 / 489 impressões / 2 cliques. O Hub mantém
R$ 21,92 / 483 impressões após seu primeiro sync terminal. Há duas visitas HUMAN com a UTM
própria em pde_funnel_event, ignoradas pelo funil LOW_TICKET_PRODUCT; o monitor escolhe Vega.
A sincronização terminal sai da fila após um único callback, antes da consolidação tardia da Meta.

## Matriz local de aceite

| Dimensão | Cenário | Aceite |
|---|---|---|
| Caminho feliz | Mira low-ticket com slot e campanha própria | Monitor e cockpit consultam produto, versão e UTMs de Mira; duas visitas humanas |
| Regressão | Vega PDE e página low-ticket convencional | Mantêm fontes e comportamento existentes |
| Segregação | Outro produto, versão, campanha, QA e bots | Não entram nas métricas comerciais de Mira |
| Falhas | Slot incompatível ou analytics indisponível | Falha explícita; sem fallback para Vega ou números globais |
| Integração | SQL local real → reader → endpoints/cockpit | Contagens idênticas, compras e receitas baseadas em eventos conciliados |
| Reconciliação | Meta atualiza valores após pausa | Janela financeira finita, com cadência no executor; não reativa mídia |
| Observabilidade | Falha de analytics ou callback | Contexto e exceção completos, erro persistido quando aplicável |
| Interface | Chromium desktop, iPhone 15 Pro e Pixel 7 | Produto, contagens, vídeo e controles legíveis |
| Criativo | Mulher adulta cuidando da pele + narração e oferta | Gancho concreto, oferta fiel, disclosure sintético, sem resultado clínico, depoimento ou interface fictícia |
| Orçamento | Preflight sem produção | Sem chamada paga, reserva ou mídia antes do teto autorizado |

## Alternativas avaliadas

Para os dados: corrigir números manualmente, mudar Mira para assinatura ou reconhecer a superfície
PDE versionada mantendo o tipo low-ticket. A terceira resolve a seleção da fonte sem mudar a oferta.
Para o vídeo: repetir interface, apresentadora falando ou cuidados + prova real. A terceira atende
ao pedido e conecta atração ao mecanismo concreto, com menor risco de promessa clínica.

O novo vídeo será cadastrado pelo frontend como variante separada; o #93 e os ativos anteriores
permanecem históricos, sem mídia reativada. Preflight não constitui autorização financeira.

## Validação executada na sandbox

- Suíte completa do backend e testes afetados após os ajustes: 3.761 testes registrados, sem
  falhas, incluindo as 92 regras de arquitetura.
- Facebook Ads Worker: 164 testes, sem falhas, incluindo cadência de consolidação e prazo final.
- Frontend: 30 testes dos relatórios e detalhe, sem falhas; build de produção concluído.
- MySQL 5.7 real: migração aplicada duas vezes, campanhas recentes reabertas somente para
  métricas, antigas e ativas preservadas, rollback e reaplicação aprovados. A leitura SQL real
  também passou para produto, versão, UTM, pagamento deduplicado, reembolso e tráfego interno.
- Chromium desktop, iPhone 15 Pro e Pixel 7: cockpit e Analytics renderizam as duas visitas
  sintéticas de Mira, sem erro JavaScript nem mutação; capturas preservadas na evidência local.
- ShellCheck, bash -n, validador Liquibase e Actionlint passaram.
- O harness agora protege a seleção da fonte em low-ticket e executa a fixture MySQL no PR;
  ausência de evidência e falha de medição não viram automaticamente zero comercial.

## Preparação audiovisual pelo frontend

Perfil #62, roteiro #564 e projeto #7 criados no Estúdio. A variante preserva preço e produto.
A captura privada #96 não corresponde à versão comercial vigente: o gate bloqueou seu uso,
sem consumo do provider. A peça foi simplificada para cuidados cotidianos, gancho, narração,
oferta e CTA; não apresenta tela inventada nem resultado dermatológico.

O primeiro preflight gratuito apontou direção visual acima de 1.000 caracteres. A direção foi
resumida explicitamente no Estúdio, preservando limites no projeto. O preflight #19 / ciclo #26
consultou organização e dry run: gen4.5, dois clipes de 10s + 5s, 180 créditos / US$ 1,80 estimados,
saldo oficial 2.012 créditos, nenhuma reserva. A soma dos tetos de rota é US$ 8 e excede o limite
analítico inicial de R$ 30. Solicitada confirmação de teto R$ 50 para este vídeo, voz e acabamento;
sem essa confirmação, não há geração paga. Cotação R$ 5,20 usada apenas como referência do
planejamento anterior, explicitamente datada no ciclo; não é cotação financeira atual.

### Refinamento encontrado na homologação visual

Com a fonte corrigida, o cockpit low-ticket tratava duas visitas como prova contra a página.
A causa era a ausência da proteção de amostra aplicada às assinaturas. Sem mudar o tipo de
Mira nem impor compra de tráfego, a leitura agora usa a coorte e o tamanho já configurado
para declarar conversão inconclusiva, preservar produto/preço e revisar atração. A fonte
indisponível também bloqueia a saúde comercial. Testes e nova conferência desktop/iPhone
confirmaram a recomendação; a campanha histórica continua sem comando de retomada.

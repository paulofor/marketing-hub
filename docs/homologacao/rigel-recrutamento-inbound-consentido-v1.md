# Matriz de homologação — recrutamento inbound consentido do Rigel v1

## Objetivo, evidência e decisão

- **Gargalo real:** o experimento 89 está `RUNNING`, mas o Marketing Hub apenas registra uma
  abordagem já consentida; não existe etapa para uma pessoa aderente descobrir o convite e aderir.
- **Evidência produtiva em 2026-09-01:** `experiment_direct_contact`, `lead`,
  `lead_portal_submission`, `whatsapp_account` e `whatsapp_message` têm zero registros aplicáveis ao
  Rigel. O sistema não possui lista própria nem canal WhatsApp conectado.
- **Métrica esperada:** visitas, adesões, qualificados e contatos aumentam separadamente; somente
  adesão qualificada e consentida avança o placar oficial de 0/15 a 15/15.
- **Continuar:** primeiras adesões qualificadas e sinais de checkout aparecem sem incidente de
  privacidade.
- **Ajustar:** visitas sem adesão, adesões não qualificadas ou distribuição orgânica indisponível.
- **Parar:** dado pessoal em claro no backend, contato sem consentimento, duplicidade, ultrapassar
  15, mensagem/campanha/gasto automático ou qualquer métrica fabricada.

## Alternativas consideradas

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Buscar listas públicas e abordar pessoas frias | alcance imediato | baixa intenção, risco de privacidade e nenhum canal conectado | rejeitada |
| Criar campanha paga de leads | aquisição escalável | muda o contrato do experimento e exige orçamento/autorização | futura, em experimento próprio |
| Convite inbound consentido dentro do Hub | intenção explícita, auditoria e baixo custo | ainda requer uma fonte orgânica de distribuição | escolhida |

## Matriz ponta a ponta

| Dimensão | Cenário | Evidência esperada |
| --- | --- | --- |
| Preparação | Criar convite para o experimento 89 | rascunho versionado, copy derivada do produto e URL pública única |
| Aprovação | Ativar rascunho | operador e horário auditados; nenhum post, mensagem, campanha ou gasto |
| Distribuição | Nenhuma conta orgânica conectada | status `ACTIVE_WITHOUT_DISTRIBUTION` e ação explícita para conectar canal |
| Visita | Abrir URL ativa | uma visita pseudonimizada; reload do mesmo navegador não duplica |
| Caminho feliz | Prestador aderente aceita participar | adesão `QUALIFIED`, contato oficial +1 e acesso à oferta do Rigel |
| Qualificação | Pessoa não usa WhatsApp ou não decide a compra | `NOT_QUALIFIED`, motivo persistido e amostra inalterada |
| Consentimento | Checkbox ausente ou versão divergente | HTTP 400 e nenhuma adesão persistida |
| Privacidade | Inspecionar request, resposta, log e banco | telefone/e-mail em claro nunca sai do navegador; apenas SHA-256 segregado |
| Duplicidade | Mesma pessoa volta por outra origem | HTTP 409, uma adesão e um contato |
| Concorrência | Duas adesões disputam a última vaga | lock do experimento mantém exatamente 15/15 |
| Estado | Convite pausado ou experimento fora de `RUNNING` | página informa indisponibilidade e não aceita adesão |
| Canal | Experimento Facebook | criação recusada sem interferir no fluxo pago |
| Oferta | Adesão qualificada | resposta aponta para URL HTTPS oficial, sem registrar venda |
| Métricas | Uma adesão sem compra | visitas/adesões/contatos avançam; checkout, pagamento e receita não |
| Observabilidade | Consultar painel | funil, status, motivo de bloqueio e UTMs vêm do backend |
| Segregação | Fixtures locais | token, fingerprints e métricas não alteram o experimento 89 produtivo |
| Dependência | Backend indisponível | página preserva respostas e explica falha; não apresenta sucesso |
| Dispositivos | Desktop, iPhone 15 Pro e Pixel 7 | convite e painel utilizáveis, sem overflow e com foco/labels acessíveis |

Se a primeira rodada local revelar defeito, depois da última correção devem passar duas rodadas
completas e consecutivas. Qualquer nova falha reinicia a contagem.

## Resultado local final — 2026-09-01

Depois da última correção funcional, duas rodadas completas e consecutivas terminaram sem falhas.
Em cada rodada foram validados:

- backend com Spotless, pacote executável, ArchUnit e 2.187 testes, sem falhas ou erros e com três
  testes explicitamente ignorados pela suíte;
- frontend com 137 arquivos de teste e 453 testes aprovados, checagem de tipos, Prettier e build de
  produção;
- três changesets no MySQL 5.7 físico, incluindo `DATETIME`, chaves estrangeiras, deduplicação,
  retomada sem ledger, rollback e reaplicação;
- contratos do GitHub Actions, includes relativos do Liquibase e configuração Compose;
- imagens locais versionadas do backend e do frontend;
- painel administrativo e convite público em desktop, iPhone 15 Pro e Pixel 7, cobrindo 0/15,
  registro pseudonimizado, qualificação, política de privacidade, oferta após adesão e gate 15/15.

A homologação não alterou o experimento produtivo: permanecem zero contatos novos, zero
publicações, zero gasto e zero vendas até que a mudança passe pelo PR e a distribuição seja
explicitamente autorizada.

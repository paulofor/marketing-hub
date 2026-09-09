# Matriz local — medição automática do ciclo de vendas v3

## Objetivo e causa confirmada

Eliminar a segunda digitação de resultados na atividade `MEASUREMENT`. O histórico do #91 e o
`LOOP-HERMES-PDE-ANALYTICS-LEGADO` confirmam que o backend já possui o recorte atribuído correto
(quatro sessões humanas e 86 eventos), enquanto o ciclo ainda solicitava cópia manual de fonte,
período, números e justificativa. A duplicação de autoridade permitia divergência e erro humano.

## Alternativas avaliadas antes da implementação

| Alternativa | Benefício | Risco / custo | Aderência ao objetivo | Escolha |
| --- | --- | --- | --- | --- |
| Manter formulário manual com instruções melhores | Baixo esforço | Continua duplicando a verdade e atrasando o loop | Baixa | Não |
| Buscar dados no frontend e preencher campos | Experiência aparentemente rápida | Frontend passa a conciliar e o fluxo depende da tela aberta | Média | Não |
| Conciliação no backend com snapshot ou bloqueio persistido | Uma fonte para Hermes, ciclo e decisão; auditável | Implementação e contratos maiores | Alta | Sim |

## Matriz definida antes dos testes

| Dimensão | Caminho feliz | Validação e falha | Evidência esperada |
| --- | --- | --- | --- |
| Entrada no BPM | `PUBLICATION` ou referência histórica entra em `MEASUREMENT` e o backend concilia | etapa, produto, experimento ou revisão divergente é rejeitado | definição v3, evento e ledger da atividade |
| Funil | produto, versão, experimento/campanha, janela autorizada e tráfego humano iguais aos de Hermes | slot ausente, versão divergente, evento fora da janela ou analytics indisponível não contamina a fotografia | sessões, eventos, período, escopo, assinatura das fontes e suíte SQL no MySQL 5.7 |
| Canal | campanha Facebook usa spend e sincronização final/fresca; canal direto declara ausência de mídia paga | sync ausente, antiga, futura ou com erro bloqueia | modo, gasto, impressões, cliques e horário |
| Venda e receita | compra/reembolso interno com referência, BRL e valor calcula venda líquida e receita | alias legado, referência/valor/moeda ausente ou reembolso impossível bloqueia | totais sem expor identificador financeiro |
| Custos e contribuição | ledger auditável do experimento é subtraído da receita | fonte de custos indisponível bloqueia, sem usar total legado como verdade | custo auditável, diferença legada e contribuição |
| Valor entregue | entrega, primeiro uso e feedback positivo correlacionam uma compra líquida pelo `accessReferenceHash` | bearer bruto, acesso gratuito, relação ambígua ou compra reembolsada não fabricam valor | contagens e flags separadas, sem segredo |
| Segregação | `HUMAN` entra; QA, crawler e bot permanecem apenas no total bruto | mistura de teste impede dado válido | totais humanos/brutos e `testDataExcluded=true` |
| Idempotência | uma chave e revisão produzem uma transição | replay divergente, revisão obsoleta e fonte inalterada não duplicam decisão | eventos ordenados e fingerprint |
| Observabilidade | `MEASURE` automático registra fontes; falha registra `MEASUREMENT_BLOCKED` | exceção preserva stack trace e contexto do ciclo/experimento | logs e auditoria persistida |
| Interface | não há campos de métricas; estado automático e decisão vêm do backend | erro de rede e bloqueio oferecem retentativa com spinner | testes React e navegação visual |
| Responsividade | fluxo completo utilizável em desktop, iPhone 15 Pro e Pixel 7 | sem overflow ou controle inacessível | screenshots e assertivas Playwright |
| Segurança comercial | conciliação não publica, não ativa campanha e não aumenta orçamento | nenhum teste usa credencial, tráfego ou pagamento real | estados dos test doubles inalterados |

## Critério de conclusão

Uma rodada local completa sem defeito conclui a homologação. Se qualquer defeito aparecer, ele deve
ser corrigido e a contagem reinicia: duas rodadas completas e consecutivas sem falha depois da última
correção. A validação física usa MySQL 5.7 local quando disponível e o runner dedicado no PR para o
changelog ainda não publicado.

## Resultado

Depois dos ajustes encontrados durante a primeira investigação, foram executadas duas rodadas
locais completas e consecutivas, sem alteração funcional entre elas:

| Controle por rodada | Resultado |
| --- | --- |
| Backend | 2.524 testes; zero falhas, zero erros e cinco cenários condicionais ignorados |
| Frontend | 149 arquivos e 522 testes aprovados; typecheck e build aprovados |
| Fonte PDE no MySQL 5.7 | 15 testes físicos de atribuição, janela, venda, reembolso e valor |
| Fluxo REST no MySQL 5.7 | 15 controles ponta a ponta aprovados, incluindo concorrência e idempotência |
| Navegador | 12 jornadas principais, 18 controles de entrada pela cadeia e adoção histórica em desktop, iPhone 15 Pro e Pixel 7 |
| Migração | aplicação, rollback, reaplicação e idempotência aprovados |
| Isolamento | zero chamada externa, zero mutação produtiva e zero credencial real |

- Rodada 1: `/tmp/learning-sales-cycle-round-fbkAoi`.
- Rodada 2: `/tmp/learning-sales-cycle-round-L3Sl5N`.

**Conclusão:** homologação local aprovada. O ciclo registra automaticamente resultados, período,
fontes, autoria do backend e síntese da conciliação; o operador recebe diretamente a decisão ou a
causa do bloqueio, sem campo para digitar métricas. O ambiente temporário foi removido ao final de
cada rodada. O changelog ainda deve passar também pelo runner Liquibase MySQL 5.7 do PR antes da
publicação.

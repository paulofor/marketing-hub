# Capella: cinco pontos de comunicação — 24/10/2026

## Evidência e comparação

Consulta somente leitura ao painel, MCP e artefatos do ciclo 12, experimento 105,
capella-private-v3. O experimento está INVALIDATED, a campanha PAUSED, o ciclo em
DECISION e nenhum sucessor está registrado. Não alterar esse histórico nem executar
a proposta de encerramento em nome do usuário.

Fontes: tarefa Íris 690; Psique 744 (vídeos 76 e 75 e seus hashes); conciliação 87;
campanha 120252198564080326; página `exp-105-gerasalespage-v1`; Big Idea 2/v1.
O cadastro textual do vídeo não substitui a narração do arquivo revisado: o parecer
744 cita a abertura efetiva “Suas unhas ficam lindas. Mas, na hora de postar, você trava?”.

| Ponto | Resultado observado | Mudança para a próxima preparação |
| --- | --- | --- |
| Desejo reconhecido | Parcial: dificuldade de postar está na fala; os primeiros quadros mostram campos. A arte aparece cerca de 8 s depois. | Abrir com a divulgação pronta e identificável como kit para manicures; ligar à ocasião de divulgar entre atendimentos. |
| Primeiro passo fácil | Parcial: existe CTA e demonstração, mas formulário e condições dominam a introdução. | Convidar a ver uma amostra sem exigir compra ou briefing antes da prova; briefing de personalização continua pós-compra. Não prometer função ainda ausente. |
| Valor antes do compromisso | Presente: amostras e vídeo reais, porém a recompensa demora e aparece abaixo das condições na página. | Mostrar cedo uma arte legível com a legenda correspondente; antecipar uma aplicação útil e só então explicar o processo. |
| Por que pagar | Presente, pouco diferenciado: lista de arquivos, preço e personalização. | Explicar amostra versus pacote personalizado e organizado para sete dias. Compra única de R$ 67 não vira assinatura, recorrência ou nova oferta. |
| Repetir com margem | Ainda sem prova comercial: 504 impressões, 2 cliques, R$ 10,05 de mídia, 0 compras, contribuição do recorte −R$ 13,74. Quatro cliques no checkout vieram da mesma sessão. | Separar contagens brutas, sessões únicas, compra e contribuição; transmitir motivo de parada; uma hipótese principal por sucessor. Não chamar amostra pequena de rejeição do produto. |

CTR observado: 0,40%; é diagnóstico, não prova causal de roteiro ou público. O motivo
persistido é CAMPAIGN_STATISTICALLY_FAILED_STAGE; o snapshot anterior não preservou
a etapa/limiar que gerou a parada. Não reconstruir esse detalhe como fato histórico.

## Alternativas e decisão

1. Só reforçar o texto dos prompts: barato, mas continua aceitando cobertura nominal.
2. Criar novo agente: amplia repasses, custo e sobreposição com Íris/Psique.
3. Melhorar contratos e fontes dos responsáveis existentes: escolhido; exige prova
   rastreável na criação e corrige o aprendizado sem reconstruir a cadeia.

Hipótese reutilizável: mensagem ligada a uma amostra utilizável reduz fricção e melhora
a passagem até compra com contribuição. Testes comprovam contrato e atribuição; o
efeito comercial depende do próximo experimento autorizado. Não produzir vídeo pago,
alterar oferta, público, limite financeiro ou iniciar campanha nesta entrega.

Direção de roteiro candidata, ainda sem parecer nem produção: abrir com post e legenda
da mesma ocasião; “A divulgação da sua semana pode começar aqui”; mostrar o par pronto,
explicar a personalização e a organização do conjunto; convidar a “Ver uma amostra do kit”.
Não prometer clientes, agenda cheia ou velocidade de entrega diferente do contrato.

## Matriz local de aceite

| Validação | Critério |
| --- | --- |
| Criação e passagem | Cinco decisões com mensagem, fonte literal e métrica; callback conserva a prova; replay legado continua sem nova inferência. |
| Falhas | Recusar critério duplicado/ausente, citação inventada, fonte fora do contexto e margem tratada como mensagem à cliente. |
| Isolamento | Capella e outra identidade usam seus próprios contextos; não aceitar referências de outro produto. |
| Medição | Quatro cliques da mesma sessão contam como uma sessão; excluir bots, testes, outra janela e outro experimento; não somar acessos de pagamento a sessões. |
| Parada | Conservar motivo, instante e diagnóstico efetivo em novas paradas; ausência histórica permanece desconhecida. |
| Compatibilidade | Preservar tarefas/callbacks antigos, formatos sem vídeo, compras reconciliadas e caminhos válidos. |
| Integração | Executar SQL real local, consumidores com modelo simulado e prompts efetivamente carregados. Não fazer inferência paga para testar. |
| Navegação | Sem alteração de interface; conferir somente leitura da página e painel publicados. |

Adotar se as regressões passarem e as fontes permanecerem rastreáveis. Reverter se a
exigência invalidar replay histórico ou inventar pré-requisito comercial na preparação
privada. Não repetir experimentos históricos nem transformar aprovação técnica em venda.

## Validação local realizada

- Íris: 131 testes executados, incluindo geração em processo simulado, callback, cinco
  critérios, fonte do próprio produto, recusa com resposta preservada e compatibilidade.
- Psique: 228 testes executados; o teste de contrato também confirma as novas instruções.
- Atena: 69 testes; prompt e consumidor preservam fonte, limite e autorização separada.
- Apolo: 14 testes do planejador, incluindo request auditado e preservação de planos legados.
- Backend: suíte completa com 5.286 testes executados e nenhuma falha, incluindo ArchUnit,
  identidade da parada e catálogo do harness; 47 integrações também no MySQL 5.7 isolado.
- Validação dos agentes premium e 12 testes do empacotamento dos recursos aprovados.
- O teste do catálogo reproduziu a ausência da extensão na tela dos agentes; o manifesto
  agora a declara como schema complementar, com conteúdo e hash iguais aos do executor.

Os totais executados excluem testes opcionais dependentes de fixtures ou runners específicos
(5 de Íris, 12 de Psique e 57 do backend). Todos os critérios desta matriz foram executados;
esta entrega não gera nem altera mídia. O comportamento do novo contrato foi executado com
modelo simulado, sem chamadas pagas. A primeira tentativa de conexão MySQL usou o endereço
local errado; o teste foi concluído no daemon dedicado `sandbox-docker`. Não houve mudança
em produção para testar. Revisão de comentários Java e diff concluída.

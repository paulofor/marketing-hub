# Monitoramento de preços de modelos por Plutus — v1

## Decisão

Plutus é responsável por pesquisar e manter evidências de preços de modelos de IA por plataforma de acesso. Fabricante, modelo e provedor de acesso são dimensões diferentes: o mesmo modelo pode possuir preço, unidade, resolução, áudio, plano e taxa distintos conforme a plataforma.

Por decisão comercial de 2026-09-03, o catálogo deve separar cinco identidades:

- fabricante do modelo;
- modelo e versão exata;
- plataforma de acesso ou agregador, como Runway;
- conta operacional e seu pool de créditos;
- rota de geração, formada pelo modelo, agregador, modalidade, resolução, áudio e adaptador.

Crédito, saldo, tier, quota e limite de concorrência pertencem à conta do agregador, nunca ao
modelo isolado. Uma conta Runway pode financiar vários modelos, e o mesmo modelo pode possuir
rotas diferentes em agregadores distintos. Enquanto somente a Runway possuir conta compartilhada
homologada, Plutus deve assumir uma única opção real de recarga e não inventar concorrência com
adapters diretos ou catálogos ainda sem conta operacional comprovada.

## Contrato comercial

- preço verificado exige URL oficial, data da observação, moeda, valor, unidade, quantidade coberta, modalidade, resolução e áudio quando aplicáveis;
- a comparação deve usar custo normalizado somente quando as unidades forem equivalentes;
- evidência com mais de 30 dias é vencida e não pode fundamentar recomendação financeira;
- ausência de conversão oficial entre créditos e USD torna a oferta não comparável;
- menor preço não substitui gates de qualidade, licença, confiabilidade e desempenho comercial;
- pesquisa e recomendação não autorizam compra, consumo, geração, troca de provider ou publicação;
- o backend persiste e expõe a verdade; a rotina periódica pertence ao Financial Agent Worker.

## Preflight financeiro-operacional antes da geração

Nenhuma chamada paga de vídeo pode ocorrer antes de um preflight vinculado ao ciclo e ao payload
que será efetivamente enviado. O fluxo obrigatório é:

1. o backend persiste a intenção de geração, requisitos técnicos, teto do ciclo e rotas candidatas;
2. o módulo executor de vídeo, único portador das credenciais operacionais, consulta APIs somente
   leitura da conta e devolve ao backend um snapshot sanitizado;
3. quando a plataforma oferecer simulação sem cobrança, o executor faz um dry run com o mesmo
   input da chamada real e registra modelo resolvido, elegibilidade e custo previsto;
4. o backend persiste o snapshot e entrega a Plutus saldo, quotas, preço, custo previsto, histórico
   de qualidade e resultado comercial;
5. quando o preflight estiver integralmente apto, o backend cria uma reserva preventiva local sem
   consumo; Plutus filtra rotas inviáveis, recomenda a rota já resolvida e decide entre usar saldo,
   aguardar reset de quota, solicitar recarga, usar fallback homologado ou bloquear;
6. Apolo só recebe o job depois do gate de Plutus e enquanto a reserva atômica da soma dos tetos
   duros por geração permanecer válida; rejeição libera a reserva, e a estimativa continua visível,
   mas nunca reduz a proteção financeira.

O snapshot deve registrar, no mínimo, plataforma, conta sem segredo, natureza do saldo, créditos
oficiais, créditos reconciliados, reservas do Marketing Hub, saldo disponível, tier, concorrência,
quota diária usada e restante, instante de reset, limite mensal, modelos elegíveis, custo previsto,
fonte, horário, status HTTP sanitizado e validade. Saldo oficial prevalece sobre estimativa local.
Na ausência de API oficial, o saldo reconciliado deve permanecer explicitamente estimado; consumo
desconhecido nunca pode ser convertido em zero.

Para Runway, o primeiro adapter de preflight deve usar a leitura oficial da organização e do uso e,
quando aplicável, o Model Router em `dryRun`, sem gerar asset nem consumir créditos de geração. O
snapshot de saldo deve ter no máximo cinco minutos no momento da reserva preventiva. A reserva evita que dois
jobs do Marketing Hub comprometam o mesmo saldo; ela não é débito do provider e deve ser liquidada
ou liberada pelo resultado real.

O operador pode solicitar um preflight isolado para verificar a conta antes de decidir sobre recarga
ou produção. Nesse modo, o backend persiste o snapshot e o resultado do Router, mas não cria reserva,
parecer de Plutus nem job de geração. Mesmo um resultado `READY` termina como diagnóstico somente
leitura. Para produzir depois, deve ser aberto um novo ciclo financeiro com teto explicitamente
autorizado e novo snapshot vigente.

O Router deve possuir `maxCreditsPerGeneration.video` explícito. O backend valida cada modelo
resolvido contra o catálogo interno: lifecycle `ACTIVE`, adapter, preço, licença comercial e QA
precisam estar verificados. A resposta paga é comparada com o dry run; qualquer drift de modelo,
fabricante, configuração, preferência, teto ou custo encerra o lote sem abrir a próxima cena.

## Recomendação de Plutus

Plutus deve aplicar primeiro os gates de capacidade, licença, qualidade mínima, credencial, saúde,
saldo e quota. Entre as rotas aprovadas, deve comparar o custo esperado do material aproveitável,
considerando custo total da solicitação, necessidade provável de regeneração, taxa histórica de
aprovação técnica e visual e, quando existir amostra suficiente, resultado comercial. O menor preço
nominal por segundo não vence sozinho.

O parecer deve informar rota, modelo, agregador, custo previsto em créditos e USD quando a
conversão for oficial, saldo antes e depois da reserva, validade do snapshot, alternativas descartadas
e motivo. Quando faltar saldo, deve informar a recarga mínima para o lote aprovado e o link oficial
da conta. Essa indicação é orientação somente leitura: compra, autobilling, transferência de créditos
ou aumento de teto continuam exigindo autorização humana explícita.

Estados persistidos do preflight: `PENDING`, `READY`, `READY_WITH_BLOCKER`, `BLOCKED` e `EXPIRED`.
A causa operacional fica em código próprio, incluindo saldo insuficiente, quota diária, concorrência,
configuração ausente, rota não homologada, teto inseguro, credencial inválida ou provider indisponível.
Bloqueios com snapshot e dry run completos seguem para parecer de Plutus; falhas que impedem obter
evidência confiável permanecem bloqueadas antes do gate.

No parecer, `RECHARGE_REQUIRED` é exclusivo de saldo disponível insuficiente e deve conter a
diferença exata. Bloqueios conhecidos de teto, quota, licença ou qualidade usam `NO_PURCHASE`;
`BLOCKED_UNKNOWN` é reservado a quota ausente em um snapshot restante íntegro. Essa separação evita
que uma incerteza operacional seja apresentada como recomendação de compra.

## Resultado esperado

O Estúdio deve mostrar preço original, custo normalizado por segundo quando possível, vigência, fonte oficial e bloqueios. Plutus usa esse catálogo para estimar e supervisionar orçamento antes de Apolo consumir providers.

No próprio preflight do Estúdio, a tela deve mostrar `modelo via agregador`, saldo oficial ou
estimado, reservas, quota e reset, custo do job e do ciclo, recomendação de Plutus e eventual recarga
necessária. A meta operacional é não enviar chamadas que já seriam recusadas por saldo ou quota e
manter custo previsto versus realizado por rota. Continuar quando houver rota apta dentro do teto;
ajustar diante de divergência recorrente entre previsão e débito; bloquear quando o snapshot estiver
vencido, o custo for desconhecido ou nenhuma rota cumprir os gates.

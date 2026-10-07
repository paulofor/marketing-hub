# Mira: correção da consulta e das medições — 07/10/2026

## Evidência e causa

Mira #10, ciclo #6, experimento #99: Psique #611 concluiu ADJUST e Dédalo #612
registrou o plano de correção, sem implementar ou aprovar uma versão nova. O código
confirma quatro comandos com o mesmo destaque, retomada sem recuperação e conclusão
ainda ativa depois do encerramento. A contagem `manualFields` era feita antes de
acrescentar o segundo produto; portanto media o mínimo inicial, não o preenchimento
real. A latência subtraía uma data do backend do relógio do executor e incluía as
confirmações posteriores. O relatório #611 traz 87 segundos embora a execução completa
registre cerca de quatro segundos. Isso não identifica a causa da ausência de vendas.

Foram comparadas três alternativas: separar consulta e homologação na tela existente;
adicionar uma rota de resultado; ou manter interfaces separadas. A primeira resolve os
achados com menor manutenção e menos navegação, preservando os contratos funcionais.

## Matriz de aceite local

| Área | Prova exigida |
| --- | --- |
| Consulta | Uma ação principal explícita; consulta repetida não regenera nem consome organização. |
| Estados | Homologação recolhida e secundária; conclusão depende do percurso; comandos concluídos desaparecem. |
| Retomada | Confirmação somente no cenário recuperado; perda da resposta após commit recupera a mesma saída. |
| Documentação | Fonte por item ou indicação honesta de rótulo informado; links HTTPS sem credenciais; horários e compatibilidade não verificados explícitos. |
| Entrada | Referência exige quatro campos de produto; reduzida dois; os dois produtos equivalentes efetivamente exigem quatro em ambas. Objetivo, fontes opcionais e correções separados. |
| Tempo | Primeiro resultado/bloqueio e duração total no mesmo relógio monotônico; atraso posterior não altera a latência. |
| Histórico e limites | Duas organizações utilizáveis, bloqueio seguro da segunda sem consumo, histórico consultável e controle de versão. |
| Integração | Backend real, MySQL 5.7 e frontend real; 18 combinações de cenário, dispositivo e condição. |
| Revisão | Três relatórios adicionais passam pelo consumidor real com seus PNGs; identidade diferente em RECOVERY. |
| Observabilidade | Entrada, saída, eventos e artefatos auditáveis; providerCalls=0; segregação AGENT_VALIDATION, sem prova humana ou comercial. |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; Safari nativo não comprovado. |

## Identidade e continuidade

A candidata corrigida é `mira-private-candidate-v2`. A revisão deste manifesto é v3,
distinta da revisão da experiência. Os manifestos v1/v2 e a página comercial histórica
continuam preservados. A política CHANGE_PER_CYCLE_V1 exige ciclo/experimento sucessores
para a candidata alterada; registrar somente nova evidência não permite substituir a
versão rejeitada. Nenhum ajuste dispensa Psique e Têmis independentes.

O contrato de medição V2 preserva a interpretação V1 das provas antigas e recusa
medição legada para candidatas privadas novas. A autorização do usuário é US$ 10 para
esta preparação e revisão de Mira inteira, não por tarefa ou ciclo técnico; somar as
tentativas anteriores ao acompanhar o teto. Não autoriza mídia nem geração paga de
vídeos. Custos não informados continuam desconhecidos.

## Resultado

O backend real com MySQL 5.7 e a interface compilada concluíram as 18 combinações,
sem chamadas a provedores. Cada condição usa os mesmos dois produtos: quatro campos
de produto efetivamente preenchidos, mais duas referências opcionais; o mínimo inicial
continua sendo quatro na referência e dois na reduzida. O objetivo predefinido não
foi editado em ADHERENT/RECOVERY; SAFETY registra sua edição separadamente. O primeiro
resultado ou bloqueio surgiu em até dois segundos na sandbox; isso não estima produção.

O teste adicional confirmou duas organizações, preservação do primeiro resultado após
bloqueio da segunda entrada, consulta idempotente e conclusão dependente da consulta.
Três cenários reais adicionais passaram pela validação de capturas do consumidor de
Psique, com produto/ciclo/experimento diferentes em RECOVERY. Os 164 testes do worker
passaram (duas verificações opcionais de outro ambiente não se aplicaram). Os testes
Java relevantes do backend e os 25 testes JavaScript passaram. A falha inicial de
conexão local ocorreu antes do navegador: a engine isolada expõe MySQL em
`sandbox-docker`, configurado por `MIRA_TEST_DB_HOST`, não no localhost da sandbox.
A rodada foi concluída e a topologia temporária removida.

Os testes preventivos rejeitam a contagem ambígua, links inseguros, mistura de relógios
e a matriz legada para candidatas novas. A matriz histórica permanece interpretável.
Nenhuma aprovação independente ou resultado de mercado é declarado neste registro.

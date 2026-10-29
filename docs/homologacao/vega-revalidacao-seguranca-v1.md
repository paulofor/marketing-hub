# Vega v17 — revalidação seletiva de segurança

## Causa observada

Têmis #865 reconheceu os 14 critérios locais do suplemento, mas a correção dos
comandos em contexto encerrado foi publicada depois da homologação #859 e de
Psique SAFETY #863. O fingerprint igual do frontend não prova a revisão do backend.
O bloqueio é legítimo: falta revisão independente dos controles alterados.

O backend permitia repetir uma revisão bloqueada com prova nova, mas conservava
SAFETY como concluído e não oferecia a revalidação seletiva pedida por Têmis.
Comparadas três alternativas — novo ciclo, repetir toda a matriz e revalidar somente
os controles alterados — escolhemos a última. Não houve alteração da experiência
em contexto válido, oferta ou variável do experimento. Mudanças nesses elementos
continuam exigindo sucessor; esta correção não amplia essa exceção.

## Matriz de aceite

| Caso | Resultado exigido |
| --- | --- |
| Pedido íntegro após Têmis | Apenas SAFETY reaberto; ADHERENT, RECOVERY e matriz anteriores preservados |
| Comando real do processo | Nova ocorrência para Psique no mesmo contexto; Têmis bloqueada até revisão atual |
| Parecer independente posterior | Mesmo produto/versão, resultado aprovado e citação do hash do suplemento |
| Outra identidade | Mesmas regras, sem exceções por nome ou ID |
| Hash, origem, revisão ou contexto inválido | Aceite antigo não libera Têmis |
| Mudança na experiência válida | Revalidação seletiva recusada; fluxo de sucessor preservado |
| Nova recusa ou resultado incompleto | Nenhuma repetição automática com o pedido antigo |
| Revisão anterior sem mudança de controles | Reavaliação documental e caminhos já válidos preservados |
| Observabilidade e gastos | Recibos, horários e hashes preservados; testes sintéticos, zero chamadas pagas |

A matriz visual já aceita continua referente a desktop, iPhone e Pixel. Este ajuste
não modifica a interface. Os testes de comandos e contexto são locais e usam doubles
somente nas bordas de persistência/integração; a prova dos controles reutiliza o
recibo MySQL 5.7 executado, sem mudar sua data ou alegar nova execução.

## Implementação e prova técnica

A proteção foi integrada no PR #5744 e publicada no backend na revisão
`5800a02e659a4fdf1af669977552a44800dd79fd`. O arquivo `VegaStyleService.java`
validado tem SHA-256 `8ebbc7108d4606f8dec9403c9fdc482fd7f7195fe6736e448e6e048b69d43853`.
A prova executada está em [controles operacionais](vega-v17-controles-operacionais-v1.md)
e no [recibo MySQL](evidencias/vega-v17-operational/controls-result.json).

O suplemento V2 vincula essa revisão ao produto 4, ciclo 18, experimento 111 e
`musa-pde-entry-v17-continuidade-clara`, ao resultado bruto #865 e à revisão anterior
#863. Declara aceite técnico `OPERATOR_VALIDATION` dos controles testados, sem
atribuir parecer a Dédalo ou Psique. O novo parecer independente permanece obrigatório.
A interface permanece na imagem `eb227baea7d995baea533a2b5e56474c1da79eb5`, com fingerprint
`843beca93e7e2593f1149eb27bd05427a5bbd9c3bec5d5c62baa46c910740652`.

O relatório anterior e seu horário permanecem imutáveis. A data do suplemento indica
quando a conciliação foi preparada, não quando os testes foram executados. Não há
aprovação comercial, transferência de orçamento ou mudança de campanha.

## Aprendizado e limites

A capacidade reutilizável liga a solicitação de revalidação ao parecer exato, à
revisão técnica e à evidência nova. Psique recebe o escopo e deve citar o relatório;
Têmis não pode aproveitar o aceite de segurança anterior como revisão da alteração.
A métrica técnica é reabrir somente a etapa afetada e impedir repetição sem causa nova.
Tempo de passagem, vendas e contribuição ainda não foram medidos após este ajuste.
Adotar com vínculo íntegro e parecer independente; reverter a disponibilidade da
revalidação se perder esses vínculos, preservando o bloqueio e todo o histórico.

O registro inicial #105 permanece intacto. A tela só admite a primeira prova da
candidata e não foi usada para sobrescrevê-la. A conciliação integra o contexto
versionado entregue ao próximo parecer, com o aceite técnico e a revisão do backend.

A consulta de prontidão também foi limitada à projeção escalar existente, acrescida
do horário do parecer. A conferência encontrou 1.418.966 bytes de prompts/auditorias
nas tarefas #863/#865, contra 16.993 bytes de resultados. Esses campos extensos não
são necessários para verificar hashes e cronologia; a regressão consulta os registros
sem hidratar entidades ou auditorias. Não atribuímos a lentidão geral observada a
esses dois registros: o ambiente também apresentou esgotamento de conexões e pouca
memória livre, sem consulta longa no momento da inspeção.

## Validação local concluída

228 testes de backend/arquitetura (incluindo consulta JPA sem auditoria e comando
real do BPM), 40 do executor de Psique e 17 do empacotador aprovados. O gate direto
recusa SAFETY antigo mesmo diante de um parecer de integridade aprovado; libera
somente após a revisão independente posterior com a citação da prova atual.
O pacote de evidências foi reconstruído com manifestos de compatibilidade novos,
sem editar os anteriores. Nenhuma chamada paga foi usada para essa validação.

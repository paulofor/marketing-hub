# Capella — experiência sem diagnóstico operacional

## Evidência e causa

A captura do usuário corresponde à sessão `d447117c-b5cc-48be-a296-0eef24ad7e84`,
produto 7, ciclo 14, experimento 107, `capella-private-v3`, em 25/10/2026.
O backend confirmou INPUT; não era a página comercial nem uma compra real.
O HTML compartilhado renderizava auditoria JSON e controles sintéticos no mesmo documento.
O harness media funcionamento e posições, mas não o texto em detalhes recolhidos.
Psique 686/746 aprovou funcionamento privado e explicitou limitações visuais; não apontou
essa contaminação. No ciclo 14, tarefas 811–817 são especificações, ainda sem parecer de Psique.

## Alternativas avaliadas

| Alternativa | Benefício | Risco/esforço | Escolha |
| --- | --- | --- | --- |
| Fechar ou esconder o diagnóstico | Alteração mínima | O conteúdo continua acessível; recorrência provável | Rejeitada |
| Criar outra aplicação de QA | Isolamento completo | Duplica mecanismo e aumenta manutenção | Desnecessária |
| Remover auditoria da página e reutilizar relatório administrativo | Resolve a origem sem perder provas | Ajustar harness e validar regressões | Adotada |

## Matriz definida antes dos testes

| Percurso | Verificação local |
| --- | --- |
| Entrada autenticada e sem acesso | Produto compreensível; demonstração identificada; sem IDs ou JSON |
| ADHERENT | Preparar, arte/legenda, copiar e baixar 36 arquivos; ações alcançáveis |
| RECOVERY | Transporte falha uma vez, reabertura e retorno preservam o mesmo ZIP |
| SAFETY | Bloqueio real preservado, sem arquivo ou resultado inventado |
| Falha da composição/API | Mensagem segura; erro bruto permanece no backend |
| Conteúdo recolhido | Diagnóstico ausente do DOM; fechar detalhes não faz o gate passar |
| Integrações/observabilidade | Backend, MySQL e compositor locais; relatório e eventos preservados |
| Outra identidade | nails-v1 e barber-v1 sem exceção por ID/nome |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados |
| Métricas/limites | AGENT_VALIDATION no relatório; nenhuma compra, mídia ou inferência real |

## Mudança reutilizável

O backend entrega mensagem de apresentação separada da causa operacional; a página
não serializa auditoria, não mostra caminhos/tamanhos nem controles de evento sintético.
O executor valida todo o texto, inclusive detalhes fechados, antes da revisão por modelo.
Psique deve avaliar essa separação, sem exigir que a segregação técnica apareça para a cliente.
O aviso das ilustrações agora diz “Imagem ilustrativa · exemplo de divulgação”, mantendo a
natureza da demonstração compreensível. Isso não altera oferta, condições, orçamento ou
autoriza campanha. Melhoria comercial ainda não medida.

## Validação local concluída

- Backend: 58 testes do kit, sem falhas. Psique: suíte de 243 casos sem falhas (12 condicionais
  de integração), seguida das validações com relatório real. Compositor: 66 testes sem falhas.
- Navegador: 52 testes JavaScript aprovados em sequência; os três testes da interface foram
  repetidos após simplificar o calendário e também passaram.
- MySQL 5.7, backend e compositor reais: 18 jornadas de dois perfis, seis callbacks simulados
  de Psique e quatro passagens Têmis/gate aceitas; sem inferência ou envio externos.
- Ajuste final do calendário/aviso: seis percursos aderentes adicionais, nos três dispositivos
  e duas identidades, com capturas e verificação do conteúdo recolhido. Nenhuma informação
  de auditoria renderizada; caminhos trocados por nomes simples sem alterar a estrutura do ZIP.
- Controles de identidade, consentimento, concorrência, idempotência, retomada, quotas,
  revogação, auditoria e segregação aprovados. Compose removido com volumes e órfãos.
- Spotless dos arquivos Java aplicáveis, `bash -n`, ShellCheck do runner existente e diff
  aprovados. A primeira execução complementar esbarrou no limite de processos e em builds
  concorrentes; os passos afetados foram executados sequencialmente com sucesso.

Os resultados comprovam a correção e a barreira de recorrência. Não constituem parecer novo
de Psique sobre acabamento comercial, evidência de demanda ou autorização de campanha.

## Compatibilidade das homologações

O primeiro CI detectou o hash do prompt v5 congelado na atestação de Mira. A alteração foi
isolada no prompt de kit v6, selecionado pelo runtime, sem alterar o schema de saída. O runner
e o prompt v5 foram restaurados aos bytes homologados; a verificação nova fica no harness e no
consumidor de cenários com um validador específico de prova do kit. Testes com duas identidades cobrem a seleção v6
e preservam o caminho v5. O empacotador real validou 656 arquivos e 156 manifestos, sem nova
atestação ou mudança de produto alheio ao pedido. Correspondência com o loop conhecido
`LOOP-ACTIONS-PROVA-IMUTAVEL-E-GRAFO-HISTORICO`: preservar o artefato congelado e versionar a
evolução específica elimina a dependência indevida sem enfraquecer o gate de integridade.
As seis passagens reais de cenário para os consumidores foram reexecutadas localmente com
modelos simulados, confirmação de callback e controles de isolamento aprovados após essa divisão.

O CI completo confirmou ainda que o consumidor técnico faz parte da prova operacional de Vega:
seus bytes foram preservados, mantendo a barreira no script técnico e antes do parecer por modelo.
O catálogo de gestão evolui para v3, registrando o prompt v6 e preservando o manifesto v2.
As 37 regressões do catálogo, detalhe do agente e evidência operacional passaram localmente,
assim como as dos consumidores afetados. O catálogo v3 foi comparado estruturalmente com o v2:
somente a versão do catálogo e o acréscimo do prompt específico diferem. Não há redução de
cobertura ou substituição de hashes históricos.

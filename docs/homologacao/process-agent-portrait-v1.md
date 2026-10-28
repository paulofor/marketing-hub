# Retrato do responsável no quadro de execução — 28/10/2026

O quadro reutiliza a figura mitológica cadastrada na gestão de agentes, em 48 × 48 pixels.
Responsável, status e link continuam vindo da mesma resposta da execução.

Foram comparadas três alternativas: duplicar imagens no frontend (cadastro divergente),
cruzar a lista por nome no navegador (consulta extra e associação local) e enriquecer a
resposta canônica. Foi escolhida a terceira: projeção somente de leitura, sem inferência,
novo endpoint, migração de banco ou chamada paga.

## Matriz local de aceite

| Caso | Prova esperada |
|---|---|
| Responsável com retrato | Mesma URL cadastrada, nome acessível, 48 px, link preservado. |
| Troca de responsável | Retrato acompanha a atualização, sem imagem do anterior. |
| Imagem ausente ou download falho | Nome, atividade e comandos continuam utilizáveis. |
| Humano, Backend ou contexto encerrado | Sem associação indevida a agente. |
| Outra identidade de produto | Mesmo contrato, sem regra por nome/ID de produto. |
| Estados de espera, bloqueio e pausa | Imagem não altera status nem anuncia execução. |
| Desktop, iPhone e Pixel em Chromium | Retrato visível, sem overflow e navegação correta. |
| Observabilidade e segregação | Fixtures sintéticas, nenhuma mutação, tarefa ou gasto real. |

## Melhoria do harness

A imagem existia na gestão, mas faltava no contrato da execução. O retorno agora resolve
somente o apelido completo e o frontend não procura nomes em mensagens. Os testes do quadro
passam a integrar o CI existente: troca de agente e falha da imagem não podem comprometer
acompanhamento ou navegação. Impacto comercial não foi medido.

## Validação observada

- 276 testes de automação, persistência e integração e 92 verificações de arquitetura aprovados.
- 50 testes do quadro e da tela de atividades aprovados; TypeScript aprovado.
- A sandbox tem 3 GiB efetivos. A primeira rodada do backend foi interrompida por memória;
  as três classes restantes passaram em execução isolada e com cache de contextos limitado.
- O build com sourcemaps excedeu a memória local. TypeScript foi executado separadamente;
  a conferência visual usa o mesmo código de produção compilado sem mapas de depuração.
  O build padrão completo permanece obrigatório no CI antes do merge.
- Chromium: desktop, iPhone 15 Pro e Pixel 7 aprovados no roteiro versionado
  `frontend/e2e/process-agent-portrait-responsive.mjs`, incluindo troca de agente,
  falha HTTP da imagem, responsável Backend, navegação e ausência de mutações.
- Prévia local conferida com o retrato real já cadastrado de Dédalo.
- Comentários Java de responsabilidade, métodos alterados e novos revisados; Spotless aplicado.

# Capella: homologação e capacidade do compositor

## Caso e aceite

Produto 7, ciclo 17, cadeia 27, experimento 110, candidata `capella-beleza-v2`.
Preservar Big Idea, seis especialidades, preço, provas históricas e autorizações:
US$ 50 cumulativos de preparação e revisão, R$ 89,95 de mídia por cinco dias após
homologação, sem novo audiovisual pago. Testes não são vendas nem satisfação humana.

A atividade 3.6 ainda não tinha tarefa: os contratos 866/867/869 estavam concluídos,
mas faltava `REGISTER_PROTOTYPE`. Existia apenas um pacote íntegro de barbearia.
A preparação de cabelo revelou a disputa entre consumidores: o legado não suportava
`hair-v2`; o consumidor PDE perdeu o claim. O histórico e a falha foram preservados.

## Matriz local

| Frente | Critério |
| --- | --- |
| Caminho útil | Post/story, legenda, cópia e ZIP de 36 arquivos; seis especialidades |
| Recuperação | Campo inválido, erro 503, retorno e recarga sem recomposição |
| Segurança | Origem não comprovada e efeito externo bloqueiam sem falso resultado |
| Dispositivos | Desktop 1440, iPhone 15 Pro e Pixel 7 em Chromium |
| Integração | Backend e compositor reais, MySQL 5.7, armazenamento em partes |
| Observabilidade | Hashes, manifestos, capturas e eventos da mesma versão |
| Segregação | Identidades sintéticas, AGENT_VALIDATION; sem compra ou provedor pago |
| Compatibilidade | Outro produto, perfil histórico e consumidor sem declaração |
| Concorrência | Claim incompatível não altera reserva; claim compatível é idempotente |

Antes da alteração, os três percursos locais passaram: 27 cenários e 57 capturas
(dois contextos beauty-v2 e um barber-v1). Isso comprovou o funcionamento atual,
mas não detectava o consumidor legado concorrente. A nova regressão exercita essa
fronteira diretamente, além dos testes de recuperação do callback existente.

O contrato HTTP é documentado em `docs/swagger/kit-private-prototype-swagger.yaml`.
Novos executores declaram o catálogo do código instalado. Backend atualizado sem
executor atualizado conserva a fila; não despacha trabalho para o legado como fallback.

Após a correção: 89 testes de backend e 75 de pagamentos aprovados, incluindo o replay
de ZIP e a declaração do catálogo. O MySQL 5.7 confirmou os seis perfis com 36 arquivos.
Na fronteira HTTP real, consumidor sem declaração recebeu `[]`, perfil incompatível
recebeu `[]` e claim legado recebeu 409 sem alterar a reserva. O compositor atualizado
concluiu essa mesma reserva com 36 arquivos. A matriz completa atualizada passou em
nove cenários e 21 capturas. Os contratos de publicação passaram em 11 seleções de
destino e cinco sondas, com `bash -n` e ShellCheck. Nenhum teste usou chamada paga.

## Limites e critério de adoção

Aceitar somente após testes locais do filtro e do claim, produção pelo pipeline e
conferência dos kits na versão atual. Rever a alteração se um executor compatível não
receber trabalho elegível; nunca retirar a proteção para encerrar artificialmente o gate.
Leitura visual premium e pareceres de Psique/Têmis continuam independentes da aprovação
técnica. O efeito sobre tempo parado, retrabalho, compras e contribuição requer medição
posterior; os cenários locais não demonstram melhora comercial.

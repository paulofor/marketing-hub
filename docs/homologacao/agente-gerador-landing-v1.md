# Matriz de homologação — Agente Gerador de Landing v1

## Objetivo

Comprovar localmente que o backend conduz a landing do diagnóstico até uma nova avaliação, usando
os executores oficiais do GeraLanding e do Gerador de Imagens, sem permitir autoaprovação,
publicação ou gasto de mídia.

## Matriz ponta a ponta

| Área | Cenário | Resultado esperado | Segregação/evidência |
|---|---|---|---|
| Caminho feliz | Quality Review aprova | versões finais dos anúncios voltam para `PENDING` no Aprovador | somente o experimento e as linhagens atuais |
| Copy/estrutura | recomenda copy e/ou wireframe | backend inicia a causa mais antiga e o pipeline avança pelos callbacks | job, prompt, response, custo e artefatos persistidos |
| Imagens | recomenda planejamento ou geração | reinicia no planejamento e usa o Gerador de Imagens oficial | URLs, jobs e manifesto de imagens |
| Acabamento | recomenda preset ou HTML | reinicia o preset e produz novo HTML | HTML fonte separado do publicável |
| Responsividade | defeito em desktop/mobile | Quality Review avalia screenshots desktop, iPhone e Android | hashes e URLs de screenshots |
| Contrato | reprovação sem etapa | ciclo bloqueado com erro funcional | stack trace e `experimentId` em log |
| Progresso | mesma causa e score não evolui | ciclo bloqueado antes de nova geração | reviews anteriores persistidos |
| Limite | quatro revisões autônomas | ciclo bloqueado | nenhuma publicação ou nova despesa |
| Integração | landing aprovada | Aprovador reavalia anúncio → landing | aprovação continua independente |
| Segurança | qualquer resultado | preço, orçamento, campanha e publicação permanecem inalterados | nenhuma chamada de publicação |

Uma rodada completa sem defeitos conclui a homologação. Quando uma rodada revelar defeito, a causa
deve ser corrigida e duas rodadas completas consecutivas sem falha devem ser executadas.

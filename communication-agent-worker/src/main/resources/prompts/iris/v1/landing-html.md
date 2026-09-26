# Atividade: HTML integral da landing

Materialize um documento HTML completo, responsivo e acessível a partir das atividades anteriores.
Inclua `<!doctype html>`, metadados, CSS, landmarks semânticos, foco visível, contraste, imagens
aprovadas com alt e carregamento adequado, CTA e checkout canônicos sem qualquer alteração.

Siga literalmente `landingInstrumentationContract`: marque seções com o atributo informado e o CTA
de checkout com os seletores canônicos. Não inclua `<script>`, handler inline ou chamada de rede. O
coletor pertence ao runtime publicador do backend, que o injeta depois e segrega `mh_test=1`; Íris
entrega somente o HTML declarativo e os hooks semânticos. Não invente endpoint, parâmetro ou evento.

Preencha `functionalOutput.landingHtml` com o documento integral, nunca com resumo, patch ou cerca de
Markdown. O HTML deve funcionar em desktop, iPhone e Android, preservar a prova real e não carregar
dados privados desnecessários. Não publique nem chame a URL externa. Se checkout, assets exigidos ou
contratos anteriores estiverem ausentes, bloqueie em vez de produzir uma página incompleta.

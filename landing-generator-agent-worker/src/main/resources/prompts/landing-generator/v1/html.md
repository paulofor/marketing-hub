# Materialização integral da landing por Dédalo

Produza somente o documento HTML completo exigido pela decisão já tomada. Comece em `<!doctype html>` e termine em `</html>`. O documento deve ser autocontido, responsivo, acessível e pronto para o Quality Review independente.

Preserve literalmente oferta, preço, CTA principal e `checkoutContract.canonicalUrl` do contexto. Todo link de compra deve usar `id="checkout-cta-primary"` ou `data-analytics-role="primary-checkout"` e copiar literalmente essa URL no `href`. Não use JavaScript, handlers `on*`, pixels novos, placeholders, publicação, recursos externos novos ou promessas não autorizadas.

Contexto congelado:
`{{CONTEXT}}`

Decisão estratégica já auditada:
`{{DECISION}}`

Não explique mudanças e não devolva fragmentos. Preencha `generatedHtml` com o documento integral.

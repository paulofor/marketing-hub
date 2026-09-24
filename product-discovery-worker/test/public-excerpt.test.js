import assert from "node:assert/strict";
import test from "node:test";
import { findRawPublicExcerpt } from "../src/public-excerpt.js";

test("recupera aspas e caracteres numéricos/nomeados do trecho original", () => {
  for (const raw of [
    "A opção &#x27;pronta&#x27; não resolveu a dificuldade.",
    "A opção &#39;pronta&#39; não resolveu a dificuldade.",
    "A opção &apos;pronta&apos; não resolveu a dificuldade.",
  ]) {
    assert.equal(
      findRawPublicExcerpt(
        `Antes. ${raw} Depois.`,
        "A opção 'pronta' não resolveu a dificuldade.",
      ),
      raw,
    );
  }
  const raw = "Caf&eacute; &amp; chá custaram &#x20AC; 10.";
  assert.equal(findRawPublicExcerpt(raw, "Café & chá custaram € 10."), raw);
});

test("preserva trecho literal e recupera um intervalo no meio da fonte", () => {
  const raw = "O relato dizia &quot;foi difícil&quot; e a pessoa desistiu.";
  assert.equal(findRawPublicExcerpt(raw, "O relato dizia"), "O relato dizia");
  assert.equal(
    findRawPublicExcerpt(raw, '"foi difícil" e a pessoa desistiu.'),
    "&quot;foi difícil&quot; e a pessoa desistiu.",
  );
});

test("não permite paráfrase, perda da negação, mudança de caixa ou acento", () => {
  const raw = "Eu n&atilde;o comprei a opção &quot;pronta&quot;.";
  for (const quote of [
    'Eu comprei a opção "pronta".',
    'Eu não COMPREI a opção "pronta".',
    'Eu nao comprei a opção "pronta".',
    'Preferi não comprar a opção "pronta".',
  ]) {
    assert.equal(findRawPublicExcerpt(raw, quote), null);
  }
});

test("não decodifica recursivamente nem aceita parte de uma entidade", () => {
  assert.equal(
    findRawPublicExcerpt(
      "A escolha &amp;quot;pronta&amp;quot; falhou.",
      'A escolha "pronta" falhou.',
    ),
    null,
  );
  assert.equal(
    findRawPublicExcerpt(
      "A escolha &amp;quot;pronta&amp;quot; falhou.",
      "A escolha &quot;pronta&quot; falhou.",
    ),
    "A escolha &amp;quot;pronta&amp;quot; falhou.",
  );
  assert.equal(
    findRawPublicExcerpt("Resultado &NotEqualTilde; observado", "Resultado ≂"),
    null,
  );
  assert.equal(
    findRawPublicExcerpt(
      "Relato &entidadeDesconhecida; preservado",
      "Relato removido preservado",
    ),
    null,
  );
});

test("mantém fronteiras UTF-16 e não inventa correspondência vazia", () => {
  assert.equal(
    findRawPublicExcerpt("Usei &#x1F600; na ocasião", "Usei 😀 na ocasião"),
    "Usei &#x1F600; na ocasião",
  );
  assert.equal(findRawPublicExcerpt("Texto", ""), null);
  assert.equal(findRawPublicExcerpt(null, "Uma frase inexistente"), null);
});

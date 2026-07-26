import test from "node:test";
import assert from "node:assert/strict";
import { analyzeSearchResults, buildSearchQueries } from "../src/research.js";

test("buildSearchQueries creates pain-oriented queries", () => {
  const queries = buildSearchQueries({
    theme: "mulheres que compram roupa online",
    targetAudience: "mulheres 30+",
  });

  assert.equal(queries.length, 4);
  assert.match(queries[0], /dificuldade problema/);
});

test("analyzeSearchResults approves strong non-sensitive PDE opportunity", () => {
  const report = analyzeSearchResults(
    {
      theme: "mulheres que compram roupa online",
      targetAudience: "mulheres 30+",
    },
    [
      {
        title: "Dificuldade para escolher roupa online",
        url: "https://forum.example/a",
        snippet: "problema insegurança caro complicado como fazer",
      },
      {
        title: "Review de consultoria de estilo",
        url: "https://reviews.example/b",
        snippet: "demorado confuso não resolve reclamação",
      },
      {
        title: "Perguntas frequentes sobre estilo",
        url: "https://questions.example/c",
        snippet: "não consigo decidir e tenho medo de errar",
      },
    ],
  );

  assert.equal(report.opportunities.length, 1);
  assert.equal(report.opportunities[0].decision, "APPROVE");
  assert.ok(report.opportunities[0].score >= 70);
});

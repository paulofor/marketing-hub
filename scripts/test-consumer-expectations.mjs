import { test } from "node:test";
import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { createWriteStream } from "node:fs";
import { mkdir, readFile, readdir } from "node:fs/promises";

await mkdir(".codex/expectation-evidence", { recursive: true });

// Preserva logs parciais e mantém processos de teste dentro da capacidade local.
async function run(name, command, args) {
  process.stdout.write(`# Executando ${name}\n`);
  const env = {
    ...process.env,
    MAVEN_OPTS: "-Xmx256m",
    // Limita só os processos deste teste; não altera POM nem instalação compartilhada.
    _JAVA_OPTIONS: name === "architecture" ? "-Xmx2048m" : "-Xmx1280m",
  };
  if (name === "build") env.NODE_OPTIONS = "--max-old-space-size=2304";
  // Testes Node dos subprocessos devem executar, sem herdar o marcador do runner externo.
  delete env.NODE_TEST_CONTEXT;
  const child = spawn(command, args, {
    stdio: ["ignore", "pipe", "pipe"],
    env,
  });
  const log = createWriteStream(`.codex/expectation-evidence/${name}.log`);
  let output = "";
  const capture = (chunk) => {
    log.write(chunk);
    output = (output + chunk).slice(-16000);
  };
  child.stdout.on("data", capture);
  child.stderr.on("data", capture);
  const code = await new Promise((resolve, reject) => {
    child.on("error", reject);
    child.on("close", resolve);
  });
  await new Promise((resolve) => log.end(resolve));
  assert.equal(code, 0, `${name}: ${output.slice(-16000)}`);
}

async function compile() {
  await run("compile", "mvn", [
    "-B",
    "-f",
    "backend/ads-service/pom.xml",
    "test-compile",
  ]);
}

async function testClasses() {
  // Seleção padrão do Surefire, incluindo os testes compartilhados compilados pelo POM.
  return (
    await readdir("backend/ads-service/target/test-classes", {
      recursive: true,
    })
  )
    .filter(
      (file) =>
        /(?:^|\/)(?:Test[^/]*|[^/]*Tests?|[^/]*TestCase)\.class$/.test(file) &&
        !file.includes("$"),
    )
    .map((file) => file.slice(0, -6).replaceAll("/", "."))
    .sort();
}

// Prova funcional independente para não repetir o percurso ao corrigir apenas build/regressão.
test(
  "Consumer expectations v1: ciencia, planejamento, harness e aprendizado ponta a ponta",
  { timeout: 900000 },
  async () => {
    await compile();
    const classes = await testClasses();
    const focused = classes.filter((name) =>
      /\.(consumerexpectation|researchintelligence)\./.test(name),
    );
    assert.ok(
      focused.some((name) =>
        name.endsWith("ConsumerExpectationIntegrationTest"),
      ),
    );
    await run("backend-focused", "mvn", [
      "-B",
      "-f",
      "backend/ads-service/pom.xml",
      "-Dtest=" + focused.join(","),
      "-Dspring.test.context.cache.maxSize=2",
      "surefire:test",
    ]);
    await run("browser", "mvn", [
      "-B",
      "-f",
      "backend/ads-service/pom.xml",
      "-Dtest=ConsumerExpectationIntegrationTest#browserUsesRealLocalBackend",
      "-Dexpectation.browser=true",
      "surefire:test",
    ]);
    const browser = await readFile(
      ".codex/expectation-evidence/browser.tap",
      "utf8",
    );
    assert.match(browser, /ok 1 - Expectativas: pesquisa/);
    assert.doesNotMatch(browser, /not ok/);
    await run("frontend", "npm", [
      "--prefix",
      "frontend",
      "test",
      "--",
      "--run",
      // A suíte completa também verifica que o Vitest não absorve checks exclusivos do Node.
      "--maxWorkers=1",
      "--minWorkers=1",
    ]);
  },
);

test(
  "Consumer expectations v1: tipagem e build frontend",
  { timeout: 900000 },
  async () => {
    await run("typecheck", "npm", ["--prefix", "frontend", "run", "typecheck"]);
    await run("build", "npm", ["--prefix", "frontend", "run", "build"]);
  },
);

test(
  "Consumer expectations v1: regressao do backend e arquitetura",
  { timeout: 7200000 },
  async () => {
    await compile();
    const classes = await testClasses();
    // Reinicia a JVM a cada lote para não acumular metadados de centenas de contextos Spring.
    const remaining = classes.filter(
      (name) =>
        !/\.(consumerexpectation|researchintelligence)\./.test(name) &&
        !name.endsWith(".ArquiteturaTest"),
    );
    for (let i = 0; i < remaining.length; i += 30) {
      await run(`backend-batch-${String(i / 30 + 1).padStart(2, "0")}`, "mvn", [
        "-B",
        "-f",
        "backend/ads-service/pom.xml",
        "-Dtest=" + remaining.slice(i, i + 30).join(","),
        "-Dspring.test.context.cache.maxSize=2",
        "surefire:test",
      ]);
    }
    await run("architecture", "mvn", [
      "-B",
      "-f",
      "backend/ads-service/pom.xml",
      "-Dtest=ArquiteturaTest",
      "surefire:test",
    ]);
  },
);

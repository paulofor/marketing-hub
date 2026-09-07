import { spawn } from "node:child_process";
import { createHash } from "node:crypto";
import { createReadStream, createWriteStream } from "node:fs";
import { mkdir, readFile, rm, writeFile } from "node:fs/promises";
import path from "node:path";
import { pipeline } from "node:stream/promises";
import { fileURLToPath } from "node:url";
import { createGzip } from "node:zlib";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const [operation, directory, ...args] = process.argv.slice(2);
const reserveMb = 4096;
const immutableReference = /^[a-z0-9][a-z0-9./_-]*:[a-f0-9]{40}$/;

// Executa uma operação limitada e preserva a causa de falhas do transporte ou Docker.
function command(executable, parameters, input, output) {
  return new Promise((resolve, reject) => {
    const child = spawn(executable, parameters, {
      stdio: [input ? "pipe" : "ignore", "pipe", "inherit"],
      timeout: 600_000,
    });
    let stdout = "";
    let inputDone = Promise.resolve();
    let outputDone = Promise.resolve();
    if (input) inputDone = pipeline(input, child.stdin);
    if (output) outputDone = pipeline(child.stdout, output, { end: output !== process.stdout });
    else child.stdout.on("data", (chunk) => { stdout += chunk; });
    // A rejeição da stream é observada imediatamente, inclusive antes do evento close.
    const streamsDone = Promise.allSettled([inputDone, outputDone]);
    child.on("error", reject);
    child.on("close", async (code, signal) => {
      const streams = await streamsDone;
      if (code !== 0) {
        reject(new Error(`${executable}: operação falhou (código=${code}, sinal=${signal ?? "nenhum"}).`));
      } else if (streams.some((result) => result.status === "rejected")) {
        reject(new Error(`${executable}: transmissão incompleta.`));
      } else resolve(stdout.trim());
    });
  });
}

// Confere a lista exata de referências que o workflow espera para a revisão em publicação.
function validateReferences(references) {
  if (!references.length || references.some((reference) => !immutableReference.test(reference))
      || new Set(references).size !== references.length) {
    throw new Error("Informe imagens distintas com tag SHA imutável de 40 caracteres.");
  }
}

async function checksum(file) {
  const hash = createHash("sha256");
  for await (const chunk of createReadStream(file)) hash.update(chunk);
  return hash.digest("hex");
}

// Valida antes de qualquer SSH; arquivo ausente/corrompido nunca autoriza publicação.
async function verifyBundle(references) {
  validateReferences(references);
  const manifest = JSON.parse(await readFile(path.join(directory, "manifest.json"), "utf8"));
  if (manifest.version !== 1 || !Array.isArray(manifest.images)
      || manifest.images.length !== references.length
      || !/^[a-f0-9]{64}$/.test(manifest.archiveSha256)) {
    throw new Error("Manifesto de imagens inválido.");
  }
  for (const [index, image] of manifest.images.entries()) {
    if (image.reference !== references[index] || !/^sha256:[a-f0-9]{64}$/.test(image.id)
        || !Number.isSafeInteger(image.sizeBytes) || image.sizeBytes <= 0) {
      throw new Error("Imagem, identidade ou tamanho divergente do contrato de publicação.");
    }
  }
  if (await checksum(path.join(directory, "images.tar.gz")) !== manifest.archiveSha256) {
    throw new Error("Checksum divergente; pacote de imagens bloqueado antes da transferência.");
  }
  return manifest;
}

// Empacota a imagem já testada; o VPS recebe as mesmas camadas e identidade do runner.
async function pack(references) {
  validateReferences(references);
  await mkdir(directory, { recursive: true });
  await rm(path.join(directory, "manifest.json"), { force: true });
  const images = [];
  for (const reference of references) {
    const result = await command("docker", ["image", "inspect", "--format", "{{.Id}} {{.Size}}", reference]);
    const [id, size] = result.split(" ");
    if (!/^sha256:[a-f0-9]{64}$/.test(id) || !/^[1-9][0-9]*$/.test(size) || !Number.isSafeInteger(Number(size))) {
      throw new Error(`Identidade/tamanho Docker inválido: ${reference}`);
    }
    images.push({ reference, id, sizeBytes: Number(size) });
  }
  const archive = path.join(directory, "images.tar.gz");
  const gzip = createGzip({ level: 1 });
  const archiveDone = pipeline(gzip, createWriteStream(archive));
  // Observa ambas as pipelines para falha de disco/compressão não deixar manifesto válido.
  const results = await Promise.allSettled([
    command("docker", ["image", "save", ...references], undefined, gzip), archiveDone,
  ]);
  for (const result of results) if (result.status === "rejected") throw result.reason;
  const manifest = { version: 1, images, archiveSha256: await checksum(archive) };
  await writeFile(path.join(directory, "manifest.json"), `${JSON.stringify(manifest, null, 2)}\n`);
  console.log(`Pacote validado: imagens=${images.length} sha256=${manifest.archiveSha256}`);
}

// Reserva espaço para camadas descompactadas e extração transitória, além dos 4 GiB operacionais.
function requiredSpace(manifest) {
  const size = manifest.images.reduce((total, image) => total + image.sizeBytes, 0);
  const minimum = reserveMb + Math.ceil(2 * size / 1_048_576);
  if (!Number.isSafeInteger(minimum) || minimum > 99_999_999) throw new Error("Tamanho total inválido.");
  return minimum;
}

// Transmite por stdin sem gravar tar no VPS, confere identidades e valida reserva antes do restart.
async function send(target, references) {
  const manifest = await verifyBundle(references);
  if (process.env.SSH_DEPLOY_READY !== "true" || !/^[a-z_][a-z0-9_-]*@[a-zA-Z0-9.-]+$/.test(target)) {
    throw new Error("Destino inválido ou preflight SSH não autenticado.");
  }
  const config = process.env.SSH_COMMON_ARGS?.match(/^-F (\/[^\s]+)$/)?.[1];
  if (!config) throw new Error("Configuração SSH autenticada ausente ou inválida.");
  const ssh = (remoteCommand, input) => command("ssh", ["-F", config, target, remoteCommand], input, process.stdout);
  const gate = (minimum) => ssh(`AGENT_VPS_DISK_MIN_FREE_MB=${minimum} bash -s`,
    createReadStream(path.join(root, "scripts/ensure-agent-vps-disk-space.sh")));
  const minimum = requiredSpace(manifest);
  console.log(`Carga de imagens: minimumMb=${minimum} operationalReserveMb=${reserveMb}`);
  await gate(minimum);
  await ssh("timeout --kill-after=5s 600s docker image load", createReadStream(path.join(directory, "images.tar.gz")));
  for (const image of manifest.images) {
    const actual = await command("ssh", ["-F", config, target,
      `docker image inspect --format '{{.Id}}' '${image.reference}'`]);
    if (actual !== image.id) throw new Error(`Identidade carregada divergente: ${image.reference}`);
    console.log(`Imagem confirmada: ${image.reference} id=${actual}`);
  }
  await gate(reserveMb);
  console.log("Imagens prontas; capacidade e identidade aprovadas para Compose sem build.");
}

try {
  if (!directory || !["pack", "verify", "send"].includes(operation)) {
    throw new Error("Uso: node scripts/agent-image-bundle.mjs pack|verify <diretório> <imagem...> ou send <diretório> <usuário@host> <imagem...>");
  }
  if (operation === "pack") await pack(args);
  if (operation === "verify") {
    const manifest = await verifyBundle(args);
    console.log(`Pacote íntegro: imagens=${manifest.images.length} minimumMb=${requiredSpace(manifest)}`);
  }
  if (operation === "send") await send(args[0], args.slice(1));
} catch (error) {
  console.error(`Imagens dos agentes: ${error.message}`);
  process.exitCode = 1;
}

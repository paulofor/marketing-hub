import { spawn } from "node:child_process";
import { createHash } from "node:crypto";
import { once } from "node:events";
import { createReadStream, createWriteStream } from "node:fs";
import { mkdir, readFile, rm, writeFile } from "node:fs/promises";
import path from "node:path";
import { pipeline } from "node:stream/promises";
import { fileURLToPath } from "node:url";
import { createGzip } from "node:zlib";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const [operation, directory, ...args] = process.argv.slice(2);
const reserveMb = 4096;
const loadAttempts = 3;
const immutableReference = /^[a-z0-9][a-z0-9./_-]*:[a-f0-9]{40}$/;
const sha256Digest = /^sha256:[a-f0-9]{64}$/;

// Encaminha logs com backpressure sem acumular listeners no stdout global entre comandos SSH.
async function forwardOutput(source, destination) {
  for await (const chunk of source) {
    if (!destination.write(chunk)) await once(destination, "drain");
  }
}

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
    if (output === process.stdout) outputDone = forwardOutput(child.stdout, output);
    else if (output) outputDone = pipeline(child.stdout, output);
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

// Ordena objetos recursivamente para que engines Docker diferentes gerem a mesma prova semântica.
function canonicalJson(value) {
  if (Array.isArray(value)) return value.map(canonicalJson);
  if (value !== null && typeof value === "object") {
    return Object.fromEntries(Object.entries(value).sort(([left], [right]) => left < right ? -1 : left > right ? 1 : 0)
      .map(([key, child]) => [key, canonicalJson(child)]));
  }
  return value;
}

// Normaliza uma lista de strings do contrato da imagem e rejeita respostas Docker incompletas.
function stringList(value, field, nullable = false) {
  if (value === null || value === undefined) return nullable ? null : [];
  if (!Array.isArray(value) || value.some((item) => typeof item !== "string")) {
    throw new Error(`Inspeção Docker inválida em ${field}.`);
  }
  return value;
}

// Normaliza mapas da configuração cujo valor é JSON e elimina variação apenas de ordem de chaves.
function jsonMap(value, field) {
  if (value === null || value === undefined) return {};
  if (typeof value !== "object" || Array.isArray(value)) {
    throw new Error(`Inspeção Docker inválida em ${field}.`);
  }
  return canonicalJson(value);
}

// Produz identidade portátil com camadas e configuração funcional, independente do image store.
function portableContent(image) {
  const config = image.Config;
  if (!config || typeof config !== "object" || Array.isArray(config)
      || image.RootFS?.Type !== "layers" || !Array.isArray(image.RootFS.Layers)
      || image.RootFS.Layers.length === 0 || image.RootFS.Layers.some((layer) => !sha256Digest.test(layer))
      || typeof image.Architecture !== "string" || !image.Architecture
      || typeof image.Os !== "string" || !image.Os) {
    throw new Error("Inspeção Docker sem conteúdo portátil válido.");
  }
  const text = (value, field) => {
    if (value === null || value === undefined) return "";
    if (typeof value !== "string") throw new Error(`Inspeção Docker inválida em ${field}.`);
    return value;
  };
  const flag = (value, field) => {
    if (value === null || value === undefined) return false;
    if (typeof value !== "boolean") throw new Error(`Inspeção Docker inválida em ${field}.`);
    return value;
  };
  const optionalNumber = (value, field) => {
    if (value === null || value === undefined) return null;
    if (!Number.isSafeInteger(value) || value < 0) throw new Error(`Inspeção Docker inválida em ${field}.`);
    return value;
  };
  let healthcheck = null;
  if (config.Healthcheck != null) {
    if (typeof config.Healthcheck !== "object" || Array.isArray(config.Healthcheck)) {
      throw new Error("Inspeção Docker inválida em Config.Healthcheck.");
    }
    healthcheck = {
      test: stringList(config.Healthcheck.Test, "Config.Healthcheck.Test"),
      interval: optionalNumber(config.Healthcheck.Interval, "Config.Healthcheck.Interval"),
      timeout: optionalNumber(config.Healthcheck.Timeout, "Config.Healthcheck.Timeout"),
      startPeriod: optionalNumber(config.Healthcheck.StartPeriod, "Config.Healthcheck.StartPeriod"),
      startInterval: optionalNumber(config.Healthcheck.StartInterval, "Config.Healthcheck.StartInterval"),
      retries: optionalNumber(config.Healthcheck.Retries, "Config.Healthcheck.Retries"),
    };
  }
  return canonicalJson({
    version: 1,
    platform: {
      architecture: image.Architecture,
      os: image.Os,
      variant: text(image.Variant, "Variant"),
    },
    rootFsLayers: image.RootFS.Layers,
    config: {
      hostname: text(config.Hostname, "Config.Hostname"),
      domainname: text(config.Domainname, "Config.Domainname"),
      user: text(config.User, "Config.User"),
      attachStdin: flag(config.AttachStdin, "Config.AttachStdin"),
      attachStdout: flag(config.AttachStdout, "Config.AttachStdout"),
      attachStderr: flag(config.AttachStderr, "Config.AttachStderr"),
      tty: flag(config.Tty, "Config.Tty"),
      openStdin: flag(config.OpenStdin, "Config.OpenStdin"),
      stdinOnce: flag(config.StdinOnce, "Config.StdinOnce"),
      env: stringList(config.Env, "Config.Env"),
      cmd: stringList(config.Cmd, "Config.Cmd", true),
      healthcheck,
      argsEscaped: flag(config.ArgsEscaped, "Config.ArgsEscaped"),
      volumes: jsonMap(config.Volumes, "Config.Volumes"),
      workingDir: text(config.WorkingDir, "Config.WorkingDir"),
      entrypoint: stringList(config.Entrypoint, "Config.Entrypoint", true),
      networkDisabled: flag(config.NetworkDisabled, "Config.NetworkDisabled"),
      macAddress: text(config.MacAddress, "Config.MacAddress"),
      onBuild: stringList(config.OnBuild, "Config.OnBuild"),
      labels: jsonMap(config.Labels, "Config.Labels"),
      stopSignal: text(config.StopSignal, "Config.StopSignal"),
      stopTimeout: optionalNumber(config.StopTimeout, "Config.StopTimeout"),
      shell: stringList(config.Shell, "Config.Shell"),
      exposedPorts: jsonMap(config.ExposedPorts, "Config.ExposedPorts"),
    },
  });
}

// Interpreta uma inspeção Docker completa e calcula a prova portátil usada entre runner e VPS.
function inspectImage(payload) {
  let response;
  try {
    response = JSON.parse(payload);
  } catch {
    throw new Error("Resposta de inspeção Docker não é JSON válido.");
  }
  if (!Array.isArray(response) || response.length !== 1) {
    throw new Error("Resposta de inspeção Docker deve conter exatamente uma imagem.");
  }
  const [image] = response;
  if (!image || !sha256Digest.test(image.Id)
      || !Number.isSafeInteger(image.Size) || image.Size <= 0) {
    throw new Error("Identidade/tamanho Docker inválido.");
  }
  const proof = createHash("sha256").update(JSON.stringify(portableContent(image))).digest("hex");
  return { id: image.Id, sizeBytes: image.Size, contentSha256: `sha256:${proof}` };
}

// Valida antes de qualquer SSH; arquivo ausente/corrompido nunca autoriza publicação.
async function verifyBundle(references) {
  validateReferences(references);
  const manifest = JSON.parse(await readFile(path.join(directory, "manifest.json"), "utf8"));
  if (manifest.version !== 2 || !Array.isArray(manifest.images)
      || manifest.images.length !== references.length
      || !/^[a-f0-9]{64}$/.test(manifest.archiveSha256)) {
    throw new Error("Manifesto de imagens inválido.");
  }
  for (const [index, image] of manifest.images.entries()) {
    if (image.reference !== references[index] || !sha256Digest.test(image.sourceId)
        || !sha256Digest.test(image.contentSha256)
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
    const inspection = inspectImage(await command("docker", ["image", "inspect", reference]));
    images.push({ reference, sourceId: inspection.id, sizeBytes: inspection.sizeBytes,
      contentSha256: inspection.contentSha256 });
  }
  const archive = path.join(directory, "images.tar.gz");
  const gzip = createGzip({ level: 1 });
  const archiveDone = pipeline(gzip, createWriteStream(archive));
  // Observa ambas as pipelines para falha de disco/compressão não deixar manifesto válido.
  const results = await Promise.allSettled([
    command("docker", ["image", "save", ...references], undefined, gzip), archiveDone,
  ]);
  for (const result of results) if (result.status === "rejected") throw result.reason;
  const manifest = { version: 2, images, archiveSha256: await checksum(archive) };
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

// Transmite por stdin sem gravar tar no VPS, recupera materialização transitória e valida antes do restart.
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
  let loadedImages = [];
  for (let attempt = 1; attempt <= loadAttempts; attempt += 1) {
    await ssh("timeout --kill-after=5s 600s docker image load",
      createReadStream(path.join(directory, "images.tar.gz")));
    loadedImages = [];
    let unavailable;
    for (const image of manifest.images) {
      let inspection;
      try {
        inspection = await command("ssh", ["-F", config, target,
          `docker image inspect '${image.reference}'`]);
      } catch (error) {
        unavailable = { image, error };
        break;
      }
      loadedImages.push({ image, actual: inspectImage(inspection) });
    }
    if (!unavailable) break;
    if (attempt === loadAttempts) {
      throw new Error(`Imagem indisponível após ${loadAttempts} cargas verificadas: ${unavailable.image.reference}; ${unavailable.error.message}`);
    }
    console.error(`Image store não materializou ${unavailable.image.reference}; repetindo o mesmo pacote íntegro (${attempt + 1}/${loadAttempts}).`);
    await gate(minimum);
  }
  for (const { image, actual } of loadedImages) {
    if (actual.contentSha256 !== image.contentSha256) {
      throw new Error(`Conteúdo carregado divergente: ${image.reference} sourceId=${image.sourceId} loadedId=${actual.id}`);
    }
    if (actual.id === image.sourceId) {
      console.log(`Imagem confirmada: ${image.reference} id=${actual.id} conteúdo=${actual.contentSha256}`);
    } else {
      console.log(`Imagem confirmada: ${image.reference} conteúdo=${actual.contentSha256}; ID do store variou de ${image.sourceId} para ${actual.id}`);
    }
  }
  await gate(reserveMb);
  console.log("Imagens prontas; capacidade e conteúdo aprovados para Compose sem build.");
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

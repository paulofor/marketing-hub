import { createHash } from "node:crypto";
import { promises as fs } from "node:fs";
import path from "node:path";
import { pathToFileURL } from "node:url";

const SOURCE_DIRECTORIES = ["docker-entrypoint.d", "public", "scripts", "src"];
const SOURCE_FILES = [
  ".npmrc",
  "Dockerfile",
  "index.html",
  "mira.html",
  "nginx.conf",
  "nginx.mira.conf",
  "package-lock.json",
  "package.json",
  "tsconfig.json",
  "tsconfig.vega.json",
  "vite.config.ts",
  "vite.mira.config.ts",
  "vite.vega.config.ts",
];

async function collectFiles(root, relativeDirectory) {
  const directory = path.join(root, relativeDirectory);
  const entries = await fs.readdir(directory, { withFileTypes: true });
  const files = [];
  for (const entry of entries.sort((left, right) => left.name.localeCompare(right.name))) {
    const relativePath = path.posix.join(relativeDirectory, entry.name);
    if (entry.isDirectory()) {
      files.push(...(await collectFiles(root, relativePath)));
    } else if (entry.isFile()) {
      files.push(relativePath);
    } else {
      throw new Error(`Fonte frontend não regular: ${relativePath}`);
    }
  }
  return files;
}

export async function sourceFingerprint(rootArgument) {
  const root = path.resolve(rootArgument);
  const files = [...SOURCE_FILES];
  for (const directory of SOURCE_DIRECTORIES) {
    files.push(...(await collectFiles(root, directory)));
  }
  const hash = createHash("sha256");
  for (const relativePath of files.sort()) {
    const content = await fs.readFile(path.join(root, relativePath));
    hash.update(relativePath);
    hash.update("\0");
    hash.update(String(content.length));
    hash.update("\0");
    hash.update(content);
    hash.update("\0");
  }
  return hash.digest("hex");
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  process.stdout.write(`${await sourceFingerprint(process.argv[2] ?? process.cwd())}\n`);
}

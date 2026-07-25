import { execFileSync } from "node:child_process";
import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const frontendDir = join(scriptDir, "..");
const publicDir = join(frontendDir, "public");
const healthzPath = join(publicDir, "healthz");

function resolveGitCommit() {
  const explicitCommit =
    process.env.FRONTEND_BUILD_COMMIT ||
    process.env.VITE_BUILD_COMMIT ||
    process.env.GITHUB_SHA ||
    process.env.COMMIT_SHA;

  if (explicitCommit) {
    return explicitCommit;
  }

  try {
    return execFileSync("git", ["rev-parse", "HEAD"], {
      cwd: join(frontendDir, ".."),
      encoding: "utf8",
      stdio: ["ignore", "pipe", "ignore"],
    }).trim();
  } catch {
    return "unknown";
  }
}

const buildInfo = {
  status: "ok",
  service: "marketinghub-frontend",
  commit: resolveGitCommit(),
  imageTag: process.env.FRONTEND_IMAGE_TAG || process.env.IMAGE_TAG || process.env.GITHUB_SHA || "local",
  builtAt: new Date().toISOString(),
};

mkdirSync(publicDir, { recursive: true });
writeFileSync(healthzPath, `${JSON.stringify(buildInfo, null, 2)}\n`);

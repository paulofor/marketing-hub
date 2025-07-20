export async function decodeInvariant(error: Error): Promise<string | undefined> {
  const match = /#(\d+)/.exec(error.message);
  if (!match) return undefined;
  const code = match[1];
  const url = `https://react.dev/errors/${code}`;
  try {
    const res = await fetch(url);
    if (res.ok) {
      const text = await res.text();
      return text;
    }
  } catch {
    // ignore network errors
  }
  return undefined;
}

export async function decodeInvariant(error: unknown): Promise<string | null> {
  let message = "";
  if (typeof error === "string") {
    message = error;
  } else if (error && typeof error === "object" && 'message' in error) {
    message = (error as any).message as string;
  }
  const match = message.match(/#(\d+)/);
  if (!match) return null;
  const code = match[1];
  try {
    const res = await fetch(`https://react.dev/errors/${code}`);
    if (res.ok) {
      const text = await res.text();
      console.error(text);
      return text;
    }
  } catch {
    console.error(message);
  }
  return null;
}

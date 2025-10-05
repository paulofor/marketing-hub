export interface PreserveLinkedValueOptions<TInput, TValue> {
  input: TInput | null | undefined;
  persisted: TValue | null | undefined;
  parse?: (value: TInput) => TValue;
  emptyInputs?: Array<TInput | null | undefined>;
}

/**
 * Keeps the association with a linked entity intact when the user does not interact with the
 * corresponding field in the form. Explicit empty selections (for example selecting "Nenhum")
 * still nullify the relationship, while untouched fields reuse the previously persisted value.
 */
export function preserveLinkedValue<TInput, TValue>({
  input,
  persisted,
  parse,
  emptyInputs = ["" as unknown as TInput],
}: PreserveLinkedValueOptions<TInput, TValue>): TValue | null | undefined {
  if (input === undefined) {
    return persisted;
  }

  if (input === null) {
    return null;
  }

  if (emptyInputs.some((value) => value === input)) {
    return null;
  }

  const parser = parse ?? ((value: TInput) => value as unknown as TValue);
  return parser(input);
}

"""Valida DEFAULT na própria declaração SQL, preservando precisão temporal e número da linha."""
import re


def timestamp_columns_without_default(sql):
    """Localiza TIMESTAMP obrigatório sem confundir DEFAULT de outra coluna, texto ou comentário."""
    literals = re.compile(r"/\*.*?\*/|--[^\n]*|\#[^\n]*|'(?:''|\\.|[^'])*'|\"(?:\"\"|\\.|[^\"])*\"|`(?:``|[^`])*`", re.DOTALL)
    masked = literals.sub(lambda match: re.sub(r"[^\n]", " ", match.group()), sql)
    violations = []
    for match in re.finditer(r"\btimestamp\b(?:\s*\(\s*\d+\s*\))?", masked, re.IGNORECASE):
        depth = 0
        end = match.end()
        while end < len(masked):
            char = masked[end]
            if char == "(":
                depth += 1
            elif char == ")":
                if depth == 0:
                    break
                depth -= 1
            elif char in ",;" and depth == 0:
                break
            end += 1
        declaration = masked[match.end():end]
        if re.search(r"\bnot\s+null\b", declaration, re.IGNORECASE) and not re.search(r"\bdefault\b", declaration, re.IGNORECASE):
            violations.append(masked.count("\n", 0, match.start()) + 1)
    return sorted(set(violations))

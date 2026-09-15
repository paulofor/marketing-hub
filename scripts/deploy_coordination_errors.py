"""Erros tipados da coordenação, sem conteúdo de respostas ou credenciais externas."""


class CoordinationError(RuntimeError):
    """Representa uma condição que impede operar ou liberar a proteção."""


class GitHubApiError(CoordinationError):
    """Preserva o status HTTP para distinguir uma revisão indisponível de outros erros."""

    def __init__(self, message, status=None):
        super().__init__(message)
        self.status = status

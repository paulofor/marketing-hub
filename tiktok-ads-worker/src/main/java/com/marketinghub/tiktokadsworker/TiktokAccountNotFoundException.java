package com.marketinghub.tiktokadsworker;

/** Sinaliza que uma conta TikTok Ads solicitada não existe no módulo. */
public class TiktokAccountNotFoundException extends RuntimeException {

    /** Cria a exceção com o identificador ausente. */
    public TiktokAccountNotFoundException(Long id) {
        super("Conta TikTok não encontrada: " + id);
    }
}

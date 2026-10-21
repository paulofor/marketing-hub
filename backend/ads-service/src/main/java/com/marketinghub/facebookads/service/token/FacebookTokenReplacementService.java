package com.marketinghub.facebookads.service.token;

import com.marketinghub.ads.FacebookAccount;
import com.marketinghub.ads.FacebookTokenVerificationClient;
import com.marketinghub.repository.jpa.ads.FacebookAccountRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Substitui somente uma credencial validada, preservando automação e alterações concorrentes. */
@Service
public class FacebookTokenReplacementService {
  private static final Logger log = LoggerFactory.getLogger(FacebookTokenReplacementService.class);
  private final FacebookAccountRepository repository;
  private final FacebookTokenVerificationClient verifier;

  /** Recebe a persistência canônica e a verificação independente da candidata. */
  public FacebookTokenReplacementService(
      FacebookAccountRepository repository, FacebookTokenVerificationClient verifier) {
    this.repository = repository;
    this.verifier = verifier;
  }

  /** Valida antes da escrita e confirma a troca sem devolver o segredo ao navegador. */
  public FacebookTokenReplacementResponse replace(
      Long id, FacebookTokenReplacementRequest request) {
    String candidate =
        request == null || request.accessToken() == null ? "" : request.accessToken().trim();
    if (candidate.length() < 8
        || candidate.length() > 8192
        || !candidate.matches("[A-Za-z0-9._~+/-]+={0,2}")) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Cole somente o token de usuário, sem URL, aspas ou prefixo Bearer.");
    }
    FacebookAccount account =
        repository
            .findById(id)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Conta Meta não encontrada."));
    if (!StringUtils.hasText(account.getAppId())
        || !StringUtils.hasText(account.getAppSecret())
        || !StringUtils.hasText(account.getAdAccountId())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Preencha App ID, App Secret e conta de anúncios antes de atualizar o token.");
    }
    LocalDateTime expiration = verifier.verify(account, candidate);
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    int changed =
        repository.replaceVerifiedToken(
            id,
            account.getAccessToken(),
            account.getAppId(),
            account.getAppSecret(),
            account.getAdAccountId(),
            account.getAuthorizedUserId(),
            candidate,
            expiration,
            now);
    if (changed != 1) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "A conta mudou durante a validação. Atualize a tela e confira antes de tentar novamente.");
    }
    boolean renewalEnabled = repository.findById(id).orElseThrow().isTokenRenewalEnabled();
    log.info(
        "Meta: token validado e substituído; accountId={}, expiresAt={}, renovaçãoAutomática={}",
        id,
        expiration,
        renewalEnabled);
    return new FacebookTokenReplacementResponse(id, expiration, now, renewalEnabled);
  }
}

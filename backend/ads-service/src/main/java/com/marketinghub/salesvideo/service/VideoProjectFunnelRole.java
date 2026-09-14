package com.marketinghub.salesvideo.service;

import com.marketinghub.salesvideo.VideoProject;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Resolve o papel comercial pelos canais canônicos, sem inferir pelo título ou usar fallback. */
public enum VideoProjectFunnelRole {
  AD,
  LANDING_HERO,
  PRE_CHECKOUT;

  /** Valida o canal antes do preflight e repete a mesma regra no retorno do vídeo. */
  public static VideoProjectFunnelRole resolve(VideoProject project) {
    String channel =
        project.getTargetChannel() == null
            ? ""
            : project.getTargetChannel().trim().toUpperCase(Locale.ROOT);
    if (channel.equals("SOCIAL_REELS_STORIES")
        || channel.startsWith("INSTAGRAM")
        || channel.startsWith("FACEBOOK")
        || channel.equals("META")
        || channel.startsWith("TIKTOK")) return AD;
    if (channel.equals("PDE_HERO_DIAGNOSTIC")
        || channel.equals("PDE")
        || channel.equals("LANDING_HERO")) return LANDING_HERO;
    if (channel.equals("PAYWALL_OFFER")) return PRE_CHECKOUT;
    throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "Defina um canal único para o vídeo: anúncio social, hero do PDE ou oferta antes do checkout.");
  }
}

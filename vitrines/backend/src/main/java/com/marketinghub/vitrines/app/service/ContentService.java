package com.marketinghub.vitrines.app.service;

import com.marketinghub.vitrines.app.model.AccessType;
import com.marketinghub.vitrines.app.model.CheckoutRequest;
import com.marketinghub.vitrines.app.model.CheckoutResponse;
import com.marketinghub.vitrines.app.model.ContentCardResponse;
import com.marketinghub.vitrines.app.model.ContentDetailResponse;
import com.marketinghub.vitrines.app.model.ContentItem;
import com.marketinghub.vitrines.app.model.Role;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContentService {

  private final List<ContentItem> contents =
      List.of(
          new ContentItem(
              "1",
              "Kit social media gratuito",
              "Modelos editáveis para captar leads rapidamente com posts e stories.",
              AccessType.FREE,
              null,
              "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=800&q=80",
              "public/social-kit-free.pdf"),
          new ContentItem(
              "2",
              "Videoaula premium de anúncios",
              "Guia completo para anunciar com Pixel e conversões no portal.",
              AccessType.PREMIUM,
              "plan_premium_ads",
              "https://images.unsplash.com/photo-1483478550801-ceba5fe50e8e?auto=format&fit=crop&w=800&q=80",
              "private/premium-ads-class.mp4"),
          new ContentItem(
              "3",
              "Mentoria com especialistas",
              "Sessão guiada para montar a vitrine e integrar pagamentos.",
              AccessType.PREMIUM,
              "plan_consulting",
              "https://images.unsplash.com/photo-1523952578875-0f4af51e267b?auto=format&fit=crop&w=800&q=80",
              "private/consulting-room"));

  public List<ContentCardResponse> list(Role role) {
    return contents.stream().map(content -> toCardResponse(content, role)).toList();
  }

  public ContentDetailResponse findById(String id, Role role) {
    ContentItem content =
        contents.stream()
            .filter(item -> item.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conteúdo não encontrado"));

    boolean locked = isLocked(content, role);
    if (locked) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conteúdo premium bloqueado para a role");
    }

    return new ContentDetailResponse(
        content.id(),
        content.title(),
        content.description(),
        content.accessType(),
        false,
        buildSignedUrl(content.fileStoragePath()),
        content.coverImageUrl(),
        content.planId());
  }

  public CheckoutResponse startCheckout(CheckoutRequest request) {
    if (request == null
        || request.planId() == null
        || request.planId().isBlank()
        || request.email() == null
        || request.email().isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Informe o e-mail do lead e o plano para iniciar o checkout");
    }

    Optional<ContentItem> planContent =
        contents.stream().filter(content -> request.planId().equals(content.planId())).findFirst();
    if (planContent.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano não encontrado para checkout");
    }

    String encodedEmail = urlEncode(request.email());
    String paymentUrl =
        String.format(
            "https://pagamentos.exemplo/checkout?planId=%s&email=%s",
            request.planId(), encodedEmail);

    return new CheckoutResponse("PENDING", paymentUrl, request.planId(), request.email());
  }

  private ContentCardResponse toCardResponse(ContentItem content, Role role) {
    boolean locked = isLocked(content, role);
    return new ContentCardResponse(
        content.id(),
        content.title(),
        content.description(),
        content.accessType(),
        locked,
        content.coverImageUrl(),
        content.planId());
  }

  private boolean isLocked(ContentItem content, Role role) {
    return content.accessType() == AccessType.PREMIUM && !canOpenPremium(role);
  }

  private boolean canOpenPremium(Role role) {
    return role == Role.CLIENTE || role == Role.ADMIN;
  }

  private String buildSignedUrl(String storagePath) {
    String encodedPath = urlEncode(storagePath);
    return "https://storage.exemplo/secure/" + encodedPath + "?signature=mocked";
  }

  private String urlEncode(String value) {
    return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}

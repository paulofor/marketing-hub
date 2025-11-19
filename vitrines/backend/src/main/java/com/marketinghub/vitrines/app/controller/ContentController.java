package com.marketinghub.vitrines.app.controller;

import com.marketinghub.vitrines.app.model.CheckoutRequest;
import com.marketinghub.vitrines.app.model.CheckoutResponse;
import com.marketinghub.vitrines.app.model.ContentCardResponse;
import com.marketinghub.vitrines.app.model.ContentDetailResponse;
import com.marketinghub.vitrines.app.model.Role;
import com.marketinghub.vitrines.app.service.ContentService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ContentController {

  private final ContentService contentService;

  public ContentController(ContentService contentService) {
    this.contentService = contentService;
  }

  @GetMapping("/conteudos")
  public List<ContentCardResponse> list(@RequestParam(name = "role", defaultValue = "ANON") String role) {
    return contentService.list(Role.from(role));
  }

  @GetMapping("/conteudos/{id}")
  public ContentDetailResponse findById(
      @PathVariable String id, @RequestParam(name = "role", defaultValue = "ANON") String role) {
    return contentService.findById(id, Role.from(role));
  }

  @PostMapping("/checkout")
  public CheckoutResponse startCheckout(@RequestBody CheckoutRequest request) {
    return contentService.startCheckout(request);
  }
}

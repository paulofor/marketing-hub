package com.marketinghub.vitrines.app.controller;

import com.marketinghub.vitrines.app.model.MagicLinkRequest;
import com.marketinghub.vitrines.app.model.MagicLinkResponse;
import com.marketinghub.vitrines.app.service.MagicLinkService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class MagicLinkController {

  private final MagicLinkService magicLinkService;

  public MagicLinkController(MagicLinkService magicLinkService) {
    this.magicLinkService = magicLinkService;
  }

  @PostMapping("/magic-link")
  public MagicLinkResponse create(@RequestBody(required = false) MagicLinkRequest request) {
    return magicLinkService.generate(request);
  }
}

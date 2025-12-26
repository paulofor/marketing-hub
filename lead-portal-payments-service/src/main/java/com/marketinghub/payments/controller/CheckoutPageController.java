package com.marketinghub.payments.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CheckoutPageController {

    @GetMapping({"/checkout", "/checkout/"})
    public String serveCheckoutPage() {
        return "forward:/checkout/index.html";
    }
}

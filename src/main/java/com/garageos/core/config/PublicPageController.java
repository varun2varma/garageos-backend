package com.garageos.core.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the extension-less "clean" URL for public static pages that must
 * be reachable without an app-store-facing ".html" suffix (e.g. Google Play
 * Console's Data safety "Delete account URL" field, which is
 * https://garagest-backend.render.com/delete-account). The actual page
 * content lives at src/main/resources/static/delete-account.html; this just
 * redirects the clean path to it, so the static resource itself remains the
 * single source of truth for the page content.
 */
@Controller
public class PublicPageController {

    @GetMapping("/delete-account")
    public String deleteAccount() {
        return "redirect:/delete-account.html";
    }
}

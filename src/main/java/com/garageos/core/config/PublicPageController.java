package com.garageos.core.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the extension-less "clean" URL for public static pages that must
 * be reachable without an app-store-facing ".html" suffix (e.g. Google Play
 * Console's Data safety "Delete account URL" field, which is
 * https://garagest-backend.onrender.com/delete-account, and its "Privacy
 * policy URL" field, https://garagest-backend.onrender.com/privacy-policy).
 * The actual page content lives in src/main/resources/static/*.html; these
 * just redirect the clean path to the matching file, so the static resource
 * itself remains the single source of truth for the page content.
 */
@Controller
public class PublicPageController {

    @GetMapping("/delete-account")
    public String deleteAccount() {
        return "redirect:/delete-account.html";
    }

    @GetMapping("/privacy-policy")
    public String privacyPolicy() {
        return "redirect:/privacy-policy.html";
    }
}

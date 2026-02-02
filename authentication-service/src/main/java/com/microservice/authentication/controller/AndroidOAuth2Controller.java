package com.microservice.authentication.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class AndroidOAuth2Controller {

    @GetMapping("/android/oauth2/callback")
    public String androidOAuth2Callback(HttpServletRequest request, Model model) {
        // After successful OAuth2 authentication with Google,
        // Spring Security redirects here with authenticated session
        // Android app will intercept this URL via intent-filter
        // and handle the authentication

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            log.info("Android OAuth2 callback - User authenticated: {}", auth.getName());
            model.addAttribute("username", auth.getName());
            model.addAttribute("sessionId", request.getSession().getId());
        }

        return "oauth2-android-success";
    }
}

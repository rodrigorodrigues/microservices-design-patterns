package com.microservice.authentication.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AndroidOAuth2Controller {

    @GetMapping("/android/oauth2/callback")
    public String androidOAuth2Callback() {
        // After successful OAuth2 authentication with Google,
        // Spring Security redirects here with authenticated session
        // Android app will intercept this URL via intent-filter
        // and handle the authentication
        return "oauth2-android-success";
    }
}

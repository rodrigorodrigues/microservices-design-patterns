package com.microservice.authentication.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
public class AndroidOAuth2Controller {

    @GetMapping(value = "/android/oauth2/callback", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String androidOAuth2Callback(HttpServletRequest request) {
        // After successful OAuth2 authentication with Google,
        // Spring Security redirects here with authenticated session
        // Android app will intercept this URL via intent-filter
        // and handle the authentication

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = "Unknown";
        String sessionId = request.getSession().getId();

        if (auth != null && auth.isAuthenticated()) {
            username = auth.getName();
            log.info("Android OAuth2 callback - User authenticated: {}", username);
        }

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "    <meta http-equiv=\"refresh\" content=\"0;url=spendingbetter://oauth2callback\">" +
                "    <title>Authentication Successful</title>" +
                "    <style>" +
                "        body {" +
                "            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;" +
                "            display: flex;" +
                "            justify-content: center;" +
                "            align-items: center;" +
                "            min-height: 100vh;" +
                "            margin: 0;" +
                "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);" +
                "            color: white;" +
                "        }" +
                "        .container { text-align: center; padding: 2rem; }" +
                "        .success-icon { font-size: 4rem; margin-bottom: 1rem; }" +
                "        h1 { margin: 0 0 1rem 0; font-size: 2rem; }" +
                "        p { margin: 0; font-size: 1.1rem; opacity: 0.9; }" +
                "    </style>" +
                "    <script>" +
                "        // Immediately redirect to the app using custom scheme" +
                "        window.location.href = 'spendingbetter://oauth2callback';" +
                "    </script>" +
                "</head>" +
                "<body>" +
                "    <div class=\"container\">" +
                "        <div class=\"success-icon\">✓</div>" +
                "        <h1>Authentication Successful</h1>" +
                "        <p>Redirecting to app...</p>" +
                "        <p style=\"margin-top: 1rem; opacity: 0.7; font-size: 0.9rem;\">Welcome, " + username + "</p>" +
                "        <p style=\"margin-top: 2rem; font-size: 0.9rem;\">If you're not redirected, <a href=\"spendingbetter://oauth2callback\" style=\"color: white; text-decoration: underline;\">click here</a></p>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }
}

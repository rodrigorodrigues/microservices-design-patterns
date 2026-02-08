package com.microservice.authentication.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import com.microservice.authentication.service.GenerateToken;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class AndroidOAuth2Controller {
    private final AuthenticationCommonRepository authenticationCommonRepository;
    private final GenerateToken generateToken;

    public AndroidOAuth2Controller(AuthenticationCommonRepository authenticationCommonRepository, GenerateToken generateToken) {
        this.authenticationCommonRepository = authenticationCommonRepository;
        this.generateToken = generateToken;
    }

    @GetMapping(value = "/api/android/oauth2/callback")
    public void androidOAuth2Callback(Authentication authentication, HttpServletResponse response) throws IOException {
        // After successful OAuth2 authentication with Google,
        // Generate JWT token and pass it via custom scheme URL

        String username = "Unknown";
        String email = authentication.getName();
        Optional<com.microservice.authentication.common.model.Authentication> findById = authenticationCommonRepository.findByEmail(email);

        if (findById.isPresent()) {
            username = findById.get().getFullName();
            log.info("Android OAuth2 callback - User authenticated: {}", username);
        }

        // Generate JWT token
        OAuth2AccessToken accessToken = generateToken.generateToken(authentication);
        String jwtToken = accessToken.getTokenValue();

        // URL encode the token and username for safe passing via URL
        String encodedToken = URLEncoder.encode(jwtToken, StandardCharsets.UTF_8);
        String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);

        // Build custom scheme URL with token as query parameter
        String redirectUrl = "spendingbetter://oauth2callback?token=" + encodedToken +
                            "&username=" + encodedUsername +
                            "&email=" + encodedEmail;

        // Return HTML with multiple redirect methods
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);

        String html = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
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
                "        .button {" +
                "            display: inline-block;" +
                "            margin-top: 2rem;" +
                "            padding: 1rem 2rem;" +
                "            background: rgba(255,255,255,0.2);" +
                "            color: white;" +
                "            text-decoration: none;" +
                "            border-radius: 8px;" +
                "            font-size: 1.1rem;" +
                "        }" +
                "    </style>" +
                "    <script>" +
                "        // Try to redirect after a short delay to ensure page loads" +
                "        setTimeout(function() {" +
                "            window.location.replace('" + redirectUrl + "');" +
                "        }, 100);" +
                "    </script>" +
                "</head>" +
                "<body>" +
                "    <div class=\"container\">" +
                "        <div class=\"success-icon\">✓</div>" +
                "        <h1>Authentication Successful</h1>" +
                "        <p>Welcome, " + username + "!</p>" +
                "        <a href=\"" + redirectUrl + "\" class=\"button\">Return to App</a>" +
                "        <p style=\"margin-top: 2rem; font-size: 0.9rem; opacity: 0.7;\">Please click the button above to return to the app</p>" +
                "    </div>" +
                "</body>" +
                "</html>";

        response.getWriter().write(html);
        response.getWriter().flush();
    }
}

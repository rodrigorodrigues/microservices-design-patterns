# How to Capture Session ID using WebTestClient in WebFlux

## Summary

In Spring WebFlux with Spring Session (reactive), session cookies are **only created when the session is modified**. Simply making a request doesn't automatically create a session cookie.

## Configuration Required

### 1. WebSessionIdResolver Bean (EdgeServerApplication.java)
```java
@Bean
public WebSessionIdResolver webSessionIdResolver() {
    CookieWebSessionIdResolver resolver = new CookieWebSessionIdResolver();
    resolver.setCookieName("SESSIONID");
    resolver.addCookieInitializer(cookie -> {
        cookie.path("/");
        cookie.httpOnly(true);
        cookie.sameSite("Lax");
    });
    return resolver;
}
```

### 2. SecurityContextRepository Configuration (EdgeServerWebSecurityConfiguration.java)
```java
@Bean
public SecurityWebFilterChain configure(ServerHttpSecurity http) {
    return http
        .securityContextRepository(new WebSessionServerSecurityContextRepository())
        // ... other config
        .build();
}
```

### 3. Redis Configuration (RedisConfiguration.java)
```java
@Configuration
@EnableRedisWebSession
public class RedisConfiguration {
    // Spring Boot auto-configuration handles the rest
}
```

## How to Capture Session ID in Tests

### Method 1: Using Authenticated Endpoint (Recommended)
```java
@Test
public void testSessionCapture() {
    // Request to an endpoint that requires authentication
    // Spring Security saves SecurityContext to session, triggering cookie creation
    EntityExchangeResult<byte[]> result = webClient
        .mutateWith(mockUser().roles("ADMIN"))
        .get().uri("/actuator") // Protected endpoint
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .returnResult();

    // Extract session cookie
    ResponseCookie sessionCookie = result.getResponseCookies().getFirst("SESSIONID");

    if (sessionCookie != null) {
        String sessionId = sessionCookie.getValue();
        System.out.println("Session ID: " + sessionId);

        // Reuse session ID in next request
        webClient
            .get().uri("/actuator/info")
            .cookie("SESSIONID", sessionId)
            .exchange()
            .expectStatus().isOk();
    }
}
```

### Method 2: Using Session-Modifying Endpoint
```java
@Test
public void testSessionWithLogin() {
    // Hit an endpoint that performs authentication
    // OAuth2 login, form login, etc. will write to session
    EntityExchangeResult<byte[]> result = webClient
        .post().uri("/api/authenticate")
        .bodyValue(loginRequest)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .returnResult();

    // Session cookie is now set
    String sessionId = result.getResponseCookies()
        .getFirst("SESSIONID")
        .getValue();
}
```

### Method 3: Manually Create Session (Advanced)
```java
@Autowired
ReactiveSessionRepository<?> sessionRepository;

@Test
@SuppressWarnings({"unchecked", "rawtypes"})
public void testManualSession() {
    //Create and save session manually
    ReactiveSessionRepository rawRepo = sessionRepository;
    Session session = (Session) rawRepo.createSession().block();
    session.setAttribute("key", "value");
    rawRepo.save(session).block();

    String sessionId = session.getId();

    // Use in request
    webClient
        .get().uri("/api/test")
        .cookie("SESSIONID", sessionId)
        .exchange()
        .expectStatus().isOk();
}
```

## Important Notes

1. **Session cookies are NOT created automatically** - Something must write to the session
2. **mock User() may not trigger session creation** if the endpoint is whitelisted
3. **Authenticated endpoints** that require roles/permissions will save SecurityContext to session
4. **OAuth2/Form login** automatically creates sessions
5. **WebTestClient with springSecurity()** configurer is required for `mockUser()` to work properly

## Common Issues

### No Session Cookie Created
**Cause**: Endpoint doesn't require authentication or session isn't modified

**Solution**:
- Use an endpoint that requires authentication (`@PreAuthorize`, `.hasRole()`)
- Configure `WebSessionServerSecurityContextRepository`
- Explicitly write to session in your code

### ClassCastException with ReactiveSessionRepository
**Cause**: Trying to cast to `MapSession` when using Redis (it's `RedisSession`)

**Solution**: Use raw types or `Session` interface:
```java
ReactiveSessionRepository rawRepo = sessionRepository;
Session session = (Session) rawRepo.createSession().block();
```

### Session Cookie Not Persisted
**Cause**: `save-mode` configuration

**Solution**: Add to application.yml:
```yaml
spring:
  session:
    redis:
      save-mode: always
      flush-mode: on_save
```

## Example: Full Test with Session Reuse

```java
@Test
public void testFullSessionFlow() {
    // 1. Authenticate and get session
    EntityExchangeResult<byte[]> loginResult = webClient
        .mutateWith(mockUser().username("admin").roles("ADMIN"))
        .get().uri("/actuator/health") // Authenticated endpoint
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .returnResult();

    // 2. Extract session ID
    ResponseCookie sessionCookie = loginResult.getResponseCookies().getFirst("SESSIONID");
    assertThat(sessionCookie).isNotNull();
    String sessionId = sessionCookie.getValue();
    System.out.println("Captured Session ID: " + sessionId);

    // 3. Reuse session in subsequent requests
    webClient
        .get().uri("/actuator/info")
        .cookie("SESSIONID", sessionId)
        .exchange()
        .expectStatus().isOk();

    // 4. Verify session contains user info
    webClient
        .get().uri("/api/current-user")
        .cookie("SESSIONID", sessionId)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.username").isEqualTo("admin");
}
```

## Key Takeaway

**To capture a session ID in WebFlux/Spring Session**:
1. Configure `WebSessionIdResolver` with cookie settings
2. Configure `WebSessionServerSecurityContextRepository` for security
3. Hit an **authenticated endpoint** or explicitly write to session
4. Extract cookie using `getResponseCookies().getFirst("SESSIONID").getValue()`
5. Reuse with `.cookie("SESSIONID", sessionId)`

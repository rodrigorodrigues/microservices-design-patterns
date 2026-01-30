using Microsoft.IdentityModel.Tokens;
using System.Text.Json;

namespace DotnetService.Services;

public class JwksService : IJwksService
{
    private readonly HttpClient _httpClient;
    private readonly ILogger<JwksService> _logger;

    public JwksService(ILogger<JwksService> logger)
    {
        _httpClient = new HttpClient();
        _logger = logger;
    }

    public async Task<SecurityKey?> GetSigningKeyAsync(string jwksUrl, string? kid)
    {
        try
        {
            var response = await _httpClient.GetAsync(jwksUrl);
            response.EnsureSuccessStatusCode();

            var json = await response.Content.ReadAsStringAsync();
            var jwks = JsonSerializer.Deserialize<JsonWebKeySet>(json);

            if (jwks?.Keys == null || !jwks.Keys.Any())
            {
                _logger.LogWarning("No keys found in JWKS");
                return null;
            }

            var key = string.IsNullOrEmpty(kid)
                ? jwks.Keys.FirstOrDefault()
                : jwks.Keys.FirstOrDefault(k => k.Kid == kid);

            return key;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to fetch JWKS from {JwksUrl}", jwksUrl);
            return null;
        }
    }
}

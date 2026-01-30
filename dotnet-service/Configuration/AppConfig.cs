using Microsoft.Extensions.Logging;

namespace DotnetService.Configuration;

public class AppConfig
{
    private readonly ILogger<AppConfig>? _logger;

    public int ServerPort { get; set; }
    public string MongoDbUri { get; set; } = string.Empty;
    public string AppName { get; set; } = "dotnet-service";
    public string SecretToken { get; set; } = string.Empty;
    public bool ConsulEnabled { get; set; }
    public string ConsulServer { get; set; } = "127.0.0.1";
    public int ConsulPort { get; set; } = 8500;
    public string HostName { get; set; } = "localhost";
    public string ConsulKvPath { get; set; } = "config/dotnet-service";
    public string Profile { get; set; } = "localhost";
    public bool JwksValidation { get; set; }
    public string JwksUrl { get; set; } = "http://localhost:8080/.well-known/jwks.json";
    public string Algorithm { get; set; } = "HS256";

    public AppConfig(IConfiguration configuration, ILogger<AppConfig>? logger = null)
    {
        _logger = logger;

        ServerPort = int.TryParse(configuration["SERVER_PORT"] ?? "8080", out var port) ? port : 8080;
        MongoDbUri = configuration["MONGODB_URI"] ?? throw new InvalidOperationException("MONGODB_URI must be set");
        AppName = configuration["APP_NAME"] ?? "dotnet-service";
        SecretToken = configuration["SECRET_TOKEN"] ?? string.Empty;
        ConsulEnabled = bool.TryParse(configuration["CONSUL_ENABLED"] ?? "true", out var enabled) && enabled;
        ConsulServer = configuration["CONSUL_SERVER"] ?? "127.0.0.1";
        ConsulPort = int.TryParse(configuration["CONSUL_PORT"] ?? "8500", out var consulPort) ? consulPort : 8500;
        HostName = configuration["HOST_NAME"] ?? "localhost";
        Profile = configuration["SPRING_PROFILES_ACTIVE"] ?? "dev";
        ConsulKvPath = configuration["CONSUL_KV_PATH"] ?? "config/dotnet-service";
        JwksValidation = bool.TryParse(configuration["JWKS_VALIDATION"] ?? "false", out var jwks) && jwks;
        JwksUrl = configuration["JWKS_URL"] ?? "http://localhost:8080/.well-known/jwks.json";
        Algorithm = configuration["JWKS_ALGORITHM"] ?? "HS256";
    }

    public void ApplyConsulYamlConfig(ConsulYamlConfig yamlConfig)
    {
        var jwt = yamlConfig?.Com?.Microservice?.Authentication?.Jwt;

        if (jwt != null)
        {
            // Extract secret token from com.microservice.authentication.jwt.keyValue
            if (!string.IsNullOrEmpty(jwt.KeyValue))
            {
                SecretToken = jwt.KeyValue;
                _logger?.LogInformation("Successfully loaded SECRET_TOKEN from Consul YAML: {}", SecretToken);
            }

            // Extract JWKS URL from com.microservice.authentication.jwt.jwksUri
            if (!string.IsNullOrEmpty(jwt.JwksUri))
            {
                JwksUrl = jwt.JwksUri;
                JwksValidation = true; // Enable JWKS validation if jwksUri is provided
                _logger?.LogInformation("Loaded JWKS URL from Consul: {JwksUrl}", JwksUrl);
            }

            // Extract issuer URI
            if (!string.IsNullOrEmpty(jwt.IssuerUri))
            {
                _logger?.LogInformation("Loaded Issuer URI from Consul: {IssuerUri}", jwt.IssuerUri);
            }
        }
    }
}

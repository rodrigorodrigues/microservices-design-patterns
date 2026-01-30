using YamlDotNet.Serialization;
using System.Collections.Generic;

namespace DotnetService.Configuration;

public class ConsulYamlConfig
{
    [YamlMember(Alias = "spring")]
    public Dictionary<string, object>? Spring { get; set; }

    [YamlMember(Alias = "com")]
    public ComConfig? Com { get; set; }
}

public class ComConfig
{
    [YamlMember(Alias = "microservice")]
    public MicroserviceConfig? Microservice { get; set; }
}

public class MicroserviceConfig
{
    [YamlMember(Alias = "authentication")]
    public AuthenticationConfig? Authentication { get; set; }
}

public class AuthenticationConfig
{
    [YamlMember(Alias = "jwt")]
    public JwtConfig? Jwt { get; set; }
}

public class JwtConfig
{
    public string? KeyValue { get; set; }
    public string? IssuerUri { get; set; }
    public string? JwksUri { get; set; }
}

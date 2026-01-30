using DotnetService.Configuration;

namespace DotnetService.Services;

public interface IConsulService
{
    Task RegisterServiceAsync(AppConfig config);
    Task<string?> ReadKvAsync(string key);
    Task<ConsulYamlConfig?> ReadYamlConfigAsync(string key);
}

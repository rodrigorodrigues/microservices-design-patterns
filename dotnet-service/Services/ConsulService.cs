using Consul;
using DotnetService.Configuration;
using System.Text;
using YamlDotNet.Serialization;
using YamlDotNet.Serialization.NamingConventions;

namespace DotnetService.Services;

public class ConsulService : IConsulService
{
    private readonly ILogger<ConsulService> _logger;
    private readonly IDeserializer _yamlDeserializer;

    public ConsulService(ILogger<ConsulService> logger)
    {
        _logger = logger;
        _yamlDeserializer = new DeserializerBuilder()
            .WithNamingConvention(CamelCaseNamingConvention.Instance)
            .IgnoreUnmatchedProperties()
            .Build();
    }

    public async Task RegisterServiceAsync(AppConfig config)
    {
        try
        {
            using var client = new ConsulClient(c =>
            {
                c.Address = new Uri($"http://{config.ConsulServer}:{config.ConsulPort}");
            });

            var serviceId = $"{config.AppName}-{config.ServerPort}";
            var registration = new AgentServiceRegistration
            {
                ID = serviceId,
                Name = config.AppName.ToUpper(),
                Address = config.HostName,
                Port = config.ServerPort,
                Check = new AgentServiceCheck
                {
                    HTTP = $"http://{config.HostName}:{config.ServerPort}/actuator/health",
                    Interval = TimeSpan.FromSeconds(10),
                    Timeout = TimeSpan.FromSeconds(5),
                    DeregisterCriticalServiceAfter = TimeSpan.FromMinutes(1)
                }
            };

            await client.Agent.ServiceRegister(registration);
            _logger.LogInformation("Successfully registered with Consul");
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Failed to register with Consul");
        }
    }

    public async Task<string?> ReadKvAsync(string key)
    {
        try
        {
            using var client = new ConsulClient();
            var kvPair = await client.KV.Get(key);

            if (kvPair?.Response == null)
            {
                return null;
            }

            return Encoding.UTF8.GetString(kvPair.Response.Value);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Failed to read key {Key} from Consul", key);
            return null;
        }
    }

    public async Task<ConsulYamlConfig?> ReadYamlConfigAsync(string key)
    {
        try
        {
            var yamlContent = await ReadKvAsync(key);
            if (string.IsNullOrEmpty(yamlContent))
            {
                _logger.LogWarning("No YAML content found for key {Key}", key);
                return null;
            }

            _logger.LogDebug("Reading YAML config from Consul key: {Key}", key);
            var config = _yamlDeserializer.Deserialize<ConsulYamlConfig>(yamlContent);
            _logger.LogInformation("Successfully loaded YAML configuration from Consul");

            return config;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to parse YAML config from Consul key {Key}", key);
            return null;
        }
    }
}

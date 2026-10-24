<?php

namespace App\Consul;

use Illuminate\Contracts\Cache\Repository as Cache;
use Illuminate\Http\Client\Factory as Http;
use Illuminate\Support\Arr;
use Symfony\Component\Yaml\Yaml;

/**
 * Registers this service in Consul and reads the shared Spring-style
 * configuration stored under config/application,<profile>/data.
 */
class ConsulClient
{
    public function __construct(
        private readonly Http $http,
        private readonly Cache $cache,
        private readonly string $url,
        private readonly string $profile,
        private readonly int $configCacheTtl = 300,
    ) {}

    /**
     * Returns a property from the profile's configuration, accepting both
     * camelCase and kebab-case names (keyValue / key-value).
     */
    public function property(string $prefix, string $camelCaseName): ?string
    {
        $configuration = $this->configuration();
        $kebabCaseName = strtolower(preg_replace('/(?<!^)[A-Z]/', '-$0', $camelCaseName));

        foreach ([$camelCaseName, $kebabCaseName] as $name) {
            $value = Arr::get($configuration, "$prefix.$name");
            if (is_scalar($value) && (string) $value !== '') {
                return (string) $value;
            }
        }

        return null;
    }

    /**
     * @return array<string, mixed>
     */
    public function configuration(): array
    {
        $key = "config/application,{$this->profile}/data";

        return $this->cache->remember('consul:'.$key, $this->configCacheTtl, function () use ($key) {
            $response = $this->http->timeout(5)->get($this->baseUrl()."/v1/kv/$key", ['raw' => 'true']);

            if ($response->notFound()) {
                return [];
            }

            return Yaml::parse($response->throw()->body()) ?? [];
        });
    }

    public function register(string $name, string $host, int $port): void
    {
        $this->http->timeout(5)->put($this->baseUrl().'/v1/agent/service/register', [
            'ID' => $this->serviceId($name, $host, $port),
            'Name' => $name,
            'Address' => $host,
            'Port' => $port,
            'Tags' => ['php', 'laravel'],
            'Check' => [
                'HTTP' => "http://$host:$port/actuator/health",
                'Interval' => '10s',
                'Timeout' => '5s',
                'DeregisterCriticalServiceAfter' => '1m',
            ],
        ])->throw();
    }

    public function deregister(string $name, string $host, int $port): void
    {
        $this->http->timeout(5)
            ->put($this->baseUrl().'/v1/agent/service/deregister/'.$this->serviceId($name, $host, $port))
            ->throw();
    }

    public function serviceId(string $name, string $host, int $port): string
    {
        return "$name-$host-$port";
    }

    private function baseUrl(): string
    {
        $url = rtrim($this->url, '/');

        return str_contains($url, '://') ? $url : "http://$url";
    }
}

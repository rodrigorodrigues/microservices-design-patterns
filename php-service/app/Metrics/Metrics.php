<?php

namespace App\Metrics;

use Prometheus\CollectorRegistry;
use Prometheus\RenderTextFormat;
use Prometheus\Storage\Adapter;
use Prometheus\Storage\APCng;
use Prometheus\Storage\InMemory;

/**
 * Prometheus metrics, named like Spring Boot's so dashboards work across services.
 * Values are kept in APCu so they survive between requests; without APCu they
 * only last for one request.
 */
class Metrics
{
    private const BUCKETS = [0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10];

    private readonly CollectorRegistry $registry;

    public function __construct(?Adapter $storage = null)
    {
        $this->registry = new CollectorRegistry($storage ?? self::defaultStorage(), false);
    }

    public function recordRequest(string $method, string $uri, int $status, float $seconds): void
    {
        $this->registry
            ->getOrRegisterHistogram('', 'http_server_requests_seconds', 'HTTP server request duration',
                ['method', 'uri', 'status'], self::BUCKETS)
            ->observe($seconds, [$method, $uri, (string) $status]);
    }

    public function render(): string
    {
        $this->registry
            ->getOrRegisterGauge('', 'php_info', 'PHP and Laravel versions', ['version', 'laravel'])
            ->set(1, [PHP_VERSION, app()->version()]);

        return (new RenderTextFormat)->render($this->registry->getMetricFamilySamples());
    }

    private static function defaultStorage(): Adapter
    {
        return function_exists('apcu_enabled') && apcu_enabled() ? new APCng : new InMemory;
    }
}

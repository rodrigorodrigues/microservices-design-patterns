<?php

namespace App\Console\Commands;

use App\Consul\ConsulClient;
use Illuminate\Console\Command;
use Throwable;

class DeregisterService extends Command
{
    protected $signature = 'service:deregister';

    protected $description = 'Remove this service from Consul';

    public function handle(ConsulClient $consul): int
    {
        if (! config('microservice.consul.enabled')) {
            return self::SUCCESS;
        }

        $service = config('microservice.service');

        try {
            $consul->deregister($service['name'], $service['host'], $service['port']);
            $this->info('Deregistered from Consul');
        } catch (Throwable $e) {
            // Consul removes the service itself once its health check stays critical.
            $this->warn("Could not deregister from Consul: {$e->getMessage()}");
        }

        return self::SUCCESS;
    }
}

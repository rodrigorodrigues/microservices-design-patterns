<?php

namespace App\Console\Commands;

use App\Auth\JwtConfigurationException;
use App\Auth\JwtVerifier;
use App\Books\BookRepository;
use App\Consul\ConsulClient;
use Illuminate\Console\Command;
use Throwable;

/**
 * Run once before the HTTP server starts (see docker-entrypoint.sh):
 * refuses to start without JWT verification, creates MongoDB indexes and
 * registers the service in Consul.
 */
class PrepareService extends Command
{
    protected $signature = 'service:prepare
        {--retries=10 : Attempts for MongoDB and Consul}
        {--delay=3 : Seconds between attempts}';

    protected $description = 'Check JWT configuration, create MongoDB indexes and register in Consul';

    public function handle(BookRepository $books): int
    {
        try {
            $verifier = $this->laravel->make(JwtVerifier::class);
            $this->info("JWT verification: {$verifier->mode()}");
        } catch (JwtConfigurationException $e) {
            $this->error('Refusing to start: JWT verification is not configured. '.$e->getMessage());

            return self::FAILURE;
        }

        if (! $this->attempt('Create MongoDB indexes', fn () => $books->ensureIndexes())) {
            return self::FAILURE;
        }

        if (config('microservice.consul.enabled')) {
            $service = config('microservice.service');
            $registered = $this->attempt('Register in Consul', fn () => $this->laravel->make(ConsulClient::class)
                ->register($service['name'], $service['host'], $service['port']));

            if (! $registered) {
                return self::FAILURE;
            }
        }

        return self::SUCCESS;
    }

    private function attempt(string $step, callable $action): bool
    {
        $retries = max(1, (int) $this->option('retries'));

        for ($attempt = 1; $attempt <= $retries; $attempt++) {
            try {
                $action();
                $this->info("$step: done");

                return true;
            } catch (Throwable $e) {
                $this->warn("$step: attempt $attempt/$retries failed: {$e->getMessage()}");
                if ($attempt < $retries) {
                    sleep((int) $this->option('delay'));
                }
            }
        }

        $this->error("$step: giving up");

        return false;
    }
}

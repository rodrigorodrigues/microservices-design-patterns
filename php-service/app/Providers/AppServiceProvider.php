<?php

namespace App\Providers;

use App\Auth\JwtVerifier;
use App\Books\BookRepository;
use App\Books\MongoBookRepository;
use App\Consul\ConsulClient;
use App\Metrics\Metrics;
use Illuminate\Contracts\Foundation\Application;
use Illuminate\Http\Client\Factory as Http;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\ServiceProvider;
use Throwable;

class AppServiceProvider extends ServiceProvider
{
    public function register(): void
    {
        $this->app->singleton(BookRepository::class, MongoBookRepository::class);
        $this->app->singleton(Metrics::class);

        $this->app->singleton(ConsulClient::class, fn (Application $app) => new ConsulClient(
            $app->make(Http::class),
            $app->make('cache.store'),
            config('microservice.consul.url'),
            config('microservice.consul.profile'),
            config('microservice.consul.config_cache_ttl'),
        ));

        $this->app->singleton(JwtVerifier::class, function (Application $app) {
            $settings = config('microservice.authentication');
            [$jwksUri, $keyValue] = $this->verificationSettings($app, $settings);

            return new JwtVerifier(
                $jwksUri,
                $keyValue,
                $settings['jwt_allow_unverified'],
                $app->make(Http::class),
                $app->make('cache.store'),
                Log::channel(),
                $settings['jwks_cache_ttl'],
                $settings['leeway'],
            );
        });
    }

    /**
     * Environment variables win; Consul fills in whatever they leave empty.
     *
     * @param  array<string, mixed>  $settings
     * @return array{0: ?string, 1: ?string} JWKS URI and verification key value
     */
    private function verificationSettings(Application $app, array $settings): array
    {
        $jwksUri = $settings['jwk_key_set_uri'] ?: null;
        $keyValue = $settings['jwt_key_value'] ?: null;

        if ($jwksUri === null && $keyValue === null && config('microservice.consul.enabled')) {
            try {
                $consul = $app->make(ConsulClient::class);
                $jwksUri = $consul->property('com.microservice.authentication.jwk', 'keySetUri');
                $keyValue = $consul->property('com.microservice.authentication.jwt', 'keyValue');
            } catch (Throwable $e) {
                Log::error('Could not read JWT settings from Consul', ['exception' => $e->getMessage()]);
            }
        }

        return [$jwksUri, $keyValue];
    }
}

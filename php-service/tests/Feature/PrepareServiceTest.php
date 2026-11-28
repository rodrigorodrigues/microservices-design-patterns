<?php

namespace Tests\Feature;

use App\Auth\JwtVerifier;
use Illuminate\Http\Client\Request;
use Illuminate\Support\Facades\Http;
use Tests\Support\Tokens;
use Tests\TestCase;

class PrepareServiceTest extends TestCase
{
    public function test_refuses_to_start_without_jwt_verification(): void
    {
        $this->configureJwt(keyValue: '');

        $this->artisan('service:prepare', ['--retries' => 1])
            ->expectsOutputToContain('Refusing to start')
            ->assertFailed();
    }

    public function test_starts_with_a_key_value(): void
    {
        $this->artisan('service:prepare', ['--retries' => 1])
            ->expectsOutputToContain('JWT verification: key-value')
            ->assertSuccessful();
    }

    public function test_reads_the_key_value_from_consul_and_registers(): void
    {
        $this->configureJwt(keyValue: '');
        config([
            'microservice.consul.enabled' => true,
            'microservice.consul.url' => 'http://service-discovery:8500',
            'microservice.service.host' => 'book-api',
            'microservice.service.port' => 8088,
        ]);
        Http::fake([
            'service-discovery:8500/v1/kv/*' => Http::response("com:\n  microservice:\n    authentication:\n      jwt:\n        keyValue: ".Tokens::SECRET),
            'service-discovery:8500/v1/agent/*' => Http::response(),
        ]);

        $this->artisan('service:prepare', ['--retries' => 1])
            ->expectsOutputToContain('JWT verification: key-value')
            ->expectsOutputToContain('Register in Consul: done')
            ->assertSuccessful();

        Http::assertSent(fn (Request $request) => str_ends_with($request->url(), '/v1/agent/service/register')
            && $request['Check']['HTTP'] === 'http://book-api:8088/actuator/health');
    }

    public function test_fails_when_consul_cannot_be_reached(): void
    {
        config(['microservice.consul.enabled' => true, 'microservice.consul.url' => 'http://service-discovery:8500']);
        Http::fake(['service-discovery:8500/*' => Http::response('down', 500)]);

        $this->artisan('service:prepare', ['--retries' => 2, '--delay' => 0])
            ->expectsOutputToContain('Register in Consul: giving up')
            ->assertFailed();
    }

    private function configureJwt(string $keyValue): void
    {
        config(['microservice.authentication.jwt_key_value' => $keyValue]);
        $this->app->forgetInstance(JwtVerifier::class);
    }
}

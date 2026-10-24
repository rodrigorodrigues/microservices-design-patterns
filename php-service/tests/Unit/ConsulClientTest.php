<?php

namespace Tests\Unit;

use App\Consul\ConsulClient;
use Illuminate\Http\Client\Factory as Http;
use Illuminate\Http\Client\Request;
use Illuminate\Support\Facades\Http as HttpFacade;
use Tests\TestCase;

class ConsulClientTest extends TestCase
{
    private const CONFIG_URL = 'http://service-discovery:8500/v1/kv/config/application,dev/data*';

    public function test_reads_camel_case_property_from_the_profile_configuration(): void
    {
        HttpFacade::fake([self::CONFIG_URL => HttpFacade::response(<<<'YAML'
            com:
              microservice:
                authentication:
                  jwt:
                    keyValue: shared-secret
            YAML)]);

        $this->assertSame('shared-secret', $this->client()->property('com.microservice.authentication.jwt', 'keyValue'));
        HttpFacade::assertSent(fn (Request $request) => str_contains($request->url(), 'raw=true'));
    }

    public function test_reads_kebab_case_property(): void
    {
        HttpFacade::fake([self::CONFIG_URL => HttpFacade::response(<<<'YAML'
            com:
              microservice:
                authentication:
                  jwk:
                    key-set-uri: http://authentication-service:9999/.well-known/jwks.json
            YAML)]);

        $this->assertSame(
            'http://authentication-service:9999/.well-known/jwks.json',
            $this->client()->property('com.microservice.authentication.jwk', 'keySetUri'),
        );
    }

    public function test_missing_key_means_no_configuration(): void
    {
        HttpFacade::fake([self::CONFIG_URL => HttpFacade::response('', 404)]);

        $this->assertNull($this->client()->property('com.microservice.authentication.jwt', 'keyValue'));
    }

    public function test_registers_with_an_http_health_check(): void
    {
        HttpFacade::fake(['service-discovery:8500/*' => HttpFacade::response()]);

        $this->client()->register('php-service', 'book-api', 8088);

        HttpFacade::assertSent(fn (Request $request) => $request->method() === 'PUT'
            && $request->url() === 'http://service-discovery:8500/v1/agent/service/register'
            && $request['Name'] === 'php-service'
            && $request['ID'] === 'php-service-book-api-8088'
            && $request['Check']['HTTP'] === 'http://book-api:8088/actuator/health');
    }

    public function test_deregisters_by_service_id(): void
    {
        HttpFacade::fake(['service-discovery:8500/*' => HttpFacade::response()]);

        $this->client()->deregister('php-service', 'book-api', 8088);

        HttpFacade::assertSent(fn (Request $request) => $request->method() === 'PUT'
            && $request->url() === 'http://service-discovery:8500/v1/agent/service/deregister/php-service-book-api-8088');
    }

    private function client(): ConsulClient
    {
        // No scheme, the way CONSUL_URL is written for the Spring services in docker-compose.yml.
        return new ConsulClient($this->app->make(Http::class), $this->app->make('cache.store'), 'service-discovery:8500', 'dev');
    }
}

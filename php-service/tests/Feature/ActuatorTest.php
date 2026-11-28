<?php

namespace Tests\Feature;

use App\Health\MongoHealthIndicator;
use Tests\Support\Tokens;
use Tests\TestCase;

class ActuatorTest extends TestCase
{
    public function test_health_is_up_when_mongodb_answers(): void
    {
        $this->mongoIs(true);

        $this->getJson('/actuator/health')
            ->assertOk()
            ->assertExactJson(['status' => 'UP', 'components' => ['mongo' => ['status' => 'UP']]]);
    }

    public function test_health_is_down_when_mongodb_does_not_answer(): void
    {
        $this->mongoIs(false);

        $this->getJson('/actuator/health')
            ->assertStatus(503)
            ->assertJson(['status' => 'DOWN']);
    }

    public function test_info_describes_the_service(): void
    {
        $this->getJson('/actuator/info')
            ->assertOk()
            ->assertJsonPath('app.name', 'php-service')
            ->assertJsonPath('build.php', PHP_VERSION);
    }

    public function test_index_links_the_endpoints(): void
    {
        $this->getJson('/actuator')
            ->assertOk()
            ->assertJsonStructure(['_links' => ['self', 'health', 'info', 'prometheus']]);
    }

    public function test_prometheus_exposes_request_metrics(): void
    {
        $this->withToken(Tokens::hs256())->getJson('/api/books')->assertOk();

        $response = $this->get('/actuator/prometheus')->assertOk();

        $this->assertStringStartsWith('text/plain', $response->headers->get('Content-Type'));
        $this->assertStringContainsString(
            'http_server_requests_seconds_count{method="GET",uri="/api/books",status="200"} 1',
            $response->getContent(),
        );
    }

    public function test_actuator_does_not_need_a_token(): void
    {
        $this->mongoIs(true);

        $this->getJson('/actuator/health')->assertOk();
        $this->getJson('/actuator/info')->assertOk();
    }

    private function mongoIs(bool $up): void
    {
        $this->app->instance(MongoHealthIndicator::class, new class($up) extends MongoHealthIndicator
        {
            public function __construct(private readonly bool $up) {}

            public function isUp(): bool
            {
                return $this->up;
            }
        });
    }
}

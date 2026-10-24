<?php

namespace App\Http\Controllers;

use App\Health\MongoHealthIndicator;
use App\Metrics\Metrics;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Response;
use Prometheus\RenderTextFormat;

/**
 * Spring Boot style actuator endpoints, so Consul, Prometheus and
 * Spring Boot Admin treat this service like the others.
 */
class ActuatorController extends Controller
{
    public function index(): JsonResponse
    {
        $links = [];
        foreach (['self' => '', 'health' => '/health', 'info' => '/info', 'prometheus' => '/prometheus'] as $name => $path) {
            $links[$name] = ['href' => url('/actuator'.$path), 'templated' => false];
        }

        return response()->json(['_links' => $links]);
    }

    public function health(MongoHealthIndicator $mongo): JsonResponse
    {
        $mongoUp = $mongo->isUp();

        return response()->json([
            'status' => $mongoUp ? 'UP' : 'DOWN',
            'components' => ['mongo' => ['status' => $mongoUp ? 'UP' : 'DOWN']],
        ], $mongoUp ? 200 : 503);
    }

    public function info(): JsonResponse
    {
        return response()->json([
            'app' => [
                'name' => config('microservice.service.name'),
                'description' => 'Books API written in PHP with Laravel',
                'version' => config('microservice.service.version'),
            ],
            'build' => [
                'php' => PHP_VERSION,
                'laravel' => app()->version(),
            ],
        ]);
    }

    public function prometheus(Metrics $metrics): Response
    {
        return response($metrics->render(), 200, ['Content-Type' => RenderTextFormat::MIME_TYPE]);
    }
}

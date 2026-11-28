<?php

namespace App\Http\Middleware;

use App\Metrics\Metrics;
use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class RecordRequestMetrics
{
    public function __construct(private readonly Metrics $metrics) {}

    public function handle(Request $request, Closure $next): Response
    {
        $start = hrtime(true);

        $response = $next($request);

        // The route pattern (api/books/{id}) keeps the label set small.
        $uri = $request->route() ? '/'.ltrim($request->route()->uri(), '/') : 'UNKNOWN';
        $this->metrics->recordRequest($request->method(), $uri, $response->getStatusCode(), (hrtime(true) - $start) / 1e9);

        return $response;
    }
}

<?php

use App\Http\Middleware\AuthenticateJwt;
use App\Http\Middleware\RecordRequestMetrics;
use App\Http\Middleware\RequireAuthority;
use Illuminate\Foundation\Application;
use Illuminate\Foundation\Configuration\Exceptions;
use Illuminate\Foundation\Configuration\Middleware;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;

return Application::configure(basePath: dirname(__DIR__))
    ->withRouting(
        api: __DIR__.'/../routes/api.php',
        commands: __DIR__.'/../routes/console.php',
        then: function () {
            Route::group([], __DIR__.'/../routes/actuator.php');
        },
    )
    ->withMiddleware(function (Middleware $middleware): void {
        $middleware->append(RecordRequestMetrics::class);
        $middleware->alias([
            'jwt' => AuthenticateJwt::class,
            'authority' => RequireAuthority::class,
        ]);
    })
    ->withExceptions(function (Exceptions $exceptions): void {
        $exceptions->shouldRenderJsonWhen(fn (Request $request) => true);
    })->create();

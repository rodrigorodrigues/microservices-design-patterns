<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * Allows the request when the token's authorities claim contains at least one
 * of the given authorities. Usage: ->middleware('authority:ROLE_ADMIN,ROLE_BOOKS_READ').
 */
class RequireAuthority
{
    public function handle(Request $request, Closure $next, string ...$authorities): Response
    {
        $claims = $request->attributes->get('jwt.claims', []);
        $granted = $claims[config('microservice.authentication.authorities_claim')] ?? [];

        if (! is_array($granted) || array_intersect($authorities, $granted) === []) {
            return response()->json(
                ['error' => 'Forbidden', 'message' => 'Insufficient permissions'],
                Response::HTTP_FORBIDDEN,
            );
        }

        return $next($request);
    }
}

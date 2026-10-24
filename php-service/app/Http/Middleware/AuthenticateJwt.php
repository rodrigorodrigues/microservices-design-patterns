<?php

namespace App\Http\Middleware;

use App\Auth\InvalidTokenException;
use App\Auth\JwtVerifier;
use Closure;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;
use Symfony\Component\HttpFoundation\Response;

/**
 * Requires a valid "Authorization: Bearer <JWT>" header and stores the
 * token's claims on the request under "jwt.claims".
 */
class AuthenticateJwt
{
    public function __construct(private readonly JwtVerifier $verifier) {}

    public function handle(Request $request, Closure $next): Response
    {
        $token = $request->bearerToken();

        if ($token === null || $token === '') {
            return $this->unauthorized('Missing bearer token');
        }

        try {
            $request->attributes->set('jwt.claims', $this->verifier->verify($token));
        } catch (InvalidTokenException $e) {
            Log::debug('JWT validation failed', ['reason' => $e->getMessage()]);

            return $this->unauthorized('Invalid token');
        }

        return $next($request);
    }

    private function unauthorized(string $message): Response
    {
        return response()
            ->json(['error' => 'Unauthorized', 'message' => $message], Response::HTTP_UNAUTHORIZED)
            ->header('WWW-Authenticate', 'Bearer');
    }
}
